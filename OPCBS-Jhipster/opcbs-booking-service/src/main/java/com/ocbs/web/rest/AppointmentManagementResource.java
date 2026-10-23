package com.ocbs.web.rest;

import com.ocbs.domain.Appointment;
import com.ocbs.domain.AvailabilitySlot;
import com.ocbs.domain.DoctorProfile;
import com.ocbs.domain.PatientProfile;
import com.ocbs.domain.User;
import com.ocbs.domain.enumeration.AppointmentStatus;
import com.ocbs.domain.enumeration.SlotStatus;
import com.ocbs.service.AppointmentOutboxService;
import com.ocbs.repository.AppointmentRepository;
import com.ocbs.repository.AvailabilitySlotRepository;
import com.ocbs.repository.DoctorProfileRepository;
import com.ocbs.repository.PatientProfileRepository;
import com.ocbs.repository.UserRepository;
import com.ocbs.security.SecurityUtils;
import com.ocbs.service.dto.AppointmentDTO;
import com.ocbs.service.dto.AvailabilitySlotDTO;
import com.ocbs.service.mapper.AppointmentMapper;
import com.ocbs.service.mapper.AvailabilitySlotMapper;
import com.ocbs.web.rest.errors.BadRequestAlertException;
import com.ocbs.web.rest.vm.BookingVM;
import jakarta.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing Patient Bookings and Doctor Appointment workflows.
 */
@RestController
@RequestMapping("/api")
@Transactional
public class AppointmentManagementResource {

    private static final Logger LOG = LoggerFactory.getLogger(AppointmentManagementResource.class);
    private static final String ENTITY_NAME = "appointment";

    private final AppointmentRepository appointmentRepository;
    private final AvailabilitySlotRepository slotRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final UserRepository userRepository;
    private final AppointmentOutboxService appointmentOutboxService;
    private final AppointmentMapper appointmentMapper;
    private final AvailabilitySlotMapper availabilitySlotMapper;
    private final com.ocbs.service.mapper.PatientProfileMapper patientProfileMapper;
    private final com.ocbs.service.DoctorDashboardService doctorDashboardService;
    private final com.ocbs.service.DoctorSubscriptionEnforcementService doctorSubscriptionEnforcementService;

    public AppointmentManagementResource(
        AppointmentRepository appointmentRepository,
        AvailabilitySlotRepository slotRepository,
        PatientProfileRepository patientProfileRepository,
        DoctorProfileRepository doctorProfileRepository,
        UserRepository userRepository,
        AppointmentOutboxService appointmentOutboxService,
        AppointmentMapper appointmentMapper,
        AvailabilitySlotMapper availabilitySlotMapper,
        com.ocbs.service.mapper.PatientProfileMapper patientProfileMapper,
        @org.springframework.beans.factory.annotation.Autowired(required = false) com.ocbs.service.DoctorDashboardService doctorDashboardService,
        com.ocbs.service.DoctorSubscriptionEnforcementService doctorSubscriptionEnforcementService
    ) {
        this.appointmentRepository = appointmentRepository;
        this.slotRepository = slotRepository;
        this.patientProfileRepository = patientProfileRepository;
        this.doctorProfileRepository = doctorProfileRepository;
        this.userRepository = userRepository;
        this.appointmentOutboxService = appointmentOutboxService;
        this.appointmentMapper = appointmentMapper;
        this.availabilitySlotMapper = availabilitySlotMapper;
        this.patientProfileMapper = patientProfileMapper;
        this.doctorDashboardService = doctorDashboardService;
        this.doctorSubscriptionEnforcementService = doctorSubscriptionEnforcementService;
    }

