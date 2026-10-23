package com.ocbs.repository;

import com.ocbs.domain.PsychometricSubmission;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PsychometricSubmissionRepository extends JpaRepository<PsychometricSubmission, Long>, JpaSpecificationExecutor<PsychometricSubmission> {

    @Query("select ps from PsychometricSubmission ps left join fetch ps.test left join fetch ps.patient p left join fetch p.user left join fetch ps.assignedByDoctor d left join fetch d.user where ps.id = :id")
    Optional<PsychometricSubmission> findOneWithDetails(@Param("id") Long id);

    @Query("select ps from PsychometricSubmission ps left join fetch ps.test left join fetch ps.assignedByDoctor d left join fetch d.user where ps.patient.id = :patientId order by ps.createdDate desc")
    List<PsychometricSubmission> findAllByPatientId(@Param("patientId") Long patientId);

    @Query("select ps from PsychometricSubmission ps left join fetch ps.test left join fetch ps.patient p left join fetch p.user where ps.assignedByDoctor.id = :doctorId order by ps.createdDate desc")
    List<PsychometricSubmission> findAllByDoctorId(@Param("doctorId") Long doctorId);
}
