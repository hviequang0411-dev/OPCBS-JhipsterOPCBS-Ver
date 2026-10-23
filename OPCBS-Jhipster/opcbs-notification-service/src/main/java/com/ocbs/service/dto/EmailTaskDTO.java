package com.ocbs.service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Task command payload for Asynchronous Email Delivery via RabbitMQ Worker Queue.
 * Queue: opcbs.email.dispatch.queue
 * DLQ: opcbs.email.dlq
 *
 * Designed for competing worker processing, rate-limited delivery, and isolated failure handling.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EmailTaskDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("taskId")
    private String taskId = UUID.randomUUID().toString();

    @JsonProperty("to")
    private String to;

    @JsonProperty("subject")
    private String subject;

    @JsonProperty("content")
    private String content;

    @JsonProperty("isMultipart")
    private boolean isMultipart = false;

    @JsonProperty("isHtml")
    private boolean isHtml = true;

    @JsonProperty("idempotencyKey")
    private String idempotencyKey;

    @JsonProperty("createdAt")
    private Instant createdAt = Instant.now();

    @JsonProperty("retryCount")
    private int retryCount = 0;

    @JsonProperty("emailCategory")
    private String emailCategory; // ACTIVATION_OTP, APPOINTMENT_ALERT, PAYMENT_INVOICE

    public EmailTaskDTO() {}

    public EmailTaskDTO(String to, String subject, String content, boolean isMultipart, boolean isHtml, String emailCategory) {
        this.taskId = UUID.randomUUID().toString();
        this.to = to;
        this.subject = subject;
        this.content = content;
        this.isMultipart = isMultipart;
        this.isHtml = isHtml;
        this.emailCategory = emailCategory;
        this.createdAt = Instant.now();
        this.idempotencyKey = "EMAIL_" + (to != null ? to.hashCode() : "") + "_" + (subject != null ? subject.hashCode() : "") + "_" + createdAt.toEpochMilli();
    }

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }

    public String getTo() { return to; }
    public void setTo(String to) { this.to = to; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public boolean isMultipart() { return isMultipart; }
    public void setMultipart(boolean multipart) { isMultipart = multipart; }

    public boolean isHtml() { return isHtml; }
    public void setHtml(boolean html) { isHtml = html; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }

    public String getEmailCategory() { return emailCategory; }
    public void setEmailCategory(String emailCategory) { this.emailCategory = emailCategory; }

    @Override
    public String toString() {
        return "EmailTaskDTO{" +
            "taskId='" + taskId + '\'' +
            ", to='" + to + '\'' +
            ", subject='" + subject + '\'' +
            ", category='" + emailCategory + '\'' +
            ", retries=" + retryCount +
            '}';
    }
}
