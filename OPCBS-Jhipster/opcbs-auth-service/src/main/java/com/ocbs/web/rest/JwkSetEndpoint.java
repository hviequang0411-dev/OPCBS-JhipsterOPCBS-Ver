package com.ocbs.web.rest;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Standard RFC 7517 JSON Web Key Set (JWKS) Endpoint.
 * Exposes the active RSA Public Key(s) and Key ID (kid) so downstream microservices,
 * API gateways, and external OIDC resource servers can retrieve public keys and verify JWT signatures.
 */
@RestController
public class JwkSetEndpoint {

    private final RSAKey rsaJwk;

    public JwkSetEndpoint(RSAKey rsaJwk) {
        this.rsaJwk = rsaJwk;
    }

    @GetMapping(value = { "/.well-known/jwks.json", "/api/jwks" }, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getJwks() {
        JWKSet jwkSet = new JWKSet(rsaJwk.toPublicJWK());
        return ResponseEntity.ok()
            .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
            .body(jwkSet.toJSONObject());
    }
}
