package com.ocbs.web.websocket;

import com.ocbs.domain.ChatMessage;
import com.ocbs.service.ChatMessageService;
import com.ocbs.web.websocket.dto.SendMessagePayload;
import java.security.Principal;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

/**
 * WebSocket STOMP Controller for Real-Time Conversation Chat Messaging.
 * Guarantees MySQL persistence prior to broadcasting to the conversation topic.
 */
@Controller
public class ConversationWebSocketController {

    private static final Logger LOG = LoggerFactory.getLogger(ConversationWebSocketController.class);

    private final ChatMessageService chatMessageService;
    private final SimpMessagingTemplate messagingTemplate;

    public ConversationWebSocketController(
        ChatMessageService chatMessageService,
        SimpMessagingTemplate messagingTemplate
    ) {
        this.chatMessageService = chatMessageService;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Handles real-time message sending over STOMP.
     * Destination: /app/conversations/{conversationId}/send
     * Broadcasts to: /topic/conversations/{conversationId} upon successful DB save.
     * Sends Acknowledgement to: /user/queue/ack
     */
    @MessageMapping("/conversations/{conversationId}/send")
    public void sendMessage(
        @DestinationVariable Long conversationId,
        @Payload SendMessagePayload payload,
        Principal principal
    ) {
        if (principal == null || principal.getName() == null) {
            LOG.warn("🚨 [STOMP Controller] Anonymous send blocked for conversation #{}", conversationId);
            return;
        }

        String senderLogin = principal.getName();
        String content = payload != null ? payload.getContent() : null;
        String clientMessageId = payload != null ? payload.getClientMessageId() : null;

        if (content == null || content.trim().isEmpty()) {
            LOG.warn("Rejected blank STOMP message in conversation #{} by user '{}'", conversationId, senderLogin);
            messagingTemplate.convertAndSendToUser(
                senderLogin,
                "/queue/errors",
                Map.of(
                    "status", "ERROR",
                    "code", "BLANK_CONTENT",
                    "message", "Nội dung tin nhắn không được để trống",
                    "clientMessageId", clientMessageId != null ? clientMessageId : ""
                )
            );
            return;
        }

        if (content.length() > 4000) {
            LOG.warn("Rejected overly long STOMP message in conversation #{} by user '{}'", conversationId, senderLogin);
            messagingTemplate.convertAndSendToUser(
                senderLogin,
                "/queue/errors",
                Map.of(
                    "status", "ERROR",
                    "code", "CONTENT_TOO_LONG",
                    "message", "Nội dung tin nhắn vượt quá giới hạn 4000 ký tự",
                    "clientMessageId", clientMessageId != null ? clientMessageId : ""
                )
            );
            return;
        }

        try {
            // 1. Strictly persist to MySQL database first (with idempotency deduplication and authorization check)
            ChatMessageService.SaveMessageResult result = chatMessageService.saveMessageWithResult(
                conversationId, senderLogin, content, clientMessageId
            );
            ChatMessage savedMessage = result.getMessage();

            // 2. Broadcast to topic subscribers only for newly created messages (avoid re-broadcasting duplicates)
            String topicDestination = "/topic/conversations/" + conversationId;
            if (result.isNew()) {
                messagingTemplate.convertAndSend(topicDestination, savedMessage);
                LOG.info("Broadcasted new chat message #{} to destination '{}' (ClientMessageId: '{}')",
                    savedMessage.getId(), topicDestination, clientMessageId != null ? clientMessageId : "N/A");
            } else {
                LOG.info("⚡ [IDEMPOTENCY] Replay detected for clientMessageId '{}'. Skipped broadcast to avoid duplicates.",
                    clientMessageId != null ? clientMessageId : "N/A");
            }

            // 3. Send explicit ACK to the sender (both for new and idempotent replayed messages)
            if (clientMessageId != null && !clientMessageId.isBlank()) {
                messagingTemplate.convertAndSendToUser(
                    senderLogin,
                    "/queue/ack",
                    com.ocbs.web.websocket.dto.ChatMessageAckDTO.success(
                        clientMessageId,
                        savedMessage.getId(),
                        conversationId,
                        savedMessage.getCreatedDate()
                    )
                );
            }

        } catch (org.springframework.security.access.AccessDeniedException e) {
            LOG.warn("Access denied sending message to conversation #{}: {}", conversationId, e.getMessage());
            messagingTemplate.convertAndSendToUser(
                senderLogin,
                "/queue/errors",
                Map.of(
                    "status", "ERROR",
                    "code", "FORBIDDEN",
                    "message", "Bạn không có quyền gửi tin nhắn trong cuộc trò chuyện này",
                    "clientMessageId", clientMessageId != null ? clientMessageId : ""
                )
            );
        } catch (Exception e) {
            LOG.error("Failed to persist and broadcast message in conversation #{}: {}", conversationId, e.getMessage());
            messagingTemplate.convertAndSendToUser(
                senderLogin,
                "/queue/errors",
                Map.of(
                    "status", "ERROR",
                    "code", "INTERNAL_ERROR",
                    "message", "Không thể lưu tin nhắn do sự cố máy chủ. Vui lòng thử lại.",
                    "clientMessageId", clientMessageId != null ? clientMessageId : ""
                )
            );
        }
    }

    @MessageExceptionHandler
    @SendToUser("/queue/errors")
    public Map<String, String> handleException(Exception exception, Principal principal) {
        String username = principal != null ? principal.getName() : "anonymous";
        LOG.warn("STOMP processing error for user '{}': {}", username, exception.getMessage());
        return Map.of(
            "status", "ERROR",
            "code", "STOMP_ERROR",
            "message", "Yêu cầu WebSocket không hợp lệ hoặc đã xảy ra lỗi máy chủ."
        );
    }
}
