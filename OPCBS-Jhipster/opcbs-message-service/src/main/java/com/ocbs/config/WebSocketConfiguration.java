package com.ocbs.config;

import com.ocbs.security.WebSocketAuthChannelInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

/**
 * WebSocket and STOMP Message Broker Configuration for opcbs-message-service.
 * Configured for single-instance in-memory simple broker deployment.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfiguration implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthChannelInterceptor authChannelInterceptor;
    private final ApplicationProperties applicationProperties;
    private final com.ocbs.security.WebSocketSessionManager sessionManager;

    public WebSocketConfiguration(
        WebSocketAuthChannelInterceptor authChannelInterceptor,
        ApplicationProperties applicationProperties,
        com.ocbs.security.WebSocketSessionManager sessionManager
    ) {
        this.authChannelInterceptor = authChannelInterceptor;
        this.applicationProperties = applicationProperties;
        this.sessionManager = sessionManager;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        String[] origins = applicationProperties.getWebsocket().getAllowedOrigins().toArray(new String[0]);

        // Standard WebSocket STOMP endpoint with environment-restricted origins
        registry.addEndpoint("/ws")
            .setAllowedOrigins(origins);

        // SockJS fallback support with environment-restricted origins
        registry.addEndpoint("/ws")
            .setAllowedOrigins(origins)
            .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Heartbeat scheduler for broker-to-client connection liveness check
        ThreadPoolTaskScheduler heartbeatScheduler = new ThreadPoolTaskScheduler();
        heartbeatScheduler.setPoolSize(1);
        heartbeatScheduler.setThreadNamePrefix("ws-heartbeat-");
        heartbeatScheduler.initialize();

        // In-memory simple broker for single instance deployment
        registry.enableSimpleBroker("/topic", "/queue")
            .setTaskScheduler(heartbeatScheduler)
            .setHeartbeatValue(new long[] { 10000, 10000 }); // 10s ping/pong

        // Application prefix for messages routed to @MessageMapping handlers
        registry.setApplicationDestinationPrefixes("/app");

        // Prefix for private user messages
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        ApplicationProperties.Websocket wsProps = applicationProperties.getWebsocket();
        registration.setMessageSizeLimit(wsProps.getMessageSizeLimit());
        registration.setSendBufferSizeLimit(wsProps.getSendBufferSizeLimit());
        registration.setSendTimeLimit(wsProps.getSendTimeLimit());

        // Decorator to intercept physical WebSocketSession lifecycle for expiration/revocation disconnection
        registration.addDecoratorFactory(handler -> new org.springframework.web.socket.handler.WebSocketHandlerDecorator(handler) {
            @Override
            public void afterConnectionEstablished(org.springframework.web.socket.WebSocketSession session) throws Exception {
                sessionManager.registerRawWebSocketSession(session);
                super.afterConnectionEstablished(session);
            }

            @Override
            public void afterConnectionClosed(org.springframework.web.socket.WebSocketSession session, org.springframework.web.socket.CloseStatus closeStatus) throws Exception {
                sessionManager.unregisterRawWebSocketSession(session.getId());
                super.afterConnectionClosed(session, closeStatus);
            }
        });
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Register RS256 JWT Authentication & Participant Authorization interceptor
        registration.interceptors(authChannelInterceptor);
        // Sized for single instance load
        registration.taskExecutor()
            .corePoolSize(2)
            .maxPoolSize(8)
            .queueCapacity(100);
    }

    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        registration.taskExecutor()
            .corePoolSize(2)
            .maxPoolSize(8)
            .queueCapacity(100);
    }
}
