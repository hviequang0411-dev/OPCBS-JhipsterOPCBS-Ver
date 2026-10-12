package com.ocbs.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.time.Instant;

public class AppointmentEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("appointmentId")
    private Long appointmentId;

    @JsonProperty("bookingCode")
    private String bookingCode;

    @JsonProperty("patientId")
    private Long patientId;

    @JsonProperty("patientName")
    private String patientName;

    @JsonProperty("doctorId")
    private Long doctorId;

    @JsonProperty("doctorName")
    private String doctorName;

    @JsonProperty("eventType")
    private String eventType; // "APPOINTMENT_BOOKED", "APPOINTMENT_ACCEPTED", "APPOINTMENT_COMPLETED"

    @JsonProperty("eventTime")
    private Instant eventTime;

    @JsonProperty("notes")
    private String notes;

    public AppointmentEvent() {}

    public AppointmentEvent(
        Long appointmentId,
        String bookingCode,
        Long patientId,
        String patientName,
        Long doctorId,
        String doctorName,
        String eventType,
        Instant eventTime,
        String notes
    ) {
        this.appointmentId = appointmentId;
        this.bookingCode = bookingCode;
        this.patientId = patientId;
        this.patientName = patientName;
        this.doctorId = doctorId;
        this.doctorName = doctorName;
        this.eventType = eventType;
        this.eventTime = eventTime != null ? eventTime : Instant.now();
        this.notes = notes;
    }

    public Long getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(Long appointmentId) {
        this.appointmentId = appointmentId;
    }

    public String getBookingCode() {
        return bookingCode;
    }

    public void setBookingCode(String bookingCode) {
        this.bookingCode = bookingCode;
    }

    public Long getPatientId() {
        return patientId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public Long getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(Long doctorId) {
        this.doctorId = doctorId;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Instant getEventTime() {
        return eventTime;
    }

    public void setEventTime(Instant eventTime) {
        this.eventTime = eventTime;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @Override
    public String toString() {
        return "AppointmentEvent{" +
            "appointmentId=" + appointmentId +
            ", bookingCode='" + bookingCode + '\'' +
            ", patientId=" + patientId +
            ", doctorId=" + doctorId +
            ", eventType='" + eventType + '\'' +
            '}';
    }
}
