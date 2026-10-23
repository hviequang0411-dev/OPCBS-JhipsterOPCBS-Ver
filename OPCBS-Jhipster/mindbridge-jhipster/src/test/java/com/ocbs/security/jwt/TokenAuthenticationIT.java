package com.ocbs.security.jwt;

import static com.ocbs.security.jwt.JwtAuthenticationTestUtils.*;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@AutoConfigureMockMvc
@AuthenticationIntegrationTest
class TokenAuthenticationIT {

    @Autowired
    private MockMvc mvc;

    @Test
    void testLoginWithValidToken() throws Exception {
        expectOk(createValidToken());
    }

    @Test
    void testReturnFalseWhenJWThasInvalidSignature() throws Exception {
        expectUnauthorized(createTokenWithDifferentSignature());
    }

    @Test
    void testReturnFalseWhenJWTisMalformed() throws Exception {
        expectUnauthorized("header.invalid-payload.signature");
    }

    @Test
    void testReturnFalseWhenJWTisExpired() throws Exception {
        expectUnauthorized(createExpiredToken());
    }

    @Test
    void testRejectHs512Token() throws Exception {
        expectUnauthorized(createHs512Token());
    }

    @Test
    void testRejectNoneAlgToken() throws Exception {
        expectUnauthorized(createNoneAlgToken());
    }

    private void expectOk(String token) throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/api/authenticate").header(AUTHORIZATION, BEARER + token)).andExpect(
            status().isNoContent()
        );
    }

    private void expectUnauthorized(String token) throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/api/authenticate").header(AUTHORIZATION, BEARER + token)).andExpect(
            status().isUnauthorized()
        );
    }
}
