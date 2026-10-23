package com.ocbs.repository;

import com.ocbs.domain.DoctorSubscription;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DoctorSubscriptionRepository extends JpaRepository<DoctorSubscription, Long> {

    List<DoctorSubscription> findAllByDoctorLoginOrderByCreatedDateDesc(String doctorLogin);

    Optional<DoctorSubscription> findFirstByDoctorLoginAndStatusOrderByCreatedDateDesc(String doctorLogin, String status);

    Optional<DoctorSubscription> findByTransactionCode(String transactionCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM DoctorSubscription s WHERE s.id = :id")
    Optional<DoctorSubscription> findByIdWithLock(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM DoctorSubscription s WHERE s.transactionCode = :transactionCode")
    Optional<DoctorSubscription> findByTransactionCodeWithLock(@Param("transactionCode") String transactionCode);
}
