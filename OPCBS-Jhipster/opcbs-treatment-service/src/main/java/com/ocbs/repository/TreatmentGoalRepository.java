package com.ocbs.repository;

import com.ocbs.domain.TreatmentGoal;
import java.util.List;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TreatmentGoalRepository extends JpaRepository<TreatmentGoal, Long> {

    List<TreatmentGoal> findByTreatmentPackage_IdOrderByPriorityDescTargetDateAsc(Long packageId);

    long countByTreatmentPackage_Id(Long packageId);

    long countByTreatmentPackage_IdAndStatus(Long packageId, String status);

    @Query("select coalesce(avg(g.progressPercent), 0) from TreatmentGoal g where g.treatmentPackage.id = :packageId")
    Double getAverageProgressPercentByPackageId(@Param("packageId") Long packageId);
}
