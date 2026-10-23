package com.ocbs.security.jwt;

import static com.ocbs.security.jwt.JwtAuthenticationTestUtils.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Collection;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@AutoConfigureMockMvc
@AuthenticationIntegrationTest
class TokenAuthenticationSecurityMetersIT {

    private static final String INVALID_TOKENS_METER_EXPECTED_NAME = "security.authentication.invalid-tokens";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private MeterRegistry meterRegistry;

    @Test
    void testValidTokenShouldNotCountAnything() throws Exception {
        Collection<Counter> counters = meterRegistry.find(INVALID_TOKENS_METER_EXPECTED_NAME).counters();

        var count = aggregate(counters);

        tryToAuthenticate(createValidToken());

        assertThat(aggregate(counters)).isEqualTo(count);
    }

    @Test
    void testTokenExpiredCount() throws Exception {
        var count = meterRegistry.get(INVALID_TOKENS_METER_EXPECTED_NAME).tag("cause", "expired").counter().count();

        tryToAuthenticate(createExpiredToken());

        assertThat(meterRegistry.get(INVALID_TOKENS_METER_EXPECTED_NAME).tag("cause", "expired").counter().count()).isEqualTo(count + 1);
    }

    @Test
    void testTokenSignatureInvalidCount() throws Exception {
        var count = meterRegistry.get(INVALID_TOKENS_METER_EXPECTED_NAME).tag("cause", "invalid-signature").counter().count();

        tryToAuthenticate(createTokenWithDifferentSignature());

        assertThat(meterRegistry.get(INVALID_TOKENS_METER_EXPECTED_NAME).tag("cause", "invalid-signature").counter().count()).isEqualTo(
            count + 1
        );
    }

    @Test
    void testTokenMalformedCount() throws Exception {
        var count = meterRegistry.get(INVALID_TOKENS_METER_EXPECTED_NAME).tag("cause", "malformed").counter().count();

        tryToAuthenticate("invalid-jwt-token-string");

        assertThat(meterRegistry.get(INVALID_TOKENS_METER_EXPECTED_NAME).tag("cause", "malformed").counter().count()).isEqualTo(count + 1);
    }

    @Test
    void testTokenInvalidCount() throws Exception {
        var count = meterRegistry.get(INVALID_TOKENS_METER_EXPECTED_NAME).tag("cause", "malformed").counter().count();

        tryToAuthenticate(createInvalidToken());

        assertThat(meterRegistry.get(INVALID_TOKENS_METER_EXPECTED_NAME).tag("cause", "malformed").counter().count()).isEqualTo(count + 1);
    }

    private void tryToAuthenticate(String token) throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/api/authenticate").header(AUTHORIZATION, BEARER + token));
    }

    private double aggregate(Collection<Counter> counters) {
        return counters.stream().mapToDouble(Counter::count).sum();
    }
}
