package com.ocbs.security;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

/**
 * Service quản lý danh sách đen Token (Token Blacklist) trên Redis theo chuẩn bảo mật Doanh nghiệp (Enterprise-Grade).
 * 
 * =========================================================================
 * CƠ CHẾ HOẠT ĐỘNG:
 * 1. Khi người dùng bấm Logout hoặc đổi mật khẩu:
 *    - Lấy mã định danh duy nhất của Token (claim 'jti' - JWT ID).
 *    - Tính toán thời gian sống còn lại (TTL) của Token = expiresAt - now.
 *    - Lưu key 'token:blacklist:{jti}' vào Redis với giá trị 'REVOKED' kèm đúng TTL còn lại.
 *    - Khi token tự nhiên hết hạn, Redis sẽ tự động dọn dẹp key nhờ TTL (không tốn dung lượng RAM vô ích).
 * 
 * 2. Khi người dùng đổi mật khẩu:
 *    - Ghi nhận thời điểm 'user:logout:{username}' = nowEpoch vào Redis.
 *    - Mọi token được cấp trước thời điểm này sẽ lập tức bị coi là mất hiệu lực.
 * 
 * 3. Fallback An toàn:
 *    - Nếu Redis tạm thời mất kết nối hoặc chạy standalone, service tự động fallback sang ConcurrentHashMap
 *      để đảm bảo hệ thống không bao giờ bị gián đoạn.
 * =========================================================================
 */
@Service
public class TokenBlacklistService {

    private static final Logger LOG = LoggerFactory.getLogger(TokenBlacklistService.class);
    private static final String BLACKLIST_PREFIX = "token:blacklist:";
    private static final String USER_LOGOUT_PREFIX = "user:logout:";

    private final StringRedisTemplate redisTemplate;

    @org.springframework.beans.factory.annotation.Value("${application.security.jwt.blacklist-fail-closed:false}")
    private boolean failClosed;

    // Circuit breaker state để tránh cố kết nối lại Redis liên tục khi Redis không hoạt động
    private volatile boolean redisAvailable = true;
    private volatile long lastRedisAttemptTime = 0L;
    private static final long REDIS_RETRY_INTERVAL_MS = 60_000L; // 60 giây thử lại một lần

    // Fallback cache in-memory nếu Redis tạm thời mất kết nối (chỉ hoạt động khi failClosed = false / dev mode)
    private final ConcurrentHashMap<String, Instant> memoryBlacklist = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> memoryUserRevocation = new ConcurrentHashMap<>();

    public TokenBlacklistService(@Autowired(required = false) StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        if (redisTemplate != null) {
            LOG.info(">>> [BẢO MẬT] TokenBlacklistService khởi tạo thành công với cụm Redis lưu trữ Blacklist.");
        } else {
            this.redisAvailable = false;
            LOG.warn(">>> [CẢNH BÁO] Không tìm thấy StringRedisTemplate. Chế độ Fail-Closed hoặc In-Memory Fallback sẽ áp dụng.");
        }
    }

    private boolean isRedisOperational() {
        if (redisTemplate == null) {
            return false;
        }
        if (!redisAvailable) {
            long now = System.currentTimeMillis();
            if (now - lastRedisAttemptTime < REDIS_RETRY_INTERVAL_MS) {
                return false;
            }
        }
        return true;
    }

    private void handleRedisFailure(Exception ex) {
        lastRedisAttemptTime = System.currentTimeMillis();
        if (redisAvailable) {
            redisAvailable = false;
            LOG.warn(">>> [REDIS OFFLINE] Không thể kết nối tới Redis ({}). Tự động kích hoạt In-Memory Blacklist Fallback.", ex.getMessage());
        } else {
            LOG.debug("Redis vẫn chưa sẵn sàng: {}", ex.getMessage());
        }
    }

    private void handleRedisSuccess() {
        if (!redisAvailable) {
            redisAvailable = true;
            LOG.info(">>> [REDIS RECONNECTED] Đã khôi phục kết nối thành công tới Redis!");
        }
    }

    /**
     * Đưa một token cụ thể (thông qua JTI) vào danh sách đen.
     *
     * @param jti Mã định danh duy nhất của token (JWT ID)
     * @param expiresAt Thời điểm hết hạn của token
     */
    public void blacklistToken(String jti, Instant expiresAt) {
        if (jti == null || jti.isBlank()) {
            LOG.warn("Không thể blacklist token vì JTI rỗng hoặc null.");
            return;
        }

        Instant now = Instant.now();
        // Thời gian sống bằng hạn còn lại của token (hoặc tối thiểu 60s, mặc định 24h)
        Duration ttl = (expiresAt != null && expiresAt.isAfter(now))
            ? Duration.between(now, expiresAt)
            : Duration.ofHours(24);

        boolean redisSuccess = false;
        if (isRedisOperational()) {
            try {
                redisTemplate.opsForValue().set(BLACKLIST_PREFIX + jti, "REVOKED", ttl);
                redisSuccess = true;
                handleRedisSuccess();
                LOG.info(">>> [REDIS BLACKLIST] Đã vô hiệu hóa token jti={} trên Redis (TTL: {} giây)", jti, ttl.getSeconds());
            } catch (Exception ex) {
                handleRedisFailure(ex);
            }
        }

        if (!redisSuccess) {
            memoryBlacklist.put(jti, expiresAt != null ? expiresAt : now.plus(ttl));
            LOG.info(">>> [MEMORY BLACKLIST] Đã lưu token jti={} vào bộ nhớ cục bộ fallback", jti);
        }
    }

