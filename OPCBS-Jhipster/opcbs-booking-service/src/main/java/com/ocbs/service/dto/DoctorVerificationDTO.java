package com.ocbs.service.dto;

import java.io.Serializable;
import java.time.Instant;

/**
 * DTO representing Doctor Account Verification details and Certificate status.
 */
public class DoctorVerificationDTO implements Serializable {

    private Long id;
    private Long doctorProfileId;
    private Long doctorId;
    private String doctorName;
    private String avatarUrl;
    private String specialization;
    private String licenseNumber;
    private Integer experienceYears;
    private String education;
    private Instant submittedAt;
    private String status; // "DRAFT", "PENDING", "SUBMITTED", "REQUIRES_ADDITIONAL_INFO", "APPROVED", "REJECTED"
    private String notes;
    private String certificateUrl;
    private String certificatePublicId;
    private String certificateFileName;
    private String certificateContentType;
    private Instant certificateUploadedAt;
    private Instant reviewedAt;
    private String reviewedBy;
    private String reviewedByName;
    private String rejectionReason;
    private Instant createdAt;
    private Instant updatedAt;

    public DoctorVerificationDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getDoctorProfileId() { return doctorProfileId; }
    public void setDoctorProfileId(Long doctorProfileId) { this.doctorProfileId = doctorProfileId; }

    public Long getDoctorId() { return doctorId; }
    public void setDoctorId(Long doctorId) {
        this.doctorId = doctorId;
        if (this.doctorProfileId == null) {
            this.doctorProfileId = doctorId;
        }
    }

    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }

    public String getLicenseNumber() { return licenseNumber; }
    public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; }

    public Integer getExperienceYears() { return experienceYears; }
    public void setExperienceYears(Integer experienceYears) { this.experienceYears = experienceYears; }

    public String getEducation() { return education; }
    public void setEducation(String education) { this.education = education; }

    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getCertificateUrl() { return certificateUrl; }
    public void setCertificateUrl(String certificateUrl) { this.certificateUrl = certificateUrl; }

    public String getCertificatePublicId() { return certificatePublicId; }
    public void setCertificatePublicId(String certificatePublicId) { this.certificatePublicId = certificatePublicId; }

    public String getCertificateFileName() { return certificateFileName; }
    public void setCertificateFileName(String certificateFileName) { this.certificateFileName = certificateFileName; }

    public String getCertificateContentType() { return certificateContentType; }
    public void setCertificateContentType(String certificateContentType) { this.certificateContentType = certificateContentType; }

    public Instant getCertificateUploadedAt() { return certificateUploadedAt; }
    public void setCertificateUploadedAt(Instant certificateUploadedAt) { this.certificateUploadedAt = certificateUploadedAt; }

    public Instant getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(Instant reviewedAt) { this.reviewedAt = reviewedAt; }

    public String getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(String reviewedBy) {
        this.reviewedBy = reviewedBy;
        if (this.reviewedByName == null) {
            this.reviewedByName = reviewedBy;
        }
    }

    public String getReviewedByName() { return reviewedByName; }
    public void setReviewedByName(String reviewedByName) { this.reviewedByName = reviewedByName; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
