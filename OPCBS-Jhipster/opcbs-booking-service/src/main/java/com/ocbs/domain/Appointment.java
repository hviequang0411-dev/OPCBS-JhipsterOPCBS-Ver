package com.ocbs.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.ocbs.domain.enumeration.AppointmentStatus;
import com.ocbs.domain.enumeration.ConsultationMode;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

/**
 * Lịch hẹn khám chữa bệnh giữa Bệnh nhân và Bác sĩ
 */
@Entity
@Table(name = "appointment")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Appointment implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull
    @Size(max = 32)
    @Column(name = "booking_code", length = 32, nullable = false, unique = true)
    private String bookingCode;

    @NotNull
    @Column(name = "scheduled_start", nullable = false)
    private Instant scheduledStart;

    @NotNull
    @Column(name = "scheduled_end", nullable = false)
    private Instant scheduledEnd;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "consultation_mode", nullable = false)
    private ConsultationMode consultationMode;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AppointmentStatus status;

    @Size(max = 100)
    @Column(name = "idempotency_key", length = 100, unique = true)
    private String idempotencyKey;

    @Size(max = 1000)
    @Column(name = "reason", length = 1000)
    private String reason;

    @Size(max = 500)
    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @Size(max = 500)
    @Column(name = "diagnosis", length = 500)
    private String diagnosis;

    @Lob
    @Column(name = "consultation_summary")
    private String consultationSummary;

    @Lob
    @Column(name = "therapy_plan")
    private String therapyPlan;

    @Lob
    @Column(name = "recommendations")
    private String recommendations;

    @Lob
    @Column(name = "follow_up_notes")
    private String followUpNotes;

    @Column(name = "next_appointment_recommended_date")
    private Instant nextAppointmentRecommendedDate;

    @Column(name = "completed_at")
    private Instant completedAt;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "user" }, allowSetters = true)
    private PatientProfile patient;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "user", "specializationses" }, allowSetters = true)
    private DoctorProfile doctor;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "doctor" }, allowSetters = true)
    private AvailabilitySlot slot;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public Appointment id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBookingCode() {
        return this.bookingCode;
    }

    public Appointment bookingCode(String bookingCode) {
        this.setBookingCode(bookingCode);
        return this;
    }

    public void setBookingCode(String bookingCode) {
        this.bookingCode = bookingCode;
    }

    public Instant getScheduledStart() {
        return this.scheduledStart;
    }

    public Appointment scheduledStart(Instant scheduledStart) {
        this.setScheduledStart(scheduledStart);
        return this;
    }

    public void setScheduledStart(Instant scheduledStart) {
        this.scheduledStart = scheduledStart;
    }

    public Instant getScheduledEnd() {
        return this.scheduledEnd;
    }

    public Appointment scheduledEnd(Instant scheduledEnd) {
        this.setScheduledEnd(scheduledEnd);
        return this;
    }

    public void setScheduledEnd(Instant scheduledEnd) {
        this.scheduledEnd = scheduledEnd;
    }

    public ConsultationMode getConsultationMode() {
        return this.consultationMode;
    }

    public Appointment consultationMode(ConsultationMode consultationMode) {
        this.setConsultationMode(consultationMode);
        return this;
    }

    public void setConsultationMode(ConsultationMode consultationMode) {
        this.consultationMode = consultationMode;
    }

    public AppointmentStatus getStatus() {
        return this.status;
    }

    public Appointment status(AppointmentStatus status) {
        this.setStatus(status);
        return this;
    }

    public void setStatus(AppointmentStatus status) {
        this.status = status;
    }

    public String getIdempotencyKey() {
        return this.idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getReason() {
        return this.reason;
    }

    public Appointment reason(String reason) {
        this.setReason(reason);
        return this;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getCancellationReason() {
        return this.cancellationReason;
    }

    public Appointment cancellationReason(String cancellationReason) {
        this.setCancellationReason(cancellationReason);
        return this;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    public String getDiagnosis() {
        return this.diagnosis;
    }

    public Appointment diagnosis(String diagnosis) {
        this.setDiagnosis(diagnosis);
        return this;
    }

    public void setDiagnosis(String diagnosis) {
        this.diagnosis = diagnosis;
    }

    public String getConsultationSummary() {
        return this.consultationSummary;
    }

    public Appointment consultationSummary(String consultationSummary) {
        this.setConsultationSummary(consultationSummary);
        return this;
    }

    public void setConsultationSummary(String consultationSummary) {
        this.consultationSummary = consultationSummary;
    }

    public String getTherapyPlan() {
        return this.therapyPlan;
    }

    public Appointment therapyPlan(String therapyPlan) {
        this.setTherapyPlan(therapyPlan);
        return this;
    }

    public void setTherapyPlan(String therapyPlan) {
        this.therapyPlan = therapyPlan;
    }

    public String getRecommendations() {
        return this.recommendations;
    }

    public Appointment recommendations(String recommendations) {
        this.setRecommendations(recommendations);
        return this;
    }

    public void setRecommendations(String recommendations) {
        this.recommendations = recommendations;
    }

    public String getFollowUpNotes() {
        return this.followUpNotes;
    }

    public Appointment followUpNotes(String followUpNotes) {
        this.setFollowUpNotes(followUpNotes);
        return this;
    }

    public void setFollowUpNotes(String followUpNotes) {
        this.followUpNotes = followUpNotes;
    }

    public Instant getNextAppointmentRecommendedDate() {
        return this.nextAppointmentRecommendedDate;
    }

    public Appointment nextAppointmentRecommendedDate(Instant nextAppointmentRecommendedDate) {
        this.setNextAppointmentRecommendedDate(nextAppointmentRecommendedDate);
        return this;
    }

    public void setNextAppointmentRecommendedDate(Instant nextAppointmentRecommendedDate) {
        this.nextAppointmentRecommendedDate = nextAppointmentRecommendedDate;
    }

    public Instant getCompletedAt() {
        return this.completedAt;
    }

    public Appointment completedAt(Instant completedAt) {
        this.setCompletedAt(completedAt);
        return this;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }

    public Appointment createdAt(Instant createdAt) {
        this.setCreatedAt(createdAt);
        return this;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return this.updatedAt;
    }

    public Appointment updatedAt(Instant updatedAt) {
        this.setUpdatedAt(updatedAt);
        return this;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public PatientProfile getPatient() {
        return this.patient;
    }

    public void setPatient(PatientProfile patientProfile) {
        this.patient = patientProfile;
    }

    public Appointment patient(PatientProfile patientProfile) {
        this.setPatient(patientProfile);
        return this;
    }

    public DoctorProfile getDoctor() {
        return this.doctor;
    }

    public void setDoctor(DoctorProfile doctorProfile) {
        this.doctor = doctorProfile;
    }

    public Appointment doctor(DoctorProfile doctorProfile) {
        this.setDoctor(doctorProfile);
        return this;
    }

    public AvailabilitySlot getSlot() {
        return this.slot;
    }

    public void setSlot(AvailabilitySlot availabilitySlot) {
        this.slot = availabilitySlot;
    }

    public Appointment slot(AvailabilitySlot availabilitySlot) {
        this.setSlot(availabilitySlot);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Appointment)) {
            return false;
        }
        return getId() != null && getId().equals(((Appointment) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Appointment{" +
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
            "}";
    }
}
