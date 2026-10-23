package com.ocbs.web.websocket.dto;

import java.io.Serializable;
import java.time.Instant;

/**
 * DTO for STOMP Message Acknowledgement protocol.
 * Sent back to the client via /user/queue/ack to confirm receipt and database persistence.
 */
public class ChatMessageAckDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String clientMessageId;
    private Long messageId;
    private Long conversationId;
    private String status; // "SENT", "DUPLICATE", "ERROR"
    private Instant createdDate;
    private String code;
    private String message;

    public ChatMessageAckDTO() {}

    public static ChatMessageAckDTO success(String clientMessageId, Long messageId, Long conversationId, Instant createdDate) {
        ChatMessageAckDTO ack = new ChatMessageAckDTO();
        ack.clientMessageId = clientMessageId;
        ack.messageId = messageId;
        ack.conversationId = conversationId;
        ack.status = "SENT";
        ack.createdDate = createdDate;
        return ack;
    }

    public static ChatMessageAckDTO duplicate(String clientMessageId, Long messageId, Long conversationId, Instant createdDate) {
        ChatMessageAckDTO ack = new ChatMessageAckDTO();
        ack.clientMessageId = clientMessageId;
        ack.messageId = messageId;
        ack.conversationId = conversationId;
        ack.status = "DUPLICATE";
        ack.createdDate = createdDate;
        return ack;
    }

    public static ChatMessageAckDTO error(String clientMessageId, Long conversationId, String code, String message) {
        ChatMessageAckDTO ack = new ChatMessageAckDTO();
        ack.clientMessageId = clientMessageId;
        ack.conversationId = conversationId;
        ack.status = "ERROR";
        ack.code = code;
        ack.message = message;
        return ack;
    }

    public String getClientMessageId() {
        return clientMessageId;
    }

    public void setClientMessageId(String clientMessageId) {
        this.clientMessageId = clientMessageId;
    }

    public Long getMessageId() {
        return messageId;
    }

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
    }

    public Long getConversationId() {
        return conversationId;
    }

    public void setConversationId(Long conversationId) {
        this.conversationId = conversationId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
