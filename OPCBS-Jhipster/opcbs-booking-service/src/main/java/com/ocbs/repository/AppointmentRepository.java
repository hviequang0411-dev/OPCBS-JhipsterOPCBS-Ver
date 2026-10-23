package com.ocbs.repository;

import com.ocbs.domain.Appointment;
import com.ocbs.domain.enumeration.AppointmentStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Appointment entity.
 */
@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long>, JpaSpecificationExecutor<Appointment> {
    default Optional<Appointment> findOneWithEagerRelationships(Long id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<Appointment> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<Appointment> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select appointment from Appointment appointment left join fetch appointment.patient p left join fetch p.user left join fetch appointment.doctor d left join fetch d.user left join fetch appointment.slot",
        countQuery = "select count(appointment) from Appointment appointment"
    )
    Page<Appointment> findAllWithToOneRelationships(Pageable pageable);

    @Query("select appointment from Appointment appointment left join fetch appointment.patient p left join fetch p.user left join fetch appointment.doctor d left join fetch d.user left join fetch appointment.slot")
    List<Appointment> findAllWithToOneRelationships();

    @Query(
        "select appointment from Appointment appointment left join fetch appointment.patient p left join fetch p.user left join fetch appointment.doctor d left join fetch d.user left join fetch appointment.slot where appointment.id =:id"
    )
    Optional<Appointment> findOneWithToOneRelationships(@Param("id") Long id);

    Optional<Appointment> findByBookingCode(String bookingCode);

    Optional<Appointment> findByIdempotencyKey(String idempotencyKey);

    @Query(
        "select a from Appointment a left join fetch a.doctor d left join fetch d.user left join fetch a.patient p left join fetch p.user left join fetch a.slot s " +
        "where a.patient.id = :patientId and (:status is null or a.status = :status) " +
        "order by a.scheduledStart desc"
    )
    List<Appointment> findByPatientIdAndStatus(@Param("patientId") Long patientId, @Param("status") AppointmentStatus status);

    @Query(
        "select a from Appointment a left join fetch a.patient p left join fetch p.user left join fetch a.doctor d left join fetch d.user left join fetch a.slot s " +
        "where a.doctor.id = :doctorId and (:status is null or a.status = :status) " +
        "order by a.scheduledStart desc"
    )
    List<Appointment> findByDoctorIdAndStatus(@Param("doctorId") Long doctorId, @Param("status") AppointmentStatus status);

    @Query(
        "select a from Appointment a left join fetch a.patient p left join fetch a.slot s " +
        "where a.doctor.id = :doctorId " +
        "order by a.scheduledStart desc"
    )
    List<Appointment> findByDoctorId(@Param("doctorId") Long doctorId);

    @Query(
        "select a from Appointment a left join fetch a.patient p left join fetch a.slot s " +
        "where a.doctor.id = :doctorId and a.scheduledStart >= :startDate and a.scheduledStart <= :endDate " +
        "order by a.scheduledStart asc"
    )
    List<Appointment> findByDoctorIdAndDateRange(
        @Param("doctorId") Long doctorId,
        @Param("startDate") java.time.Instant startDate,
        @Param("endDate") java.time.Instant endDate
    );

    @Query(
        "select a from Appointment a left join fetch a.patient p " +
        "where a.doctor.id = :doctorId and a.status = :status"
    )
    List<Appointment> findCompletedByDoctorId(
        @Param("doctorId") Long doctorId,
        @Param("status") AppointmentStatus status
    );

    @Query(
        "select a from Appointment a left join fetch a.patient p left join fetch a.doctor d left join fetch a.slot s " +
        "where a.patient.id = :patientId and a.doctor.id = :doctorId " +
        "order by a.scheduledStart desc"
    )
    List<Appointment> findByDoctorIdAndPatientId(@Param("doctorId") Long doctorId, @Param("patientId") Long patientId);

    @Query(
        "select a from Appointment a left join fetch a.patient p left join fetch a.doctor d left join fetch a.slot s " +
        "where a.patient.id = :patientId " +
        "order by a.scheduledStart desc"
    )
    List<Appointment> findByPatientId(@Param("patientId") Long patientId);

    /**
     * TỐI ƯU HÓA CHỐNG LỖI N+1 QUERY (Patient Clinical Records):
     * - Eager fetch đầy đủ p.user và d.user để MapStruct không kích hoạt lazy query phụ vào jhi_user.
     * - Lọc điều kiện bệnh án (COMPLETED hoặc có chẩn đoán/lời dặn) trực tiếp ở tầng SQL Database.
     */
    @Query(
        "select distinct a from Appointment a " +
        "left join fetch a.patient p " +
        "left join fetch p.user " +
        "left join fetch a.doctor d " +
        "left join fetch d.user " +
        "left join fetch a.slot s " +
        "where a.patient.id = :patientId " +
        "and (a.status = com.ocbs.domain.enumeration.AppointmentStatus.COMPLETED " +
        "     or a.diagnosis is not null " +
        "     or a.consultationSummary is not null " +
        "     or a.recommendations is not null) " +
        "order by a.scheduledStart desc"
    )
    List<Appointment> findClinicalRecordsByPatientId(@Param("patientId") Long patientId);

    long countByDoctorIdAndStatus(Long doctorId, AppointmentStatus status);

    long countByPatientIdAndStatus(Long patientId, AppointmentStatus status);

    long countByStatus(AppointmentStatus status);

    @Query("select a from Appointment a left join fetch a.patient p left join fetch p.user left join fetch a.doctor d left join fetch d.user left join fetch a.slot s where (:status is null or a.status = :status) order by a.scheduledStart desc")
    List<Appointment> findByStatus(@Param("status") AppointmentStatus status);

    @Query("select a from Appointment a left join fetch a.patient p left join fetch a.slot s where a.slot.id in :slotIds")
    List<Appointment> findBySlot_IdIn(@Param("slotIds") List<Long> slotIds);

    Optional<Appointment> findFirstBySlot_Id(Long slotId);

    /**
     * KIỂM TRA ZERO RAW DATA - TẦNG DATABASE:
     * 1. Đếm số ca khám active của bệnh nhân với bác sĩ trong cùng ngày.
     */
    @Query(
        "select count(a) from Appointment a " +
        "where a.patient.id = :patientId " +
        "and a.doctor.id = :doctorId " +
        "and a.status in :activeStatuses " +
        "and a.scheduledStart >= :startOfDay and a.scheduledStart <= :endOfDay"
    )
    long countActiveAppointmentsOnDate(
        @Param("patientId") Long patientId,
        @Param("doctorId") Long doctorId,
        @Param("startOfDay") java.time.Instant startOfDay,
        @Param("endOfDay") java.time.Instant endOfDay,
        @Param("activeStatuses") List<AppointmentStatus> activeStatuses
    );

    /**
     * KIỂM TRA ZERO RAW DATA - TẦNG DATABASE:
     * 2. Đếm số ca khám active của bệnh nhân với bác sĩ trong 1 tuần (Tối đa 3).
     */
    @Query(
        "select count(a) from Appointment a " +
        "where a.patient.id = :patientId " +
        "and a.doctor.id = :doctorId " +
        "and a.status in :activeStatuses " +
        "and a.scheduledStart >= :weekStart and a.scheduledStart <= :weekEnd"
    )
    long countActiveAppointmentsWithDoctorInWeek(
        @Param("patientId") Long patientId,
        @Param("doctorId") Long doctorId,
        @Param("weekStart") java.time.Instant weekStart,
        @Param("weekEnd") java.time.Instant weekEnd,
        @Param("activeStatuses") List<AppointmentStatus> activeStatuses
    );

    /**
     * KIỂM TRA ZERO RAW DATA - TẦNG DATABASE:
     * 3. Đếm tổng số ca khám active của bệnh nhân toàn hệ thống trong 1 tuần (Tối đa 5 chống spam).
     */
    @Query(
        "select count(a) from Appointment a " +
        "where a.patient.id = :patientId " +
        "and a.status in :activeStatuses " +
        "and a.scheduledStart >= :weekStart and a.scheduledStart <= :weekEnd"
    )
    long countTotalActiveAppointmentsInWeek(
        @Param("patientId") Long patientId,
        @Param("weekStart") java.time.Instant weekStart,
        @Param("weekEnd") java.time.Instant weekEnd,
        @Param("activeStatuses") List<AppointmentStatus> activeStatuses
    );

    /**
     * KIỂM TRA ZERO RAW DATA - TẦNG DATABASE:
     * 4. Kiểm tra xem bệnh nhân có lịch hẹn nào trùng khung giờ khám hay không.
     */
    @Query(
        "select count(a) > 0 from Appointment a " +
        "where a.patient.id = :patientId " +
        "and a.status in :activeStatuses " +
        "and ((a.scheduledStart <= :startTime and a.scheduledEnd > :startTime) " +
        "     or (a.scheduledStart < :endTime and a.scheduledEnd >= :endTime) " +
        "     or (a.scheduledStart >= :startTime and a.scheduledEnd <= :endTime))"
    )
    boolean existsOverlappingAppointment(
        @Param("patientId") Long patientId,
        @Param("startTime") java.time.Instant startTime,
        @Param("endTime") java.time.Instant endTime,
        @Param("activeStatuses") List<AppointmentStatus> activeStatuses
    );

    @Query("SELECT COUNT(DISTINCT a.patient.id) FROM Appointment a WHERE a.doctor.id = :doctorId AND a.status IN :statuses")
    long countDistinctPatientsByDoctorId(
        @Param("doctorId") Long doctorId,
        @Param("statuses") List<AppointmentStatus> statuses
    );

    @Query("SELECT COUNT(a) > 0 FROM Appointment a WHERE a.doctor.id = :doctorId AND a.patient.id = :patientId AND a.status IN :statuses")
    boolean existsByDoctorIdAndPatientIdAndStatusIn(
        @Param("doctorId") Long doctorId,
        @Param("patientId") Long patientId,
        @Param("statuses") List<AppointmentStatus> statuses
    );
}

