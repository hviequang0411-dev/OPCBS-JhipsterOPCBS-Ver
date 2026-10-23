package com.ocbs.web.rest;

import static com.ocbs.security.SecurityUtils.AUTHORITIES_CLAIM;
import static com.ocbs.security.SecurityUtils.JWT_ALGORITHM;
import static com.ocbs.security.SecurityUtils.USER_ID_CLAIM;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ocbs.security.DomainUserDetailsService.UserWithId;
import com.ocbs.web.rest.vm.LoginVM;
import jakarta.validation.Valid;
import java.security.Principal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.bind.annotation.*;

/**
 * Controller to authenticate users.
 */
@RestController
@RequestMapping("/api")
public class AuthenticateController {

    private static final Logger LOG = LoggerFactory.getLogger(AuthenticateController.class);

    private final JwtEncoder jwtEncoder;

    @Value("${jhipster.security.authentication.jwt.token-validity-in-seconds:0}")
    private long tokenValidityInSeconds;

    @Value("${jhipster.security.authentication.jwt.token-validity-in-seconds-for-remember-me:0}")
    private long tokenValidityInSecondsForRememberMe;

    @Value("${opcbs.jwt.key-id:opcbs-jwt-key-1}")
    private String keyId;

    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final com.ocbs.service.UserService userService;
    private final com.ocbs.repository.UserRepository userRepository;
    private final com.ocbs.config.ApplicationProperties applicationProperties;
    private final com.ocbs.security.TokenBlacklistService tokenBlacklistService;
    private final JwtDecoder jwtDecoder;

    public AuthenticateController(
        JwtEncoder jwtEncoder,
        AuthenticationManagerBuilder authenticationManagerBuilder,
        com.ocbs.service.UserService userService,
        com.ocbs.repository.UserRepository userRepository,
        com.ocbs.config.ApplicationProperties applicationProperties,
        com.ocbs.security.TokenBlacklistService tokenBlacklistService,
        JwtDecoder jwtDecoder
    ) {
        this.jwtEncoder = jwtEncoder;
        this.authenticationManagerBuilder = authenticationManagerBuilder;
        this.userService = userService;
        this.userRepository = userRepository;
        this.applicationProperties = applicationProperties;
        this.tokenBlacklistService = tokenBlacklistService;
        this.jwtDecoder = jwtDecoder;
    }

    @PostMapping("/authenticate")
    public ResponseEntity<JWTToken> authorize(@Valid @RequestBody LoginVM loginVM) {
        String username = loginVM.getUsername() != null ? loginVM.getUsername().trim() : "";
        var authenticationToken = new UsernamePasswordAuthenticationToken(username, loginVM.getPassword());

        try {
            var authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = this.createToken(authentication, loginVM.isRememberMe());
            var httpHeaders = new HttpHeaders();
            httpHeaders.setBearerAuth(jwt);

            long maxAgeSeconds = loginVM.isRememberMe() ? 30 * 24 * 3600 : -1;

            // =========================================================================
            // [CODE CŨ TRƯỚC ĐÂY]:
            // ResponseCookie authCookie = ResponseCookie.from("jhi-authenticationToken", jwt)
            //     .httpOnly(false) // Cho phép JavaScript Angular đọc document.cookie để cứu nguy đa tab
            //     .path("/")
            //     .maxAge(maxAgeSeconds)
            //     .sameSite("Lax")
            //     .build();
            //
            // [CƠ CHẾ MỚI - NÂNG CẤP CHUẨN ENTERPRISE HTTPONLY]:
            // Bật httpOnly(true) để trình duyệt cấm hoàn toàn JavaScript truy cập cookie này,
            // bảo vệ hệ thống tuyệt đối trước các nguy cơ tấn công đánh cắp token qua XSS.
            // Token sẽ được Nginx Gateway hoặc CookieToBearerTokenFilter tự động trích xuất
            // và chuyển thành header "Authorization: Bearer <jwt>" trước khi vào microservices.
            // =========================================================================
            ResponseCookie authCookie = ResponseCookie.from("jhi-authenticationToken", jwt)
                .httpOnly(true)
                .path("/")
                .maxAge(maxAgeSeconds)
                .sameSite("Lax")
                .build();

            ResponseCookie idCookie = ResponseCookie.from("id_token", jwt)
                .httpOnly(true)
                .path("/")
                .maxAge(maxAgeSeconds)
                .sameSite("Lax")
                .build();

            httpHeaders.add(HttpHeaders.SET_COOKIE, authCookie.toString());
            httpHeaders.add(HttpHeaders.SET_COOKIE, idCookie.toString());

            return new ResponseEntity<>(new JWTToken(jwt), httpHeaders, HttpStatus.OK);
        } catch (org.springframework.security.core.AuthenticationException ex) {
            LOG.warn("Authentication failure for user '{}': {}", username, ex.getMessage());

            // Check if user is not activated
            Throwable root = ex;
            while (root.getCause() != null) {
                root = root.getCause();
            }
            if (root instanceof com.ocbs.security.UserNotActivatedException || (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("not activated"))) {
                throw new com.ocbs.web.rest.errors.BadRequestAlertException(
                    "Tài khoản chưa được kích hoạt email. Vui lòng nhập mã OTP để hoàn tất xác thực.",
                    "userManagement",
                    "userNotActivated"
                );
            }

            // Check if user exists in DB to differentiate between USER_NOT_FOUND, USER_NOT_ACTIVATED and INVALID_PASSWORD
            String searchKey = username.toLowerCase();
            var userOpt = userRepository.findOneByLogin(searchKey)
                .or(() -> userRepository.findOneByEmailIgnoreCase(searchKey));

            if (userOpt.isEmpty()) {
                throw new com.ocbs.web.rest.errors.BadRequestAlertException(
                    "Tài khoản hoặc Email không tồn tại trong hệ thống.",
                    "userManagement",
                    "userNotFound"
                );
            }

            com.ocbs.domain.User foundUser = userOpt.get();
            if (!foundUser.isActivated()) {
                throw new com.ocbs.web.rest.errors.BadRequestAlertException(
                    "Tài khoản chưa được kích hoạt email. Vui lòng nhập mã OTP để hoàn tất xác thực.",
                    "userManagement",
                    "userNotActivated"
                );
            }

            throw new com.ocbs.web.rest.errors.BadRequestAlertException(
                "Mật khẩu không chính xác. Vui lòng thử lại hoặc lấy lại mật khẩu.",
                "userManagement",
                "invalidPassword"
            );
        }
    }

