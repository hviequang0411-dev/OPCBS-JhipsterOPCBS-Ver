package com.ocbs.repository;

import com.ocbs.domain.TreatmentSession;
import java.util.List;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

@Repository
public interface TreatmentSessionRepository extends JpaRepository<TreatmentSession, Long> {

    List<TreatmentSession> findByTreatmentPackage_IdOrderBySessionNumberAsc(Long packageId);

    long countByTreatmentPackage_Id(Long packageId);

    long countByTreatmentPackage_IdAndStatus(Long packageId, String status);

    boolean existsByAppointmentId(Long appointmentId);
}
