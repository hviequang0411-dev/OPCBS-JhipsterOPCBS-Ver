package com.ocbs.security.jwt;

import static com.ocbs.security.AuthoritiesConstants.ADMIN;
import static com.ocbs.security.SecurityUtils.AUTHORITIES_CLAIM;
import static com.ocbs.security.SecurityUtils.JWT_ALGORITHM;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.ocbs.security.RsaKeyHelper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

public class JwtAuthenticationTestUtils {

    public static final String BEARER = "Bearer ";

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

    // Different key pair for invalid signature test
    private static final String DIFFERENT_RSA_PRIVATE_KEY =
        "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDSY9KKEEifdqkuVshSWJn0kjU2nrBcwN3C8w2vwhH5VKyJ" +
        "Y8poE1Sr6ebrwFd7n7WeMg329y7P8DriF9VGxNtvIH3g5c1uwPMpOHhT9eaJmuDnnE0Xz5hRkhTUAn8+r2ZtkbXUfWW5rP65" +
        "XVira6Mmv/a+uBdB5COltdAqVVcn+7mXHSMkSFED4/WyLqrVBWFZKd5mx/HFVynjERsw1uu9vAL3zqH3EOY/GkFR+LI+NwF5" +
        "ahMTpJhff20ilH2EHBjJQO3f3M3Fqi4ApUjNA6qGpU4Jno05LhSdOtH8BXGqjgziUL/a635vXUIzLdykS6+yaavmLavdJDLU" +
        "fXImxXJXAgMBAAECggEACRO0dC9yqqqYbv7ggqj+723VbOdYzY+mobAR0ZjqZJryHLVY+LeIyif7xgUU4SkJUWtwbczJUMBZ" +
        "EgMcOQLqQregprDTdalOdGNS0bCEw0AS3vs/2jyjIjXOvzziV4aCwUxtj17foiykCsRLvI2WyueyUhUoL7gTj6Ux93maGofu" +
        "OS7fqIB9Zx7AvaYqHHpIPV7mmC9zaRjThaPYJLw0kx7fQJebdATozdkpeACFdvbWUMxr77A+DKfmd0FCrCvnAcK+jV84omsl" +
        "7zZDymCAskGEqir7/B+cB3fnPHLzrGz1/ZdLSdUiI5AYDEwwG//fe1vurgT/a6C2iBF7XSnh5QKBgQDr2mjwWgCWJSccoH3k" +
        "QJkaok8U9FD6jkHxQtPzkCW8M6raeFoG6+Q2wbNLev8C+qWCtEd0usZibL2Jh3KsZgwA8JIkZN8ho6kpzdxA1NVx8d3apGVk" +
        "VJWT0KEaoXozZTfrxleDFG8jRP3jAOidLVRdCBO+MUV3s30o3qKw2z5EfQKBgQDkXJZs4eXGZ2SKFRgl56NlHySrj7Ru52MH" +
        "a7z4fqm7wVia8V3gS3hNRs9eZdjpkpT0o9ctkgcFvsrXahWlWQre+RK20Zy/25WDpSnocuzj50lKlzAAXeju+oKQncCa51gs" +
        "jswGH5VkkFgSyn21Jh22mYxiao7kYtx863yOFPSuYwKBgQDLRf7TyH5g6bx/68MVCqfqUKGgCcIRcLJc4aATd9VOE6nZiYju" +
        "FjknCGMjVuqbArPLpt0/OgrpoZbqBZdMBtepp64NN/Mn5wpRQV/ncIhWIIN5gd7xTgVs1I9FiI7t5PsBdnzOOLpgx4GaOJpD" +
        "fWIMnH2Hmqeot0XUB65oh01vcQKBgEnSbIZyWFdVITlKLG5EfZu4/5++avPXNTo25/Xk7HXXwQ8Fq/VQMGm61FqNzhtj2Wdk" +
        "d7ExnbeLwAL3Nlwnds4x8ozLIkMbdzABepS8YjwijpzgVxInSZ/Ht+GMPbFVzDlLSEVKfH7v8qjQcObYlY5sAOfnzp2iH+j6" +
        "L1+3AopFAoGBAI29FlMbMFeFj/NezdAe+2/YbVkIVAjp1/h+A5gQWTlCiXKJH//A2ZFW43M8AaEHcJHzQFVuNwHNJDy2S7g5" +
        "b8ODcKmmx/+mXVmNlitd3GmmIGEcPF41F8iR2PvQEJlDpukzPwJf7viWQXpt3XAzxteUK1zhO8Ijl/dYj+d1kXTU";

