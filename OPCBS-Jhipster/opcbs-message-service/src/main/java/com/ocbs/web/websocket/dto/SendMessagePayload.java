package com.ocbs.web.websocket.dto;

import java.io.Serializable;

/**
 * DTO for sending a chat message over WebSocket STOMP.
 */
public class SendMessagePayload implements Serializable {

    private static final long serialVersionUID = 1L;

    private String content;
    private String clientMessageId;

    public SendMessagePayload() {}

    public SendMessagePayload(String content) {
        this.content = content;
    }

    public SendMessagePayload(String content, String clientMessageId) {
        this.content = content;
        this.clientMessageId = clientMessageId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getClientMessageId() {
        return clientMessageId;
    }

    public void setClientMessageId(String clientMessageId) {
        this.clientMessageId = clientMessageId;
    }
}
