package com.ocbs.repository;

import com.ocbs.domain.PaymentEventOutbox;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentEventOutboxRepository extends JpaRepository<PaymentEventOutbox, Long> {
    @Query("select e from PaymentEventOutbox e where e.status = 'PENDING' or (e.status = 'FAILED' and e.nextRetryAt <= :now and e.retryCount < :maxRetries) order by e.createdAt")
    List<PaymentEventOutbox> findReady(@Param("now") Instant now, @Param("maxRetries") int maxRetries, Pageable pageable);
}
