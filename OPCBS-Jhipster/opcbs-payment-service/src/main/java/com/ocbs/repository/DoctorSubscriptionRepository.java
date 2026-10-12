package com.ocbs.repository;

import com.ocbs.domain.DoctorSubscription;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DoctorSubscriptionRepository extends JpaRepository<DoctorSubscription, Long> {

    List<DoctorSubscription> findAllByDoctorLoginOrderByCreatedDateDesc(String doctorLogin);

    Optional<DoctorSubscription> findFirstByDoctorLoginAndStatusOrderByCreatedDateDesc(String doctorLogin, String status);

    Optional<DoctorSubscription> findByTransactionCode(String transactionCode);
}
