package com.ocbs.service.dto;

import com.ocbs.domain.enumeration.AppointmentStatus;
import com.ocbs.domain.enumeration.ConsultationMode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * A DTO for the {@link com.ocbs.domain.Appointment} entity.
 */
@Schema(description = "Lịch hẹn khám chữa bệnh giữa Bệnh nhân và Bác sĩ")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class AppointmentDTO implements Serializable {

    private Long id;

    @NotNull
    @Size(max = 32)
    private String bookingCode;

    @NotNull
    private Instant scheduledStart;

    @NotNull
    private Instant scheduledEnd;

    @NotNull
    private ConsultationMode consultationMode;

    @NotNull
    private AppointmentStatus status;

    @Size(max = 1000)
    private String reason;

    @Size(max = 500)
    private String cancellationReason;

    @Size(max = 500)
    private String diagnosis;

    private String consultationSummary;

    private String therapyPlan;

    private String recommendations;

    private String followUpNotes;

    private Instant nextAppointmentRecommendedDate;

    private Instant completedAt;

    @NotNull
    private Instant createdAt;

    private Instant updatedAt;

    @NotNull
    private PatientProfileDTO patient;

    @NotNull
    private DoctorProfileDTO doctor;

    @NotNull
    private AvailabilitySlotDTO slot;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBookingCode() {
        return bookingCode;
    }

    public void setBookingCode(String bookingCode) {
        this.bookingCode = bookingCode;
    }

    public Instant getScheduledStart() {
        return scheduledStart;
    }

    public void setScheduledStart(Instant scheduledStart) {
        this.scheduledStart = scheduledStart;
    }

    public Instant getScheduledEnd() {
        return scheduledEnd;
    }

    public void setScheduledEnd(Instant scheduledEnd) {
        this.scheduledEnd = scheduledEnd;
    }

    public ConsultationMode getConsultationMode() {
        return consultationMode;
    }

    public void setConsultationMode(ConsultationMode consultationMode) {
        this.consultationMode = consultationMode;
    }

    public AppointmentStatus getStatus() {
        return status;
    }

    public void setStatus(AppointmentStatus status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    public String getDiagnosis() {
        return diagnosis;
    }

    public void setDiagnosis(String diagnosis) {
        this.diagnosis = diagnosis;
    }

    public String getConsultationSummary() {
        return consultationSummary;
    }

    public void setConsultationSummary(String consultationSummary) {
        this.consultationSummary = consultationSummary;
    }

    public String getTherapyPlan() {
        return therapyPlan;
    }

    public void setTherapyPlan(String therapyPlan) {
        this.therapyPlan = therapyPlan;
    }

    public String getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(String recommendations) {
        this.recommendations = recommendations;
    }

    public String getFollowUpNotes() {
        return followUpNotes;
    }

    public void setFollowUpNotes(String followUpNotes) {
        this.followUpNotes = followUpNotes;
    }

    public Instant getNextAppointmentRecommendedDate() {
        return nextAppointmentRecommendedDate;
    }

    public void setNextAppointmentRecommendedDate(Instant nextAppointmentRecommendedDate) {
        this.nextAppointmentRecommendedDate = nextAppointmentRecommendedDate;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public PatientProfileDTO getPatient() {
        return patient;
    }

    public void setPatient(PatientProfileDTO patient) {
        this.patient = patient;
    }

    public DoctorProfileDTO getDoctor() {
        return doctor;
    }

    public void setDoctor(DoctorProfileDTO doctor) {
        this.doctor = doctor;
    }

    public AvailabilitySlotDTO getSlot() {
        return slot;
    }

    public void setSlot(AvailabilitySlotDTO slot) {
        this.slot = slot;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AppointmentDTO)) {
            return false;
        }

        AppointmentDTO appointmentDTO = (AppointmentDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, appointmentDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "AppointmentDTO{" +
            "id=" + getId() +
            ", bookingCode='" + getBookingCode() + "'" +
            ", scheduledStart='" + getScheduledStart() + "'" +
            ", scheduledEnd='" + getScheduledEnd() + "'" +
            ", consultationMode='" + getConsultationMode() + "'" +
            ", status='" + getStatus() + "'" +
            ", reason='" + getReason() + "'" +
            ", cancellationReason='" + getCancellationReason() + "'" +
            ", createdAt='" + getCreatedAt() + "'" +
            ", updatedAt='" + getUpdatedAt() + "'" +
            ", patient=" + getPatient() +
            ", doctor=" + getDoctor() +
            ", slot=" + getSlot() +
            "}";
    }
}
