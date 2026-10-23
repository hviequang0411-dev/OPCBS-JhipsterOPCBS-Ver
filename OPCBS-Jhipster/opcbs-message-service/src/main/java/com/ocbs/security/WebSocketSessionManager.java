package com.ocbs.security;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

/**
 * Manages active WebSocket connections and monitors token expiration and revocation in real-time.
 * Guarantees that open WebSocket TCP sessions do NOT continue receiving messages once the JWT expires or is revoked.
 */
@Component
public class WebSocketSessionManager {

    private static final Logger LOG = LoggerFactory.getLogger(WebSocketSessionManager.class);

    public static final CloseStatus CLOSE_STATUS_TOKEN_EXPIRED = new CloseStatus(4001, "Token expired");
    public static final CloseStatus CLOSE_STATUS_TOKEN_REVOKED = new CloseStatus(4002, "Token revoked");

    private final TokenRevocationService tokenRevocationService;
    private final SimpMessagingTemplate messagingTemplate;

    // Map storing underlying WebSocketSession by webSocketSession.getId()
    private final Map<String, WebSocketSession> rawSessions = new ConcurrentHashMap<>();

    // Map storing session authentication metadata by STOMP sessionId or WebSocket sessionId
    private final Map<String, StompSessionAuthInfo> sessionAuthInfos = new ConcurrentHashMap<>();

    public WebSocketSessionManager(
        TokenRevocationService tokenRevocationService,
        @Lazy SimpMessagingTemplate messagingTemplate
    ) {
        this.tokenRevocationService = tokenRevocationService;
        this.messagingTemplate = messagingTemplate;
    }

    public static class StompSessionAuthInfo {
        private final String sessionId;
        private final String username;
        private final String jti;
        private final Instant issuedAt;
        private final Instant expiresAt;

        public StompSessionAuthInfo(String sessionId, String username, String jti, Instant issuedAt, Instant expiresAt) {
            this.sessionId = sessionId;
            this.username = username;
            this.jti = jti;
            this.issuedAt = issuedAt;
            this.expiresAt = expiresAt;
        }

        public String getSessionId() {
            return sessionId;
        }

        public String getUsername() {
            return username;
        }

        public String getJti() {
            return jti;
        }

        public Instant getIssuedAt() {
            return issuedAt;
        }

        public Instant getExpiresAt() {
            return expiresAt;
        }
    }

    public void registerRawWebSocketSession(WebSocketSession session) {
        if (session != null && session.isOpen()) {
            rawSessions.put(session.getId(), session);
            LOG.debug("Registered raw WebSocket session: {}", session.getId());
        }
    }

    public void unregisterRawWebSocketSession(String sessionId) {
        if (sessionId != null) {
            rawSessions.remove(sessionId);
            sessionAuthInfos.remove(sessionId);
            LOG.debug("Unregistered raw WebSocket session: {}", sessionId);
        }
    }

    public void bindStompAuth(String sessionId, String username, String jti, Instant issuedAt, Instant expiresAt) {
        if (sessionId == null) return;
        StompSessionAuthInfo authInfo = new StompSessionAuthInfo(sessionId, username, jti, issuedAt, expiresAt);
        sessionAuthInfos.put(sessionId, authInfo);
        LOG.info("Bound STOMP auth for user '{}' on session '{}' (ExpiresAt: {})",
            username, sessionId, expiresAt != null ? expiresAt.toString() : "N/A");
    }

    public void unregisterStompSession(String sessionId) {
        if (sessionId != null) {
            sessionAuthInfos.remove(sessionId);
            rawSessions.remove(sessionId);
        }
    }

    /**
     * Periodic scanner running every 5 seconds to actively detect and terminate expired or revoked WebSocket sessions.
     */
    @Scheduled(fixedDelay = 5000)
    public void scanAndEvictExpiredOrRevokedSessions() {
        if (sessionAuthInfos.isEmpty()) {
            return;
        }

        Instant now = Instant.now();

        for (Map.Entry<String, StompSessionAuthInfo> entry : sessionAuthInfos.entrySet()) {
            String sessionId = entry.getKey();
            StompSessionAuthInfo auth = entry.getValue();

            // 1. Check if token expired
            if (auth.getExpiresAt() != null && now.isAfter(auth.getExpiresAt())) {
                LOG.warn("🚨 [Session Eviction] WebSocket session '{}' for user '{}' expired at {}. Terminating session.",
                    sessionId, auth.getUsername(), auth.getExpiresAt());
                terminateSession(sessionId, auth.getUsername(), CLOSE_STATUS_TOKEN_EXPIRED, "SESSION_EXPIRED",
                    "Phiên đăng nhập đã hết hạn. Vui lòng làm mới trang hoặc đăng nhập lại.");
                continue;
            }

            // 2. Check if token is revoked in Redis blacklist
            if (tokenRevocationService.isRevoked(auth.getJti(), auth.getUsername(), auth.getIssuedAt())) {
                LOG.warn("🚨 [Session Eviction] WebSocket session '{}' for user '{}' has been revoked. Terminating session.",
                    sessionId, auth.getUsername());
                terminateSession(sessionId, auth.getUsername(), CLOSE_STATUS_TOKEN_REVOKED, "SESSION_REVOKED",
                    "Phiên đăng nhập đã bị thu hồi. Vui lòng đăng nhập lại.");
            }
        }
    }

    private void terminateSession(String sessionId, String username, CloseStatus status, String errorCode, String errorMessage) {
        // 1. Notify user queue before terminating connection
        try {
            if (username != null && messagingTemplate != null) {
                messagingTemplate.convertAndSendToUser(
                    username,
                    "/queue/errors",
                    Map.of(
                        "status", "ERROR",
                        "code", errorCode,
                        "message", errorMessage
                    )
                );
            }
        } catch (Exception ex) {
            LOG.debug("Could not deliver disconnect error notification to user '{}': {}", username, ex.getMessage());
        }

        // 2. Close physical WebSocket session
        WebSocketSession rawSession = rawSessions.get(sessionId);
        if (rawSession != null && rawSession.isOpen()) {
            try {
                rawSession.close(status);
                LOG.info("Closed physical WebSocket connection '{}' with status {}", sessionId, status);
            } catch (IOException e) {
                LOG.warn("Error closing WebSocket connection '{}': {}", sessionId, e.getMessage());
            }
        }

        // 3. Remove from tracking
        unregisterStompSession(sessionId);
    }
}
