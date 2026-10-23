package com.ocbs.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.ocbs.domain.enumeration.TreatmentPackageStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Table(name = "treatment_package")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class TreatmentPackage extends AbstractAuditingEntity<Long> implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull
    @Size(max = 255)
    @Column(name = "name", length = 255, nullable = false)
    private String name;

    @Lob
    @Column(name = "description")
    private String description;

    @Lob
    @Column(name = "target_outcome")
    private String targetOutcome;

    @Lob
    @Column(name = "recommended_exercises")
    private String recommendedExercises;

    @Lob
    @Column(name = "instructions")
    private String instructions;

    @NotNull
    @Column(name = "session_quantity", nullable = false)
    private Integer sessionQuantity = 1;

    @NotNull
    @Column(name = "remaining_sessions", nullable = false)
    private Integer remainingSessions = 1;

    @NotNull
    @Column(name = "validity_days", nullable = false)
    private Integer validityDays = 30;

    @NotNull
    @Column(name = "recommended_sessions_per_week", nullable = false)
    private Integer recommendedSessionsPerWeek = 1;

    @Column(name = "expiration_date")
    private Instant expirationDate;

    @NotNull
    @Column(name = "price", precision = 21, scale = 2, nullable = false)
    private BigDecimal price = BigDecimal.ZERO;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50, nullable = false)
    private TreatmentPackageStatus status = TreatmentPackageStatus.CREATED;

    @Column(name = "assigned_date")
    private Instant assignedDate;

    @Column(name = "accepted_date")
    private Instant acceptedDate;

    @Column(name = "acceptance_expires_at")
    private Instant acceptanceExpiresAt;

    @Column(name = "active_date")
    private Instant activeDate;

    @Column(name = "expired_at")
    private Instant expiredAt;

    @Lob
    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Lob
    @Column(name = "cancellation_reason")
    private String cancellationReason;

    @Column(name = "cancellation_requested_at")
    private Instant cancellationRequestedAt;

    // === HOLD (BẢO LƯU) LIFECYCLE MANAGEMENT ===

    @Column(name = "is_hold_requested", nullable = false)
    private Boolean isHoldRequested = false;

    @Column(name = "hold_requested_at")
    private Instant holdRequestedAt;

    @Column(name = "hold_start_date")
    private Instant holdStartDate;

    @Column(name = "hold_end_date")
    private Instant holdEndDate;

    @Column(name = "hold_duration_days")
    private Integer holdDurationDays;

    @Lob
    @Column(name = "hold_reason")
    private String holdReason;

    @Column(name = "hold_approved_at")
    private Instant holdApprovedAt;

    @Column(name = "hold_approved_by_doctor_id")
    private Long holdApprovedByDoctorId;

    @Column(name = "hold_rejected_at")
    private Instant holdRejectedAt;

    @Lob
    @Column(name = "hold_rejection_reason")
    private String holdRejectionReason;

    @Column(name = "total_hold_days", nullable = false)
    private Integer totalHoldDays = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "user", "specialization" }, allowSetters = true)
    private DoctorProfile doctor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "user" }, allowSetters = true)
    private PatientProfile patient;

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTargetOutcome() {
        return targetOutcome;
    }

    public void setTargetOutcome(String targetOutcome) {
        this.targetOutcome = targetOutcome;
    }

    public String getRecommendedExercises() {
        return recommendedExercises;
    }

    public void setRecommendedExercises(String recommendedExercises) {
        this.recommendedExercises = recommendedExercises;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public Integer getSessionQuantity() {
        return sessionQuantity;
    }

    public void setSessionQuantity(Integer sessionQuantity) {
        this.sessionQuantity = sessionQuantity;
    }

    public Integer getRemainingSessions() {
        return remainingSessions;
    }

    public void setRemainingSessions(Integer remainingSessions) {
        this.remainingSessions = remainingSessions;
    }

    public Integer getValidityDays() {
        return validityDays;
    }

    public void setValidityDays(Integer validityDays) {
        this.validityDays = validityDays;
    }

    public Integer getRecommendedSessionsPerWeek() {
        return recommendedSessionsPerWeek;
    }

    public void setRecommendedSessionsPerWeek(Integer recommendedSessionsPerWeek) {
        this.recommendedSessionsPerWeek = recommendedSessionsPerWeek;
    }

    public Instant getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(Instant expirationDate) {
        this.expirationDate = expirationDate;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public TreatmentPackageStatus getStatus() {
        return status;
    }

    public void setStatus(TreatmentPackageStatus status) {
        this.status = status;
    }

    public Instant getAssignedDate() {
        return assignedDate;
    }

    public void setAssignedDate(Instant assignedDate) {
        this.assignedDate = assignedDate;
    }

    public Instant getAcceptedDate() {
        return acceptedDate;
    }

    public void setAcceptedDate(Instant acceptedDate) {
        this.acceptedDate = acceptedDate;
    }

    public Instant getAcceptanceExpiresAt() {
        return acceptanceExpiresAt;
    }

    public void setAcceptanceExpiresAt(Instant acceptanceExpiresAt) {
        this.acceptanceExpiresAt = acceptanceExpiresAt;
    }

    public Instant getActiveDate() {
        return activeDate;
    }

    public void setActiveDate(Instant activeDate) {
        this.activeDate = activeDate;
    }

    public Instant getExpiredAt() {
        return expiredAt;
    }

    public void setExpiredAt(Instant expiredAt) {
        this.expiredAt = expiredAt;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    public Instant getCancellationRequestedAt() {
        return cancellationRequestedAt;
    }

    public void setCancellationRequestedAt(Instant cancellationRequestedAt) {
        this.cancellationRequestedAt = cancellationRequestedAt;
    }

    public DoctorProfile getDoctor() {
        return doctor;
    }

    public void setDoctor(DoctorProfile doctor) {
        this.doctor = doctor;
    }

    public PatientProfile getPatient() {
        return patient;
    }

    public void setPatient(PatientProfile patient) {
        this.patient = patient;
    }

    public Boolean getIsHoldRequested() {
        return isHoldRequested;
    }

    public void setIsHoldRequested(Boolean isHoldRequested) {
        this.isHoldRequested = isHoldRequested;
    }

    public Instant getHoldRequestedAt() {
        return holdRequestedAt;
    }

    public void setHoldRequestedAt(Instant holdRequestedAt) {
        this.holdRequestedAt = holdRequestedAt;
    }

    public Instant getHoldStartDate() {
        return holdStartDate;
    }

    public void setHoldStartDate(Instant holdStartDate) {
        this.holdStartDate = holdStartDate;
    }

    public Instant getHoldEndDate() {
        return holdEndDate;
    }

    public void setHoldEndDate(Instant holdEndDate) {
        this.holdEndDate = holdEndDate;
    }

    public Integer getHoldDurationDays() {
        return holdDurationDays;
    }

    public void setHoldDurationDays(Integer holdDurationDays) {
        this.holdDurationDays = holdDurationDays;
    }

    public String getHoldReason() {
        return holdReason;
    }

    public void setHoldReason(String holdReason) {
        this.holdReason = holdReason;
    }

    public Instant getHoldApprovedAt() {
        return holdApprovedAt;
    }

    public void setHoldApprovedAt(Instant holdApprovedAt) {
        this.holdApprovedAt = holdApprovedAt;
    }

    public Long getHoldApprovedByDoctorId() {
        return holdApprovedByDoctorId;
    }

    public void setHoldApprovedByDoctorId(Long holdApprovedByDoctorId) {
        this.holdApprovedByDoctorId = holdApprovedByDoctorId;
    }

    public Instant getHoldRejectedAt() {
        return holdRejectedAt;
    }

    public void setHoldRejectedAt(Instant holdRejectedAt) {
        this.holdRejectedAt = holdRejectedAt;
    }

    public String getHoldRejectionReason() {
        return holdRejectionReason;
    }

    public void setHoldRejectionReason(String holdRejectionReason) {
        this.holdRejectionReason = holdRejectionReason;
    }

    public Integer getTotalHoldDays() {
        return totalHoldDays;
    }

    public void setTotalHoldDays(Integer totalHoldDays) {
        this.totalHoldDays = totalHoldDays;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TreatmentPackage)) {
            return false;
        }
        return id != null && id.equals(((TreatmentPackage) o).id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
