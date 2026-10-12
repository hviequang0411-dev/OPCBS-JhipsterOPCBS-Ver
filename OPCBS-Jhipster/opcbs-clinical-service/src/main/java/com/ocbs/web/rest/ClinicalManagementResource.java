package com.ocbs.web.rest;

import com.ocbs.domain.Appointment;
import com.ocbs.domain.DoctorProfile;
import com.ocbs.domain.PatientProfile;
import com.ocbs.domain.enumeration.AppointmentStatus;
import com.ocbs.repository.AppointmentRepository;
import com.ocbs.repository.DoctorProfileRepository;
import com.ocbs.repository.PatientProfileRepository;
import com.ocbs.security.SecurityUtils;
import com.ocbs.service.dto.AppointmentDTO;
import com.ocbs.service.mapper.AppointmentMapper;
import com.ocbs.web.rest.errors.BadRequestAlertException;
import com.ocbs.web.rest.vm.BookingVM;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for Clinical Records, Consultation Notes and Doctor's Patients in Clinical Domain.
 */
@RestController
@RequestMapping("/api")
@Transactional
public class ClinicalManagementResource {

    private static final Logger LOG = LoggerFactory.getLogger(ClinicalManagementResource.class);
    private static final String ENTITY_NAME = "clinicalRecord";

    private final AppointmentRepository appointmentRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final AppointmentMapper appointmentMapper;
    private final com.ocbs.service.mapper.PatientProfileMapper patientProfileMapper;
    private final com.ocbs.service.ClinicalPdfService clinicalPdfService;

    public ClinicalManagementResource(
        AppointmentRepository appointmentRepository,
        PatientProfileRepository patientProfileRepository,
        DoctorProfileRepository doctorProfileRepository,
        AppointmentMapper appointmentMapper,
        com.ocbs.service.mapper.PatientProfileMapper patientProfileMapper,
        com.ocbs.service.ClinicalPdfService clinicalPdfService
    ) {
        this.appointmentRepository = appointmentRepository;
        this.patientProfileRepository = patientProfileRepository;
        this.doctorProfileRepository = doctorProfileRepository;
        this.appointmentMapper = appointmentMapper;
        this.patientProfileMapper = patientProfileMapper;
        this.clinicalPdfService = clinicalPdfService;
    }

    // ==========================================
    // 1. DOCTOR PATIENT CLINICAL MANAGEMENT
    // ==========================================

    /**
     * {@code GET /doctor/my-patients} : Get all distinct patients treated by current doctor with aggregated stats.
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

    /**
     * {@code GET /doctor/patients/:patientId} : Get patient detailed clinical record.
     */
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

    /**
     * {@code GET /doctor/patients/:patientId/export-pdf} : Export comprehensive clinical record as PDF.
     */
    @GetMapping("/doctor/patients/{patientId}/export-pdf")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR', 'ROLE_ADMIN')")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> exportPatientClinicalRecordPdf(@PathVariable(name = "patientId") Long patientId) {
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

        byte[] pdfBytes = clinicalPdfService.generatePatientClinicalRecordPdf(
            patient,
            doctorAppointments,
            doctorOpt.orElse(null)
        );

        String filename = "benh-an-patient-" + patientId + ".pdf";
        return ResponseEntity.ok()
            .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
            .body(pdfBytes);
    }

    // ==========================================
    // 2. CONSULTATION NOTES & CLINICAL RECORDS
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

        if (noteVM.isMarkCompleted() && appointment.getStatus() != AppointmentStatus.CANCELLED && appointment.getStatus() != AppointmentStatus.REJECTED) {
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
        }

        Appointment updated = appointmentRepository.save(appointment);
        return ResponseEntity.ok(appointmentMapper.toDto(updated));
    }

    /**
     * {@code GET /patient/my-clinical-records} : Patient views all completed clinical consultation notes.
     */
    @GetMapping("/patient/my-clinical-records")
    @PreAuthorize("hasAnyAuthority('ROLE_PATIENT', 'ROLE_ADMIN')")
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
