package com.ocbs.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.time.Instant;

/**
 * Idempotent Inbox entity for Kafka event processing in Treatment Service.
 * Guarantees that session deduction and event processing per (appointmentId, eventType) occurs at most once.
 */
@Entity
@Table(name = "treatment_inbox")
public class TreatmentInbox implements Serializable {

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
    @Column(name = "appointment_id", nullable = false)
    private Long appointmentId;

    @NotNull
    @Size(max = 100)
    @Column(name = "event_type", length = 100, nullable = false)
    private String eventType = "APPOINTMENT_COMPLETED";

    @NotNull
    @Size(max = 50)
    @Column(name = "status", length = 50, nullable = false)
    private String status = "PROCESSED";

    @NotNull
    @Size(max = 150)
    @Column(name = "idempotency_key", length = 150, nullable = false)
    private String idempotencyKey;

    @NotNull
    @Size(max = 100)
    @Column(name = "consumer_group", length = 100, nullable = false)
    private String consumerGroup = "treatment-service-group";

    @NotNull
    @Column(name = "processed_at", nullable = false)
    private Instant processedAt = Instant.now();

    public TreatmentInbox() {}

    public TreatmentInbox(String eventId, Long appointmentId, String idempotencyKey, String consumerGroup) {
        this(eventId, appointmentId, "APPOINTMENT_COMPLETED", "PROCESSED", idempotencyKey, consumerGroup);
    }

    public TreatmentInbox(String eventId, Long appointmentId, String eventType, String status, String idempotencyKey, String consumerGroup) {
        this.eventId = eventId;
        this.appointmentId = appointmentId;
        this.eventType = eventType != null ? eventType : "APPOINTMENT_COMPLETED";
        this.status = status != null ? status : "PROCESSED";
        this.idempotencyKey = idempotencyKey;
        this.consumerGroup = consumerGroup != null ? consumerGroup : "treatment-service-group";
        this.processedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public Long getAppointmentId() { return appointmentId; }
    public void setAppointmentId(Long appointmentId) { this.appointmentId = appointmentId; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public String getConsumerGroup() { return consumerGroup; }
    public void setConsumerGroup(String consumerGroup) { this.consumerGroup = consumerGroup; }

    public Instant getProcessedAt() { return processedAt; }
    public void setProcessedAt(Instant processedAt) { this.processedAt = processedAt; }

    @Override
    public String toString() {
        return "TreatmentInbox{" +
            "id=" + id +
            ", eventId='" + eventId + '\'' +
            ", appointmentId=" + appointmentId +
            ", eventType='" + eventType + '\'' +
            ", status='" + status + '\'' +
            ", idempotencyKey='" + idempotencyKey + '\'' +
            '}';
    }
}
