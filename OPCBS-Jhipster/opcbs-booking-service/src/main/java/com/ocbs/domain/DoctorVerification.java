package com.ocbs.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * Quản lý hồ sơ xác thực và duyệt chứng chỉ hành nghề bác sĩ (Doctor Verification).
 */
@Entity
@Table(name = "doctor_verification")
public class DoctorVerification implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_profile_id", nullable = false)
    @JsonIgnoreProperties(value = { "user", "specializationses" }, allowSetters = true)
    private DoctorProfile doctorProfile;

    @Size(max = 160)
    @Column(name = "doctor_name", length = 160)
    private String doctorName;

    @Size(max = 100)
    @Column(name = "license_number", length = 100)
    private String licenseNumber;

    @Size(max = 150)
    @Column(name = "specialization", length = 150)
    private String specialization;

    @Column(name = "experience_years")
    private Integer experienceYears;

    @Size(max = 255)
    @Column(name = "education", length = 255)
    private String education;

    @NotNull
    @Size(max = 50)
    @Column(name = "status", length = 50, nullable = false)
    private String status = "DRAFT"; // DRAFT, SUBMITTED, REQUIRES_ADDITIONAL_INFO, APPROVED, REJECTED

    @Lob
    @Column(name = "notes")
    private String notes;

    @Size(max = 500)
    @Column(name = "certificate_url", length = 500)
    private String certificateUrl;

    @Size(max = 200)
    @Column(name = "certificate_public_id", length = 200)
    private String certificatePublicId;

    @Size(max = 255)
    @Column(name = "certificate_file_name", length = 255)
    private String certificateFileName;

    @Size(max = 100)
    @Column(name = "certificate_content_type", length = 100)
    private String certificateContentType;

    @Column(name = "certificate_uploaded_at")
    private Instant certificateUploadedAt;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Size(max = 100)
    @Column(name = "reviewed_by", length = 100)
    private String reviewedBy;

    @Lob
    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        this.updatedAt = Instant.now();
        if (this.status == null) {
            this.status = "DRAFT";
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public DoctorProfile getDoctorProfile() {
        return doctorProfile;
    }

    public void setDoctorProfile(DoctorProfile doctorProfile) {
        this.doctorProfile = doctorProfile;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
    }

    public String getSpecialization() {
        return specialization;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }

    public Integer getExperienceYears() {
        return experienceYears;
    }

    public void setExperienceYears(Integer experienceYears) {
        this.experienceYears = experienceYears;
    }

    public String getEducation() {
        return education;
    }

    public void setEducation(String education) {
        this.education = education;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getCertificateUrl() {
        return certificateUrl;
    }

    public void setCertificateUrl(String certificateUrl) {
        this.certificateUrl = certificateUrl;
    }

    public String getCertificatePublicId() {
        return certificatePublicId;
    }

    public void setCertificatePublicId(String certificatePublicId) {
        this.certificatePublicId = certificatePublicId;
    }

    public String getCertificateFileName() {
        return certificateFileName;
    }

    public void setCertificateFileName(String certificateFileName) {
        this.certificateFileName = certificateFileName;
    }

    public String getCertificateContentType() {
        return certificateContentType;
    }

    public void setCertificateContentType(String certificateContentType) {
        this.certificateContentType = certificateContentType;
    }

    public Instant getCertificateUploadedAt() {
        return certificateUploadedAt;
    }

    public void setCertificateUploadedAt(Instant certificateUploadedAt) {
        this.certificateUploadedAt = certificateUploadedAt;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Instant submittedAt) {
        this.submittedAt = submittedAt;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(Instant reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public String getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(String reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DoctorVerification that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    @Override
    public String toString() {
        return "DoctorVerification{" +
            "id=" + id +
            ", doctorName='" + doctorName + '\'' +
            ", licenseNumber='" + licenseNumber + '\'' +
            ", status='" + status + '\'' +
            ", submittedAt=" + submittedAt +
            ", reviewedAt=" + reviewedAt +
            '}';
    }
}
