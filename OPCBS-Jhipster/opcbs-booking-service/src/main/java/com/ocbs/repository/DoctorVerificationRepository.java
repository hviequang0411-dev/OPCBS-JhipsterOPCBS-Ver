package com.ocbs.repository;

import com.ocbs.domain.DoctorVerification;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the DoctorVerification entity.
 */
@Repository
public interface DoctorVerificationRepository extends JpaRepository<DoctorVerification, Long>, JpaSpecificationExecutor<DoctorVerification> {

    Optional<DoctorVerification> findTopByDoctorProfile_IdOrderByCreatedAtDesc(Long doctorProfileId);

    List<DoctorVerification> findByDoctorProfile_IdOrderByCreatedAtDesc(Long doctorProfileId);

    @Query("SELECT v FROM DoctorVerification v LEFT JOIN FETCH v.doctorProfile dp LEFT JOIN FETCH dp.user WHERE v.id = :id")
    Optional<DoctorVerification> findByIdWithProfile(@Param("id") Long id);

    @Query(
        value = "SELECT v FROM DoctorVerification v LEFT JOIN FETCH v.doctorProfile dp LEFT JOIN FETCH dp.user WHERE " +
            "(:status IS NULL OR :status = '' OR UPPER(v.status) = UPPER(:status)) AND " +
            "(:search IS NULL OR :search = '' OR " +
            " LOWER(v.doctorName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            " LOWER(v.licenseNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            " LOWER(v.specialization) LIKE LOWER(CONCAT('%', :search, '%')))",
        countQuery = "SELECT count(v) FROM DoctorVerification v WHERE " +
            "(:status IS NULL OR :status = '' OR UPPER(v.status) = UPPER(:status)) AND " +
            "(:search IS NULL OR :search = '' OR " +
            " LOWER(v.doctorName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            " LOWER(v.licenseNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            " LOWER(v.specialization) LIKE LOWER(CONCAT('%', :search, '%')))"
    )
    Page<DoctorVerification> findAllFiltered(
        @Param("status") String status,
        @Param("search") String search,
        Pageable pageable
    );

    @Query("SELECT v.status, count(v) FROM DoctorVerification v GROUP BY v.status")
    List<Object[]> countByStatusGroup();
}
