package com.ocbs.repository;

import com.ocbs.domain.TreatmentInbox;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the TreatmentInbox entity.
 */
@Repository
public interface TreatmentInboxRepository extends JpaRepository<TreatmentInbox, Long> {

    Optional<TreatmentInbox> findByEventId(String eventId);

    Optional<TreatmentInbox> findByAppointmentId(Long appointmentId);

    boolean existsByEventId(String eventId);

    boolean existsByAppointmentId(Long appointmentId);

    boolean existsByAppointmentIdAndEventType(Long appointmentId, String eventType);
}
