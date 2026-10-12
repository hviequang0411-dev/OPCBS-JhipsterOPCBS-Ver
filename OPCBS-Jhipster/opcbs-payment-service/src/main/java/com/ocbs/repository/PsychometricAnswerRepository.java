package com.ocbs.repository;

import com.ocbs.domain.PsychometricAnswer;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PsychometricAnswerRepository extends JpaRepository<PsychometricAnswer, Long>, JpaSpecificationExecutor<PsychometricAnswer> {

    @Query("select pa from PsychometricAnswer pa left join fetch pa.question where pa.submission.id = :submissionId")
    List<PsychometricAnswer> findAllBySubmissionId(@Param("submissionId") Long submissionId);
}
