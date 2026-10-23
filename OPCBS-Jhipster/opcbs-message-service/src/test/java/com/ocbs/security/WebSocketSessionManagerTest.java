package com.ocbs.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

@ExtendWith(MockitoExtension.class)
class WebSocketSessionManagerTest {

    @Mock
    private TokenRevocationService tokenRevocationService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private WebSocketSession webSocketSession;

    @InjectMocks
    private WebSocketSessionManager sessionManager;

    @BeforeEach
    void setUp() {
        when(webSocketSession.isOpen()).thenReturn(true);
        when(webSocketSession.getId()).thenReturn("sess-101");
    }

    @Test
    @DisplayName("7a. Active session scanner terminates session when JWT expiration has passed")
    void scanAndEvict_terminatesExpiredSession() throws IOException {
        sessionManager.registerRawWebSocketSession(webSocketSession);

        Instant expiredTime = Instant.now().minus(10, ChronoUnit.SECONDS);
        sessionManager.bindStompAuth("sess-101", "patient1", "jti-expired", Instant.now().minus(1, ChronoUnit.HOURS), expiredTime);

        // Run scanner
        sessionManager.scanAndEvictExpiredOrRevokedSessions();

        // Verify close called with 4001
        verify(webSocketSession, times(1)).close(eq(WebSocketSessionManager.CLOSE_STATUS_TOKEN_EXPIRED));

        // Verify error alert dispatched to user queue
        verify(messagingTemplate, times(1)).convertAndSendToUser(
            eq("patient1"),
            eq("/queue/errors"),
            any(Map.class)
        );
    }

    @Test
    @DisplayName("7b. Active session scanner terminates session when token is revoked in Redis")
    void scanAndEvict_terminatesRevokedSession() throws IOException {
        sessionManager.registerRawWebSocketSession(webSocketSession);

        Instant validExpiry = Instant.now().plus(30, ChronoUnit.MINUTES);
        Instant issuedAt = Instant.now().minus(5, ChronoUnit.MINUTES);
        sessionManager.bindStompAuth("sess-101", "patient1", "jti-revoked-token", issuedAt, validExpiry);

        when(tokenRevocationService.isRevoked("jti-revoked-token", "patient1", issuedAt)).thenReturn(true);

        // Run scanner
        sessionManager.scanAndEvictExpiredOrRevokedSessions();

        // Verify close called with 4002
        verify(webSocketSession, times(1)).close(eq(WebSocketSessionManager.CLOSE_STATUS_TOKEN_REVOKED));

        // Verify error alert dispatched to user queue
        verify(messagingTemplate, times(1)).convertAndSendToUser(
            eq("patient1"),
            eq("/queue/errors"),
            any(Map.class)
        );
    }

    @Test
    @DisplayName("7c. Valid unexpired and unrevoked session is kept open")
    void scanAndEvict_keepsValidSessionOpen() throws IOException {
        sessionManager.registerRawWebSocketSession(webSocketSession);

        Instant validExpiry = Instant.now().plus(30, ChronoUnit.MINUTES);
        Instant issuedAt = Instant.now().minus(5, ChronoUnit.MINUTES);
        sessionManager.bindStompAuth("sess-101", "doctor1", "jti-valid-token", issuedAt, validExpiry);

        when(tokenRevocationService.isRevoked("jti-valid-token", "doctor1", issuedAt)).thenReturn(false);

        // Run scanner
        sessionManager.scanAndEvictExpiredOrRevokedSessions();

        // Verify session was NOT closed
        verify(webSocketSession, never()).close(any(CloseStatus.class));
        verify(messagingTemplate, never()).convertAndSendToUser(any(), any(), any(Map.class));
    }
}
