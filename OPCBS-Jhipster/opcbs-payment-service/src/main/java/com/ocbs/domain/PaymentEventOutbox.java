package com.ocbs.domain;

import jakarta.persistence.*;
import java.time.Instant;

/** Durable event record written with payment state changes before Kafka publication. */
@Entity
@Table(name = "payment_event_outbox")
public class PaymentEventOutbox {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "event_id", nullable = false, unique = true, length = 100) private String eventId;
    @Column(name = "event_type", nullable = false, length = 80) private String eventType;
    @Column(name = "aggregate_key", nullable = false, length = 100) private String aggregateKey;
    @Lob @Column(name = "payload", nullable = false) private String payload;
    @Column(name = "status", nullable = false, length = 20) private String status = "PENDING";
    @Column(name = "retry_count", nullable = false) private int retryCount;
    @Column(name = "next_retry_at") private Instant nextRetryAt;
    @Column(name = "created_at", nullable = false) private Instant createdAt = Instant.now();
    @Column(name = "sent_at") private Instant sentAt;
    @Column(name = "error_message", length = 1000) private String errorMessage;

    public Long getId() { return id; }
    public String getEventId() { return eventId; }
    public void setEventId(String value) { eventId = value; }
    public String getEventType() { return eventType; }
    public void setEventType(String value) { eventType = value; }
    public String getAggregateKey() { return aggregateKey; }
    public void setAggregateKey(String value) { aggregateKey = value; }
    public String getPayload() { return payload; }
    public void setPayload(String value) { payload = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }
    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int value) { retryCount = value; }
    public Instant getNextRetryAt() { return nextRetryAt; }
    public void setNextRetryAt(Instant value) { nextRetryAt = value; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getSentAt() { return sentAt; }
    public void setSentAt(Instant value) { sentAt = value; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String value) { errorMessage = value; }
}