    /**
     * Xóa cache Redis của Bác sĩ sau khi giao dịch DB commit thành công (Post-Commit Invalidation).
     * Tuyệt đối không làm fail nghiệp vụ chính nếu Redis gặp sự cố.
     */
    private void evictDoctorDashboardAfterCommit(DoctorProfile doctor) {
        if (doctor == null || doctorDashboardService == null) {
            return;
        }
        String doctorLogin = null;
        if (doctor.getUser() != null && doctor.getUser().getLogin() != null) {
            doctorLogin = doctor.getUser().getLogin();
        } else if (doctor.getId() != null) {
            doctorLogin = doctorProfileRepository.findById(doctor.getId())
                .filter(d -> d.getUser() != null)
                .map(d -> d.getUser().getLogin())
                .orElse(null);
        }
        if (doctorLogin == null || doctorLogin.isBlank()) {
            return;
        }

        final String finalLogin = doctorLogin;
        if (org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()) {
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        try {
                            doctorDashboardService.evictDoctorDashboardCache(finalLogin);
                        } catch (Exception ex) {
                            LOG.warn("⚠️ [Post-Commit Eviction] Không thể xóa cache bác sĩ '{}': {}", finalLogin, ex.getMessage());
                        }
                    }
                }
            );
        } else {
            try {
                doctorDashboardService.evictDoctorDashboardCache(finalLogin);
            } catch (Exception ex) {
                LOG.warn("⚠️ [Direct Eviction] Không thể xóa cache bác sĩ '{}': {}", finalLogin, ex.getMessage());
            }
        }
    }

    // ==========================================
    // 1. PATIENT BOOKING & SLOT ENDPOINTS
    // ==========================================

    /**
     * {@code GET /patient/doctors/:doctorId/slots} : Lấy danh sách ca khám trống (AVAILABLE) sắp tới của bác sĩ cho bệnh nhân xem/đặt lịch.
     */
    @GetMapping({"/patient/doctors/{doctorId}/slots", "/patient/doctors/slots", "/patient/doctors/slot"})
    public ResponseEntity<List<AvailabilitySlotDTO>> getDoctorAvailableSlotsForPatient(
        @PathVariable(name = "doctorId", required = false) Long pathDoctorId,
        @RequestParam(name = "doctorId", required = false) Long queryDoctorId,
        @RequestParam(name = "startDate", required = false) Instant startDate,
        @RequestParam(name = "endDate", required = false) Instant endDate
    ) {
        Long doctorId = pathDoctorId != null ? pathDoctorId : queryDoctorId;
        if (doctorId == null) {
            throw new BadRequestAlertException("Cần cung cấp doctorId để lấy lịch rảnh", ENTITY_NAME, "doctoridrequired");
        }

        Instant effectiveStart = startDate != null ? startDate : Instant.now();
        List<AvailabilitySlot> slots;
        if (endDate != null) {
            slots = slotRepository.findByDoctor_IdAndStatusAndStartTimeBetweenOrderByStartTimeAsc(
                doctorId, SlotStatus.AVAILABLE, effectiveStart, endDate
            );
        } else {
            slots = slotRepository.findByDoctor_IdAndStatusAndStartTimeGreaterThanEqualOrderByStartTimeAsc(
                doctorId, SlotStatus.AVAILABLE, effectiveStart
            );
        }
        LOG.debug("Found {} available upcoming slots for doctor ID {}", slots.size(), doctorId);
        return ResponseEntity.ok(availabilitySlotMapper.toDto(slots));
    }

    /**
     * {@code POST /patient/appointments/book} : Patient books an appointment slot.
     */
    @PostMapping("/patient/appointments/book")
    @PreAuthorize("hasAuthority('ROLE_PATIENT') or hasAuthority('ROLE_ADMIN')")
    @Transactional
    public ResponseEntity<AppointmentDTO> bookAppointment(
        @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
        @Valid @RequestBody BookingVM.CreateBookingVM bookingVM
    ) throws URISyntaxException {
        LOG.debug("REST request by Patient to book an appointment: {}", bookingVM);

        // IDEMPOTENCY GUARD: Kiểm tra xem request với Idempotency-Key này đã được xử lý chưa
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<Appointment> existingAppt = appointmentRepository.findByIdempotencyKey(idempotencyKey);
            if (existingAppt.isPresent()) {
                LOG.info("⚡ [Idempotency Guard] Trả lại kết quả lịch hẹn đã đặt trước đó cho Idempotency-Key: {}", idempotencyKey);
                return ResponseEntity.ok(appointmentMapper.toDto(existingAppt.get()));
            }
        }

        String login = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new BadRequestAlertException("User not authenticated", ENTITY_NAME, "unauthenticated"));

        PatientProfile patient = patientProfileRepository
            .findByUser_Login(login)
            .orElseGet(() -> {
                User user = userRepository.findOneByLogin(login)
                    .orElseThrow(() -> new BadRequestAlertException("Patient profile not found", ENTITY_NAME, "patientnotfound"));
                PatientProfile newProfile = new PatientProfile();
                String name = ((user.getFirstName() != null ? user.getFirstName() + " " : "") + (user.getLastName() != null ? user.getLastName() : "")).trim();
                if (name.isBlank()) {
                    name = user.getLogin();
                }
                newProfile.setFullName(name);
                newProfile.setPhone(bookingVM.getContactPhone() != null && !bookingVM.getContactPhone().isBlank() ? bookingVM.getContactPhone() : "0900000000");
                newProfile.setUser(user);
                return patientProfileRepository.save(newProfile);
            });

        // PESSIMISTIC LOCK: Khóa slot ở tầng DB ngăn hai request đặt cùng slot đồng thời (Race Condition Protection)
        AvailabilitySlot slot = slotRepository
            .findByIdWithLock(bookingVM.getSlotId())
            .orElseThrow(() -> new BadRequestAlertException("Khung giờ không tồn tại", ENTITY_NAME, "slotnotfound"));

        if (slot.getStatus() != SlotStatus.AVAILABLE) {
            throw new BadRequestAlertException("Khung giờ này không còn trống hoặc đã có người đặt", ENTITY_NAME, "slotnotavailable");
        }

        if (slot.getStartTime().isBefore(Instant.now())) {
            throw new BadRequestAlertException("Không thể đặt khung giờ trong quá khứ", ENTITY_NAME, "slotinpast");
        }

        DoctorProfile doctor = slot.getDoctor();
        if (doctor == null) {
            throw new BadRequestAlertException("Bác sĩ không tồn tại cho khung giờ này", ENTITY_NAME, "doctornotfound");
        }

        // KIỂM TRA QUYỀN LỢI GÓI DỊCH VỤ: Bác sĩ phải có gói ACTIVE hợp lệ và chưa vượt giới hạn thân chủ (maxPatientCapacity)
        doctorSubscriptionEnforcementService.validateDoctorCanBeBooked(doctor, patient.getId());

        // --------------------------------------------------------------------------------------------------
        // NGHIỆP VỤ BẢO VỆ CHỐNG SPAM & RATE LIMITING Ở TẦNG DB (ZERO RAW DATA)
        // --------------------------------------------------------------------------------------------------
        List<AppointmentStatus> activeStatuses = List.of(
            AppointmentStatus.PENDING,
            AppointmentStatus.ACCEPTED,
            AppointmentStatus.IN_PROGRESS
        );

        ZoneId zone = ZoneId.systemDefault();
        LocalDate slotLocalDate = slot.getStartTime().atZone(zone).toLocalDate();
        Instant startOfDay = slotLocalDate.atStartOfDay(zone).toInstant();
        Instant endOfDay = slotLocalDate.plusDays(1).atStartOfDay(zone).minusNanos(1).toInstant();

        // 1. Ràng buộc: 1 bệnh nhân chỉ có thể đặt 1 bác sĩ tối đa 1 lần trong ngày
        long sameDayCount = appointmentRepository.countActiveAppointmentsOnDate(
            patient.getId(), doctor.getId(), startOfDay, endOfDay, activeStatuses
        );
        if (sameDayCount > 0) {
            throw new BadRequestAlertException(
                "Bạn đã có lịch hẹn với Bác sĩ " + doctor.getFullName() + " vào ngày này. Mỗi bệnh nhân chỉ được đặt tối đa 1 ca khám/ngày với cùng một bác sĩ.",
                ENTITY_NAME,
                "maxonedailyappointmentwithdoctor"
            );
        }

        // 2. Ràng buộc: Chống trùng khung giờ khám (Overlapping Time Conflict)
        boolean hasOverlap = appointmentRepository.existsOverlappingAppointment(
            patient.getId(), slot.getStartTime(), slot.getEndTime(), activeStatuses
        );
        if (hasOverlap) {
            throw new BadRequestAlertException(
                "Bạn đã có một lịch hẹn khác diễn ra trong cùng khung giờ này. Vui lòng chọn thời gian khác.",
                ENTITY_NAME,
                "overlappingappointment"
            );
        }

        // 3. Ràng buộc: Rate Limiting theo tuần (Thứ Hai đến Chủ Nhật)
        LocalDate mondayOfWeek = slotLocalDate.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        LocalDate sundayOfWeek = mondayOfWeek.plusDays(6);
        Instant weekStart = mondayOfWeek.atStartOfDay(zone).toInstant();
        Instant weekEnd = sundayOfWeek.plusDays(1).atStartOfDay(zone).minusNanos(1).toInstant();

        // 3.1. Tối đa 3 ca khám active trong tuần với cùng 1 bác sĩ (Chuẩn C# gốc)
        long countWithDoctorInWeek = appointmentRepository.countActiveAppointmentsWithDoctorInWeek(
            patient.getId(), doctor.getId(), weekStart, weekEnd, activeStatuses
        );
        if (countWithDoctorInWeek >= 3) {
            throw new BadRequestAlertException(
                "Bạn đã đạt giới hạn 3 lịch hẹn đang hoạt động trong tuần (" + mondayOfWeek + " đến " + sundayOfWeek + ") với Bác sĩ " + doctor.getFullName() + ".",
                ENTITY_NAME,
                "maxthreeappointmentsperweekwithdoctor"
            );
        }

        // 3.2. Tối đa 5 ca khám active trong tuần trên toàn hệ thống (Chống spam / bot đặt chỗ)
        long totalCountInWeek = appointmentRepository.countTotalActiveAppointmentsInWeek(
            patient.getId(), weekStart, weekEnd, activeStatuses
        );
        if (totalCountInWeek >= 5) {
            throw new BadRequestAlertException(
                "Bạn đã đạt hạn mức tối đa 5 lịch hẹn trong tuần trên toàn hệ thống MindBridge. Vui lòng hoàn thành các buổi khám trước khi đặt thêm.",
                ENTITY_NAME,
                "maxtotalappointmentsperweek"
            );
        }

        // Cập nhật thông tin liên hệ của bệnh nhân nếu có
        if (bookingVM.getContactPhone() != null && !bookingVM.getContactPhone().isBlank()) {
            patient.setPhone(bookingVM.getContactPhone());
        }
        if (bookingVM.getPatientAddress() != null && !bookingVM.getPatientAddress().isBlank()) {
            patient.setAddress(bookingVM.getPatientAddress());
        }
        patientProfileRepository.save(patient);

        // Sinh mã đặt lịch chuẩn: MB-yyyyMMdd-XXXX
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomPart = UUID.randomUUID().toString().substring(0, 5).toUpperCase();
        String bookingCode = "MB-" + datePart + "-" + randomPart;

        // Tạo Appointment
        Appointment appointment = new Appointment();
        appointment.setBookingCode(bookingCode);
        appointment.setScheduledStart(slot.getStartTime());
        appointment.setScheduledEnd(slot.getEndTime());
        appointment.setConsultationMode(bookingVM.getConsultationMode());
        appointment.setStatus(AppointmentStatus.PENDING);

        // Tổng hợp ghi chú lâm sàng / triệu chứng tiền khám
        StringBuilder reasonBuilder = new StringBuilder();
        if (bookingVM.getReason() != null && !bookingVM.getReason().isBlank()) {
            reasonBuilder.append("Lý do: ").append(bookingVM.getReason()).append("\n");
        }
        if (bookingVM.getSymptoms() != null && !bookingVM.getSymptoms().isBlank()) {
            reasonBuilder.append("Triệu chứng: ").append(bookingVM.getSymptoms()).append("\n");
        }
        if (bookingVM.getMedicalHistory() != null && !bookingVM.getMedicalHistory().isBlank()) {
            reasonBuilder.append("Tiền sử: ").append(bookingVM.getMedicalHistory()).append("\n");
        }
        if (bookingVM.getExpectations() != null && !bookingVM.getExpectations().isBlank()) {
            reasonBuilder.append("Kỳ vọng: ").append(bookingVM.getExpectations());
        }
        appointment.setReason(reasonBuilder.toString().trim());

        appointment.setCreatedAt(Instant.now());
        appointment.setUpdatedAt(Instant.now());
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setSlot(slot);
        appointment.setIdempotencyKey(idempotencyKey);

        Appointment savedAppointment = appointmentRepository.save(appointment);

        // Chuyển slot sang BOOKED
        slot.setStatus(SlotStatus.BOOKED);
        slotRepository.save(slot);

        // Kafka Notification consumer là nơi duy nhất tạo thông báo; Booking chỉ ghi outbox cùng transaction.
        // Lưu sự kiện vào Transactional Outbox (commit đồng bộ với savedAppointment)
        try {
            appointmentOutboxService.recordAppointmentEvent(
                savedAppointment,
                "APPOINTMENT_BOOKED",
                null,
                savedAppointment.getStatus() != null ? savedAppointment.getStatus().name() : "PENDING",
                null
            );
        } catch (Exception ex) {
            throw new IllegalStateException("Could not persist APPOINTMENT_BOOKED outbox event; rolling back appointment change", ex);
        }

        evictDoctorDashboardAfterCommit(doctor);

        AppointmentDTO result = appointmentMapper.toDto(savedAppointment);
        return ResponseEntity.created(new URI("/api/patient/appointments/" + result.getId())).body(result);
    }

    /**
     * {@code GET /patient/my-appointments} : Get all appointments for current patient.
     */
    @GetMapping("/patient/my-appointments")
    @PreAuthorize("hasAuthority('ROLE_PATIENT')")
    public ResponseEntity<List<AppointmentDTO>> getMyPatientAppointments(
        @RequestParam(name = "status", required = false) AppointmentStatus status
    ) {
        String login = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new BadRequestAlertException("User not authenticated", ENTITY_NAME, "unauthenticated"));

        PatientProfile patient = patientProfileRepository
            .findByUser_Login(login)
            .orElseThrow(() -> new BadRequestAlertException("Patient profile not found", ENTITY_NAME, "patientnotfound"));

        List<Appointment> list = appointmentRepository.findByPatientIdAndStatus(patient.getId(), status);
        return ResponseEntity.ok(appointmentMapper.toDto(list));
    }

    /**
     * {@code PUT /patient/appointments/:id/cancel} : Patient cancels an appointment.
     */
    @PutMapping("/patient/appointments/{id}/cancel")
    @PreAuthorize("hasAuthority('ROLE_PATIENT')")
    public ResponseEntity<AppointmentDTO> cancelAppointmentByPatient(
        @PathVariable(name = "id") Long id,
        @RequestBody(required = false) BookingVM.CancelBookingVM cancelVM
    ) {
        String login = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new BadRequestAlertException("User not authenticated", ENTITY_NAME, "unauthenticated"));

        PatientProfile patient = patientProfileRepository
            .findByUser_Login(login)
            .orElseThrow(() -> new BadRequestAlertException("Patient profile not found", ENTITY_NAME, "patientnotfound"));

        Appointment appointment = appointmentRepository
            .findOneWithToOneRelationships(id)
            .orElseThrow(() -> new BadRequestAlertException("Không tìm thấy lịch hẹn", ENTITY_NAME, "idnotfound"));

        if (!appointment.getPatient().getId().equals(patient.getId())) {
            throw new BadRequestAlertException("Bạn không có quyền hủy lịch hẹn của người khác", ENTITY_NAME, "accessdenied");
        }

        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new BadRequestAlertException("Không thể hủy ca khám đã hoàn thành", ENTITY_NAME, "cannotcancelcompleted");
        }

        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new BadRequestAlertException("Lịch hẹn này đã bị hủy trước đó", ENTITY_NAME, "alreadycancelled");
        }

        AppointmentStatus oldStatus = appointment.getStatus();
        String reason = cancelVM != null ? cancelVM.getCancellationReason() : null;
        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setUpdatedAt(Instant.now());
        if (reason != null) {
            appointment.setCancellationReason(reason);
        }

        // Trả lại slot về AVAILABLE nếu slot vẫn tồn tại
        AvailabilitySlot slot = appointment.getSlot();
        if (slot != null) {
            slot.setStatus(SlotStatus.AVAILABLE);
            slotRepository.save(slot);
        }

        Appointment updated = appointmentRepository.save(appointment);

        // Lưu sự kiện vào Transactional Outbox (commit đồng bộ với updated appointment)
        try {
            appointmentOutboxService.recordAppointmentEvent(
                updated,
                "APPOINTMENT_CANCELLED",
                oldStatus != null ? oldStatus.name() : null,
                AppointmentStatus.CANCELLED.name(),
                reason
            );
        } catch (Exception ex) {
            throw new IllegalStateException("Could not persist APPOINTMENT_CANCELLED outbox event; rolling back appointment change", ex);
        }

        evictDoctorDashboardAfterCommit(appointment.getDoctor());

        return ResponseEntity.ok(appointmentMapper.toDto(updated));
    }

    // ==========================================
    // 2. DOCTOR APPOINTMENT MANAGEMENT ENDPOINTS
    // ==========================================

    /**
     * {@code GET /doctor/appointments} : Get all appointments for current doctor.
     */
    @GetMapping("/doctor/appointments")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR', 'ROLE_ADMIN')")
    @Transactional(readOnly = true)
    public ResponseEntity<List<AppointmentDTO>> getMyDoctorAppointments(
        @RequestParam(name = "status", required = false) AppointmentStatus status
    ) {
        String login = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new BadRequestAlertException("User not authenticated", ENTITY_NAME, "unauthenticated"));

        Optional<DoctorProfile> doctorOpt = doctorProfileRepository.findByUser_Login(login);
        List<Appointment> list;
        if (doctorOpt.isPresent()) {
            list = appointmentRepository.findByDoctorIdAndStatus(doctorOpt.get().getId(), status);
        } else if (SecurityUtils.hasCurrentUserAnyOfAuthorities(com.ocbs.security.AuthoritiesConstants.ADMIN)) {
            list = appointmentRepository.findByStatus(status);
        } else {
            throw new BadRequestAlertException("Doctor profile not found", ENTITY_NAME, "doctornotfound");
        }

        return ResponseEntity.ok(appointmentMapper.toDto(list));
    }

    /**
     * {@code GET /doctor/appointments/stats} : Get stats for doctor dashboard.
     */
    @GetMapping("/doctor/appointments/stats")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR', 'ROLE_ADMIN')")
    @Transactional(readOnly = true)
    public ResponseEntity<BookingVM.AppointmentStatsVM> getDoctorAppointmentStats() {
        String login = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new BadRequestAlertException("User not authenticated", ENTITY_NAME, "unauthenticated"));

        Optional<DoctorProfile> doctorOpt = doctorProfileRepository.findByUser_Login(login);
        if (doctorOpt.isPresent()) {
            Long docId = doctorOpt.get().getId();
            long pending = appointmentRepository.countByDoctorIdAndStatus(docId, AppointmentStatus.PENDING);
            long accepted = appointmentRepository.countByDoctorIdAndStatus(docId, AppointmentStatus.ACCEPTED);
            long inProgress = appointmentRepository.countByDoctorIdAndStatus(docId, AppointmentStatus.IN_PROGRESS);
            long completed = appointmentRepository.countByDoctorIdAndStatus(docId, AppointmentStatus.COMPLETED);
            long cancelled = appointmentRepository.countByDoctorIdAndStatus(docId, AppointmentStatus.CANCELLED);
            long rejected = appointmentRepository.countByDoctorIdAndStatus(docId, AppointmentStatus.REJECTED);
            long total = pending + accepted + inProgress + completed + cancelled + rejected;

            return ResponseEntity.ok(new BookingVM.AppointmentStatsVM(total, pending, accepted, inProgress, completed, cancelled, rejected));
        } else if (SecurityUtils.hasCurrentUserAnyOfAuthorities(com.ocbs.security.AuthoritiesConstants.ADMIN)) {
            long pending = appointmentRepository.countByStatus(AppointmentStatus.PENDING);
            long accepted = appointmentRepository.countByStatus(AppointmentStatus.ACCEPTED);
            long inProgress = appointmentRepository.countByStatus(AppointmentStatus.IN_PROGRESS);
            long completed = appointmentRepository.countByStatus(AppointmentStatus.COMPLETED);
            long cancelled = appointmentRepository.countByStatus(AppointmentStatus.CANCELLED);
            long rejected = appointmentRepository.countByStatus(AppointmentStatus.REJECTED);
            long total = pending + accepted + inProgress + completed + cancelled + rejected;

            return ResponseEntity.ok(new BookingVM.AppointmentStatsVM(total, pending, accepted, inProgress, completed, cancelled, rejected));
        } else {
            throw new BadRequestAlertException("Doctor profile not found", ENTITY_NAME, "doctornotfound");
        }
    }

    /**
     * {@code PUT /doctor/appointments/:id/accept} : Doctor accepts appointment.
     */
    @PutMapping("/doctor/appointments/{id}/accept")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR', 'ROLE_ADMIN')")
    public ResponseEntity<AppointmentDTO> acceptAppointment(@PathVariable(name = "id") Long id) {
        String login = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new BadRequestAlertException("User not authenticated", ENTITY_NAME, "unauthenticated"));

        Optional<DoctorProfile> doctorOpt = doctorProfileRepository.findByUser_Login(login);

        Appointment appointment = appointmentRepository
            .findOneWithToOneRelationships(id)
            .orElseThrow(() -> new BadRequestAlertException("Không tìm thấy lịch hẹn", ENTITY_NAME, "idnotfound"));

        if (doctorOpt.isPresent() && appointment.getDoctor() != null) {
            if (!appointment.getDoctor().getId().equals(doctorOpt.get().getId()) && !SecurityUtils.hasCurrentUserAnyOfAuthorities(com.ocbs.security.AuthoritiesConstants.ADMIN)) {
                throw new BadRequestAlertException("Bạn không có quyền thao tác trên lịch hẹn này", ENTITY_NAME, "accessdenied");
            }
        }

        if (appointment.getStatus() != AppointmentStatus.PENDING) {
            throw new BadRequestAlertException("Chỉ có thể chấp nhận lịch hẹn đang ở trạng thái chờ duyệt (PENDING)", ENTITY_NAME, "invalidstatus");
        }

        // KIỂM TRA QUYỀN LỢI GÓI DỊCH VỤ: Bác sĩ phải có gói ACTIVE hợp lệ và chưa vượt giới hạn thân chủ (maxPatientCapacity)
        if (appointment.getDoctor() != null && appointment.getPatient() != null) {
            doctorSubscriptionEnforcementService.validateDoctorCanAcceptPatient(appointment.getDoctor(), appointment.getPatient().getId());
        }

        appointment.setStatus(AppointmentStatus.ACCEPTED);
        appointment.setUpdatedAt(Instant.now());

        Appointment updated = appointmentRepository.save(appointment);

        // Lưu sự kiện vào Transactional Outbox (commit đồng bộ với updated appointment)
        try {
            appointmentOutboxService.recordAppointmentEvent(
                updated,
                "APPOINTMENT_CONFIRMED",
                AppointmentStatus.PENDING.name(),
                AppointmentStatus.ACCEPTED.name(),
                null
            );
        } catch (Exception ex) {
            throw new IllegalStateException("Could not persist APPOINTMENT_CONFIRMED outbox event; rolling back appointment change", ex);
        }

        evictDoctorDashboardAfterCommit(updated.getDoctor());

        return ResponseEntity.ok(appointmentMapper.toDto(updated));
    }

    /**
     * {@code PUT /doctor/appointments/:id/start} : Doctor starts the appointment (IN_PROGRESS).
     */
    @PutMapping("/doctor/appointments/{id}/start")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR', 'ROLE_ADMIN')")
    public ResponseEntity<AppointmentDTO> startAppointment(@PathVariable(name = "id") Long id) {
        String login = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new BadRequestAlertException("User not authenticated", ENTITY_NAME, "unauthenticated"));

        Optional<DoctorProfile> doctorOpt = doctorProfileRepository.findByUser_Login(login);

        Appointment appointment = appointmentRepository
            .findOneWithToOneRelationships(id)
            .orElseThrow(() -> new BadRequestAlertException("Không tìm thấy lịch hẹn", ENTITY_NAME, "idnotfound"));

        if (doctorOpt.isPresent() && appointment.getDoctor() != null) {
            if (!appointment.getDoctor().getId().equals(doctorOpt.get().getId()) && !SecurityUtils.hasCurrentUserAnyOfAuthorities(com.ocbs.security.AuthoritiesConstants.ADMIN)) {
                throw new BadRequestAlertException("Bạn không có quyền thao tác trên lịch hẹn này", ENTITY_NAME, "accessdenied");
            }
        }

        if (appointment.getStatus() != AppointmentStatus.ACCEPTED) {
            throw new BadRequestAlertException("Chỉ có thể bắt đầu ca khám đã được xác nhận (ACCEPTED)", ENTITY_NAME, "invalidstatus");
        }

        // Cho phép bắt đầu sớm trước tối đa 15 phút
        if (appointment.getScheduledStart() != null) {
            Instant allowedStartTime = appointment.getScheduledStart().minus(java.time.Duration.ofMinutes(15));
            if (Instant.now().isBefore(allowedStartTime)) {
                throw new BadRequestAlertException("Chưa đến thời gian bắt đầu ca khám (chỉ có thể bắt đầu trước tối đa 15 phút so với giờ hẹn)", ENTITY_NAME, "notstartedyet");
            }
        }

        appointment.setStatus(AppointmentStatus.IN_PROGRESS);
        appointment.setUpdatedAt(Instant.now());

        Appointment updated = appointmentRepository.save(appointment);

        // Publish the start transition through the same transactional outbox as other lifecycle changes.
        appointmentOutboxService.recordAppointmentEvent(
            updated,
            "APPOINTMENT_STARTED",
            AppointmentStatus.ACCEPTED.name(),
            AppointmentStatus.IN_PROGRESS.name(),
            null
        );

        evictDoctorDashboardAfterCommit(updated.getDoctor());

        return ResponseEntity.ok(appointmentMapper.toDto(updated));
    }

    /**
     * {@code PUT /doctor/appointments/:id/reject} : Doctor rejects appointment.
     */
    @PutMapping("/doctor/appointments/{id}/reject")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR', 'ROLE_ADMIN')")
    public ResponseEntity<AppointmentDTO> rejectAppointment(
        @PathVariable(name = "id") Long id,
        @RequestBody(required = false) BookingVM.RejectAppointmentVM rejectVM
    ) {
        String login = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new BadRequestAlertException("User not authenticated", ENTITY_NAME, "unauthenticated"));

        Optional<DoctorProfile> doctorOpt = doctorProfileRepository.findByUser_Login(login);

        Appointment appointment = appointmentRepository
            .findOneWithToOneRelationships(id)
            .orElseThrow(() -> new BadRequestAlertException("Không tìm thấy lịch hẹn", ENTITY_NAME, "idnotfound"));

        if (doctorOpt.isPresent() && appointment.getDoctor() != null) {
            if (!appointment.getDoctor().getId().equals(doctorOpt.get().getId()) && !SecurityUtils.hasCurrentUserAnyOfAuthorities(com.ocbs.security.AuthoritiesConstants.ADMIN)) {
                throw new BadRequestAlertException("Bạn không có quyền thao tác trên lịch hẹn này", ENTITY_NAME, "accessdenied");
            }
        }

        AppointmentStatus oldStatus = appointment.getStatus();
        String reason = rejectVM != null ? rejectVM.getRejectionReason() : null;
        appointment.setStatus(AppointmentStatus.REJECTED);
        appointment.setUpdatedAt(Instant.now());
        if (reason != null) {
            appointment.setCancellationReason(reason);
        }

        // Trả slot về AVAILABLE cho bệnh nhân khác có thể đặt
        AvailabilitySlot slot = appointment.getSlot();
        if (slot != null) {
            slot.setStatus(SlotStatus.AVAILABLE);
            slotRepository.save(slot);
        }

        Appointment updated = appointmentRepository.save(appointment);

        // Rejection is its own lifecycle event; reporting it as cancellation lost the actual outcome downstream.
        try {
            appointmentOutboxService.recordAppointmentEvent(
                updated,
                "APPOINTMENT_REJECTED",
                oldStatus != null ? oldStatus.name() : null,
                AppointmentStatus.REJECTED.name(),
                reason
            );
        } catch (Exception ex) {
            throw new IllegalStateException("Could not persist APPOINTMENT_REJECTED outbox event; rolling back appointment change", ex);
        }

        evictDoctorDashboardAfterCommit(updated.getDoctor());

        return ResponseEntity.ok(appointmentMapper.toDto(updated));
    }

    /**
     * {@code PUT /doctor/appointments/:id/complete} : Doctor marks appointment completed.
     * Constraint: Phải có ghi chú / chẩn đoán lâm sàng mới được kết thúc.
     */
    @PutMapping("/doctor/appointments/{id}/complete")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR', 'ROLE_ADMIN')")
    public ResponseEntity<AppointmentDTO> completeAppointment(@PathVariable(name = "id") Long id) {
        String login = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new BadRequestAlertException("User not authenticated", ENTITY_NAME, "unauthenticated"));

        Optional<DoctorProfile> doctorOpt = doctorProfileRepository.findByUser_Login(login);

        Appointment appointment = appointmentRepository
            .findOneWithToOneRelationships(id)
            .orElseThrow(() -> new BadRequestAlertException("Không tìm thấy lịch hẹn", ENTITY_NAME, "idnotfound"));

        if (doctorOpt.isPresent() && appointment.getDoctor() != null) {
            if (!appointment.getDoctor().getId().equals(doctorOpt.get().getId()) && !SecurityUtils.hasCurrentUserAnyOfAuthorities(com.ocbs.security.AuthoritiesConstants.ADMIN)) {
                throw new BadRequestAlertException("Bạn không có quyền thao tác trên lịch hẹn này", ENTITY_NAME, "accessdenied");
            }
        }

        if (appointment.getStatus() != AppointmentStatus.ACCEPTED && appointment.getStatus() != AppointmentStatus.IN_PROGRESS) {
            throw new BadRequestAlertException("Chỉ có thể hoàn thành ca khám đang diễn ra (IN_PROGRESS) hoặc đã xác nhận (ACCEPTED)", ENTITY_NAME, "invalidstatus");
        }

        // Constraint: Phải có ghi chú/chẩn đoán mới được kết thúc
        boolean hasNotes = (appointment.getDiagnosis() != null && !appointment.getDiagnosis().isBlank())
            || (appointment.getConsultationSummary() != null && !appointment.getConsultationSummary().isBlank())
            || (appointment.getTherapyPlan() != null && !appointment.getTherapyPlan().isBlank())
            || (appointment.getRecommendations() != null && !appointment.getRecommendations().isBlank())
            || (appointment.getFollowUpNotes() != null && !appointment.getFollowUpNotes().isBlank());

        if (!hasNotes) {
            throw new BadRequestAlertException("Vui lòng ghi chép hồ sơ / chẩn đoán / ghi chú lâm sàng trước khi hoàn thành ca khám", ENTITY_NAME, "notesrequired");
        }

        AppointmentStatus oldStatus = appointment.getStatus();
        appointment.setStatus(AppointmentStatus.COMPLETED);
        if (appointment.getCompletedAt() == null) {
            appointment.setCompletedAt(Instant.now());
        }
        appointment.setUpdatedAt(Instant.now());

        Appointment updated = appointmentRepository.save(appointment);

        // Treatment consumer owns package deduction; doing it here as well would deduct twice.

        // Lưu sự kiện vào Transactional Outbox (commit đồng bộ với updated appointment)
        try {
            appointmentOutboxService.recordAppointmentEvent(
                updated,
                "APPOINTMENT_COMPLETED",
                oldStatus.name(),
                AppointmentStatus.COMPLETED.name(),
                null
            );
        } catch (Exception ex) {
            throw new IllegalStateException("Could not persist APPOINTMENT_COMPLETED outbox event; rolling back appointment change", ex);
        }

        evictDoctorDashboardAfterCommit(updated.getDoctor());

        return ResponseEntity.ok(appointmentMapper.toDto(updated));
    }

    // ==========================================
    // 3. DOCTOR PATIENT MANAGEMENT ENDPOINTS
    // ==========================================

    /**
     * {@code GET /doctor/my-patients} : Get all distinct patients treated by current doctor or in clinic with aggregated stats.
     */
    @GetMapping("/doctor/my-patients")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR', 'ROLE_ADMIN')")
    @Transactional(readOnly = true)
    public ResponseEntity<List<BookingVM.DoctorPatientSummaryVM>> getMyDoctorPatients() {
        String login = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new BadRequestAlertException("User not authenticated", ENTITY_NAME, "unauthenticated"));

        Optional<DoctorProfile> doctorOpt = doctorProfileRepository.findByUser_Login(login);
        boolean isAdmin = SecurityUtils.hasCurrentUserAnyOfAuthorities(com.ocbs.security.AuthoritiesConstants.ADMIN);

        List<Appointment> relevantAppointments;
        if (!isAdmin && doctorOpt.isPresent()) {
            Long doctorId = doctorOpt.get().getId();
            relevantAppointments = appointmentRepository.findAllWithToOneRelationships().stream()
                .filter(a -> a.getDoctor() != null && doctorId.equals(a.getDoctor().getId()))
                .toList();
        } else {
            relevantAppointments = appointmentRepository.findAllWithToOneRelationships();
        }

        // Nhóm appointments theo patient ID
        java.util.Map<Long, List<Appointment>> appointmentsByPatient = relevantAppointments.stream()
            .filter(a -> a.getPatient() != null && a.getPatient().getId() != null)
            .collect(java.util.stream.Collectors.groupingBy(a -> a.getPatient().getId()));

        List<PatientProfile> allPatients = patientProfileRepository.findAllWithToOneRelationships();
        List<PatientProfile> targetPatients;
        if (!isAdmin && doctorOpt.isPresent()) {
            java.util.Set<Long> myPatientIds = appointmentsByPatient.keySet();
            targetPatients = allPatients.stream()
                .filter(p -> myPatientIds.contains(p.getId()))
                .toList();
        } else {
            targetPatients = allPatients;
        }

        List<BookingVM.DoctorPatientSummaryVM> summaries = targetPatients.stream()
            .map(p -> {
                BookingVM.DoctorPatientSummaryVM vm = new BookingVM.DoctorPatientSummaryVM();
                vm.setPatientId(p.getId());
                vm.setFullName(p.getFullName());
                vm.setPhone(p.getPhone());
                if (p.getUser() != null) {
                    vm.setEmail(p.getUser().getEmail());
                    vm.setCreatedAt(p.getUser().getCreatedDate());
                    vm.setGuest(false);
                } else {
                    vm.setEmail(null);
                    vm.setCreatedAt(null);
                    vm.setGuest(false);
                }
                vm.setGender(p.getGender());
                vm.setDateOfBirth(p.getDateOfBirth());
                vm.setAddress(p.getAddress());
                vm.setMedicalHistory(p.getMedicalHistory());
                vm.setAllergies(p.getAllergies());
                vm.setBloodGroup(p.getBloodGroup());

                List<Appointment> patientApps = appointmentsByPatient.getOrDefault(p.getId(), java.util.Collections.emptyList());
                long total = patientApps.size();
                long completed = patientApps.stream().filter(a -> a.getStatus() == AppointmentStatus.COMPLETED).count();
                long upcoming = patientApps.stream().filter(a -> a.getStatus() == AppointmentStatus.PENDING || a.getStatus() == AppointmentStatus.ACCEPTED).count();

                Instant lastDate = null;
                String lastDiag = null;
                if (!patientApps.isEmpty()) {
                    List<Appointment> sortedApps = new java.util.ArrayList<>(patientApps);
                    sortedApps.sort((a, b) -> {
                        if (a.getScheduledStart() == null) return 1;
                        if (b.getScheduledStart() == null) return -1;
                        return b.getScheduledStart().compareTo(a.getScheduledStart());
                    });
                    lastDate = sortedApps.get(0).getScheduledStart();
                    lastDiag = sortedApps.stream()
                        .filter(a -> a.getDiagnosis() != null && !a.getDiagnosis().isBlank())
                        .map(Appointment::getDiagnosis)
                        .findFirst()
                        .orElse(null);
                }

                vm.setTotalAppointments(total);
                vm.setCompletedAppointments(completed);
                vm.setUpcomingAppointments(upcoming);
                vm.setLastAppointmentDate(lastDate);
                vm.setLastDiagnosis(lastDiag);

                return vm;
            })
            .sorted((a, b) -> {
                if (a.getLastAppointmentDate() == null && b.getLastAppointmentDate() == null) {
                    if (a.getFullName() != null && b.getFullName() != null) {
                        return a.getFullName().compareToIgnoreCase(b.getFullName());
                    }
                    return 0;
                }
                if (a.getLastAppointmentDate() == null) return 1;
                if (b.getLastAppointmentDate() == null) return -1;
                return b.getLastAppointmentDate().compareTo(a.getLastAppointmentDate());
            })
            .collect(java.util.stream.Collectors.toList());

        return ResponseEntity.ok(summaries);
    }

    @GetMapping("/doctor/patients/{patientId}")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR', 'ROLE_ADMIN')")
    @Transactional(readOnly = true)
    public ResponseEntity<BookingVM.PatientDetailRecordVM> getPatientDetailRecord(@PathVariable(name = "patientId") Long patientId) {
        String login = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new BadRequestAlertException("User not authenticated", ENTITY_NAME, "unauthenticated"));

        Optional<DoctorProfile> doctorOpt = doctorProfileRepository.findByUser_Login(login);
        boolean isAdmin = SecurityUtils.hasCurrentUserAnyOfAuthorities(com.ocbs.security.AuthoritiesConstants.ADMIN);

        PatientProfile patient = patientProfileRepository
            .findOneWithToOneRelationships(patientId)
            .orElseGet(() -> patientProfileRepository.findById(patientId)
                .orElseThrow(() -> new BadRequestAlertException("Không tìm thấy hồ sơ bệnh nhân", ENTITY_NAME, "patientnotfound")));

        List<Appointment> allPatientAppointments = appointmentRepository.findByPatientId(patient.getId());

        List<Appointment> doctorAppointments;
        if (!isAdmin && doctorOpt.isPresent()) {
            Long doctorId = doctorOpt.get().getId();
            doctorAppointments = allPatientAppointments.stream()
                .filter(a -> a.getDoctor() != null && doctorId.equals(a.getDoctor().getId()))
                .toList();

            if (doctorAppointments.isEmpty()) {
                throw new BadRequestAlertException("Bạn không có quyền truy cập hồ sơ của bệnh nhân này vì chưa phụ trách ca khám nào", ENTITY_NAME, "accessdenied");
            }
        } else {
            doctorAppointments = allPatientAppointments;
        }

        long total = doctorAppointments.size();
        long completed = doctorAppointments.stream().filter(a -> a.getStatus() == AppointmentStatus.COMPLETED).count();

        BookingVM.PatientDetailRecordVM vm = new BookingVM.PatientDetailRecordVM(
            patientProfileMapper.toDto(patient),
            total,
            completed,
            appointmentMapper.toDto(doctorAppointments)
        );

        return ResponseEntity.ok(vm);
    }

    // ==========================================
    // 4. CONSULTATION NOTES & CLINICAL RECORDS
    // ==========================================

    /**
     * {@code POST /doctor/appointments/:id/consultation-note} : Doctor creates/updates consultation note for an appointment.
     */
    @PostMapping("/doctor/appointments/{id}/consultation-note")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR', 'ROLE_ADMIN')")
    public ResponseEntity<AppointmentDTO> saveConsultationNote(
        @PathVariable(name = "id") Long id,
        @Valid @RequestBody BookingVM.SaveConsultationNoteVM noteVM
    ) {
        String login = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new BadRequestAlertException("User not authenticated", ENTITY_NAME, "unauthenticated"));

        Optional<DoctorProfile> doctorOpt = doctorProfileRepository.findByUser_Login(login);

        Appointment appointment = appointmentRepository
            .findOneWithToOneRelationships(id)
            .orElseThrow(() -> new BadRequestAlertException("Không tìm thấy lịch hẹn", ENTITY_NAME, "idnotfound"));

        if (doctorOpt.isPresent() && appointment.getDoctor() != null) {
            if (!appointment.getDoctor().getId().equals(doctorOpt.get().getId()) && !SecurityUtils.hasCurrentUserAnyOfAuthorities(com.ocbs.security.AuthoritiesConstants.ADMIN)) {
                throw new BadRequestAlertException("Bạn không có quyền cập nhật ghi chú cho lịch hẹn này", ENTITY_NAME, "accessdenied");
            }
        }

        appointment.setDiagnosis(noteVM.getDiagnosis());
        appointment.setConsultationSummary(noteVM.getConsultationSummary());
        appointment.setTherapyPlan(noteVM.getTherapyPlan());
        appointment.setRecommendations(noteVM.getRecommendations());
        appointment.setFollowUpNotes(noteVM.getFollowUpNotes());
        appointment.setNextAppointmentRecommendedDate(noteVM.getNextAppointmentRecommendedDate());
        appointment.setUpdatedAt(Instant.now());

        boolean isNewlyCompleted = false;
        if (noteVM.isMarkCompleted() && appointment.getStatus() != AppointmentStatus.COMPLETED && appointment.getStatus() != AppointmentStatus.CANCELLED && appointment.getStatus() != AppointmentStatus.REJECTED) {
            boolean hasNotes = (noteVM.getDiagnosis() != null && !noteVM.getDiagnosis().isBlank())
                || (noteVM.getConsultationSummary() != null && !noteVM.getConsultationSummary().isBlank())
                || (noteVM.getTherapyPlan() != null && !noteVM.getTherapyPlan().isBlank())
                || (noteVM.getRecommendations() != null && !noteVM.getRecommendations().isBlank())
                || (noteVM.getFollowUpNotes() != null && !noteVM.getFollowUpNotes().isBlank());

            if (!hasNotes) {
                throw new BadRequestAlertException("Vui lòng ghi chép ít nhất một nội dung lâm sàng (chẩn đoán, tóm tắt, phác đồ,...) trước khi đánh dấu hoàn thành", ENTITY_NAME, "notesrequired");
            }

            appointment.setStatus(AppointmentStatus.COMPLETED);
            if (appointment.getCompletedAt() == null) {
                appointment.setCompletedAt(Instant.now());
            }
            isNewlyCompleted = true;
        }

        Appointment updated = appointmentRepository.save(appointment);

        // A completion event is emitted only on the actual status transition; downstream services own side effects.
        if (isNewlyCompleted) {
            // Lưu sự kiện vào Transactional Outbox (commit đồng bộ với updated appointment)
            try {
                appointmentOutboxService.recordAppointmentEvent(
                    updated,
                    "APPOINTMENT_COMPLETED",
                    AppointmentStatus.IN_PROGRESS.name(),
                    AppointmentStatus.COMPLETED.name(),
                    null
                );
            } catch (Exception ex) {
                throw new IllegalStateException("Could not persist APPOINTMENT_COMPLETED outbox event; rolling back appointment change", ex);
            }
        }

        evictDoctorDashboardAfterCommit(updated.getDoctor());

        return ResponseEntity.ok(appointmentMapper.toDto(updated));
    }

    /**
     * {@code GET /patient/my-clinical-records} : Patient views all completed clinical consultation notes.
     */
    @GetMapping("/patient/my-clinical-records")
    @PreAuthorize("hasAuthority('ROLE_PATIENT') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<AppointmentDTO>> getMyPatientClinicalRecords() {
        String login = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new BadRequestAlertException("User not authenticated", ENTITY_NAME, "unauthenticated"));

        Optional<PatientProfile> patientOpt = patientProfileRepository.findByUser_Login(login);
        if (patientOpt.isEmpty()) {
            return ResponseEntity.ok(java.util.Collections.emptyList());
        }

        PatientProfile patient = patientOpt.get();

        /*
        // =========================================================================================
        // CODE CŨ (GẶP LỖI N+1 VÀ LỌC STREAM IN-MEMORY):
        // 1. Kéo toàn bộ appointments của patient về, thiếu join fetch p.user và d.user.
        // 2. Khi appointmentMapper.toDto() chạy, MapStruct gọi getDoctor().getUser() và getPatient().getUser()
        //    -> Kích hoạt Lazy Loading và sinh ra N query phụ vào bảng jhi_user (1 + 2N queries).
        // 3. Phải tốn CPU/RAM lọc bằng Java Stream.
        // =========================================================================================
        List<Appointment> completedList = appointmentRepository.findByPatientId(patient.getId()).stream()
            .filter(a -> a.getStatus() == AppointmentStatus.COMPLETED || a.getDiagnosis() != null || a.getConsultationSummary() != null || a.getRecommendations() != null)
            .collect(java.util.stream.Collectors.toList());

        return ResponseEntity.ok(appointmentMapper.toDto(completedList));
        */

        // =========================================================================================
        // CODE MỚI ĐÃ TỐI ƯU (1 QUERY DUY NHẤT, LOẠI BỎ HOÀN TOÀN LỖI N+1):
        // - Eager join fetch a.patient p, p.user, a.doctor d, d.user, a.slot s.
        // - Lọc trực tiếp ở tầng SQL Database -> Database Index tối ưu, 0 query phụ khi map sang DTO.
        // =========================================================================================
        List<Appointment> completedList = appointmentRepository.findClinicalRecordsByPatientId(patient.getId());
        return ResponseEntity.ok(appointmentMapper.toDto(completedList));
    }

}
