package com.ocbs.web.rest;

import com.ocbs.domain.Appointment;
import com.ocbs.domain.AvailabilitySlot;
import com.ocbs.domain.DoctorProfile;
import com.ocbs.domain.Notification;
import com.ocbs.domain.PatientProfile;
import com.ocbs.domain.TreatmentPackage;
import com.ocbs.domain.User;
import com.ocbs.domain.enumeration.AppointmentStatus;
import com.ocbs.domain.enumeration.SlotStatus;
import com.ocbs.domain.enumeration.TreatmentPackageStatus;
import com.ocbs.event.AppointmentEvent;
import com.ocbs.event.AppointmentEventProducer;
import com.ocbs.repository.AppointmentRepository;
import com.ocbs.repository.AvailabilitySlotRepository;
import com.ocbs.repository.DoctorProfileRepository;
import com.ocbs.repository.NotificationRepository;
import com.ocbs.repository.PatientProfileRepository;
import com.ocbs.repository.TreatmentPackageRepository;
import com.ocbs.repository.UserRepository;
import com.ocbs.security.SecurityUtils;
import com.ocbs.service.dto.AppointmentDTO;
import com.ocbs.service.mapper.AppointmentMapper;
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
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault());

    private final AppointmentRepository appointmentRepository;
    private final AvailabilitySlotRepository slotRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final UserRepository userRepository;
    private final TreatmentPackageRepository treatmentPackageRepository;
    private final NotificationRepository notificationRepository;
    private final AppointmentEventProducer appointmentEventProducer;
    private final AppointmentMapper appointmentMapper;
    private final com.ocbs.service.mapper.PatientProfileMapper patientProfileMapper;

    public AppointmentManagementResource(
        AppointmentRepository appointmentRepository,
        AvailabilitySlotRepository slotRepository,
        PatientProfileRepository patientProfileRepository,
        DoctorProfileRepository doctorProfileRepository,
        UserRepository userRepository,
        TreatmentPackageRepository treatmentPackageRepository,
        NotificationRepository notificationRepository,
        AppointmentEventProducer appointmentEventProducer,
        AppointmentMapper appointmentMapper,
        com.ocbs.service.mapper.PatientProfileMapper patientProfileMapper
    ) {
        this.appointmentRepository = appointmentRepository;
        this.slotRepository = slotRepository;
        this.patientProfileRepository = patientProfileRepository;
        this.doctorProfileRepository = doctorProfileRepository;
        this.userRepository = userRepository;
        this.treatmentPackageRepository = treatmentPackageRepository;
        this.notificationRepository = notificationRepository;
        this.appointmentEventProducer = appointmentEventProducer;
        this.appointmentMapper = appointmentMapper;
        this.patientProfileMapper = patientProfileMapper;
    }

    // ==========================================
    // 1. PATIENT BOOKING ENDPOINTS
    // ==========================================

    /**
     * {@code POST /patient/appointments/book} : Patient books an appointment slot.
     */
    @PostMapping("/patient/appointments/book")
    @PreAuthorize("hasAuthority('ROLE_PATIENT') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<AppointmentDTO> bookAppointment(@Valid @RequestBody BookingVM.CreateBookingVM bookingVM) throws URISyntaxException {
        LOG.debug("REST request by Patient to book an appointment: {}", bookingVM);

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

        AvailabilitySlot slot = slotRepository
            .findById(bookingVM.getSlotId())
            .orElseThrow(() -> new BadRequestAlertException("Khung giờ không tồn tại", ENTITY_NAME, "slotnotfound"));
//        Find with lock
//        AvailabilitySlot slot = slotRepository.findByIdWithLock(bookingVM.getSlotId())
//            .orElseThrow(() -> new BadRequestAlertException("Khung giờ không tồn tại", ENTI   TY_NAME, "slotnotfound"));

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

        Appointment savedAppointment = appointmentRepository.save(appointment);

        // Chuyển slot sang BOOKED
        slot.setStatus(SlotStatus.BOOKED);
        slotRepository.save(slot);

        String formattedTime = TIME_FORMATTER.format(savedAppointment.getScheduledStart());

        // Gửi thông báo đến Bác sĩ
        if (doctor.getUser() != null) {
            createNotification(
                doctor.getUser().getLogin(),
                doctor.getUser().getId(),
                "Lịch hẹn mới chờ duyệt",
                "Bệnh nhân " + patient.getFullName() + " vừa đặt lịch hẹn mới mã " + bookingCode + " vào " + formattedTime + ". Vui lòng kiểm tra và duyệt lịch hẹn.",
                "APPOINTMENT",
                savedAppointment.getId(),
                "APPOINTMENT",
                "/doctor/appointments"
            );
        }

        // Gửi thông báo xác nhận đến Bệnh nhân
        if (patient.getUser() != null) {
            createNotification(
                patient.getUser().getLogin(),
                patient.getUser().getId(),
                "Đặt lịch hẹn thành công",
                "Bạn đã gửi yêu cầu đặt lịch hẹn thành công với Bác sĩ " + doctor.getFullName() + " (Mã: " + bookingCode + ", thời gian: " + formattedTime + "). Đang chờ bác sĩ xác nhận.",
                "APPOINTMENT",
                savedAppointment.getId(),
                "APPOINTMENT",
                "/patient/appointments"
            );
        }

        // Phát sự kiện Kafka
        try {
            AppointmentEvent event = new AppointmentEvent(
                savedAppointment.getId(),
                bookingCode,
                patient.getId(),
                patient.getFullName(),
                doctor.getId(),
                doctor.getFullName(),
                "APPOINTMENT_BOOKED"
            );
            appointmentEventProducer.publishAppointmentEvent(event);
        } catch (Exception ex) {
            LOG.warn("Kafka event publish error: {}", ex.getMessage());
        }

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

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setUpdatedAt(Instant.now());
        if (cancelVM != null && cancelVM.getCancellationReason() != null) {
            appointment.setCancellationReason(cancelVM.getCancellationReason());
        }

        // Trả lại slot về AVAILABLE nếu slot vẫn tồn tại
        AvailabilitySlot slot = appointment.getSlot();
        if (slot != null) {
            slot.setStatus(SlotStatus.AVAILABLE);
            slotRepository.save(slot);
        }

        Appointment updated = appointmentRepository.save(appointment);

        // Gửi thông báo đến Bác sĩ
        if (appointment.getDoctor() != null && appointment.getDoctor().getUser() != null) {
            createNotification(
                appointment.getDoctor().getUser().getLogin(),
                appointment.getDoctor().getUser().getId(),
                "Bệnh nhân đã hủy lịch hẹn",
                "Bệnh nhân " + patient.getFullName() + " đã hủy lịch hẹn mã " + appointment.getBookingCode() + (appointment.getCancellationReason() != null ? ". Lý do: " + appointment.getCancellationReason() : ""),
                "APPOINTMENT",
                appointment.getId(),
                "APPOINTMENT",
                "/doctor/appointments"
            );
        }

        // Phát sự kiện Kafka
        try {
            AppointmentEvent event = new AppointmentEvent(
                updated.getId(),
                updated.getBookingCode(),
                patient.getId(),
                patient.getFullName(),
                appointment.getDoctor() != null ? appointment.getDoctor().getId() : null,
                appointment.getDoctor() != null ? appointment.getDoctor().getFullName() : null,
                "APPOINTMENT_CANCELLED"
            );
            appointmentEventProducer.publishAppointmentEvent(event);
        } catch (Exception ex) {
            LOG.warn("Kafka event publish error: {}", ex.getMessage());
        }

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

        appointment.setStatus(AppointmentStatus.ACCEPTED);
        appointment.setUpdatedAt(Instant.now());

        Appointment updated = appointmentRepository.save(appointment);

        String formattedTime = TIME_FORMATTER.format(updated.getScheduledStart());

        // Gửi thông báo đến Bệnh nhân
        if (updated.getPatient() != null && updated.getPatient().getUser() != null) {
            String doctorName = updated.getDoctor() != null ? updated.getDoctor().getFullName() : "Bác sĩ";
            createNotification(
                updated.getPatient().getUser().getLogin(),
                updated.getPatient().getUser().getId(),
                "Lịch hẹn đã được chấp nhận",
                "Bác sĩ " + doctorName + " đã chấp nhận lịch hẹn mã " + updated.getBookingCode() + " vào lúc " + formattedTime + ". Vui lòng chú ý thời gian tham gia đúng giờ.",
                "APPOINTMENT",
                updated.getId(),
                "APPOINTMENT",
                "/patient/appointments"
            );
        }

        // Phát sự kiện Kafka
        try {
            AppointmentEvent event = new AppointmentEvent(
                updated.getId(),
                updated.getBookingCode(),
                updated.getPatient() != null ? updated.getPatient().getId() : null,
                updated.getPatient() != null ? updated.getPatient().getFullName() : null,
                updated.getDoctor() != null ? updated.getDoctor().getId() : null,
                updated.getDoctor() != null ? updated.getDoctor().getFullName() : null,
                "APPOINTMENT_ACCEPTED"
            );
            appointmentEventProducer.publishAppointmentEvent(event);
        } catch (Exception ex) {
            LOG.warn("Kafka event publish error: {}", ex.getMessage());
        }

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

        // Gửi thông báo đến Bệnh nhân
        if (updated.getPatient() != null && updated.getPatient().getUser() != null) {
            createNotification(
                updated.getPatient().getUser().getLogin(),
                updated.getPatient().getUser().getId(),
                "Ca khám đang bắt đầu",
                "Bác sĩ đã bắt đầu phiên tư vấn cho lịch hẹn mã " + updated.getBookingCode() + ". Vui lòng chuẩn bị tham gia.",
                "APPOINTMENT",
                updated.getId(),
                "APPOINTMENT",
                "/patient/appointments"
            );
        }

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

        appointment.setStatus(AppointmentStatus.REJECTED);
        appointment.setUpdatedAt(Instant.now());
        if (rejectVM != null && rejectVM.getRejectionReason() != null) {
            appointment.setCancellationReason(rejectVM.getRejectionReason());
        }

        // Trả slot về AVAILABLE cho bệnh nhân khác có thể đặt
        AvailabilitySlot slot = appointment.getSlot();
        if (slot != null) {
            slot.setStatus(SlotStatus.AVAILABLE);
            slotRepository.save(slot);
        }

        Appointment updated = appointmentRepository.save(appointment);

        // Gửi thông báo đến Bệnh nhân
        if (updated.getPatient() != null && updated.getPatient().getUser() != null) {
            String reasonText = updated.getCancellationReason() != null ? ". Lý do: " + updated.getCancellationReason() : "";
            createNotification(
                updated.getPatient().getUser().getLogin(),
                updated.getPatient().getUser().getId(),
                "Lịch hẹn đã bị từ chối",
                "Bác sĩ đã từ chối lịch hẹn mã " + updated.getBookingCode() + reasonText + ". Bạn có thể chọn thời gian hoặc bác sĩ khác.",
                "APPOINTMENT",
                updated.getId(),
                "APPOINTMENT",
                "/patient/appointments"
            );
        }

        // Phát sự kiện Kafka
        try {
            AppointmentEvent event = new AppointmentEvent(
                updated.getId(),
                updated.getBookingCode(),
                updated.getPatient() != null ? updated.getPatient().getId() : null,
                updated.getPatient() != null ? updated.getPatient().getFullName() : null,
                updated.getDoctor() != null ? updated.getDoctor().getId() : null,
                updated.getDoctor() != null ? updated.getDoctor().getFullName() : null,
                "APPOINTMENT_REJECTED"
            );
            appointmentEventProducer.publishAppointmentEvent(event);
        } catch (Exception ex) {
            LOG.warn("Kafka event publish error: {}", ex.getMessage());
        }

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

        appointment.setStatus(AppointmentStatus.COMPLETED);
        if (appointment.getCompletedAt() == null) {
            appointment.setCompletedAt(Instant.now());
        }
        appointment.setUpdatedAt(Instant.now());

        Appointment updated = appointmentRepository.save(appointment);

        // TỰ ĐỘNG TRỪ SỐ BUỔI TRONG GÓI TRỊ LIỆU (TREATMENT PACKAGE DEDUCTION)
        if (updated.getPatient() != null) {
            Long docId = updated.getDoctor() != null ? updated.getDoctor().getId() : null;
            deductPackageSession(updated.getPatient().getId(), docId, updated.getBookingCode());
        }

        // Gửi thông báo hoàn thành đến Bệnh nhân
        if (updated.getPatient() != null && updated.getPatient().getUser() != null) {
            String doctorName = updated.getDoctor() != null ? updated.getDoctor().getFullName() : "Bác sĩ";
            createNotification(
                updated.getPatient().getUser().getLogin(),
                updated.getPatient().getUser().getId(),
                "Ca khám đã hoàn thành",
                "Bác sĩ " + doctorName + " đã hoàn tất ca khám mã " + updated.getBookingCode() + ". Bạn có thể xem kết quả chẩn đoán và phác đồ điều trị tại mục Hồ Sơ Bệnh Án.",
                "CLINICAL",
                updated.getId(),
                "APPOINTMENT",
                "/patient/my-clinical-records"
            );
        }

        // Gửi thông báo đến Bác sĩ
        if (updated.getDoctor() != null && updated.getDoctor().getUser() != null) {
            createNotification(
                updated.getDoctor().getUser().getLogin(),
                updated.getDoctor().getUser().getId(),
                "Hoàn tất ca khám",
                "Đã lưu hồ sơ bệnh án và hoàn tất ca khám mã " + updated.getBookingCode() + " cho bệnh nhân " + (updated.getPatient() != null ? updated.getPatient().getFullName() : "") + ".",
                "CLINICAL",
                updated.getId(),
                "APPOINTMENT",
                "/doctor/appointments"
            );
        }

        // Phát sự kiện Kafka
        try {
            AppointmentEvent event = new AppointmentEvent(
                updated.getId(),
                updated.getBookingCode(),
                updated.getPatient() != null ? updated.getPatient().getId() : null,
                updated.getPatient() != null ? updated.getPatient().getFullName() : null,
                updated.getDoctor() != null ? updated.getDoctor().getId() : null,
                updated.getDoctor() != null ? updated.getDoctor().getFullName() : null,
                "APPOINTMENT_COMPLETED"
            );
            appointmentEventProducer.publishAppointmentEvent(event);
        } catch (Exception ex) {
            LOG.warn("Kafka event publish error: {}", ex.getMessage());
        }

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

        // Nếu ca khám vừa được đánh dấu hoàn tất: Tự động trừ buổi và gửi thông báo
        if (isNewlyCompleted) {
            if (updated.getPatient() != null) {
                Long docId = updated.getDoctor() != null ? updated.getDoctor().getId() : null;
                deductPackageSession(updated.getPatient().getId(), docId, updated.getBookingCode());
            }

            // Gửi thông báo đến Bệnh nhân
            if (updated.getPatient() != null && updated.getPatient().getUser() != null) {
                String doctorName = updated.getDoctor() != null ? updated.getDoctor().getFullName() : "Bác sĩ";
                createNotification(
                    updated.getPatient().getUser().getLogin(),
                    updated.getPatient().getUser().getId(),
                    "Hồ sơ ca khám đã hoàn thành",
                    "Bác sĩ " + doctorName + " đã cập nhật kết quả lâm sàng và hoàn tất ca khám mã " + updated.getBookingCode() + ". Bạn có thể xem kết quả tại mục Hồ Sơ Bệnh Án.",
                    "CLINICAL",
                    updated.getId(),
                    "APPOINTMENT",
                    "/patient/my-clinical-records"
                );
            }

            // Gửi thông báo đến Bác sĩ
            if (updated.getDoctor() != null && updated.getDoctor().getUser() != null) {
                createNotification(
                    updated.getDoctor().getUser().getLogin(),
                    updated.getDoctor().getUser().getId(),
                    "Hoàn tất hồ sơ bệnh án",
                    "Đã lưu chẩn đoán lâm sàng và hoàn thành ca khám " + updated.getBookingCode() + ".",
                    "CLINICAL",
                    updated.getId(),
                    "APPOINTMENT",
                    "/doctor/appointments"
                );
            }

            // Phát sự kiện Kafka
            try {
                AppointmentEvent event = new AppointmentEvent(
                    updated.getId(),
                    updated.getBookingCode(),
                    updated.getPatient() != null ? updated.getPatient().getId() : null,
                    updated.getPatient() != null ? updated.getPatient().getFullName() : null,
                    updated.getDoctor() != null ? updated.getDoctor().getId() : null,
                    updated.getDoctor() != null ? updated.getDoctor().getFullName() : null,
                    "APPOINTMENT_COMPLETED"
                );
                appointmentEventProducer.publishAppointmentEvent(event);
            } catch (Exception ex) {
                LOG.warn("Kafka event publish error: {}", ex.getMessage());
            }
        }

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

    // ==========================================
    // 5. HELPER METHODS: DEDUCT PACKAGE & NOTIFICATION
    // ==========================================

    /**
     * Tự động trừ 1 buổi trong gói trị liệu ACTIVE của bệnh nhân.
     * Ưu tiên gói trị liệu liên kết trực tiếp với bác sĩ phụ trách, nếu không có thì trừ vào gói chung của bệnh nhân.
     */
    private void deductPackageSession(Long patientId, Long doctorId, String bookingCode) {
        if (patientId == null) return;
        try {
            List<TreatmentPackage> activePackages = null;
            if (doctorId != null) {
                activePackages = treatmentPackageRepository.findActivePackagesByPatientAndDoctor(patientId, doctorId);
            }
            if (activePackages == null || activePackages.isEmpty()) {
                activePackages = treatmentPackageRepository.findActivePackagesByPatient(patientId);
            }

            if (activePackages != null && !activePackages.isEmpty()) {
                TreatmentPackage targetPackage = activePackages.get(0);
                int currentRemaining = targetPackage.getRemainingSessions() != null ? targetPackage.getRemainingSessions() : 0;

                if (currentRemaining > 0) {
                    int newRemaining = currentRemaining - 1;
                    targetPackage.setRemainingSessions(newRemaining);
                    LOG.info("⚡ [Treatment Package] Deducted 1 session for Package ID: {}, Name: '{}', BookingCode: '{}'. Remaining: {}/{}",
                        targetPackage.getId(), targetPackage.getName(), bookingCode, newRemaining, targetPackage.getSessionQuantity());

                    if (newRemaining <= 0) {
                        targetPackage.setStatus(TreatmentPackageStatus.COMPLETED);
                        LOG.info("🏁 [Treatment Package] Package ID: {} has 0 sessions remaining. Marked as COMPLETED!", targetPackage.getId());
                    }

                    treatmentPackageRepository.save(targetPackage);

                    // Gửi thông báo trừ buổi cho bệnh nhân
                    if (targetPackage.getPatient() != null && targetPackage.getPatient().getUser() != null) {
                        String userLogin = targetPackage.getPatient().getUser().getLogin();
                        String statusNote = newRemaining <= 0 ? " (Bạn đã hoàn thành toàn bộ gói trị liệu!)" : "";
                        createNotification(
                            userLogin,
                            targetPackage.getPatient().getUser().getId(),
                            "Đã trừ 1 buổi trong gói trị liệu",
                            "Gói trị liệu '" + targetPackage.getName() + "' vừa được trừ 1 buổi cho ca khám " + bookingCode + ". Số buổi còn lại: " + newRemaining + "/" + targetPackage.getSessionQuantity() + statusNote,
                            "TREATMENT",
                            targetPackage.getId(),
                            "TREATMENT_PACKAGE",
                            "/patient/treatment-packages"
                        );
                    }
                }
            } else {
                LOG.info("ℹ️ [Treatment Package] No active package found for Patient ID: {} to deduct session for BookingCode: {}", patientId, bookingCode);
            }
        } catch (Exception ex) {
            LOG.error("❌ [Treatment Package] Error while deducting package session for patientId {}: {}", patientId, ex.getMessage(), ex);
        }
    }

    /**
     * Tạo và lưu thông báo in-app vào cơ sở dữ liệu.
     */
    private void createNotification(String userLogin, Long userId, String title, String message, String type, Long entityId, String entityType, String actionUrl) {
        try {
            if (userLogin == null || userLogin.isBlank()) return;
            Notification notification = new Notification();
            notification.setUserLogin(userLogin);
            notification.setUserId(userId);
            notification.setTitle(title);
            notification.setMessage(message);
            notification.setNotificationType(type);
            notification.setRelatedEntityId(entityId);
            notification.setRelatedEntityType(entityType);
            notification.setIsRead(false);
            notification.setActionUrl(actionUrl);
            notificationRepository.save(notification);
            LOG.info("🔔 [Notification] Saved notification for user '{}': {}", userLogin, title);
        } catch (Exception ex) {
            LOG.error("❌ [Notification] Error saving notification for user '{}': {}", userLogin, ex.getMessage());
        }
    }
}
