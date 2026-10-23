package com.ocbs.config;

import static com.ocbs.security.SecurityUtils.JWT_ALGORITHM;

import com.ocbs.management.SecurityMetersService;
import com.ocbs.security.RsaKeyHelper;
import java.security.interfaces.RSAPublicKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Spring Security JWT Verification Configuration for Downstream Microservices.
 * Verifies access tokens using RS256 asymmetric encryption.
 * Does NOT hold or use the RSA Private Key (signing is restricted to opcbs-auth-service).
 */
@Configuration
public class SecurityJwtConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(SecurityJwtConfiguration.class);

    // Dedicated DEV-ONLY RSA Public Key corresponding to Auth Service's DEV_RSA_PRIVATE_KEY
    public static final String DEV_RSA_PUBLIC_KEY =
        "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAvJ170PciSTl6FbfEyIGpxYNee4X0G6ZK/21QkJQ1k2Nj3d+DoaKa" +
        "3yCkXzEeJP9GkgBWfvbgpLEXrsoZ/H5IbcyFp2XkcFQUK9b/SFzBadoNJyuzBztlYABdj8cuxwjZ7GI1JA1HhUpRhYXAPhb+" +
        "KuN6LyUoc5O2983s7MRpPVFi0bOxN/B9cNFJuT4iXL7sCNa1T6zkKQ5FfUQES8gVIClOAhZ5zexMi6HcKRsis7RQY28IMVNK" +
        "P+2dE1UUa7RvB3Xt4Eb22RH7xtYyGrU9BeF3InGa2+F8aiHJEnqz5FJ9TI6gBZbPbINDsx3dqDs4Sv6IA5qx9kVJf10/QlWW" +
        "VwIDAQAB";

    @Value("${opcbs.jwt.jwk-set-uri:}")
    private String jwkSetUri;

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:}")
    private String springJwkSetUri;

    @Value("${opcbs.jwt.public-key:}")
    private String publicKeyString;

    @Value("${opcbs.jwt.public-key-path:}")
    private String publicKeyPath;

    private final Environment env;

    public SecurityJwtConfiguration(Environment env) {
        this.env = env;
    }

    @Bean
    public JwtDecoder jwtDecoder(SecurityMetersService metersService) {
        NimbusJwtDecoder delegate = buildNimbusDecoder();

        return token -> {
            try {
                return delegate.decode(token);
            } catch (Exception e) {
                if (e.getMessage() != null && e.getMessage().contains("Invalid signature")) {
                    metersService.trackTokenInvalidSignature();
                } else if (e.getMessage() != null && e.getMessage().contains("Jwt expired at")) {
                    metersService.trackTokenExpired();
                } else if (
                    e.getMessage() != null &&
                    (e.getMessage().contains("Invalid JWT serialization") ||
                        e.getMessage().contains("Malformed token") ||
                        e.getMessage().contains("Invalid unsecured/JWS/JWE"))
                ) {
                    metersService.trackTokenMalformed();
                } else {
                    LOG.error("JWT verification error: {}", e.getMessage());
                }
                throw e;
            }
        };
    }

    private NimbusJwtDecoder buildNimbusDecoder() {
        String effectiveJwkSetUri = (jwkSetUri != null && !jwkSetUri.isBlank()) ? jwkSetUri : springJwkSetUri;

        // 1. If JWKS URI is configured: use Remote JWKS Decoder with strict RS256 enforcement
        if (effectiveJwkSetUri != null && !effectiveJwkSetUri.isBlank()) {
            LOG.info("Configuring JWT decoder using JWK Set URI: {}", effectiveJwkSetUri);
            return NimbusJwtDecoder.withJwkSetUri(effectiveJwkSetUri)
                .jwsAlgorithm(JWT_ALGORITHM)
                .build();
        }

        // 2. If Public Key is configured via path or inline string
        RSAPublicKey publicKey = null;
        if (publicKeyPath != null && !publicKeyPath.isBlank()) {
            publicKey = RsaKeyHelper.parsePublicKey(publicKeyPath);
        } else if (publicKeyString != null && !publicKeyString.isBlank()) {
            publicKey = RsaKeyHelper.parsePublicKey(publicKeyString);
        }

        if (publicKey != null) {
            LOG.info("Configuring JWT decoder using static RSA Public Key.");
            return NimbusJwtDecoder.withPublicKey(publicKey)
                .signatureAlgorithm(JWT_ALGORITHM)
                .build();
        }

        // 3. Fail-fast in production if verification key is missing
        boolean isProd = env.acceptsProfiles(Profiles.of("prod", "production"));
        if (isProd) {
            LOG.error(">>> [FATAL SECURITY ERROR] Neither JWK Set URI nor RSA Public Key is configured for JWT verification in PRODUCTION profile! Aborting startup.");
            throw new IllegalStateException(
                "CRITICAL SECURITY ERROR: Verification service requires JWK Set URI or RSA Public Key in production profile!"
            );
        }

        // 4. In dev profile: use dev test public key
        LOG.warn(">>> [DEV SECURITY NOTICE] No JWKS URI or Public Key configured. Using dedicated DEV-ONLY Public Key.");
        publicKey = RsaKeyHelper.parsePublicKey(DEV_RSA_PUBLIC_KEY);
        return NimbusJwtDecoder.withPublicKey(publicKey)
            .signatureAlgorithm(JWT_ALGORITHM)
            .build();
    }
}
