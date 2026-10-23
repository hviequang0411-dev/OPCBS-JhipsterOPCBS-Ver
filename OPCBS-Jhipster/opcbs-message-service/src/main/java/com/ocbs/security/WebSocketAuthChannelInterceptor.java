package com.ocbs.security;

import com.ocbs.service.ChatMessageService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * STOMP Channel Interceptor for enforcing strict RS256 JWT Authentication and Participant Authorization.
 * Guarantees no anonymous access, validates claims, and prevents impersonation.
 */
@Component
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private static final Logger LOG = LoggerFactory.getLogger(WebSocketAuthChannelInterceptor.class);

    private final JwtDecoder jwtDecoder;
    private final ChatMessageService chatMessageService;
    private final TokenRevocationService tokenRevocationService;
    private final WebSocketSessionManager sessionManager;

    private static final java.util.regex.Pattern SEND_DESTINATION_PATTERN =
        java.util.regex.Pattern.compile("^/app/conversations/([0-9]+)/send$");

    private static final java.util.regex.Pattern TOPIC_DESTINATION_PATTERN =
        java.util.regex.Pattern.compile("^/topic/conversations/([0-9]+)$");

    public WebSocketAuthChannelInterceptor(
        JwtDecoder jwtDecoder,
        ChatMessageService chatMessageService,
        TokenRevocationService tokenRevocationService,
        WebSocketSessionManager sessionManager
    ) {
        this.jwtDecoder = jwtDecoder;
        this.chatMessageService = chatMessageService;
        this.tokenRevocationService = tokenRevocationService;
        this.sessionManager = sessionManager;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();
        if (StompCommand.CONNECT.equals(command)) {
            handleConnect(accessor);
        } else if (StompCommand.SUBSCRIBE.equals(command)) {
            handleSubscribe(accessor);
        } else if (StompCommand.SEND.equals(command)) {
            handleSend(accessor);
        } else if (StompCommand.DISCONNECT.equals(command)) {
            handleDisconnect(accessor);
        }

        return message;
    }

    private void handleConnect(StompHeaderAccessor accessor) {
        String token = extractToken(accessor);
        if (token == null || token.isBlank()) {
            LOG.warn("🚨 [STOMP Auth] Connection rejected: Missing JWT token in STOMP headers");
            throw new AuthenticationCredentialsNotFoundException("STOMP Authentication failed: Missing JWT token");
        }

        try {
            Jwt jwt = jwtDecoder.decode(token);
            String username = jwt.getSubject();
            if (username == null || username.isBlank()) {
                LOG.warn("🚨 [STOMP Auth] Connection rejected: JWT subject is null");
                throw new BadCredentialsException("STOMP Authentication failed: Missing subject claim");
            }

            // 1. Verify token expiration at connect time
            if (jwt.getExpiresAt() != null && java.time.Instant.now().isAfter(jwt.getExpiresAt())) {
                LOG.warn("🚨 [STOMP Auth] Connection rejected: JWT token expired for user '{}'", username);
                throw new BadCredentialsException("STOMP Authentication failed: Token has expired");
            }

            // 2. Verify token revocation (Redis blacklist & user logout) at connect time
            if (tokenRevocationService.isRevoked(jwt.getId(), username, jwt.getIssuedAt())) {
                LOG.warn("🚨 [STOMP Auth] Connection rejected: JWT token has been revoked for user '{}'", username);
                throw new BadCredentialsException("STOMP Authentication failed: Token has been revoked");
            }

            Collection<GrantedAuthority> authorities = extractAuthorities(jwt);
            JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt, authorities, username);
            accessor.setUser(authentication);

            // 3. Register session in WebSocketSessionManager for active expiration and revocation surveillance
            sessionManager.bindStompAuth(accessor.getSessionId(), username, jwt.getId(), jwt.getIssuedAt(), jwt.getExpiresAt());

            LOG.info("✅ [STOMP Auth] STOMP Client connected successfully as user '{}' with authorities {}",
                username, authorities);
        } catch (JwtException e) {
            LOG.warn("🚨 [STOMP Auth] Connection rejected: Invalid or expired JWT token ({})", e.getMessage());
            throw new BadCredentialsException("STOMP Authentication failed: Invalid or expired token", e);
        } catch (Exception e) {
            LOG.warn("🚨 [STOMP Auth] Connection rejected: Unexpected authentication error ({})", e.getMessage());
            throw new BadCredentialsException("STOMP Authentication failed: " + e.getMessage(), e);
        }
    }

    private void handleDisconnect(StompHeaderAccessor accessor) {
        String sessionId = accessor.getSessionId();
        if (sessionId != null) {
            sessionManager.unregisterStompSession(sessionId);
        }
    }

    private void verifySessionActive(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Access denied: Anonymous access is strictly prohibited");
        }
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            // 1. Check expiration
            if (jwt.getExpiresAt() != null && java.time.Instant.now().isAfter(jwt.getExpiresAt())) {
                LOG.warn("🚨 [STOMP Security] Token expired during active WebSocket session for user '{}'", auth.getName());
                throw new AccessDeniedException("Session expired: JWT token is no longer valid. Please re-authenticate.");
            }
            // 2. Check revocation
            if (tokenRevocationService.isRevoked(jwt.getId(), auth.getName(), jwt.getIssuedAt())) {
                LOG.warn("🚨 [STOMP Security] Token revoked during active WebSocket session for user '{}'", auth.getName());
                throw new AccessDeniedException("Session revoked: Token has been invalidated. Please re-authenticate.");
            }
        }
    }

    private void handleSubscribe(StompHeaderAccessor accessor) {
        Authentication auth = (Authentication) accessor.getUser();
        verifySessionActive(auth);

        String destination = accessor.getDestination();
        if (destination == null || destination.isBlank()) {
            LOG.warn("🚨 [STOMP Security] User '{}' attempted SUBSCRIBE with null/blank destination", auth.getName());
            throw new AccessDeniedException("Access denied: Destination cannot be empty");
        }

        // 1. Allow ONLY recognized user queues: /user/queue/ack and /user/queue/errors
        if (destination.startsWith("/user/")) {
            if ("/user/queue/ack".equals(destination) || "/user/queue/errors".equals(destination)) {
                return;
            }
            LOG.warn("🚨 [STOMP Security] User '{}' attempted SUBSCRIBE to unauthorized user destination '{}'",
                auth.getName(), destination);
            throw new AccessDeniedException("Access denied: Subscription to user destination '" + destination + "' is prohibited");
        }

        // 2. Allow topic subscription only to conversation topic if user is an authorized participant or Admin
        java.util.regex.Matcher matcher = TOPIC_DESTINATION_PATTERN.matcher(destination);
        if (matcher.matches()) {
            Long conversationId;
            try {
                conversationId = Long.parseLong(matcher.group(1));
            } catch (NumberFormatException e) {
                LOG.warn("🚨 [STOMP Security] Invalid conversation ID in SUBSCRIBE destination: {}", destination);
                throw new AccessDeniedException("Access denied: Invalid conversation ID format");
            }

            boolean authorized = chatMessageService.isUserParticipantOrAdmin(conversationId, auth.getName());
            if (!authorized) {
                LOG.warn("🚨 [STOMP Security Alert] User '{}' unauthorized to SUBSCRIBE to conversation #{}",
                    auth.getName(), conversationId);
                throw new AccessDeniedException("Access denied: Not a participant of conversation #" + conversationId);
            }
            LOG.debug("User '{}' authorized to SUBSCRIBE to conversation #{}", auth.getName(), conversationId);
            return;
        }

        // 3. Reject any other destination
        LOG.warn("🚨 [STOMP Security] User '{}' attempted SUBSCRIBE to forbidden destination '{}'",
            auth.getName(), destination);
        throw new AccessDeniedException("Access denied: Unauthorized subscription destination");
    }

    private void handleSend(StompHeaderAccessor accessor) {
        Authentication auth = (Authentication) accessor.getUser();
        verifySessionActive(auth);

        String destination = accessor.getDestination();
        if (destination == null || destination.isBlank()) {
            LOG.warn("🚨 [STOMP Security] User '{}' attempted SEND with null/blank destination", auth.getName());
            throw new AccessDeniedException("Access denied: Destination cannot be empty");
        }

        // 1. CRITICAL: Strictly reject any direct SEND to broker destinations (/topic/**, /queue/**, /user/**)
        if (destination.startsWith("/topic/") || destination.startsWith("/queue/") || destination.startsWith("/user/")) {
            LOG.warn("🚨 [STOMP Security Violation] Client '{}' attempted direct publishing to broker destination '{}'",
                auth.getName(), destination);
            throw new AccessDeniedException("Access denied: Direct publishing to broker destinations is strictly prohibited");
        }

        // 2. Only allow destinations under /app/**
        if (!destination.startsWith("/app/")) {
            LOG.warn("🚨 [STOMP Security] Client '{}' sent message to unrecognized prefix: '{}'",
                auth.getName(), destination);
            throw new AccessDeniedException("Access denied: Destination must begin with /app/");
        }

        // 3. Validate against recognized SEND pattern: /app/conversations/{conversationId}/send
        java.util.regex.Matcher matcher = SEND_DESTINATION_PATTERN.matcher(destination);
        if (!matcher.matches()) {
            LOG.warn("🚨 [STOMP Security] Client '{}' sent message to unrecognized or malformed destination: '{}'",
                auth.getName(), destination);
            throw new AccessDeniedException("Access denied: Unrecognized destination format");
        }

        Long conversationId;
        try {
            conversationId = Long.parseLong(matcher.group(1));
        } catch (NumberFormatException e) {
            LOG.warn("🚨 [STOMP Security] Client '{}' provided malformed conversation ID in destination '{}'",
                auth.getName(), destination);
            throw new AccessDeniedException("Access denied: Invalid conversation ID format");
        }

        // 4. Verify user is a participant of the conversation or Admin
        boolean authorized = chatMessageService.isUserParticipantOrAdmin(conversationId, auth.getName());
        if (!authorized) {
            LOG.warn("🚨 [STOMP Security Alert] User '{}' unauthorized to SEND to conversation #{}",
                auth.getName(), conversationId);
            throw new AccessDeniedException("Access denied: Not a participant of conversation #" + conversationId);
        }
    }

    private String extractToken(StompHeaderAccessor accessor) {
        // 1. Check native "Authorization" or "authorization" header
        List<String> authHeaders = accessor.getNativeHeader("Authorization");
        if (authHeaders == null || authHeaders.isEmpty()) {
            authHeaders = accessor.getNativeHeader("authorization");
        }
        if (authHeaders != null && !authHeaders.isEmpty()) {
            String header = authHeaders.get(0);
            if (header != null && header.toLowerCase().startsWith("bearer ")) {
                return header.substring(7).trim();
            }
            return header.trim();
        }

        // 2. Check native "token" header
        List<String> tokenHeaders = accessor.getNativeHeader("token");
        if (tokenHeaders != null && !tokenHeaders.isEmpty()) {
            return tokenHeaders.get(0).trim();
        }

        // 3. Check passcode if sent via standard STOMP connect frame
        String passcode = accessor.getPasscode();
        if (passcode != null && !passcode.isBlank()) {
            if (passcode.toLowerCase().startsWith("bearer ")) {
                return passcode.substring(7).trim();
            }
            return passcode.trim();
        }

        return null;
    }

    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        Object authClaim = jwt.getClaims().get("auth");
        if (authClaim == null) {
            authClaim = jwt.getClaims().get("roles");
        }
        if (authClaim == null) {
            authClaim = jwt.getClaims().get("scope");
        }

        if (authClaim instanceof String authStr) {
            return Arrays.stream(authStr.split("[,\\s]+"))
                .filter(s -> !s.isBlank())
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
        } else if (authClaim instanceof Collection<?> authList) {
            List<GrantedAuthority> authorities = new ArrayList<>();
            for (Object item : authList) {
                if (item != null) {
                    authorities.add(new SimpleGrantedAuthority(item.toString()));
                }
            }
            return authorities;
        }

        return Collections.emptyList();
    }

    private Long extractConversationId(String destination, String prefix) {
        try {
            String remainder = destination.substring(prefix.length());
            int slashIndex = remainder.indexOf('/');
            String idStr = slashIndex != -1 ? remainder.substring(0, slashIndex) : remainder;
            return Long.parseLong(idStr);
        } catch (Exception e) {
            LOG.warn("Failed to extract conversationId from destination: {}", destination);
            return null;
        }
    }

    private Long extractConversationIdFromSend(String destination) {
        // e.g. /app/conversations/{id}/send
        try {
            String prefix = "/app/conversations/";
            String remainder = destination.substring(prefix.length());
            int slashIndex = remainder.indexOf('/');
            String idStr = slashIndex != -1 ? remainder.substring(0, slashIndex) : remainder;
            return Long.parseLong(idStr);
        } catch (Exception e) {
            LOG.warn("Failed to extract conversationId from SEND destination: {}", destination);
            return null;
        }
    }
}
