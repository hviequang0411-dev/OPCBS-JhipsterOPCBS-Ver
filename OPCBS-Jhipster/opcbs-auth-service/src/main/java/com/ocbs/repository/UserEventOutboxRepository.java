package com.ocbs.repository;

import com.ocbs.domain.UserEventOutbox;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserEventOutboxRepository extends JpaRepository<UserEventOutbox, Long> {
    @Query("select e from UserEventOutbox e where e.status = 'PENDING' or (e.status = 'FAILED' and e.nextRetryAt <= :now and e.retryCount < :maxRetries) order by e.createdAt")
    List<UserEventOutbox> findReady(@Param("now") Instant now, @Param("maxRetries") int maxRetries, Pageable pageable);
}
