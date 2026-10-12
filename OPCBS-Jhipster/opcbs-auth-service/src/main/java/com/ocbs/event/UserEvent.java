package com.ocbs.event;

import java.io.Serializable;
import java.time.Instant;

/**
 * Event published when user accounts are registered or updated.
 */
public class UserEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;
    private String login;
    private String email;
    private String fullName;
    private String role; // "ROLE_PATIENT", "ROLE_DOCTOR"
    private String eventType; // "USER_REGISTERED", "USER_ACTIVATED"
    private Instant eventTime;

    public UserEvent() {}

    public UserEvent(Long userId, String login, String email, String fullName, String role, String eventType) {
        this.userId = userId;
        this.login = login;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
        this.eventType = eventType;
        this.eventTime = Instant.now();
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public Instant getEventTime() { return eventTime; }
    public void setEventTime(Instant eventTime) { this.eventTime = eventTime; }

    @Override
    public String toString() {
        return "UserEvent{" +
            "userId=" + userId +
            ", login='" + login + '\'' +
            ", email='" + email + '\'' +
            ", role='" + role + '\'' +
            ", eventType='" + eventType + '\'' +
            ", eventTime=" + eventTime +
            '}';
    }
}
