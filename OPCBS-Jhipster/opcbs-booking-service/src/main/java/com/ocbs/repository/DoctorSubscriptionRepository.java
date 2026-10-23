package com.ocbs.repository;

import com.ocbs.domain.DoctorSubscription;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the DoctorSubscription entity in Booking Service.
 */
@Repository
public interface DoctorSubscriptionRepository extends JpaRepository<DoctorSubscription, Long> {

    @Query(
        "SELECT s FROM DoctorSubscription s WHERE s.doctorLogin = :doctorLogin AND s.status = 'ACTIVE' AND s.expirationDate > :now ORDER BY s.expirationDate DESC"
    )
    List<DoctorSubscription> findActiveValidByDoctorLogin(@Param("doctorLogin") String doctorLogin, @Param("now") Instant now);

    @Query(
        "SELECT s FROM DoctorSubscription s WHERE s.doctorId = :doctorId AND s.status = 'ACTIVE' AND s.expirationDate > :now ORDER BY s.expirationDate DESC"
    )
    List<DoctorSubscription> findActiveValidByDoctorId(@Param("doctorId") Long doctorId, @Param("now") Instant now);

    List<DoctorSubscription> findByDoctorLoginOrderByExpirationDateDesc(String doctorLogin);

    List<DoctorSubscription> findByDoctorIdOrderByExpirationDateDesc(Long doctorId);
}
