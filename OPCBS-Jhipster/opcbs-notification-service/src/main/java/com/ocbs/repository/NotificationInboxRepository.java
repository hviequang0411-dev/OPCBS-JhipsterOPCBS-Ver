package com.ocbs.repository;

import com.ocbs.domain.NotificationInbox;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the NotificationInbox entity.
 */
@Repository
public interface NotificationInboxRepository extends JpaRepository<NotificationInbox, Long> {

    Optional<NotificationInbox> findByEventId(String eventId);

    boolean existsByEventId(String eventId);

    boolean existsByConsumerGroupAndIdempotencyKey(String consumerGroup, String idempotencyKey);
}
