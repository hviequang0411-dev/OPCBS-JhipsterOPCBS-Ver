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
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
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

    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final com.ocbs.service.UserService userService;
    private final com.ocbs.repository.UserRepository userRepository;
    private final com.ocbs.config.ApplicationProperties applicationProperties;

    public AuthenticateController(
        JwtEncoder jwtEncoder,
        AuthenticationManagerBuilder authenticationManagerBuilder,
        com.ocbs.service.UserService userService,
        com.ocbs.repository.UserRepository userRepository,
        com.ocbs.config.ApplicationProperties applicationProperties
    ) {
        this.jwtEncoder = jwtEncoder;
        this.authenticationManagerBuilder = authenticationManagerBuilder;
        this.userService = userService;
        this.userRepository = userRepository;
        this.applicationProperties = applicationProperties;
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

            // Check if user exists in DB to differentiate between USER_NOT_FOUND and INVALID_PASSWORD
            String searchKey = username.toLowerCase();
            boolean userExists = userRepository.findOneByLogin(searchKey).isPresent() ||
                                 userRepository.findOneByEmailIgnoreCase(searchKey).isPresent();

            if (!userExists) {
                throw new com.ocbs.web.rest.errors.BadRequestAlertException(
                    "Tài khoản hoặc Email không tồn tại trong hệ thống.",
                    "userManagement",
                    "userNotFound"
                );
            } else {
                throw new com.ocbs.web.rest.errors.BadRequestAlertException(
                    "Mật khẩu không chính xác. Vui lòng thử lại hoặc lấy lại mật khẩu.",
                    "userManagement",
                    "invalidPassword"
                );
            }
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
            .issuedAt(now)
            .expiresAt(validity)
            .subject(authentication.getName())
            .claim(AUTHORITIES_CLAIM, authorities);
        if (authentication.getPrincipal() instanceof UserWithId user) {
            builder.claim(USER_ID_CLAIM, user.getId());
        }

        JwsHeader jwsHeader = JwsHeader.with(JWT_ALGORITHM).build();
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
