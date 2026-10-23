package com.ocbs.web.rest;

import com.ocbs.domain.Appointment;
import com.ocbs.domain.DoctorProfile;
import com.ocbs.domain.PatientProfile;
import com.ocbs.domain.enumeration.AppointmentStatus;
import com.ocbs.repository.AppointmentRepository;
import com.ocbs.repository.ClinicalPsychometricAlertRepository;
import com.ocbs.repository.DoctorProfileRepository;
import com.ocbs.repository.PatientProfileRepository;
import com.ocbs.repository.PsychometricSubmissionRepository;
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
    private final PsychometricSubmissionRepository psychometricSubmissionRepository;
    private final ClinicalPsychometricAlertRepository clinicalPsychometricAlertRepository;
    private final com.ocbs.repository.EmotionJournalRepository emotionJournalRepository;
    private final com.ocbs.repository.TreatmentPackageRepository treatmentPackageRepository;

    public ClinicalManagementResource(
        AppointmentRepository appointmentRepository,
        PatientProfileRepository patientProfileRepository,
        DoctorProfileRepository doctorProfileRepository,
        AppointmentMapper appointmentMapper,
        com.ocbs.service.mapper.PatientProfileMapper patientProfileMapper,
        com.ocbs.service.ClinicalPdfService clinicalPdfService,
        PsychometricSubmissionRepository psychometricSubmissionRepository,
        ClinicalPsychometricAlertRepository clinicalPsychometricAlertRepository,
        com.ocbs.repository.EmotionJournalRepository emotionJournalRepository,
        com.ocbs.repository.TreatmentPackageRepository treatmentPackageRepository
    ) {
        this.appointmentRepository = appointmentRepository;
        this.patientProfileRepository = patientProfileRepository;
        this.doctorProfileRepository = doctorProfileRepository;
        this.appointmentMapper = appointmentMapper;
        this.patientProfileMapper = patientProfileMapper;
        this.clinicalPdfService = clinicalPdfService;
        this.psychometricSubmissionRepository = psychometricSubmissionRepository;
        this.clinicalPsychometricAlertRepository = clinicalPsychometricAlertRepository;
        this.emotionJournalRepository = emotionJournalRepository;
        this.treatmentPackageRepository = treatmentPackageRepository;
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
        } else if (isAdmin) {
            targetPatients = allPatients;
        } else {
            targetPatients = java.util.Collections.emptyList();
        }

        LOG.info("AUDIT: User '{}' retrieved patient list. Found {} records", login, targetPatients.size());

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
        } else if (isAdmin) {
            doctorAppointments = allPatientAppointments;
        } else {
            throw new BadRequestAlertException("Bạn không có quyền truy cập hồ sơ của bệnh nhân này", ENTITY_NAME, "accessdenied");
        }

        LOG.info("AUDIT: User '{}' viewed clinical record of patient ID {}", login, patientId);

        long total = doctorAppointments.size();
        long completed = doctorAppointments.stream().filter(a -> a.getStatus() == AppointmentStatus.COMPLETED).count();

        // 1. Lấy kết quả bài test tâm lý của bệnh nhân (nếu có)
        List<com.ocbs.domain.PsychometricSubmission> submissions = psychometricSubmissionRepository.findAllByPatientId(patientId);

        // 2. Lấy nhật ký cảm xúc do bệnh nhân chia sẻ (nếu có)
        List<com.ocbs.domain.EmotionJournal> sharedJournals = emotionJournalRepository.findSharedByPatientId(patientId);

        // 3. Lấy danh sách gói trị liệu được gán cho bệnh nhân
        List<com.ocbs.domain.TreatmentPackage> treatmentPackages = treatmentPackageRepository.findAllByPatientId(patientId);

        // 4. Phát hiện nguy cơ tâm lý & cảm xúc tiêu cực để cảnh báo nội dung đỏ cho Bác sĩ
        boolean hasNegativeTest = submissions.stream().anyMatch(s -> {
            String interp = s.getInterpretation() != null ? s.getInterpretation().toLowerCase() : "";
            return interp.contains("nặng") || interp.contains("rất nặng") || interp.contains("severe") || interp.contains("critical") || (s.getTotalScore() != null && s.getTotalScore() >= 14);
        });

        boolean hasNegativeEmotion = sharedJournals.stream().anyMatch(j ->
            (j.getMoodScale() != null && j.getMoodScale() <= 2) ||
            (j.getStressScale() != null && j.getStressScale() >= 4) ||
            (j.getDepressionScale() != null && j.getDepressionScale() >= 4)
        );

        boolean hasNegativeRisk = hasNegativeTest || hasNegativeEmotion;
        String warningMessage = null;
        if (hasNegativeTest && hasNegativeEmotion) {
            warningMessage = "⚠️ CẢNH BÁO NGUY CƠ CAO: Bệnh nhân có kết quả bài test tâm lý ở mức Nặng/Rất Nặng kết hợp cùng nhật ký cảm xúc tiêu cực kéo dài (Stress/Trầm cảm cao). Bác sĩ cần ưu tiên can thiệp khẩn cấp!";
        } else if (hasNegativeTest) {
            warningMessage = "⚠️ CẢNH BÁO LÂM SÀNG: Bệnh nhân có bài trắc nghiệm tâm lý đạt mức độ Nặng/Rất Nặng. Đề xuất bác sĩ đánh giá kỹ trong buổi tham vấn.";
        } else if (hasNegativeEmotion) {
            warningMessage = "⚠️ CẢNH BÁO TÂM TRẠNG: Bệnh nhân chia sẻ các nhật ký cảm xúc có mức độ căng thẳng/buồn bã tiêu cực (Thang điểm cảm xúc 1-2/5, Căng thẳng >= 4/5).";
        }

        // 5. Thống kê trực quan hóa: Thang đo cảm xúc
        java.util.Map<String, Long> moodDistribution = new java.util.LinkedHashMap<>();
        moodDistribution.put("Rất tệ (1⭐)", sharedJournals.stream().filter(j -> j.getMoodScale() != null && j.getMoodScale() == 1).count());
        moodDistribution.put("Tệ (2⭐)", sharedJournals.stream().filter(j -> j.getMoodScale() != null && j.getMoodScale() == 2).count());
        moodDistribution.put("Bình thường (3⭐)", sharedJournals.stream().filter(j -> j.getMoodScale() != null && j.getMoodScale() == 3).count());
        moodDistribution.put("Tốt (4⭐)", sharedJournals.stream().filter(j -> j.getMoodScale() != null && j.getMoodScale() == 4).count());
        moodDistribution.put("Rất tốt (5⭐)", sharedJournals.stream().filter(j -> j.getMoodScale() != null && j.getMoodScale() == 5).count());

        // 6. Thống kê trực quan hóa: Tỷ lệ phân loại kết quả bài test
        java.util.Map<String, Long> testSeverityDistribution = new java.util.LinkedHashMap<>();
        testSeverityDistribution.put("Bình thường", submissions.stream().filter(s -> {
            String interp = s.getInterpretation() != null ? s.getInterpretation().toLowerCase() : "";
            return interp.contains("bình thường") || interp.contains("normal");
        }).count());
        testSeverityDistribution.put("Nhẹ (Mild)", submissions.stream().filter(s -> {
            String interp = s.getInterpretation() != null ? s.getInterpretation().toLowerCase() : "";
            return interp.contains("nhẹ") || interp.contains("mild");
        }).count());
        testSeverityDistribution.put("Vừa (Moderate)", submissions.stream().filter(s -> {
            String interp = s.getInterpretation() != null ? s.getInterpretation().toLowerCase() : "";
            return interp.contains("vừa") || interp.contains("moderate");
        }).count());
        testSeverityDistribution.put("Nặng / Rất Nặng (Severe)", submissions.stream().filter(s -> {
            String interp = s.getInterpretation() != null ? s.getInterpretation().toLowerCase() : "";
            return interp.contains("nặng") || interp.contains("rất nặng") || interp.contains("severe");
        }).count());

        BookingVM.PatientDetailRecordVM vm = new BookingVM.PatientDetailRecordVM(
            patientProfileMapper.toDto(patient),
            total,
            completed,
            appointmentMapper.toDto(doctorAppointments)
        );
        vm.setPsychometricSubmissions(submissions);
        vm.setSharedJournals(sharedJournals);
        vm.setTreatmentPackages(treatmentPackages);
        vm.setHasNegativeRisk(hasNegativeRisk);
        vm.setWarningMessage(warningMessage);
        vm.setMoodDistribution(moodDistribution);
        vm.setTestSeverityDistribution(testSeverityDistribution);

        return ResponseEntity.ok(vm);
    }

    /**
     * {@code POST /doctor/patients/:patientId/treatment-packages} : Doctor creates and assigns a treatment package to patient.
     */
    @PostMapping("/doctor/patients/{patientId}/treatment-packages")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR', 'ROLE_ADMIN')")
    public ResponseEntity<com.ocbs.domain.TreatmentPackage> createPackageForPatient(
        @PathVariable("patientId") Long patientId,
        @Valid @RequestBody com.ocbs.domain.TreatmentPackage treatmentPackage
    ) {
        String login = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new BadRequestAlertException("User not authenticated", ENTITY_NAME, "unauthenticated"));

        Optional<DoctorProfile> doctorOpt = doctorProfileRepository.findByUser_Login(login);
        if (doctorOpt.isEmpty() && !SecurityUtils.hasCurrentUserAnyOfAuthorities(com.ocbs.security.AuthoritiesConstants.ADMIN)) {
            throw new BadRequestAlertException("Chỉ bác sĩ mới có quyền gán gói điều trị", ENTITY_NAME, "accessdenied");
        }

        PatientProfile patient = patientProfileRepository.findById(patientId)
            .orElseThrow(() -> new BadRequestAlertException("Không tìm thấy bệnh nhân", ENTITY_NAME, "patientnotfound"));

        if (doctorOpt.isPresent()) {
            treatmentPackage.setDoctor(doctorOpt.get());
        }
        treatmentPackage.setPatient(patient);
        if (treatmentPackage.getStatus() == null) {
            treatmentPackage.setStatus(com.ocbs.domain.enumeration.TreatmentPackageStatus.PROPOSED);
        }
        if (treatmentPackage.getRemainingSessions() == null) {
            treatmentPackage.setRemainingSessions(treatmentPackage.getSessionQuantity() != null ? treatmentPackage.getSessionQuantity() : 1);
        }
        treatmentPackage.setAssignedDate(Instant.now());
        treatmentPackage.setAcceptanceExpiresAt(Instant.now().plus(7, java.time.temporal.ChronoUnit.DAYS));

        com.ocbs.domain.TreatmentPackage saved = treatmentPackageRepository.save(treatmentPackage);
        LOG.info("✅ [Clinical Service] Doctor '{}' created treatment package #{} for patient #{}",
            login, saved.getId(), patientId);

        return ResponseEntity.ok(saved);
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
        } else if (isAdmin) {
            doctorAppointments = allPatientAppointments;
        } else {
            throw new BadRequestAlertException("Bạn không có quyền xuất hồ sơ của bệnh nhân này", ENTITY_NAME, "accessdenied");
        }

        LOG.info("AUDIT: User '{}' exported clinical PDF for patient ID {}", login, patientId);

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
        boolean isAdmin = SecurityUtils.hasCurrentUserAnyOfAuthorities(com.ocbs.security.AuthoritiesConstants.ADMIN);

        Appointment appointment = appointmentRepository
            .findOneWithToOneRelationships(id)
            .orElseThrow(() -> new BadRequestAlertException("Không tìm thấy lịch hẹn", ENTITY_NAME, "idnotfound"));

        if (!isAdmin) {
            if (doctorOpt.isEmpty() || appointment.getDoctor() == null || !appointment.getDoctor().getId().equals(doctorOpt.get().getId())) {
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
        LOG.info("AUDIT: User '{}' updated consultation note for appointment ID {}", login, id);
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
        LOG.info("AUDIT: Patient user '{}' viewed their clinical records", login);

        List<Appointment> completedList = appointmentRepository.findClinicalRecordsByPatientId(patient.getId());
        return ResponseEntity.ok(appointmentMapper.toDto(completedList));
    }

    /**
     * {@code POST /clinical/psychometrics-alert} : Receive real-time severe score event from opcbs-psychometric-service.
     * Records clinical high-risk alert for the patient and supervising doctor.
     */
    @PostMapping("/clinical/psychometrics-alert")
    @PreAuthorize("hasAuthority('ROLE_PATIENT')")
    public ResponseEntity<java.util.Map<String, Object>> receivePsychometricAlert(
        @RequestBody java.util.Map<String, Object> alertPayload
    ) {
        Long submissionId = parseRequiredId(alertPayload.get("submissionId"), "submissionId");
        Long patientProfileId = parseRequiredId(alertPayload.get("patientId"), "patientId");
        String severityLevel = String.valueOf(alertPayload.getOrDefault("severityLevel", "Nguy cơ cao"));
        if (severityLevel.length() > 80) {
            throw new BadRequestAlertException("Invalid severity level", ENTITY_NAME, "invalidseverity");
        }

        String login = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new BadRequestAlertException("User not authenticated", ENTITY_NAME, "unauthenticated"));
        PatientProfile currentPatient = patientProfileRepository.findByUser_Login(login)
            .orElseThrow(() -> new BadRequestAlertException("Patient profile not found", ENTITY_NAME, "patientnotfound"));
        if (!currentPatient.getId().equals(patientProfileId)) {
            throw new org.springframework.security.access.AccessDeniedException("Alert patient does not match authenticated patient");
        }

        com.ocbs.domain.PsychometricSubmission submission = psychometricSubmissionRepository.findOneWithDetails(submissionId)
            .orElseThrow(() -> new BadRequestAlertException("Assessment submission not found", ENTITY_NAME, "submissionnotfound"));
        if (submission.getPatient() == null || !submission.getPatient().getId().equals(patientProfileId) ||
            !"COMPLETED".equalsIgnoreCase(submission.getStatus())) {
            throw new org.springframework.security.access.AccessDeniedException("Assessment submission ownership or state is invalid");
        }

        if (clinicalPsychometricAlertRepository.existsBySubmissionId(submissionId)) {
            return ResponseEntity.ok(java.util.Map.of("status", "DUPLICATE", "submissionId", submissionId));
        }

        int inserted = clinicalPsychometricAlertRepository.insertIfAbsent(submissionId, patientProfileId, severityLevel);
        if (inserted == 0) {
            return ResponseEntity.ok(java.util.Map.of("status", "DUPLICATE", "submissionId", submissionId));
        }

        LOG.warn("Clinical high-risk alert recorded for submissionId={}.", submissionId);
        return ResponseEntity.ok(java.util.Map.of(
            "status", "RECORDED",
            "submissionId", submissionId,
            "receivedAt", Instant.now().toString()
        ));
    }

    private Long parseRequiredId(Object value, String field) {
        if (value instanceof Number number) return number.longValue();
        if (value instanceof String string) {
            try {
                return Long.parseLong(string);
            } catch (NumberFormatException ignored) {
                // The field is rejected below with the same safe validation response.
            }
        }
        throw new BadRequestAlertException("Missing or invalid " + field, ENTITY_NAME, "invalidalert");
    }
}