    private static final String DIFFERENT_RSA_PUBLIC_KEY =
        "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA0mPSihBIn3apLlbIUliZ9JI1Np6wXMDdwvMNr8IR+VSsiWPKaBNU" +
        "q+nm68BXe5+1njIN9vcuz/A64hfVRsTbbyB94OXNbsDzKTh4U/XmiZrg55xNF8+YUZIU1AJ/Pq9mbZG11H1luaz+uV1Yq2uj" +
        "Jr/2vrgXQeQjpbXQKlVXJ/u5lx0jJEhRA+P1si6q1QVhWSneZsfxxVcp4xEbMNbrvbwC986h9xDmPxpBUfiyPjcBeWoTE6SY" +
        "X39tIpR9hBwYyUDt39zNxaouAKVIzQOqhqVOCZ6NOS4UnTrR/AVxqo4M4lC/2ut+b11CMy3cpEuvsmmr5i2r3SQy1H1yJsVy" +
        "VwIDAQAB";

    @Bean
    private TestAuthenticationResource authenticationResource() {
        return new TestAuthenticationResource();
    }

    @Bean
    private MeterRegistry meterRegistry() {
        return new SimpleMeterRegistry();
    }

    public static String createValidToken() {
        return createValidTokenForUser("anonymous");
    }

    public static String createValidToken(String ignoredKey) {
        return createValidTokenForUser("anonymous");
    }

    public static String createValidTokenForUser(String user) {
        return createValidTokenForUser(null, user);
    }

    public static String createValidTokenForUser(String ignoredKey, String user) {
        JwtEncoder encoder = rsaJwtEncoder(DEV_RSA_PUBLIC_KEY, DEV_RSA_PRIVATE_KEY, "dev-test-key");

        var now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuedAt(now)
            .expiresAt(now.plusSeconds(60))
            .subject(user)
            .claims(customClaim -> customClaim.put(AUTHORITIES_CLAIM, List.of(ADMIN)))
            .build();

        JwsHeader jwsHeader = JwsHeader.with(JWT_ALGORITHM).keyId("dev-test-key").build();
        return encoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).getTokenValue();
    }

    public static String createTokenWithDifferentSignature() {
        JwtEncoder encoder = rsaJwtEncoder(DIFFERENT_RSA_PUBLIC_KEY, DIFFERENT_RSA_PRIVATE_KEY, "different-key");

        var now = Instant.now();
        var past = now.plusSeconds(60);

        JwtClaimsSet claims = JwtClaimsSet.builder().issuedAt(now).expiresAt(past).subject("anonymous").build();

        JwsHeader jwsHeader = JwsHeader.with(JWT_ALGORITHM).keyId("different-key").build();
        return encoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).getTokenValue();
    }

    public static String createExpiredToken() {
        return createExpiredToken(null);
    }

    public static String createExpiredToken(String ignoredKey) {
        JwtEncoder encoder = rsaJwtEncoder(DEV_RSA_PUBLIC_KEY, DEV_RSA_PRIVATE_KEY, "dev-test-key");

        var now = Instant.now();
        var past = now.minusSeconds(600);

        JwtClaimsSet claims = JwtClaimsSet.builder().issuedAt(past).expiresAt(past.plusSeconds(1)).subject("anonymous").build();

        JwsHeader jwsHeader = JwsHeader.with(JWT_ALGORITHM).keyId("dev-test-key").build();
        return encoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).getTokenValue();
    }

    public static String createInvalidToken() {
        return createInvalidToken(null);
    }

    public static String createInvalidToken(String ignoredKey) {
        return createValidToken().substring(1);
    }

    public static String createSignedInvalidJwt(String ignoredKey) {
        return "invalid.jwt.token";
    }

    public static String createHs512Token() {
        SecretKey secretKey = new SecretKeySpec(
            "0123456789012345678901234567890123456789012345678901234567890123".getBytes(),
            "HmacSHA512"
        );
        JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(secretKey));
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder().issuedAt(now).expiresAt(now.plusSeconds(60)).subject("anonymous").build();
        JwsHeader jwsHeader = JwsHeader.with(MacAlgorithm.HS512).build();
        return encoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).getTokenValue();
    }

    public static String createNoneAlgToken() {
        long exp = Instant.now().plusSeconds(60).getEpochSecond();
        String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(
            ("{\"sub\":\"anonymous\",\"exp\":" + exp + "}").getBytes()
        );
        return "eyJhbGciOiJub25lIiwidHlwIjoiSldUIn0." + payload + ".";
    }

    private static JwtEncoder rsaJwtEncoder(String publicKeyBase64, String privateKeyBase64, String keyId) {
        RSAPublicKey publicKey = RsaKeyHelper.parsePublicKey(publicKeyBase64);
        RSAPrivateKey privateKey = RsaKeyHelper.parsePrivateKey(privateKeyBase64);
        RSAKey rsaJWK = new RSAKey.Builder(publicKey).privateKey(privateKey).keyID(keyId).build();
        JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(new JWKSet(rsaJWK));
        return new NimbusJwtEncoder(jwkSource);
    }
}
