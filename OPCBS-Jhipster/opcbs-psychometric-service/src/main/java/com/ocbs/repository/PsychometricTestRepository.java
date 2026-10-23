package com.ocbs.repository;

import com.ocbs.domain.PsychometricTest;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PsychometricTestRepository extends JpaRepository<PsychometricTest, Long>, JpaSpecificationExecutor<PsychometricTest> {

    @Query("select pt from PsychometricTest pt left join fetch pt.questions q where pt.id = :id")
    Optional<PsychometricTest> findOneWithQuestions(@Param("id") Long id);

    @Query("select pt from PsychometricTest pt where pt.isActive = true order by pt.id asc")
    List<PsychometricTest> findAllActive();

    @Query("select pt from PsychometricTest pt where pt.testType = :testType and pt.isActive = true")
    Optional<PsychometricTest> findFirstByTestType(@Param("testType") String testType);

    @Query("select distinct pt from PsychometricTest pt left join fetch pt.questions q order by pt.id asc")
    List<PsychometricTest> findAllWithQuestions();
}
