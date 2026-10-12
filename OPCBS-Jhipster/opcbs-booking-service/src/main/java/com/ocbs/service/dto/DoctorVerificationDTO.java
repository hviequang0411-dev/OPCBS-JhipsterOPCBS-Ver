package com.ocbs.service.dto;

import java.io.Serializable;
import java.time.Instant;

/**
 * DTO representing Doctor Account Verification details and Certificate status.
 */
public class DoctorVerificationDTO implements Serializable {

    private Long doctorId;
    private String doctorName;
    private String specialization;
    private String licenseNumber;
    private Integer experienceYears;
    private String education;
    private Instant submittedAt;
    private String status; // "DRAFT", "PENDING", "SUBMITTED", "APPROVED", "REJECTED"
    private String notes;
    private String certificateUrl;
    private String certificateFileName;
    private Instant certificateUploadedAt;
    private Instant reviewedAt;
    private String reviewedByName;
    private String rejectionReason;

    public DoctorVerificationDTO() {}

    public Long getDoctorId() { return doctorId; }
    public void setDoctorId(Long doctorId) { this.doctorId = doctorId; }

    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

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

    public String getCertificateFileName() { return certificateFileName; }
    public void setCertificateFileName(String certificateFileName) { this.certificateFileName = certificateFileName; }

    public Instant getCertificateUploadedAt() { return certificateUploadedAt; }
    public void setCertificateUploadedAt(Instant certificateUploadedAt) { this.certificateUploadedAt = certificateUploadedAt; }

    public Instant getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(Instant reviewedAt) { this.reviewedAt = reviewedAt; }

    public String getReviewedByName() { return reviewedByName; }
    public void setReviewedByName(String reviewedByName) { this.reviewedByName = reviewedByName; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
}
