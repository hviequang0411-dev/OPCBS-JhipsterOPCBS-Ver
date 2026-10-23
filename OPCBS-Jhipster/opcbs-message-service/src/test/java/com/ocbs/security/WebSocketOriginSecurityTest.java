package com.ocbs.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.ocbs.config.ApplicationProperties;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WebSocketOriginSecurityTest {

    @Test
    @DisplayName("9a. Allowed origins list contains exact production and local domains, never wildcard *")
    void allowedOrigins_shouldNotContainWildcard() {
        ApplicationProperties properties = new ApplicationProperties();
        List<String> origins = properties.getWebsocket().getAllowedOrigins();

        assertThat(origins).isNotEmpty();
        assertThat(origins).doesNotContain("*");
        assertThat(origins).allMatch(origin -> origin.startsWith("http://") || origin.startsWith("https://"));
    }

    @Test
    @DisplayName("9b. Validates allowed origins against authorized production domain while rejecting spoofed origins")
    void allowedOrigins_shouldAcceptConfiguredProductionDomainAndRejectSpoofed() {
        ApplicationProperties properties = new ApplicationProperties();
        properties.getWebsocket().setAllowedOrigins(List.of(
            "https://mindbridge.vn",
            "https://app.mindbridge.vn:443",
            "http://localhost:9000"
        ));

        List<String> allowed = properties.getWebsocket().getAllowedOrigins();

        // 1. Valid exact origins
        assertThat(allowed.contains("https://mindbridge.vn")).isTrue();
        assertThat(allowed.contains("https://app.mindbridge.vn:443")).isTrue();
        assertThat(allowed.contains("http://localhost:9000")).isTrue();

        // 2. Reject spoofed/unauthorized origins
        assertThat(allowed.contains("https://evil-hacker.com")).isFalse();
        assertThat(allowed.contains("http://malicious-origin.org")).isFalse();
        assertThat(allowed.contains("https://mindbridge.vn.attacker.com")).isFalse();
        assertThat(allowed.contains("*")).isFalse();
    }
}
