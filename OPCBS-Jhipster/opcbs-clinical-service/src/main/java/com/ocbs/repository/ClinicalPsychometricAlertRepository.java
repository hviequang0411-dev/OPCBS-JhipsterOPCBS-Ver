package com.ocbs.repository;

import com.ocbs.domain.ClinicalPsychometricAlert;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClinicalPsychometricAlertRepository extends JpaRepository<ClinicalPsychometricAlert, Long> {
    boolean existsBySubmissionId(Long submissionId);

    @Modifying
    @Query(value = "INSERT IGNORE INTO clinical_psychometric_alert (submission_id, patient_profile_id, severity_level, received_at) " +
        "VALUES (:submissionId, :patientId, :severity, UTC_TIMESTAMP(6))", nativeQuery = true)
    int insertIfAbsent(
        @Param("submissionId") Long submissionId,
        @Param("patientId") Long patientId,
        @Param("severity") String severity
    );
}
