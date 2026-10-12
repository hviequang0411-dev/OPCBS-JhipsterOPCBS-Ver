package com.ocbs.repository;

import com.ocbs.domain.Conversation;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Conversation entity.
 */
@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    List<Conversation> findByPatientIdOrderByLastMessageTimeDesc(Long patientId);

    List<Conversation> findByDoctorIdOrderByLastMessageTimeDesc(Long doctorId);

    @Query("SELECT c FROM Conversation c WHERE (c.patientId = :userId OR c.doctorId = :userId) ORDER BY c.lastMessageTime DESC")
    List<Conversation> findAllByUserId(@Param("userId") Long userId);

    Optional<Conversation> findByPatientIdAndDoctorId(Long patientId, Long doctorId);

    Optional<Conversation> findByAppointmentId(Long appointmentId);
}
