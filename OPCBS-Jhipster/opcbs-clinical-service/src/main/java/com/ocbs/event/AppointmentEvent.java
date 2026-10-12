package com.ocbs.event;

import java.io.Serializable;
import java.time.Instant;

/**
 * Event published when an appointment status changes (BOOKED, ACCEPTED, STARTED, COMPLETED, CANCELLED).
 */
public class AppointmentEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long appointmentId;
    private String bookingCode;
    private Long patientId;
    private String patientName;
    private Long doctorId;
    private String doctorName;
    private String eventType;
    private Instant eventTime;

    public AppointmentEvent() {}

    public Long getAppointmentId() { return appointmentId; }
    public void setAppointmentId(Long appointmentId) { this.appointmentId = appointmentId; }

    public String getBookingCode() { return bookingCode; }
    public void setBookingCode(String bookingCode) { this.bookingCode = bookingCode; }

    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public Long getDoctorId() { return doctorId; }
    public void setDoctorId(Long doctorId) { this.doctorId = doctorId; }

    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public Instant getEventTime() { return eventTime; }
    public void setEventTime(Instant eventTime) { this.eventTime = eventTime; }
}
