package com.ocbs.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Minimal, idempotent receipt for a high-risk assessment alert; raw clinical scores are not copied here. */
@Entity
@Table(name = "clinical_psychometric_alert")
public class ClinicalPsychometricAlert {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "submission_id", nullable = false, unique = true) private Long submissionId;
    @Column(name = "patient_profile_id", nullable = false) private Long patientProfileId;
    @Column(name = "severity_level", nullable = false, length = 80) private String severityLevel;
    @Column(name = "received_at", nullable = false) private Instant receivedAt = Instant.now();

    public Long getId() { return id; }
    public Long getSubmissionId() { return submissionId; }
    public void setSubmissionId(Long value) { submissionId = value; }
    public Long getPatientProfileId() { return patientProfileId; }
    public void setPatientProfileId(Long value) { patientProfileId = value; }
    public String getSeverityLevel() { return severityLevel; }
    public void setSeverityLevel(String value) { severityLevel = value; }
    public Instant getReceivedAt() { return receivedAt; }
}
