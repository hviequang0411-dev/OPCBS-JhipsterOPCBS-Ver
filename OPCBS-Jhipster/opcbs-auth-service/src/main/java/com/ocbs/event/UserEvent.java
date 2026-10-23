package com.ocbs.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Standardized Domain Event published when user accounts are registered or updated.
 * Contract Version: 1.0
 * Topics: opcbs-user-events
 *
 * Privacy Compliance:
 * - Emits only identity references (userId, login, role, eventType).
 * - Excludes password hashes, JWT tokens, reset keys, and personal credentials.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("eventId")
    private String eventId = UUID.randomUUID().toString();

    @JsonProperty("eventVersion")
    private String eventVersion = "1.0";

    @JsonProperty("eventType")
    private String eventType; // USER_REGISTERED, USER_ACTIVATED

    @JsonProperty("occurredAt")
    private Instant occurredAt = Instant.now();

    @JsonProperty("aggregateId")
    private Long aggregateId;

    @JsonProperty("idempotencyKey")
    private String idempotencyKey;

    @JsonProperty("correlationId")
    private String correlationId;

    @JsonProperty("userId")
    private Long userId;

    @JsonProperty("login")
    private String login;

    @JsonProperty("email")
    private String email;

    @JsonProperty("fullName")
    private String fullName;

    @JsonProperty("role")
    private String role; // "ROLE_PATIENT", "ROLE_DOCTOR"

    public UserEvent() {}

    public UserEvent(Long userId, String login, String email, String fullName, String role, String eventType) {
        this.eventId = UUID.randomUUID().toString();
        this.eventVersion = "1.0";
        this.userId = userId;
        this.aggregateId = userId;
        this.login = login;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
        this.eventType = eventType;
        this.occurredAt = Instant.now();
        this.idempotencyKey = "USER_" + (login != null ? login : userId) + "_" + eventType;
        this.correlationId = "CORR_USER_" + (login != null ? login : userId);
    }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getEventVersion() { return eventVersion; }
    public void setEventVersion(String eventVersion) { this.eventVersion = eventVersion; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; }

    @JsonProperty("eventTime")
    public Instant getEventTime() { return occurredAt; }
    public void setEventTime(Instant eventTime) { this.occurredAt = eventTime; }

    public Long getAggregateId() { return aggregateId != null ? aggregateId : userId; }
    public void setAggregateId(Long aggregateId) {
        this.aggregateId = aggregateId;
        if (this.userId == null) {
            this.userId = aggregateId;
        }
    }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public String getCorrelationId() { return correlationId != null ? correlationId : eventId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) {
        this.userId = userId;
        if (this.aggregateId == null) {
            this.aggregateId = userId;
        }
    }

    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    @Override
    public String toString() {
        return "UserEvent{" +
            "eventId='" + eventId + '\'' +
            ", userId=" + userId +
            ", login='" + login + '\'' +
            ", role='" + role + '\'' +
            ", eventType='" + eventType + '\'' +
            '}';
    }
}
