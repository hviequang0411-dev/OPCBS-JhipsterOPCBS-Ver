package com.ocbs.security.jwt;

import static com.ocbs.security.SecurityUtils.AUTHORITIES_CLAIM;
import static com.ocbs.security.SecurityUtils.JWT_ALGORITHM;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;
import com.ocbs.config.SecurityJwtConfiguration;
import com.ocbs.security.AuthoritiesConstants;
import com.ocbs.security.RsaKeyHelper;
import com.ocbs.web.rest.JwkSetEndpoint;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

class AuthJwtRS256Test {

    private RSAPrivateKey privateKey;
    private RSAPublicKey publicKey;
    private RSAKey rsaJwk;
    private JwtEncoder jwtEncoder;
    private JwtDecoder jwtDecoder;
    private static final String TEST_KID = "opcbs-test-kid-2026";

    @BeforeEach
    void setUp() throws Exception {
        privateKey = RsaKeyHelper.parsePrivateKey(SecurityJwtConfiguration.DEV_RSA_PRIVATE_KEY);
        publicKey = RsaKeyHelper.parsePublicKey(SecurityJwtConfiguration.DEV_RSA_PUBLIC_KEY);

        rsaJwk = new RSAKey.Builder(publicKey)
            .privateKey(privateKey)
            .keyID(TEST_KID)
            .keyUse(com.nimbusds.jose.jwk.KeyUse.SIGNATURE)
            .algorithm(com.nimbusds.jose.JWSAlgorithm.RS256)
            .build();

        jwtEncoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(rsaJwk)));
        jwtDecoder = NimbusJwtDecoder.withPublicKey(publicKey).signatureAlgorithm(JWT_ALGORITHM).build();
    }

    @Test
    void testTokenIssuedWithRS256AndKid() {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer("opcbs-auth-service")
            .issuedAt(now)
            .expiresAt(now.plus(1, ChronoUnit.HOURS))
            .subject("testuser")
            .claim(AUTHORITIES_CLAIM, List.of(AuthoritiesConstants.USER))
            .claim("userId", 1001L)
            .build();

        JwsHeader header = JwsHeader.with(JWT_ALGORITHM).keyId(TEST_KID).build();
        Jwt encodedJwt = jwtEncoder.encode(JwtEncoderParameters.from(header, claims));

        assertThat(encodedJwt.getHeaders()).containsEntry("alg", JWT_ALGORITHM);
        assertThat(encodedJwt.getHeaders()).containsEntry("kid", TEST_KID);
        assertThat(encodedJwt.getSubject()).isEqualTo("testuser");
        assertThat(encodedJwt.getClaimAsStringList(AUTHORITIES_CLAIM)).containsExactly(AuthoritiesConstants.USER);
        assertThat((Long) encodedJwt.getClaim("userId")).isEqualTo(1001L);
    }

    @Test
    void testJwksEndpointRFC7517FormatAndNoPrivateKeysLeaked() {
        JwkSetEndpoint endpoint = new JwkSetEndpoint(rsaJwk);
        Map<String, Object> jwkSet = endpoint.getJwks().getBody();

        assertThat(jwkSet).containsKey("keys");
        List<?> keys = (List<?>) jwkSet.get("keys");
        assertThat(keys).hasSize(1);

        @SuppressWarnings("unchecked")
        Map<String, Object> keyMap = (Map<String, Object>) keys.get(0);
        assertThat(keyMap.get("kty")).isEqualTo("RSA");
        assertThat(keyMap.get("use")).isEqualTo("sig");
        assertThat(keyMap.get("alg")).isEqualTo("RS256");
        assertThat(keyMap.get("kid")).isEqualTo(TEST_KID);
        assertThat(keyMap).containsKey("n");
        assertThat(keyMap).containsKey("e");

        // CRITICAL SECURITY ASSERTION: Private key parameters must NEVER be in JWKS
        assertThat(keyMap).doesNotContainKey("d");
        assertThat(keyMap).doesNotContainKey("p");
        assertThat(keyMap).doesNotContainKey("q");
        assertThat(keyMap).doesNotContainKey("dp");
        assertThat(keyMap).doesNotContainKey("dq");
        assertThat(keyMap).doesNotContainKey("qi");
    }

    @Test
    void testTokenSignatureVerificationSuccess() {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer("opcbs-auth-service")
            .issuedAt(now)
            .expiresAt(now.plus(1, ChronoUnit.HOURS))
            .subject("doctor_alice")
            .claim(AUTHORITIES_CLAIM, List.of(AuthoritiesConstants.DOCTOR))
            .build();

        JwsHeader header = JwsHeader.with(JWT_ALGORITHM).keyId(TEST_KID).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        Jwt decoded = jwtDecoder.decode(token);
        assertThat(decoded.getSubject()).isEqualTo("doctor_alice");
        assertThat(decoded.getClaimAsStringList(AUTHORITIES_CLAIM)).containsExactly(AuthoritiesConstants.DOCTOR);
    }

    @Test
    void testTokenWithDifferentSignatureRejected() throws Exception {
        // Generate another RSA key pair
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair foreignPair = kpg.generateKeyPair();

        RSAKey foreignRsa = new RSAKey.Builder((RSAPublicKey) foreignPair.getPublic())
            .privateKey(foreignPair.getPrivate())
            .keyID("foreign-key")
            .build();
        JwtEncoder foreignEncoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(foreignRsa)));

        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuedAt(now)
            .expiresAt(now.plus(1, ChronoUnit.HOURS))
            .subject("attacker")
            .build();
        JwsHeader header = JwsHeader.with(JWT_ALGORITHM).keyId("foreign-key").build();
        String badToken = foreignEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        // The verifier configured with our public key must reject the token signed by foreign key
        assertThatThrownBy(() -> jwtDecoder.decode(badToken))
            .isInstanceOf(BadJwtException.class);
    }

    @Test
    void testExpiredTokenRejected() {
        Instant past = Instant.now().minus(2, ChronoUnit.HOURS);
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuedAt(past)
            .expiresAt(past.plus(1, ChronoUnit.HOURS))
            .subject("expired_user")
            .build();
        JwsHeader header = JwsHeader.with(JWT_ALGORITHM).keyId(TEST_KID).build();
        String expiredToken = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        assertThatThrownBy(() -> jwtDecoder.decode(expiredToken))
            .satisfies(e -> assertThat(e).isInstanceOfAny(BadJwtException.class, JwtValidationException.class));
    }

    @Test
    void testHs512TokenRejectedByRs256Decoder() {
        SecretKey secretKey = new SecretKeySpec(
            "0123456789012345678901234567890123456789012345678901234567890123".getBytes(),
            "HmacSHA512"
        );
        JwtEncoder hmacEncoder = new NimbusJwtEncoder(new ImmutableSecret<>(secretKey));
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuedAt(now)
            .expiresAt(now.plus(1, ChronoUnit.HOURS))
            .subject("hmac_user")
            .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS512).build();
        String hs512Token = hmacEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        // Must reject HS512 because decoder enforces RS256
        assertThatThrownBy(() -> jwtDecoder.decode(hs512Token))
            .isInstanceOf(BadJwtException.class);
    }

    @Test
    void testNoneAlgorithmTokenRejected() {
        long exp = Instant.now().plus(1, ChronoUnit.HOURS).getEpochSecond();
        String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(
            ("{\"sub\":\"none_user\",\"exp\":" + exp + "}").getBytes()
        );
        String noneToken = "eyJhbGciOiJub25lIiwidHlwIjoiSldUIn0." + payload + ".";

        assertThatThrownBy(() -> jwtDecoder.decode(noneToken))
            .isInstanceOf(BadJwtException.class);
    }

    @Test
    void testRsaKeyHelperPublicDerivation() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair pair = kpg.generateKeyPair();

        RSAPublicKey derived = RsaKeyHelper.derivePublicKey((RSAPrivateCrtKey) pair.getPrivate());
        assertThat(derived.getModulus()).isEqualTo(((RSAPublicKey) pair.getPublic()).getModulus());
        assertThat(derived.getPublicExponent()).isEqualTo(((RSAPublicKey) pair.getPublic()).getPublicExponent());
    }
}
