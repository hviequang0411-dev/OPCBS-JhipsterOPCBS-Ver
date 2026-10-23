package com.ocbs.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * Mục Tiêu Điều Trị (Treatment Goal) - Chuẩn Y Khoa Tham Chiếu C# Gốc.
 * Theo dõi tiến độ định lượng và định tính của bệnh nhân trong phác đồ.
 */
@Entity
@Table(name = "treatment_goal")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class TreatmentGoal implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull
    @Size(max = 255)
    @Column(name = "title", length = 255, nullable = false)
    private String title;

    @Lob
    @Column(name = "description")
    private String description;

    @Column(name = "category", length = 50)
    private String category = "OTHER"; // EMOTION, BEHAVIOR, COGNITIVE, SOCIAL, OTHER

    @Column(name = "priority", length = 20)
    private String priority = "MEDIUM"; // LOW, MEDIUM, HIGH, URGENT

    @Column(name = "status", length = 30)
    private String status = "NOT_STARTED"; // NOT_STARTED, IN_PROGRESS, ACHIEVED, PAUSED, CANCELLED

    @NotNull
    @Column(name = "progress_percent", nullable = false)
    private Integer progressPercent = 0;

    @Column(name = "target_value", precision = 10, scale = 2)
    private BigDecimal targetValue;

    @Column(name = "current_value", precision = 10, scale = 2)
    private BigDecimal currentValue;

    @Column(name = "unit", length = 50)
    private String unit;

    @Column(name = "target_date")
    private Instant targetDate;

    @Column(name = "achieved_date")
    private Instant achievedDate;

    @Lob
    @Column(name = "doctor_notes")
    private String doctorNotes;

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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getProgressPercent() {
        return progressPercent;
    }

    public void setProgressPercent(Integer progressPercent) {
        this.progressPercent = progressPercent;
    }

    public BigDecimal getTargetValue() {
        return targetValue;
    }

    public void setTargetValue(BigDecimal targetValue) {
        this.targetValue = targetValue;
    }

    public BigDecimal getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(BigDecimal currentValue) {
        this.currentValue = currentValue;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public Instant getTargetDate() {
        return targetDate;
    }

    public void setTargetDate(Instant targetDate) {
        this.targetDate = targetDate;
    }

    public Instant getAchievedDate() {
        return achievedDate;
    }

    public void setAchievedDate(Instant achievedDate) {
        this.achievedDate = achievedDate;
    }

    public String getDoctorNotes() {
        return doctorNotes;
    }

    public void setDoctorNotes(String doctorNotes) {
        this.doctorNotes = doctorNotes;
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
        if (!(o instanceof TreatmentGoal)) return false;
        return id != null && id.equals(((TreatmentGoal) o).id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
