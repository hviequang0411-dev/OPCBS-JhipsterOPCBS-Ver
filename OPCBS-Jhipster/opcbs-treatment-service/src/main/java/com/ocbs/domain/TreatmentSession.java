package com.ocbs.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.time.Instant;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * Buổi Trị Liệu Lâm Sàng (Treatment Session) - Chuẩn Y Khoa Tham Chiếu C# Gốc.
 * Theo dõi diễn biến từng buổi khám, chỉ số tâm lý trước/sau và đánh giá chuyên môn.
 */
@Entity
@Table(name = "treatment_session")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class TreatmentSession implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull
    @Column(name = "session_number", nullable = false)
    private Integer sessionNumber;

    @Size(max = 255)
    @Column(name = "title", length = 255)
    private String title;

    @Lob
    @Column(name = "session_summary")
    private String sessionSummary;

    @Lob
    @Column(name = "doctor_clinical_assessment")
    private String doctorClinicalAssessment;

    @Lob
    @Column(name = "patient_friendly_summary")
    private String patientFriendlySummary;

    @Column(name = "mood_before")
    private Integer moodBefore; // 1 - 10

    @Column(name = "mood_after")
    private Integer moodAfter; // 1 - 10

    @Column(name = "status", length = 30)
    private String status = "PLANNED"; // PLANNED, SCHEDULED, COMPLETED, CANCELLED, NO_SHOW

    @Column(name = "appointment_id")
    private Long appointmentId;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "treatmentGoals", "treatmentSessions" }, allowSetters = true)
    @JoinColumn(name = "package_id", nullable = false)
    private TreatmentPackage treatmentPackage;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getSessionNumber() {
        return sessionNumber;
    }

    public void setSessionNumber(Integer sessionNumber) {
        this.sessionNumber = sessionNumber;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSessionSummary() {
        return sessionSummary;
    }

    public void setSessionSummary(String sessionSummary) {
        this.sessionSummary = sessionSummary;
    }

    public String getDoctorClinicalAssessment() {
        return doctorClinicalAssessment;
    }

    public void setDoctorClinicalAssessment(String doctorClinicalAssessment) {
        this.doctorClinicalAssessment = doctorClinicalAssessment;
    }

    public String getPatientFriendlySummary() {
        return patientFriendlySummary;
    }

    public void setPatientFriendlySummary(String patientFriendlySummary) {
        this.patientFriendlySummary = patientFriendlySummary;
    }

    public Integer getMoodBefore() {
        return moodBefore;
    }

    public void setMoodBefore(Integer moodBefore) {
        this.moodBefore = moodBefore;
    }

    public Integer getMoodAfter() {
        return moodAfter;
    }

    public void setMoodAfter(Integer moodAfter) {
        this.moodAfter = moodAfter;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(Long appointmentId) {
        this.appointmentId = appointmentId;
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

    public TreatmentPackage getTreatmentPackage() {
        return treatmentPackage;
    }

    public void setTreatmentPackage(TreatmentPackage treatmentPackage) {
        this.treatmentPackage = treatmentPackage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TreatmentSession)) return false;
        return id != null && id.equals(((TreatmentSession) o).id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
