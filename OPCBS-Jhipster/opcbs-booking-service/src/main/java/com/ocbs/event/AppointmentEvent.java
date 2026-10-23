package com.ocbs.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Standardized Domain Event for Appointment Lifecycle.
 * Contract Version: 1.0
 * Topics: opcbs-appointment-events
 *
 * Privacy & Security Compliance:
 * - Emits only stable identifiers (patientId, doctorId, appointmentId, bookingCode) and workflow state.
 * - Does NOT include personal names, login credentials, passwords, JWT tokens, medical diagnoses or private reasons.
 * - Consumers resolve recipient names and notification channels via local repository lookup using entity IDs.
 * - Retains legacy getter/setter compatibility with @JsonInclude(NON_NULL) for smooth transition.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AppointmentEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("eventId")
    private String eventId = UUID.randomUUID().toString();

    @JsonProperty("eventVersion")
    private String eventVersion = "1.0";

    @JsonProperty("idempotencyKey")
    private String idempotencyKey;

    @JsonProperty("eventType")
    private String eventType; // APPOINTMENT_BOOKED, APPOINTMENT_CONFIRMED, APPOINTMENT_REJECTED, APPOINTMENT_CANCELLED, APPOINTMENT_COMPLETED

    @JsonProperty("occurredAt")
    private Instant occurredAt = Instant.now();

    @JsonProperty("appointmentId")
    private Long appointmentId;

    @JsonProperty("bookingCode")
    private String bookingCode;

    @JsonProperty("patientId")
    private Long patientId;

    @JsonProperty("doctorId")
    private Long doctorId;

    @JsonProperty("previousStatus")
    private String previousStatus;

    @JsonProperty("newStatus")
    private String newStatus;

    @JsonProperty("aggregateId")
    private Long aggregateId;

    @JsonProperty("correlationId")
    private String correlationId;

    // --- DEPRECATED FIELDS KEPT ONLY FOR BACKWARD COMPATIBILITY IN TRANSITION ---
    @JsonProperty("patientLogin")
    private String patientLogin;

    @JsonProperty("patientName")
    private String patientName;

    @JsonProperty("doctorLogin")
    private String doctorLogin;

    @JsonProperty("doctorName")
    private String doctorName;

    @JsonProperty("reason")
    private String reason;

    @JsonProperty("patientEmail")
    private String patientEmail;

    @JsonProperty("doctorEmail")
    private String doctorEmail;

    public AppointmentEvent() {}

    /**
     * Primary constructor emitting privacy-compliant domain event (ID-only).
     */
    public AppointmentEvent(
        Long appointmentId,
        String bookingCode,
        Long patientId,
        Long doctorId,
        String eventType,
        String previousStatus,
        String newStatus
    ) {
        this.eventId = UUID.randomUUID().toString();
        this.eventVersion = "1.0";
        this.appointmentId = appointmentId;
        this.aggregateId = appointmentId;
        this.bookingCode = bookingCode;
        this.patientId = patientId;
        this.doctorId = doctorId;
        this.eventType = eventType;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.occurredAt = Instant.now();
        this.idempotencyKey = "APPT_" + (bookingCode != null ? bookingCode : appointmentId) + "_" + eventType;
        this.correlationId = "CORR_" + (bookingCode != null ? bookingCode : appointmentId);
    }

    /**
     * Legacy constructor for transitional compatibility.
     */
    public AppointmentEvent(
        Long appointmentId,
        String bookingCode,
        Long patientId,
        String patientLogin,
        String patientName,
        Long doctorId,
        String doctorLogin,
        String doctorName,
        String eventType,
        String previousStatus,
        String newStatus,
        String reason
    ) {
        this(appointmentId, bookingCode, patientId, doctorId, eventType, previousStatus, newStatus);
        this.patientLogin = patientLogin;
        this.patientName = patientName;
        this.doctorLogin = doctorLogin;
        this.doctorName = doctorName;
        this.reason = reason;
    }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getEventVersion() { return eventVersion; }
    public void setEventVersion(String eventVersion) { this.eventVersion = eventVersion; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; }

    @JsonProperty("eventTime")
    public Instant getEventTime() { return occurredAt; }
    public void setEventTime(Instant eventTime) { this.occurredAt = eventTime; }

    public Long getAppointmentId() { return appointmentId; }
    public void setAppointmentId(Long appointmentId) {
        this.appointmentId = appointmentId;
        if (this.aggregateId == null) {
            this.aggregateId = appointmentId;
        }
    }

    public Long getAggregateId() { return aggregateId != null ? aggregateId : appointmentId; }
    public void setAggregateId(Long aggregateId) {
        this.aggregateId = aggregateId;
        if (this.appointmentId == null) {
            this.appointmentId = aggregateId;
        }
    }

    public String getCorrelationId() { return correlationId != null ? correlationId : eventId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }

    public String getBookingCode() { return bookingCode; }
    public void setBookingCode(String bookingCode) { this.bookingCode = bookingCode; }

    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }

    public Long getDoctorId() { return doctorId; }
    public void setDoctorId(Long doctorId) { this.doctorId = doctorId; }

    public String getPreviousStatus() { return previousStatus; }
    public void setPreviousStatus(String previousStatus) { this.previousStatus = previousStatus; }

    public String getNewStatus() { return newStatus; }
    public void setNewStatus(String newStatus) { this.newStatus = newStatus; }

    // Deprecated accessors
    public String getPatientLogin() { return patientLogin; }
    public void setPatientLogin(String patientLogin) { this.patientLogin = patientLogin; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getDoctorLogin() { return doctorLogin; }
    public void setDoctorLogin(String doctorLogin) { this.doctorLogin = doctorLogin; }

    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getPatientEmail() { return patientEmail; }
    public void setPatientEmail(String patientEmail) { this.patientEmail = patientEmail; }

    public String getDoctorEmail() { return doctorEmail; }
    public void setDoctorEmail(String doctorEmail) { this.doctorEmail = doctorEmail; }

    @Override
    public String toString() {
        return "AppointmentEvent{" +
            "eventId='" + eventId + '\'' +
            ", version='" + eventVersion + '\'' +
            ", type='" + eventType + '\'' +
            ", apptId=" + appointmentId +
            ", code='" + bookingCode + '\'' +
            ", patientId=" + patientId +
            ", doctorId=" + doctorId +
            ", occurredAt=" + occurredAt +
            '}';
    }
}