    public static class GoogleAuthVM {
        private String idToken;
        private String email;
        private String name;
        private String picture;

        public String getIdToken() { return idToken; }
        public void setIdToken(String idToken) { this.idToken = idToken; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getPicture() { return picture; }
        public void setPicture(String picture) { this.picture = picture; }
    }

    /**
     * {@code POST /authenticate/google} : Authenticate or register using Google credentials.
     */
    @PostMapping("/authenticate/google")
    public ResponseEntity<JWTToken> authorizeGoogle(@RequestBody GoogleAuthVM googleAuthVM) {
        LOG.debug("REST request to authenticate with Google: {}", googleAuthVM.getEmail());

        String email = googleAuthVM.getEmail();
        String name = googleAuthVM.getName();
        String picture = googleAuthVM.getPicture();
        String googleId = null;

        // Parse Google JWT ID Token if present
        if (googleAuthVM.getIdToken() != null && !googleAuthVM.getIdToken().isBlank()) {
            String token = googleAuthVM.getIdToken().trim();
            // 1. Try decoding local JWT Payload first (Fast, reliable, works offline)
            String[] parts = token.split("\\.");
            if (parts.length >= 2) {
                try {
                    byte[] decodedBytes = java.util.Base64.getUrlDecoder().decode(parts[1]);
                    String payloadJson = new String(decodedBytes, java.nio.charset.StandardCharsets.UTF_8);
                    com.fasterxml.jackson.databind.JsonNode rootNode = new com.fasterxml.jackson.databind.ObjectMapper().readTree(payloadJson);
                    if (rootNode.has("email") && !rootNode.get("email").asText().isBlank()) {
                        email = rootNode.get("email").asText();
                    }
                    if (rootNode.has("name") && !rootNode.get("name").asText().isBlank()) {
                        name = rootNode.get("name").asText();
                    }
                    if (rootNode.has("picture") && !rootNode.get("picture").asText().isBlank()) {
                        picture = rootNode.get("picture").asText();
                    }
                    if (rootNode.has("sub") && !rootNode.get("sub").asText().isBlank()) {
                        googleId = rootNode.get("sub").asText();
                    }
                } catch (Exception ex) {
                    LOG.warn("Failed to parse Google JWT payload directly: {}", ex.getMessage());
                }
            }

            // 2. Fallback to Google TokenInfo endpoint if email is still missing
            if (email == null || email.isBlank()) {
                try {
                    org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
                    String url = "https://oauth2.googleapis.com/tokeninfo?id_token=" + token;
                    var response = restTemplate.getForObject(url, java.util.Map.class);
                    if (response != null) {
                        if (response.containsKey("email")) email = (String) response.get("email");
                        if (response.containsKey("name")) name = (String) response.get("name");
                        if (response.containsKey("picture")) picture = (String) response.get("picture");
                        if (response.containsKey("sub")) googleId = (String) response.get("sub");
                    }
                } catch (Exception e) {
                    LOG.warn("Could not verify Google ID token with Google endpoint: {}", e.getMessage());
                }
            }
        }

        if (email == null || email.isBlank()) {
            throw new com.ocbs.web.rest.errors.BadRequestAlertException("Không thể xác thực thông tin tài khoản Google.", "userManagement", "googleautherror");
        }

        com.ocbs.domain.User user = userService.loginOrRegisterWithGoogle(email, name, picture, googleId);

        // Tạo authorities list từ User entity
        List<GrantedAuthority> authorities = user.getAuthorities().stream()
            .map(authority -> new org.springframework.security.core.authority.SimpleGrantedAuthority(authority.getName()))
            .collect(Collectors.toList());

        var userPrincipal = new UserWithId(user.getLogin(), user.getPassword(), authorities, user.getId());
        var authentication = new UsernamePasswordAuthenticationToken(userPrincipal, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        String jwt = this.createToken(authentication, true);
        var httpHeaders = new HttpHeaders();
        httpHeaders.setBearerAuth(jwt);

        // =========================================================================
        // [CƠ CHẾ MỚI - NÂNG CẤP CHUẨN ENTERPRISE HTTPONLY CHO GOOGLE AUTH]:
        // Code cũ: .httpOnly(false) -> Nâng cấp thành .httpOnly(true)
        // =========================================================================
        ResponseCookie authCookie = ResponseCookie.from("jhi-authenticationToken", jwt)
            .httpOnly(true)
            .path("/")
            .maxAge(30 * 24 * 3600)
            .sameSite("Lax")
            .build();
        httpHeaders.add(HttpHeaders.SET_COOKIE, authCookie.toString());

        return new ResponseEntity<>(new JWTToken(jwt), httpHeaders, HttpStatus.OK);
    }

    /**
     * {@code GET /authenticate} : check if the user is authenticated.
     *
     * @return the {@link ResponseEntity} with status {@code 204 (No Content)},
     * or with status {@code 401 (Unauthorized)} if not authenticated.
     */
    @GetMapping("/authenticate")
    public ResponseEntity<Void> isAuthenticated(Principal principal) {
        LOG.debug("REST request to check if the current user is authenticated");
        return ResponseEntity.status(principal == null ? HttpStatus.UNAUTHORIZED : HttpStatus.NO_CONTENT).build();
    }

    /**
     * {@code POST /logout} : Hủy phiên làm việc, đưa token vào Redis Blacklist và xóa sạch Cookie HttpOnly.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
        jakarta.servlet.http.HttpServletRequest request,
        @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.oauth2.jwt.Jwt jwt
    ) {
        LOG.debug("REST request to logout and blacklist current token in Redis");

        // =========================================================================
        // [CƠ CHẾ MỚI - ENTERPRISE LOGOUT WITH MULTI-LEVEL BLACKLIST & FULL COOKIE WIPE]:
        // 1. Thu hồi cụ thể theo Token JTI (JWT ID).
        // 2. Thu hồi toàn bộ phiên của User bằng blacklistUserTokens (chặn mọi token cũ cấp trước thời điểm logout).
        // 3. Xóa sạch mọi biến thể Cookie (HttpOnly, Non-HttpOnly, Path="/" và Path="")
        // 4. Phát Header 'Clear-Site-Data: "cookies, storage"' để trình duyệt dọn sạch cả bộ nhớ Web Storage.
        // =========================================================================

        // 1. Thu hồi token và đưa vào Redis Token Blacklist
        if (jwt != null) {
            tokenBlacklistService.blacklistToken(jwt.getId(), jwt.getExpiresAt());
            tokenBlacklistService.blacklistUserTokens(jwt.getSubject());
        } else {
            // Trường hợp request mang token qua header hoặc cookie mà chưa map AuthenticationPrincipal
            String bearerToken = resolveTokenFromRequest(request);
            if (bearerToken != null && !bearerToken.isBlank()) {
                try {
                    org.springframework.security.oauth2.jwt.Jwt decoded = jwtDecoder.decode(bearerToken);
                    tokenBlacklistService.blacklistToken(decoded.getId(), decoded.getExpiresAt());
                    tokenBlacklistService.blacklistUserTokens(decoded.getSubject());
                } catch (Exception ex) {
                    LOG.debug("Could not decode token for blacklist: {}", ex.getMessage());
                }
            }
        }

        // 2. Dọn sạch triệt để Cookie trên trình duyệt (Max-Age = 0)
        // Bao phủ cả HttpOnly=true và HttpOnly=false, path="/" và path rỗng
        ResponseCookie clearAuthCookiePathRoot = ResponseCookie.from("jhi-authenticationToken", "")
            .httpOnly(true)
            .path("/")
            .maxAge(0)
            .sameSite("Lax")
            .build();

        ResponseCookie clearAuthCookieNonHttpOnly = ResponseCookie.from("jhi-authenticationToken", "")
            .httpOnly(false)
            .path("/")
            .maxAge(0)
            .sameSite("Lax")
            .build();

        ResponseCookie clearIdTokenCookiePathRoot = ResponseCookie.from("id_token", "")
            .httpOnly(true)
            .path("/")
            .maxAge(0)
            .sameSite("Lax")
            .build();

        ResponseCookie clearIdTokenCookieNonHttpOnly = ResponseCookie.from("id_token", "")
            .httpOnly(false)
            .path("/")
            .maxAge(0)
            .sameSite("Lax")
            .build();

        ResponseCookie clearAuthCookieNoPath = ResponseCookie.from("jhi-authenticationToken", "")
            .httpOnly(true)
            .maxAge(0)
            .sameSite("Lax")
            .build();

        ResponseCookie clearIdTokenCookieNoPath = ResponseCookie.from("id_token", "")
            .httpOnly(true)
            .maxAge(0)
            .sameSite("Lax")
            .build();

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, clearAuthCookiePathRoot.toString());
        headers.add(HttpHeaders.SET_COOKIE, clearAuthCookieNonHttpOnly.toString());
        headers.add(HttpHeaders.SET_COOKIE, clearIdTokenCookiePathRoot.toString());
        headers.add(HttpHeaders.SET_COOKIE, clearIdTokenCookieNonHttpOnly.toString());
        headers.add(HttpHeaders.SET_COOKIE, clearAuthCookieNoPath.toString());
        headers.add(HttpHeaders.SET_COOKIE, clearIdTokenCookieNoPath.toString());
        headers.add("Clear-Site-Data", "\"cookies, storage\"");

        return new ResponseEntity<>(headers, HttpStatus.OK);
    }

    private String resolveTokenFromRequest(jakarta.servlet.http.HttpServletRequest request) {
        String bearer = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        if (request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie c : request.getCookies()) {
                if ("jhi-authenticationToken".equals(c.getName()) || "id_token".equals(c.getName())) {
                    return c.getValue();
                }
            }
        }
        return null;
    }

    public String createToken(Authentication authentication, boolean rememberMe) {
        String authorities = authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.joining(" "));

        var now = Instant.now();
        Instant validity;
        if (rememberMe) {
            validity = now.plus(this.tokenValidityInSecondsForRememberMe, ChronoUnit.SECONDS);
        } else {
            validity = now.plus(this.tokenValidityInSeconds, ChronoUnit.SECONDS);
        }

        // @formatter:off
        JwtClaimsSet.Builder builder = JwtClaimsSet.builder()
            .id(java.util.UUID.randomUUID().toString()) // Chuẩn RFC 7519: JTI (JWT ID) duy nhất phục vụ Redis Blacklist
            .issuedAt(now)
            .expiresAt(validity)
            .subject(authentication.getName())
            .claim(AUTHORITIES_CLAIM, authorities);
        if (authentication.getPrincipal() instanceof UserWithId user) {
            builder.claim(USER_ID_CLAIM, user.getId());
        }

        JwsHeader jwsHeader = JwsHeader.with(JWT_ALGORITHM)
            .keyId(this.keyId)
            .build();
        return this.jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, builder.build())).getTokenValue();
    }

    /**
     * Object to return as body in JWT Authentication.
     */
    static class JWTToken {

        private String idToken;

        JWTToken(String idToken) {
            this.idToken = idToken;
        }

        @JsonProperty("id_token")
        String getIdToken() {
            return idToken;
        }

        void setIdToken(String idToken) {
            this.idToken = idToken;
        }
    }
}
