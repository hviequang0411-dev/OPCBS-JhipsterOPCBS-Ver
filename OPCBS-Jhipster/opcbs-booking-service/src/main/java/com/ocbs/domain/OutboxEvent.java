package com.ocbs.domain;

import com.ocbs.domain.enumeration.OutboxStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.time.Instant;

/**
 * Transactional Outbox Entity for persistent appointment event dispatching.
 */
@Entity
@Table(name = "outbox_event")
public class OutboxEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull
    @Size(max = 100)
    @Column(name = "event_id", length = 100, nullable = false, unique = true)
    private String eventId;

    @NotNull
    @Size(max = 100)
    @Column(name = "event_type", length = 100, nullable = false)
    private String eventType;

    @NotNull
    @Size(max = 20)
    @Column(name = "event_version", length = 20, nullable = false)
    private String eventVersion = "1.0";

    @NotNull
    @Size(max = 50)
    @Column(name = "aggregate_type", length = 50, nullable = false)
    private String aggregateType = "APPOINTMENT";

    @NotNull
    @Column(name = "aggregate_id", nullable = false)
    private Long aggregateId;

    @NotNull
    @Size(max = 150)
    @Column(name = "idempotency_key", length = 150, nullable = false)
    private String idempotencyKey;

    @NotNull
    @Lob
    @Column(name = "payload", nullable = false)
    private String payload;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private OutboxStatus status = OutboxStatus.PENDING;

    @NotNull
    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    @Column(name = "last_retry_at")
    private Instant lastRetryAt;

    @Column(name = "next_retry_at")
    private Instant nextRetryAt;

    @Size(max = 1000)
    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "sent_at")
    private Instant sentAt;

    public OutboxEvent() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getEventVersion() { return eventVersion; }
    public void setEventVersion(String eventVersion) { this.eventVersion = eventVersion; }

    public String getAggregateType() { return aggregateType; }
    public void setAggregateType(String aggregateType) { this.aggregateType = aggregateType; }

    public Long getAggregateId() { return aggregateId; }
    public void setAggregateId(Long aggregateId) { this.aggregateId = aggregateId; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }

    public OutboxStatus getStatus() { return status; }
    public void setStatus(OutboxStatus status) { this.status = status; }

    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }

    public Instant getLastRetryAt() { return lastRetryAt; }
    public void setLastRetryAt(Instant lastRetryAt) { this.lastRetryAt = lastRetryAt; }

    public Instant getNextRetryAt() { return nextRetryAt; }
    public void setNextRetryAt(Instant nextRetryAt) { this.nextRetryAt = nextRetryAt; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getSentAt() { return sentAt; }
    public void setSentAt(Instant sentAt) { this.sentAt = sentAt; }

    @Override
    public String toString() {
        return "OutboxEvent{" +
            "id=" + id +
            ", eventId='" + eventId + '\'' +
            ", eventType='" + eventType + '\'' +
            ", aggregateId=" + aggregateId +
            ", status=" + status +
            ", retryCount=" + retryCount +
            '}';
    }
}
