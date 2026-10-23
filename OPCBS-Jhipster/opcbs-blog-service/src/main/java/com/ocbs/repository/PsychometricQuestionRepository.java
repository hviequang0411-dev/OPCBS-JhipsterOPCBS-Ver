package com.ocbs.repository;

import com.ocbs.domain.PsychometricQuestion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PsychometricQuestionRepository extends JpaRepository<PsychometricQuestion, Long>, JpaSpecificationExecutor<PsychometricQuestion> {

    @Query("select pq from PsychometricQuestion pq where pq.test.id = :testId order by pq.questionNumber asc")
    List<PsychometricQuestion> findAllByTestId(@Param("testId") Long testId);
}
