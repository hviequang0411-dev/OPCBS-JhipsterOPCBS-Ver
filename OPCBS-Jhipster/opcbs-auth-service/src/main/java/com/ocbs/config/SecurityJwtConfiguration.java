package com.ocbs.config;

import static com.ocbs.security.SecurityUtils.JWT_ALGORITHM;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.ocbs.management.SecurityMetersService;
import com.ocbs.security.RsaKeyHelper;
import com.ocbs.security.TokenBlacklistService;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration
public class SecurityJwtConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(SecurityJwtConfiguration.class);

    // Dedicated DEV-ONLY RSA 2048-bit key pair (NEVER used in production)
    public static final String DEV_RSA_PRIVATE_KEY =
        "MIIEvwIBADANBgkqhkiG9w0BAQEFAASCBKkwggSlAgEAAoIBAQC8nXvQ9yJJOXoVt8TIganFg157hfQbpkr/bVCQlDWTY2Pd" +
        "34OhoprfIKRfMR4k/0aSAFZ+9uCksReuyhn8fkhtzIWnZeRwVBQr1v9IXMFp2g0nK7MHO2VgAF2Pxy7HCNnsYjUkDUeFSlGF" +
        "hcA+Fv4q43ovJShzk7b3zezsxGk9UWLRs7E38H1w0Um5PiJcvuwI1rVPrOQpDkV9RARLyBUgKU4CFnnN7EyLodwpGyKztFBj" +
        "bwgxU0o/7Z0TVRRrtG8Hde3gRvbZEfvG1jIatT0F4XcicZrb4XxqIckSerPkUn1MjqAFls9sg0OzHd2oOzhK/ogDmrH2RUl/" +
        "XT9CVZZXAgMBAAECggEADGfVSXi/vg40O/Jy1HdGydFqqPg2Bo2I+ETh3CiLX47VdjXpy/tMcWmNth8eavOJWjgAeGhXfS2S" +
        "r7HmRKevrt0ZW1nFzSeSjhGdh85mAEbRv6ZNp6ed8tMDVF4W8wtfff81nJHtBtTNCNmzu8G6ZOrCUhCBGRz5l1Hph+chPuGa" +
        "+VVDpDcfTJj5rklw7SQ6Kv0ISvUyaezwOZHbJYta+if+PrLUht9tmFISZRyQt5vcJ2Qmu+/EmQa7kN4j0M9OyR9H1u6J++Pq" +
        "dSl8tVmEEMrw+Ut1R++a6Zy3Yt3mTOklSxC062Yy+aSi1wemhxFuzfB70PuthR87i6IOP5Ab9QKBgQDviuT7TehuBakg3Ia2" +
        "DYzaK71pl5egT8qGakjPEucZFPBBm/vDDd7ipIBA+1bRWXOGPPiEAltj7zcWQ2aL393j3JU8+h5G+E2ObR+lhMykJYkmTEVz" +
        "8V9jxt+vdU8RSgZAoyyBwofGX3oFwHadPLlOwtd8Jt0dt4kf1sfBWra7FQKBgQDJkt8pwV0L15YiE38UQkyCdDvu3RVkNXnm" +
        "qDGJaclJZgCD4T6pOhnqyzVH9nKT6xIX9zraDlVglN94bxWZ3IMJKtB613eXad/vBgk6QFCBOIncW+wnqiqBVEMUgGEL4ezY" +
        "QCT6LSMHCoFjgIlJOfLwT1Q6Ec5Ed3eOt+aJTPK2uwKBgQCBxfyxzQ5b9MMg43gzzvl9BO9T8mWZ20B8O76AONxscTSMRuZJ" +
        "UQ1sOfSlv8TUHCMDGasFUYWTiXiNnixDISBEHy/W3iZjCohx3dqkQGLhlthLoEyMOLsWAhsTInmYg0MJ55zoG6yVIwqnbRKV" +
        "st0tt08omqF2RQWL8z87GFsg1QKBgQCkewE4jD0Fnbw3QBtxB4xw4rHI/oQnCe/XhCUv0K9cAiyncFP2OJ/PVFO8zd++zZEt" +
        "FyLhrdKDBde36UUCEMqn31TQ+CLw3YbJ/u3mp3ZJiz4LHsL7tsIKWByIjR0jWg4ug2Do943ef/cGn2rm4q9W4hSDceYTPjzD" +
        "q1IldTGUYwKBgQCCs0xhwD1E+vR2RcJ6hJH30HybRhiGCnLY2HlOkt6eiQRjSgpZEpqsSRD3JBXO0gZ6Qw+ayPdp0zDxCrjC" +
        "xMBx0qGrEliZnm0Tjm+ek9ujzucg/SB+VaVX/zhT+ph57wZqCTYYg7yWRf36gubqkcXAjo0e56kkOZa3OZCOdbO6QQ==";

    public static final String DEV_RSA_PUBLIC_KEY =
        "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAvJ170PciSTl6FbfEyIGpxYNee4X0G6ZK/21QkJQ1k2Nj3d+DoaKa" +
        "3yCkXzEeJP9GkgBWfvbgpLEXrsoZ/H5IbcyFp2XkcFQUK9b/SFzBadoNJyuzBztlYABdj8cuxwjZ7GI1JA1HhUpRhYXAPhb+" +
        "KuN6LyUoc5O2983s7MRpPVFi0bOxN/B9cNFJuT4iXL7sCNa1T6zkKQ5FfUQES8gVIClOAhZ5zexMi6HcKRsis7RQY28IMVNK" +
        "P+2dE1UUa7RvB3Xt4Eb22RH7xtYyGrU9BeF3InGa2+F8aiHJEnqz5FJ9TI6gBZbPbINDsx3dqDs4Sv6IA5qx9kVJf10/QlWW" +
        "VwIDAQAB";

    @Value("${opcbs.jwt.private-key:}")
    private String privateKeyString;

    @Value("${opcbs.jwt.private-key-path:}")
    private String privateKeyPath;

    @Value("${opcbs.jwt.public-key:}")
    private String publicKeyString;

    @Value("${opcbs.jwt.public-key-path:}")
    private String publicKeyPath;

    @Value("${opcbs.jwt.key-id:opcbs-jwt-key-1}")
    private String keyId;

    private final Environment env;

    public SecurityJwtConfiguration(Environment env) {
        this.env = env;
    }

    @Bean
    public RSAKey rsaJwk() {
        RSAPrivateKey privateKey = null;
        RSAPublicKey publicKey = null;

        // 1. Load private key from configured path or inline string
        if (privateKeyPath != null && !privateKeyPath.isBlank()) {
            privateKey = RsaKeyHelper.parsePrivateKey(privateKeyPath);
        } else if (privateKeyString != null && !privateKeyString.isBlank()) {
            privateKey = RsaKeyHelper.parsePrivateKey(privateKeyString);
        }

        // 2. Load public key from configured path or inline string
        if (publicKeyPath != null && !publicKeyPath.isBlank()) {
            publicKey = RsaKeyHelper.parsePublicKey(publicKeyPath);
        } else if (publicKeyString != null && !publicKeyString.isBlank()) {
            publicKey = RsaKeyHelper.parsePublicKey(publicKeyString);
        }

        // 3. If public key was not explicitly provided, derive it from the CRT private key
        if (privateKey != null && publicKey == null) {
            publicKey = RsaKeyHelper.derivePublicKey(privateKey);
        }

        // 4. Production Fail-Fast Validation
        boolean isProd = env.acceptsProfiles(Profiles.of("prod", "production"));
        if (privateKey == null) {
            if (isProd) {
                LOG.error(">>> [FATAL SECURITY ERROR] RSA Private Key is missing in PRODUCTION profile! Aborting startup.");
                throw new IllegalStateException(
                    "CRITICAL SECURITY ERROR: RSA Private Key must be configured in production via opcbs.jwt.private-key or private-key-path!"
                );
            }
            LOG.warn(">>> [DEV SECURITY NOTICE] No RSA Private Key configured. Using dedicated DEV-ONLY key pair.");
            privateKey = RsaKeyHelper.parsePrivateKey(DEV_RSA_PRIVATE_KEY);
            publicKey = RsaKeyHelper.parsePublicKey(DEV_RSA_PUBLIC_KEY);
        }

        LOG.info("Configured RSA JWT signing key with Key ID (kid): {}", keyId);

        return new RSAKey.Builder(publicKey)
            .privateKey(privateKey)
            .keyID(keyId)
            .keyUse(com.nimbusds.jose.jwk.KeyUse.SIGNATURE)
            .algorithm(JWSAlgorithm.RS256)
            .build();
    }

    @Bean
    public JwtEncoder jwtEncoder(RSAKey rsaJwk) {
        JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(new JWKSet(rsaJwk));
        return new NimbusJwtEncoder(jwkSource);
    }

    @Bean
    public JwtDecoder jwtDecoder(RSAKey rsaJwk, SecurityMetersService metersService, TokenBlacklistService blacklistService) {
        RSAPublicKey publicKey;
        try {
            publicKey = rsaJwk.toRSAPublicKey();
        } catch (com.nimbusds.jose.JOSEException e) {
            throw new IllegalStateException("Failed to extract RSA Public Key from RSAKey JWK: " + e.getMessage(), e);
        }

        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withPublicKey(publicKey)
            .signatureAlgorithm(JWT_ALGORITHM)
            .build();

        return token -> {
            try {
                org.springframework.security.oauth2.jwt.Jwt jwt = jwtDecoder.decode(token);

                // Redis Token Blacklist Verification
                if (blacklistService.isBlacklisted(jwt)) {
                    LOG.warn(">>> [SECURITY ALERT] Yêu cầu bị chặn: Token mang JTI={} đã bị thu hồi trong Redis Blacklist!", jwt.getId());
                    throw new org.springframework.security.oauth2.jwt.BadJwtException(
                        "Token đã bị hủy phiên đăng nhập hoặc thu hồi (Redis Token Blacklist)."
                    );
                }

                return jwt;
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
                    LOG.error("Lỗi xác thực JWT: {}", e.getMessage());
                }
                throw e;
            }
        };
    }
}
