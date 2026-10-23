package com.ocbs.repository;

import com.ocbs.domain.EmotionJournal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EmotionJournalRepository extends JpaRepository<EmotionJournal, Long>, JpaSpecificationExecutor<EmotionJournal> {

    @Query("select ej from EmotionJournal ej where ej.patient.id = :patientId order by ej.createdDate desc")
    List<EmotionJournal> findAllByPatientId(@Param("patientId") Long patientId);

    @Query("select ej from EmotionJournal ej where ej.patient.id = :patientId and ej.isShared = true order by ej.createdDate desc")
    List<EmotionJournal> findSharedByPatientId(@Param("patientId") Long patientId);
}
