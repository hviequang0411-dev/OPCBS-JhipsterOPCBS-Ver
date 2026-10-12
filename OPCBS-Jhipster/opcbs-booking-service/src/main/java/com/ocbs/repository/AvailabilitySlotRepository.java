package com.ocbs.repository;

import com.ocbs.domain.AvailabilitySlot;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the AvailabilitySlot entity.
 */
@Repository
public interface AvailabilitySlotRepository extends JpaRepository<AvailabilitySlot, Long>, JpaSpecificationExecutor<AvailabilitySlot> {
    default Optional<AvailabilitySlot> findOneWithEagerRelationships(Long id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<AvailabilitySlot> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<AvailabilitySlot> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM AvailabilitySlot s LEFT JOIN FETCH s.doctor WHERE s.id = :id")
    Optional<AvailabilitySlot> findByIdWithLock(@Param("id") Long id);

    @Query(
        value = "select availabilitySlot from AvailabilitySlot availabilitySlot left join fetch availabilitySlot.doctor",
        countQuery = "select count(availabilitySlot) from AvailabilitySlot availabilitySlot"
    )
    Page<AvailabilitySlot> findAllWithToOneRelationships(Pageable pageable);

    @Query("select availabilitySlot from AvailabilitySlot availabilitySlot left join fetch availabilitySlot.doctor")
    List<AvailabilitySlot> findAllWithToOneRelationships();

    @Query(
        "select availabilitySlot from AvailabilitySlot availabilitySlot left join fetch availabilitySlot.doctor where availabilitySlot.id =:id"
    )
    Optional<AvailabilitySlot> findOneWithToOneRelationships(@Param("id") Long id);

    List<AvailabilitySlot> findByDoctor_IdOrderByStartTimeAsc(Long doctorId);

    List<AvailabilitySlot> findByDoctor_IdAndStartTimeBetweenOrderByStartTimeAsc(
        Long doctorId,
        java.time.Instant start,
        java.time.Instant end
    );

    @Query(
        "select count(s) > 0 from AvailabilitySlot s where s.doctor.id = :doctorId and s.startTime < :endTime and s.endTime > :startTime"
    )
    boolean existsOverlap(
        @Param("doctorId") Long doctorId,
        @Param("startTime") java.time.Instant startTime,
        @Param("endTime") java.time.Instant endTime
    );

    @Query(
        "select count(s) > 0 from AvailabilitySlot s where s.doctor.id = :doctorId and s.id <> :slotId and s.startTime < :endTime and s.endTime > :startTime"
    )
    boolean existsOverlapExcluding(
        @Param("doctorId") Long doctorId,
        @Param("slotId") Long slotId,
        @Param("startTime") java.time.Instant startTime,
        @Param("endTime") java.time.Instant endTime
    );
}
