package com.ocbs.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.time.Instant;

/**
 * Idempotent Inbox entity for Kafka event processing in Notification Service.
 */
@Entity
@Table(name = "notification_inbox")
public class NotificationInbox implements Serializable {

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
    @Size(max = 150)
    @Column(name = "idempotency_key", length = 150, nullable = false)
    private String idempotencyKey;

    @NotNull
    @Size(max = 100)
    @Column(name = "event_type", length = 100, nullable = false)
    private String eventType;

    @NotNull
    @Size(max = 100)
    @Column(name = "consumer_group", length = 100, nullable = false)
    private String consumerGroup = "notification-service-group";

    @NotNull
    @Column(name = "processed_at", nullable = false)
    private Instant processedAt = Instant.now();

    public NotificationInbox() {}

    public NotificationInbox(String eventId, String idempotencyKey, String eventType, String consumerGroup) {
        this.eventId = eventId;
        this.idempotencyKey = idempotencyKey;
        this.eventType = eventType;
        this.consumerGroup = consumerGroup != null ? consumerGroup : "notification-service-group";
        this.processedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getConsumerGroup() { return consumerGroup; }
    public void setConsumerGroup(String consumerGroup) { this.consumerGroup = consumerGroup; }

    public Instant getProcessedAt() { return processedAt; }
    public void setProcessedAt(Instant processedAt) { this.processedAt = processedAt; }

    @Override
    public String toString() {
        return "NotificationInbox{" +
            "id=" + id +
            ", eventId='" + eventId + '\'' +
            ", idempotencyKey='" + idempotencyKey + '\'' +
            ", eventType='" + eventType + '\'' +
            '}';
    }
}
