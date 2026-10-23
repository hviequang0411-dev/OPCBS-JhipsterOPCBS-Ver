package com.ocbs.repository;

import com.ocbs.domain.OutboxEvent;
import com.ocbs.domain.enumeration.OutboxStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the OutboxEvent entity.
 */
@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    Optional<OutboxEvent> findByEventId(String eventId);

    @Query("SELECT o FROM OutboxEvent o WHERE o.status IN :statuses ORDER BY o.createdAt ASC")
    List<OutboxEvent> findPendingOrFailedEvents(@Param("statuses") Collection<OutboxStatus> statuses, Pageable pageable);

    @Query("SELECT o FROM OutboxEvent o WHERE (o.status = com.ocbs.domain.enumeration.OutboxStatus.PENDING OR (o.status = com.ocbs.domain.enumeration.OutboxStatus.FAILED AND (o.nextRetryAt IS NULL OR o.nextRetryAt <= :now))) AND o.retryCount < :maxRetries ORDER BY o.createdAt ASC")
    List<OutboxEvent> findReadyToDispatchEvents(@Param("now") java.time.Instant now, @Param("maxRetries") int maxRetries, Pageable pageable);

    @Query("SELECT COUNT(o) FROM OutboxEvent o WHERE o.status = com.ocbs.domain.enumeration.OutboxStatus.FAILED AND o.retryCount >= :maxRetries")
    long countExceededMaxRetries(@Param("maxRetries") int maxRetries);

    long countByStatus(OutboxStatus status);
}
