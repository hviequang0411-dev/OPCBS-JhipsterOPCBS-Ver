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

    @Query("SELECT c FROM Conversation c WHERE (c.patientId IN :ids OR c.doctorId IN :ids) ORDER BY c.lastMessageTime DESC")
    List<Conversation> findAllByParticipantIds(@Param("ids") java.util.Collection<Long> ids);

    Optional<Conversation> findByPatientIdAndDoctorId(Long patientId, Long doctorId);

    @Query("SELECT c FROM Conversation c WHERE (c.doctorId IN :doctorIds AND c.patientId IN :patientIds)")
    List<Conversation> findExistingConversationsBetween(
        @Param("doctorIds") java.util.Collection<Long> doctorIds,
        @Param("patientIds") java.util.Collection<Long> patientIds
    );

    Optional<Conversation> findByAppointmentId(Long appointmentId);
}
