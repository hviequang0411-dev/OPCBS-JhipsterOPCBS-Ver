package com.ocbs.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.ocbs.service.ChatMessageService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class WebSocketAuthChannelInterceptorTest {

    @Mock
    private JwtDecoder jwtDecoder;

    @Mock
    private ChatMessageService chatMessageService;

    @Mock
    private TokenRevocationService tokenRevocationService;

    @Mock
    private WebSocketSessionManager sessionManager;

    @Mock
    private MessageChannel channel;

    @InjectMocks
    private WebSocketAuthChannelInterceptor interceptor;

    private Jwt validJwt;
    private Jwt expiredJwt;

    @BeforeEach
    void setUp() {
        validJwt = Jwt.withTokenValue("valid-token")
            .header("alg", "RS256")
            .claim("sub", "doctor1")
            .claim("jti", "jti-123456")
            .claim("auth", "ROLE_DOCTOR")
            .issuedAt(Instant.now().minus(5, ChronoUnit.MINUTES))
            .expiresAt(Instant.now().plus(55, ChronoUnit.MINUTES))
            .build();

        expiredJwt = Jwt.withTokenValue("expired-token")
            .header("alg", "RS256")
            .claim("sub", "doctor1")
            .claim("jti", "jti-999999")
            .claim("auth", "ROLE_DOCTOR")
            .issuedAt(Instant.now().minus(2, ChronoUnit.HOURS))
            .expiresAt(Instant.now().minus(1, ChronoUnit.HOURS))
            .build();
    }

    private Message<byte[]> buildMessage(StompCommand command, String destination, Jwt jwt) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        if (jwt != null) {
            accessor.setUser(new JwtAuthenticationToken(jwt, Collections.emptyList(), jwt.getSubject()));
        }
        if (destination != null) {
            accessor.setDestination(destination);
        }
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    @Test
    @DisplayName("6a. Allows SUBSCRIBE to permitted user queues: /user/queue/ack and /user/queue/errors")
    void handleSubscribe_allowsPermittedUserQueues() {
        // Test /user/queue/ack
        Message<byte[]> msg1 = buildMessage(StompCommand.SUBSCRIBE, "/user/queue/ack", validJwt);
        Message<?> result1 = interceptor.preSend(msg1, channel);
        assertThat(result1).isNotNull();

        // Test /user/queue/errors
        Message<byte[]> msg2 = buildMessage(StompCommand.SUBSCRIBE, "/user/queue/errors", validJwt);
        Message<?> result2 = interceptor.preSend(msg2, channel);
        assertThat(result2).isNotNull();
    }

    @Test
    @DisplayName("6b. Rejects SUBSCRIBE to other /user/** destinations outside allowlist")
    void handleSubscribe_rejectsUnauthorizedUserQueues() {
        Message<byte[]> msg1 = buildMessage(StompCommand.SUBSCRIBE, "/user/queue/unauthorized-queue", validJwt);
        assertThatThrownBy(() -> interceptor.preSend(msg1, channel))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessageContaining("prohibited");

        Message<byte[]> msg2 = buildMessage(StompCommand.SUBSCRIBE, "/user/topic/some-topic", validJwt);
        assertThatThrownBy(() -> interceptor.preSend(msg2, channel))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessageContaining("prohibited");
    }

    @Test
    @DisplayName("6c. Rejects client direct SEND to broker destinations (/topic/**, /queue/**, /user/**)")
    void handleSend_rejectsDirectSendToBrokerDestinations() {
        // Direct SEND to /topic
        Message<byte[]> msg1 = buildMessage(StompCommand.SEND, "/topic/conversations/100", validJwt);
        assertThatThrownBy(() -> interceptor.preSend(msg1, channel))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessageContaining("Direct publishing to broker destinations is strictly prohibited");

        // Direct SEND to /queue
        Message<byte[]> msg2 = buildMessage(StompCommand.SEND, "/queue/ack", validJwt);
        assertThatThrownBy(() -> interceptor.preSend(msg2, channel))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessageContaining("Direct publishing to broker destinations is strictly prohibited");

        // Direct SEND to /user
        Message<byte[]> msg3 = buildMessage(StompCommand.SEND, "/user/queue/errors", validJwt);
        assertThatThrownBy(() -> interceptor.preSend(msg3, channel))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessageContaining("Direct publishing to broker destinations is strictly prohibited");
    }

    @Test
    @DisplayName("6d. Allows client SEND to /app/conversations/{id}/send when user is a valid participant")
    void handleSend_allowsAuthorizedParticipant() {
        when(chatMessageService.isUserParticipantOrAdmin(100L, "doctor1")).thenReturn(true);

        Message<byte[]> msg = buildMessage(StompCommand.SEND, "/app/conversations/100/send", validJwt);
        Message<?> result = interceptor.preSend(msg, channel);
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("7a. Rejects STOMP CONNECT when JWT token has expired")
    void handleConnect_rejectsExpiredToken() {
        when(jwtDecoder.decode("expired-token")).thenReturn(expiredJwt);

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer expired-token");
        Message<byte[]> msg = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThatThrownBy(() -> interceptor.preSend(msg, channel))
            .isInstanceOf(BadCredentialsException.class)
            .hasMessageContaining("expired");
    }

    @Test
    @DisplayName("7b. Rejects STOMP CONNECT when token is revoked in Redis blacklist")
    void handleConnect_rejectsRevokedToken() {
        when(jwtDecoder.decode("valid-token")).thenReturn(validJwt);
        when(tokenRevocationService.isRevoked("jti-123456", "doctor1", validJwt.getIssuedAt())).thenReturn(true);

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer valid-token");
        Message<byte[]> msg = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThatThrownBy(() -> interceptor.preSend(msg, channel))
            .isInstanceOf(BadCredentialsException.class)
            .hasMessageContaining("revoked");
    }

    @Test
    @DisplayName("7c. Rejects SEND during active session when token has expired")
    void handleSend_rejectsExpiredActiveSession() {
        Message<byte[]> msg = buildMessage(StompCommand.SEND, "/app/conversations/100/send", expiredJwt);

        assertThatThrownBy(() -> interceptor.preSend(msg, channel))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessageContaining("Session expired");
    }

    @Test
    @DisplayName("7d. Rejects SEND during active session when token was revoked (e.g. user logged out)")
    void handleSend_rejectsRevokedActiveSession() {
        when(tokenRevocationService.isRevoked("jti-123456", "doctor1", validJwt.getIssuedAt())).thenReturn(true);

        Message<byte[]> msg = buildMessage(StompCommand.SEND, "/app/conversations/100/send", validJwt);

        assertThatThrownBy(() -> interceptor.preSend(msg, channel))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessageContaining("Session revoked");
    }
}
