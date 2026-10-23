package com.ocbs.security;

import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Service to check for token revocation (blacklist and user-level logout) against the shared Redis cluster.
 * Interoperable with opcbs-auth-service TokenBlacklistService in single-instance and containerized deployments.
 * Operates safely with circuit breaker and fails gracefully if Redis is unavailable in local dev/tests.
 */
@Service
public class TokenRevocationService {

    private static final Logger LOG = LoggerFactory.getLogger(TokenRevocationService.class);
    private static final String BLACKLIST_PREFIX = "token:blacklist:";
    private static final String USER_LOGOUT_PREFIX = "user:logout:";

    private final StringRedisTemplate redisTemplate;
    private volatile boolean redisAvailable = true;
    private volatile long lastRedisAttemptTime = 0L;
    private static final long REDIS_RETRY_INTERVAL_MS = 30_000L;

    public TokenRevocationService(@Autowired(required = false) StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        if (redisTemplate != null) {
            LOG.info("✅ [TokenRevocation] Initialized with Redis connection for distributed token blacklist checks");
        } else {
            this.redisAvailable = false;
            LOG.warn("⚠️ [TokenRevocation] StringRedisTemplate bean not found. Token revocation check will operate in offline fallback mode");
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
            LOG.warn("⚠️ [TokenRevocation] Redis communication failed ({}). Revocation checking downgraded to fallback", ex.getMessage());
        }
    }

    private void handleRedisSuccess() {
        if (!redisAvailable) {
            redisAvailable = true;
            LOG.info("✅ [TokenRevocation] Redis connection re-established!");
        }
    }

    /**
     * Checks if a token is revoked by checking its JTI and whether the user logged out/changed password.
     *
     * @param jti JWT identifier claim
     * @param username username / subject claim
     * @param issuedAt token issue time
     * @return true if token is revoked, false otherwise
     */
    public boolean isRevoked(String jti, String username, Instant issuedAt) {
        if (!isRedisOperational()) {
            return false;
        }

        try {
            // 1. Check specific JTI blacklist entry
            if (jti != null && !jti.isBlank()) {
                Boolean hasKey = redisTemplate.hasKey(BLACKLIST_PREFIX + jti);
                if (Boolean.TRUE.equals(hasKey)) {
                    LOG.warn("🚨 [Security Alert] Token with JTI '{}' detected in Redis blacklist", jti);
                    handleRedisSuccess();
                    return true;
                }
            }

            // 2. Check user-level logout/revocation timestamp
            if (username != null && !username.isBlank()) {
                String revokedAtStr = redisTemplate.opsForValue().get(USER_LOGOUT_PREFIX + username.toLowerCase());
                if (revokedAtStr != null) {
                    try {
                        long revokedAt = Long.parseLong(revokedAtStr);
                        if (issuedAt != null && issuedAt.toEpochMilli() <= revokedAt) {
                            LOG.warn("🚨 [Security Alert] Token for user '{}' issued before user revocation timestamp (epoch={})",
                                username, revokedAt);
                            handleRedisSuccess();
                            return true;
                        }
                    } catch (NumberFormatException e) {
                        LOG.warn("Malformed revocation timestamp for user '{}': {}", username, revokedAtStr);
                    }
                }
            }

            handleRedisSuccess();
        } catch (Exception e) {
            handleRedisFailure(e);
        }

        return false;
    }
}