    /**
     * Vô hiệu hóa toàn bộ token cũ của một User (áp dụng khi Logout hoặc Đổi mật khẩu).
     *
     * @param username Tên đăng nhập của tài khoản
     */
    public void blacklistUserTokens(String username) {
        if (username == null || username.isBlank()) {
            return;
        }
        long nowEpoch = System.currentTimeMillis();
        boolean redisSuccess = false;
        if (isRedisOperational()) {
            try {
                // Lưu mốc thời gian logout/đổi mật khẩu với TTL 30 ngày (bằng hạn RememberMe tối đa)
                redisTemplate.opsForValue().set(USER_LOGOUT_PREFIX + username.toLowerCase(), String.valueOf(nowEpoch), Duration.ofDays(30));
                redisSuccess = true;
                handleRedisSuccess();
                LOG.info(">>> [REDIS BLACKLIST] Đã hủy toàn bộ phiên làm việc cũ của user '{}' tại thời điểm epoch={}", username, nowEpoch);
            } catch (Exception ex) {
                handleRedisFailure(ex);
            }
        }
        if (!redisSuccess) {
            memoryUserRevocation.put(username.toLowerCase(), nowEpoch);
            LOG.info(">>> [MEMORY BLACKLIST] Đã lưu mốc user logout cho user '{}' vào bộ nhớ cục bộ fallback", username);
        }
    }

    /**
     * Kiểm tra xem một Jwt Token có nằm trong danh sách đen hay không.
     *
     * @param jwt Đối tượng Jwt đã được decode
     * @return true nếu token đã bị thu hồi/đăng xuất; false nếu token vẫn hợp lệ
     */
    public boolean isBlacklisted(Jwt jwt) {
        if (jwt == null) {
            return false;
        }

        // 1. Kiểm tra JTI cụ thể
        String jti = jwt.getId();
        if (jti != null && isBlacklisted(jti)) {
            return true;
        }

        // 2. Kiểm tra xem user có logout hoặc đổi mật khẩu sau thời điểm cấp token này không
        String username = jwt.getSubject();
        if (username != null) {
            Long revokedAt = null;
            if (isRedisOperational()) {
                try {
                    String revokedAtStr = redisTemplate.opsForValue().get(USER_LOGOUT_PREFIX + username.toLowerCase());
                    if (revokedAtStr != null) {
                        revokedAt = Long.parseLong(revokedAtStr);
                    }
                    handleRedisSuccess();
                } catch (Exception ex) {
                    handleRedisFailure(ex);
                }
            } else if (failClosed) {
                LOG.error("Security alert: Redis cluster offline. Fail-closed policy active; rejecting authentication request.");
                return true;
            }

            if (revokedAt == null && !failClosed) {
                revokedAt = memoryUserRevocation.get(username.toLowerCase());
            }

            if (revokedAt != null) {
                Instant issuedAt = jwt.getIssuedAt();
                // Nếu token được cấp trước hoặc tại thời điểm revoked (logout/đổi mật khẩu) -> Chặn
                if (issuedAt != null && issuedAt.toEpochMilli() <= revokedAt) {
                    LOG.warn(">>> [SECURITY REJECT] Token của user '{}' được cấp trước thời điểm hủy phiên (Logout/Đổi mật khẩu). Từ chối xác thực!", username);
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Kiểm tra nhanh theo JTI chuỗi.
     */
    public boolean isBlacklisted(String jti) {
        if (jti == null || jti.isBlank()) {
            return false;
        }

        // Kiểm tra trên Redis
        if (isRedisOperational()) {
            try {
                Boolean hasKey = redisTemplate.hasKey(BLACKLIST_PREFIX + jti);
                handleRedisSuccess();
                if (Boolean.TRUE.equals(hasKey)) {
                    LOG.warn(">>> [SECURITY REJECT] Yêu cầu bị chặn do mang token đã bị thu hồi (Blacklisted JTI)");
                    return true;
                }
            } catch (Exception ex) {
                handleRedisFailure(ex);
                if (failClosed) {
                    LOG.error("Security alert: Redis failure during blacklist verification in fail-closed mode. Rejecting token.");
                    return true;
                }
            }
        } else if (failClosed) {
            LOG.error("Security alert: Redis store offline in fail-closed mode. Rejecting token verification.");
            return true;
        }

        // Kiểm tra trên memory fallback (chỉ dev mode khi failClosed = false)
        if (!failClosed) {
            Instant exp = memoryBlacklist.get(jti);
            if (exp != null) {
                if (Instant.now().isBefore(exp)) {
                    return true;
                } else {
                    memoryBlacklist.remove(jti);
                }
            }
        }

        return false;
    }
}
