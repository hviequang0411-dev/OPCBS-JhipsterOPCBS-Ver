package com.ocbs.web.rest;

import com.ocbs.domain.DoctorProfile;
import com.ocbs.domain.PatientProfile;
import com.ocbs.domain.TreatmentGoal;
import com.ocbs.domain.TreatmentPackage;
import com.ocbs.domain.TreatmentSession;
import com.ocbs.domain.enumeration.TreatmentPackageStatus;
import com.ocbs.repository.DoctorProfileRepository;
import com.ocbs.repository.PatientProfileRepository;
import com.ocbs.repository.TreatmentGoalRepository;
import com.ocbs.repository.TreatmentPackageRepository;
import com.ocbs.repository.TreatmentSessionRepository;
import com.ocbs.security.AuthoritiesConstants;
import com.ocbs.security.SecurityUtils;
import com.ocbs.web.rest.errors.BadRequestAlertException;
import jakarta.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@Transactional
public class TreatmentPackageResource {

    private static final Logger LOG = LoggerFactory.getLogger(TreatmentPackageResource.class);
    private static final String ENTITY_NAME = "treatmentPackage";

    private final TreatmentPackageRepository treatmentPackageRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final TreatmentGoalRepository treatmentGoalRepository;
    private final TreatmentSessionRepository treatmentSessionRepository;

    public TreatmentPackageResource(
        TreatmentPackageRepository treatmentPackageRepository,
        DoctorProfileRepository doctorProfileRepository,
        PatientProfileRepository patientProfileRepository,
        TreatmentGoalRepository treatmentGoalRepository,
        TreatmentSessionRepository treatmentSessionRepository
    ) {
        this.treatmentPackageRepository = treatmentPackageRepository;
        this.doctorProfileRepository = doctorProfileRepository;
        this.patientProfileRepository = patientProfileRepository;
        this.treatmentGoalRepository = treatmentGoalRepository;
        this.treatmentSessionRepository = treatmentSessionRepository;
    }

    private DoctorProfile getCurrentDoctor() {
        String login = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new BadRequestAlertException("User not authenticated", ENTITY_NAME, "unauthenticated"));
        return doctorProfileRepository
            .findByUser_Login(login)
            .orElseThrow(() -> new BadRequestAlertException("Doctor profile not found", ENTITY_NAME, "doctornotfound"));
    }

    private PatientProfile getCurrentPatient() {
        String login = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new BadRequestAlertException("User not authenticated", ENTITY_NAME, "unauthenticated"));
        return patientProfileRepository
            .findByUser_Login(login)
            .orElseThrow(() -> new BadRequestAlertException("Patient profile not found", ENTITY_NAME, "patientnotfound"));
    }

    private void checkPackageAccess(TreatmentPackage pkg) {
        boolean isAdmin = SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.ADMIN);
        if (isAdmin) return;

        if (SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.PATIENT)) {
            PatientProfile patient = getCurrentPatient();
            if (pkg.getPatient() == null || !pkg.getPatient().getId().equals(patient.getId())) {
                throw new BadRequestAlertException("Bạn không có quyền truy cập gói điều trị này", ENTITY_NAME, "forbidden");
            }
        } else if (SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.DOCTOR)) {
            DoctorProfile doctor = getCurrentDoctor();
            if (pkg.getDoctor() == null || !pkg.getDoctor().getId().equals(doctor.getId())) {
                throw new BadRequestAlertException("Bạn không có quyền truy cập gói điều trị này", ENTITY_NAME, "forbidden");
            }
        } else {
            throw new BadRequestAlertException("Không có quyền truy cập", ENTITY_NAME, "forbidden");
        }
    }

    private void checkDoctorOrAdminOwnership(TreatmentPackage pkg) {
        boolean isAdmin = SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.ADMIN);
        if (isAdmin) return;

        if (SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.DOCTOR)) {
            DoctorProfile doctor = getCurrentDoctor();
            if (pkg.getDoctor() == null || !pkg.getDoctor().getId().equals(doctor.getId())) {
                throw new BadRequestAlertException("Bạn chỉ có quyền quản lý gói điều trị của chính mình", ENTITY_NAME, "forbidden");
            }
        } else {
            throw new BadRequestAlertException("Chỉ bác sĩ phụ trách hoặc quản trị viên mới có quyền thao tác", ENTITY_NAME, "forbidden");
        }
    }

    @GetMapping("/doctor/treatment-packages")
    @PreAuthorize("hasAuthority('ROLE_DOCTOR')")
    public ResponseEntity<List<TreatmentPackage>> getDoctorTreatmentPackages() {
        DoctorProfile doctor = getCurrentDoctor();
        List<TreatmentPackage> packages = treatmentPackageRepository.findAllByDoctorId(doctor.getId());
        return ResponseEntity.ok(packages);
    }

    @GetMapping("/doctor/treatment-packages/{id}")
    @PreAuthorize("hasAuthority('ROLE_DOCTOR')")
    public ResponseEntity<TreatmentPackage> getDoctorTreatmentPackageById(@PathVariable("id") Long id) {
        DoctorProfile doctor = getCurrentDoctor();
        TreatmentPackage pkg = treatmentPackageRepository
            .findOneWithEagerRelationships(id)
            .orElseThrow(() -> new BadRequestAlertException("Package not found", ENTITY_NAME, "idnotfound"));

        if (!pkg.getDoctor().getId().equals(doctor.getId())) {
            throw new BadRequestAlertException("Unauthorized access to this treatment package", ENTITY_NAME, "forbidden");
        }
        return ResponseEntity.ok(pkg);
    }

    @PostMapping("/doctor/treatment-packages")
    @PreAuthorize("hasAuthority('ROLE_DOCTOR')")
    public ResponseEntity<TreatmentPackage> createTreatmentPackage(@Valid @RequestBody TreatmentPackage treatmentPackage)
        throws URISyntaxException {
        if (treatmentPackage.getId() != null) {
            throw new BadRequestAlertException("A new treatment package cannot already have an ID", ENTITY_NAME, "idexists");
        }
        DoctorProfile doctor = getCurrentDoctor();
        treatmentPackage.setDoctor(doctor);
        if (treatmentPackage.getStatus() == null) {
            treatmentPackage.setStatus(TreatmentPackageStatus.CREATED);
        }
        if (treatmentPackage.getRemainingSessions() == null) {
            treatmentPackage.setRemainingSessions(treatmentPackage.getSessionQuantity());
        }

        if (treatmentPackage.getPatient() != null && treatmentPackage.getPatient().getId() != null) {
            PatientProfile p = patientProfileRepository
                .findById(treatmentPackage.getPatient().getId())
                .orElseThrow(() -> new BadRequestAlertException("Patient not found", ENTITY_NAME, "patientnotfound"));
            treatmentPackage.setPatient(p);
            treatmentPackage.setStatus(TreatmentPackageStatus.PROPOSED);
            treatmentPackage.setAssignedDate(Instant.now());
            treatmentPackage.setAcceptanceExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
        }

        TreatmentPackage result = treatmentPackageRepository.save(treatmentPackage);
        return ResponseEntity.created(new URI("/api/doctor/treatment-packages/" + result.getId())).body(result);
    }

    @PutMapping("/doctor/treatment-packages/{id}")
    @PreAuthorize("hasAuthority('ROLE_DOCTOR')")
    public ResponseEntity<TreatmentPackage> updateTreatmentPackage(
        @PathVariable("id") Long id,
        @Valid @RequestBody TreatmentPackage treatmentPackage
    ) {
        DoctorProfile doctor = getCurrentDoctor();
        TreatmentPackage existing = treatmentPackageRepository
            .findOneWithEagerRelationships(id)
            .orElseThrow(() -> new BadRequestAlertException("Package not found", ENTITY_NAME, "idnotfound"));

        if (!existing.getDoctor().getId().equals(doctor.getId())) {
            throw new BadRequestAlertException("Unauthorized access to this treatment package", ENTITY_NAME, "forbidden");
        }

        existing.setName(treatmentPackage.getName());
        existing.setDescription(treatmentPackage.getDescription());
        existing.setTargetOutcome(treatmentPackage.getTargetOutcome());
        existing.setRecommendedExercises(treatmentPackage.getRecommendedExercises());
        existing.setInstructions(treatmentPackage.getInstructions());
        existing.setSessionQuantity(treatmentPackage.getSessionQuantity());
        existing.setRemainingSessions(treatmentPackage.getRemainingSessions() != null ? treatmentPackage.getRemainingSessions() : treatmentPackage.getSessionQuantity());
        existing.setValidityDays(treatmentPackage.getValidityDays());
        existing.setRecommendedSessionsPerWeek(treatmentPackage.getRecommendedSessionsPerWeek());
        existing.setPrice(treatmentPackage.getPrice());

        if (treatmentPackage.getPatient() != null && treatmentPackage.getPatient().getId() != null) {
            PatientProfile p = patientProfileRepository.findById(treatmentPackage.getPatient().getId()).orElse(null);
            existing.setPatient(p);
            if (existing.getStatus() == TreatmentPackageStatus.CREATED) {
                existing.setStatus(TreatmentPackageStatus.PROPOSED);
                existing.setAssignedDate(Instant.now());
                existing.setAcceptanceExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
            }
        }

        TreatmentPackage result = treatmentPackageRepository.save(existing);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/doctor/treatment-packages/{id}")
    @PreAuthorize("hasAuthority('ROLE_DOCTOR')")
    public ResponseEntity<Void> deleteTreatmentPackage(@PathVariable("id") Long id) {
        DoctorProfile doctor = getCurrentDoctor();
        TreatmentPackage existing = treatmentPackageRepository
            .findById(id)
            .orElseThrow(() -> new BadRequestAlertException("Package not found", ENTITY_NAME, "idnotfound"));

        if (!existing.getDoctor().getId().equals(doctor.getId())) {
            throw new BadRequestAlertException("Unauthorized access to this treatment package", ENTITY_NAME, "forbidden");
        }
        treatmentPackageRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/patient/my-treatment-packages")
    @PreAuthorize("hasAuthority('ROLE_PATIENT')")
    public ResponseEntity<List<TreatmentPackage>> getPatientTreatmentPackages() {
        PatientProfile patient = getCurrentPatient();
        List<TreatmentPackage> packages = treatmentPackageRepository.findAllByPatientId(patient.getId());
        return ResponseEntity.ok(packages);
    }

    @GetMapping("/patient/my-treatment-packages/{id}")
    @PreAuthorize("hasAuthority('ROLE_PATIENT')")
    public ResponseEntity<TreatmentPackage> getPatientTreatmentPackageById(@PathVariable("id") Long id) {
        PatientProfile patient = getCurrentPatient();
        TreatmentPackage pkg = treatmentPackageRepository
            .findOneWithEagerRelationships(id)
            .orElseThrow(() -> new BadRequestAlertException("Package not found", ENTITY_NAME, "idnotfound"));

        if (pkg.getPatient() == null || !pkg.getPatient().getId().equals(patient.getId())) {
            throw new BadRequestAlertException("Unauthorized access to this treatment package", ENTITY_NAME, "forbidden");
        }
        return ResponseEntity.ok(pkg);
    }

    @PutMapping("/patient/my-treatment-packages/{id}/accept")
    @PreAuthorize("hasAuthority('ROLE_PATIENT')")
    public ResponseEntity<TreatmentPackage> acceptTreatmentPackage(@PathVariable("id") Long id) {
        PatientProfile patient = getCurrentPatient();
        TreatmentPackage pkg = treatmentPackageRepository
            .findOneWithEagerRelationships(id)
            .orElseThrow(() -> new BadRequestAlertException("Package not found", ENTITY_NAME, "idnotfound"));

        if (pkg.getPatient() == null || !pkg.getPatient().getId().equals(patient.getId())) {
            throw new BadRequestAlertException("Unauthorized access to this treatment package", ENTITY_NAME, "forbidden");
        }

        if (pkg.getStatus() != TreatmentPackageStatus.PROPOSED && pkg.getStatus() != TreatmentPackageStatus.CREATED) {
            throw new BadRequestAlertException("Chỉ có thể đồng ý gói đang được đề xuất", ENTITY_NAME, "invalidstatus");
        }

        Instant now = Instant.now();
        pkg.setStatus(TreatmentPackageStatus.ACTIVE);
        pkg.setAcceptedDate(now);
        pkg.setActiveDate(now);
        pkg.setRemainingSessions(pkg.getSessionQuantity());
        pkg.setExpirationDate(now.plus(pkg.getValidityDays(), ChronoUnit.DAYS));

        TreatmentPackage saved = treatmentPackageRepository.save(pkg);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/patient/my-treatment-packages/{id}/reject")
    @PreAuthorize("hasAuthority('ROLE_PATIENT')")
    public ResponseEntity<TreatmentPackage> rejectTreatmentPackage(
        @PathVariable("id") Long id,
        @RequestBody(required = false) Map<String, String> body
    ) {
        PatientProfile patient = getCurrentPatient();
        TreatmentPackage pkg = treatmentPackageRepository
            .findOneWithEagerRelationships(id)
            .orElseThrow(() -> new BadRequestAlertException("Package not found", ENTITY_NAME, "idnotfound"));

        if (pkg.getPatient() == null || !pkg.getPatient().getId().equals(patient.getId())) {
            throw new BadRequestAlertException("Unauthorized access to this treatment package", ENTITY_NAME, "forbidden");
        }

        pkg.setStatus(TreatmentPackageStatus.REJECTED);
        if (body != null && body.containsKey("reason")) {
            pkg.setRejectionReason(body.get("reason"));
        }

        TreatmentPackage saved = treatmentPackageRepository.save(pkg);
        return ResponseEntity.ok(saved);
    }

    /**
     * {@code POST /patient/my-treatment-packages/:id/use-session} : Patient or Doctor records 1 session used from active package.
     */
    @PostMapping("/patient/my-treatment-packages/{id}/use-session")
    @PreAuthorize("hasAnyAuthority('ROLE_PATIENT', 'ROLE_DOCTOR', 'ROLE_ADMIN')")
    public ResponseEntity<TreatmentPackage> usePackageSession(@PathVariable("id") Long id) {
        TreatmentPackage pkg = treatmentPackageRepository
            .findOneWithEagerRelationships(id)
            .orElseThrow(() -> new BadRequestAlertException("Package not found", ENTITY_NAME, "idnotfound"));

        checkPackageAccess(pkg);

        if (pkg.getStatus() != TreatmentPackageStatus.ACTIVE) {
            throw new BadRequestAlertException("Gói trị liệu chưa được kích hoạt hoặc đã hết hạn", ENTITY_NAME, "packagenotactive");
        }

        int currentRemaining = pkg.getRemainingSessions() != null ? pkg.getRemainingSessions() : 0;
        if (currentRemaining <= 0) {
            pkg.setStatus(TreatmentPackageStatus.COMPLETED);
            treatmentPackageRepository.save(pkg);
            throw new BadRequestAlertException("Gói trị liệu đã hết số buổi khả dụng", ENTITY_NAME, "nosessionsleft");
        }

        int newRemaining = currentRemaining - 1;
        pkg.setRemainingSessions(newRemaining);
        if (newRemaining == 0) {
            pkg.setStatus(TreatmentPackageStatus.COMPLETED);
        }

        TreatmentPackage result = treatmentPackageRepository.save(pkg);
        LOG.info("⚡ Manually deducted 1 session from Package ID: {}. Remaining: {}/{}", pkg.getId(), newRemaining, pkg.getSessionQuantity());
        return ResponseEntity.ok(result);
    }

    /**
     * {@code POST /patient/my-treatment-packages/:id/request-hold} : Bệnh nhân gửi yêu cầu bảo lưu phác đồ điều trị.
     */
    @PostMapping("/patient/my-treatment-packages/{id}/request-hold")
    @PreAuthorize("hasAuthority('ROLE_PATIENT')")
    public ResponseEntity<TreatmentPackage> requestHoldPackage(
        @PathVariable("id") Long id,
        @RequestBody Map<String, Object> body
    ) {
        PatientProfile patient = getCurrentPatient();
        TreatmentPackage pkg = treatmentPackageRepository
            .findOneWithEagerRelationships(id)
            .orElseThrow(() -> new BadRequestAlertException("Không tìm thấy phác đồ điều trị", ENTITY_NAME, "idnotfound"));

        if (!pkg.getPatient().getId().equals(patient.getId())) {
            throw new BadRequestAlertException("Bạn không có quyền yêu cầu bảo lưu phác đồ này", ENTITY_NAME, "forbidden");
        }

        if (pkg.getStatus() != TreatmentPackageStatus.ACTIVE) {
            throw new BadRequestAlertException("Chỉ có thể yêu cầu bảo lưu cho phác đồ đang kích hoạt (ACTIVE)", ENTITY_NAME, "notactive");
        }

        Integer durationDays = body.get("durationDays") != null ? Integer.valueOf(body.get("durationDays").toString()) : 30;
        if (durationDays < 1 || durationDays > 180) {
            throw new BadRequestAlertException("Thời gian bảo lưu hợp lệ từ 1 đến 180 ngày", ENTITY_NAME, "invalidholdduration");
        }
        String reason = body.get("reason") != null ? body.get("reason").toString() : "Bận công việc cá nhân";

        pkg.setIsHoldRequested(true);
        pkg.setHoldDurationDays(durationDays);
        pkg.setHoldRequestedAt(Instant.now());
        pkg.setHoldReason(reason);

        TreatmentPackage saved = treatmentPackageRepository.save(pkg);
        LOG.info("📋 Bệnh nhân {} yêu cầu bảo lưu phác đồ ID: {} trong {} ngày", patient.getId(), pkg.getId(), durationDays);
        return ResponseEntity.ok(saved);
    }

    /**
     * {@code POST /patient/my-treatment-packages/:id/cancel-hold-request} : Bệnh nhân hủy yêu cầu bảo lưu đang chờ duyệt.
     */
    @PostMapping("/patient/my-treatment-packages/{id}/cancel-hold-request")
    @PreAuthorize("hasAuthority('ROLE_PATIENT')")
    public ResponseEntity<TreatmentPackage> cancelHoldRequest(@PathVariable("id") Long id) {
        PatientProfile patient = getCurrentPatient();
        TreatmentPackage pkg = treatmentPackageRepository
            .findOneWithEagerRelationships(id)
            .orElseThrow(() -> new BadRequestAlertException("Không tìm thấy phác đồ điều trị", ENTITY_NAME, "idnotfound"));

        if (!pkg.getPatient().getId().equals(patient.getId())) {
            throw new BadRequestAlertException("Bạn không có quyền thực hiện thao tác này", ENTITY_NAME, "forbidden");
        }

        if (!Boolean.TRUE.equals(pkg.getIsHoldRequested())) {
            throw new BadRequestAlertException("Phác đồ không có yêu cầu bảo lưu nào đang chờ duyệt", ENTITY_NAME, "noholdpending");
        }

        pkg.setIsHoldRequested(false);
        pkg.setHoldDurationDays(null);
        pkg.setHoldRequestedAt(null);
        pkg.setHoldReason(null);

        TreatmentPackage saved = treatmentPackageRepository.save(pkg);
        LOG.info("❌ Bệnh nhân {} đã hủy yêu cầu bảo lưu phác đồ ID: {}", patient.getId(), pkg.getId());
        return ResponseEntity.ok(saved);
    }

    /**
     * {@code POST /doctor/treatment-packages/:id/approve-hold} : Bác sĩ phê duyệt bảo lưu, chuyển sang ON_HOLD và tự động gia hạn hạn chót.
     */
    @PostMapping("/doctor/treatment-packages/{id}/approve-hold")
    @PreAuthorize("hasAuthority('ROLE_DOCTOR')")
    public ResponseEntity<TreatmentPackage> approveHoldPackage(
        @PathVariable("id") Long id,
        @RequestBody(required = false) Map<String, Object> body
    ) {
        DoctorProfile doctor = getCurrentDoctor();
        TreatmentPackage pkg = treatmentPackageRepository
            .findOneWithEagerRelationships(id)
            .orElseThrow(() -> new BadRequestAlertException("Không tìm thấy phác đồ điều trị", ENTITY_NAME, "idnotfound"));

        if (!pkg.getDoctor().getId().equals(doctor.getId())) {
            throw new BadRequestAlertException("Bạn chỉ có quyền quản lý phác đồ điều trị của chính mình", ENTITY_NAME, "forbidden");
        }

        if (!Boolean.TRUE.equals(pkg.getIsHoldRequested())) {
            throw new BadRequestAlertException("Không có yêu cầu bảo lưu nào đang chờ duyệt cho phác đồ này", ENTITY_NAME, "noholdpending");
        }

        int durationDays = pkg.getHoldDurationDays() != null && pkg.getHoldDurationDays() > 0 ? pkg.getHoldDurationDays() : 30;

        pkg.setStatus(TreatmentPackageStatus.ON_HOLD);
        pkg.setIsHoldRequested(false);
        pkg.setHoldApprovedAt(Instant.now());
        pkg.setHoldApprovedByDoctorId(doctor.getId());
        pkg.setHoldStartDate(Instant.now());
        pkg.setHoldEndDate(Instant.now().plus(durationDays, ChronoUnit.DAYS));
        pkg.setTotalHoldDays((pkg.getTotalHoldDays() != null ? pkg.getTotalHoldDays() : 0) + durationDays);

        // Tự động gia hạn hạn hết hạn của gói điều trị theo đúng số ngày bảo lưu
        if (pkg.getExpirationDate() != null) {
            pkg.setExpirationDate(pkg.getExpirationDate().plus(durationDays, ChronoUnit.DAYS));
        }

        TreatmentPackage saved = treatmentPackageRepository.save(pkg);
        LOG.info("✅ Bác sĩ {} đã duyệt bảo lưu phác đồ ID: {} thêm {} ngày. Hạn mới: {}", doctor.getId(), pkg.getId(), durationDays, pkg.getExpirationDate());
        return ResponseEntity.ok(saved);
    }

    /**
     * {@code POST /doctor/treatment-packages/:id/reject-hold} : Bác sĩ từ chối yêu cầu bảo lưu.
     */
    @PostMapping("/doctor/treatment-packages/{id}/reject-hold")
    @PreAuthorize("hasAuthority('ROLE_DOCTOR')")
    public ResponseEntity<TreatmentPackage> rejectHoldPackage(
        @PathVariable("id") Long id,
        @RequestBody(required = false) Map<String, Object> body
    ) {
        DoctorProfile doctor = getCurrentDoctor();
        TreatmentPackage pkg = treatmentPackageRepository
            .findOneWithEagerRelationships(id)
            .orElseThrow(() -> new BadRequestAlertException("Không tìm thấy phác đồ điều trị", ENTITY_NAME, "idnotfound"));

        if (!pkg.getDoctor().getId().equals(doctor.getId())) {
            throw new BadRequestAlertException("Bạn chỉ có quyền quản lý phác đồ điều trị của chính mình", ENTITY_NAME, "forbidden");
        }

        if (!Boolean.TRUE.equals(pkg.getIsHoldRequested())) {
            throw new BadRequestAlertException("Không có yêu cầu bảo lưu nào đang chờ duyệt", ENTITY_NAME, "noholdpending");
        }

        String doctorNote = body != null && body.get("doctorNote") != null ? body.get("doctorNote").toString() : "Bác sĩ từ chối yêu cầu bảo lưu do tiến độ điều trị cần liên tục";

        pkg.setIsHoldRequested(false);
        pkg.setHoldRejectedAt(Instant.now());
        pkg.setHoldRejectionReason(doctorNote);

        TreatmentPackage saved = treatmentPackageRepository.save(pkg);
        LOG.info("🚫 Bác sĩ {} đã từ chối yêu cầu bảo lưu phác đồ ID: {}", doctor.getId(), pkg.getId());
        return ResponseEntity.ok(saved);
    }

    /**
     * {@code POST /treatment-packages/:id/resume} : Mở lại phác đồ điều trị trước hoặc đúng hạn.
     */
    @PostMapping("/treatment-packages/{id}/resume")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR', 'ROLE_PATIENT', 'ROLE_ADMIN')")
    public ResponseEntity<TreatmentPackage> resumePackage(@PathVariable("id") Long id) {
        TreatmentPackage pkg = treatmentPackageRepository
            .findOneWithEagerRelationships(id)
            .orElseThrow(() -> new BadRequestAlertException("Không tìm thấy phác đồ điều trị", ENTITY_NAME, "idnotfound"));

        checkPackageAccess(pkg);

        if (pkg.getStatus() != TreatmentPackageStatus.ON_HOLD) {
            throw new BadRequestAlertException("Chỉ có thể kích hoạt lại phác đồ đang ở trạng thái TẠM DỪNG / BẢO LƯU (ON_HOLD)", ENTITY_NAME, "notonhold");
        }

        pkg.setStatus(TreatmentPackageStatus.ACTIVE);
        pkg.setIsHoldRequested(false);
        pkg.setHoldStartDate(null);
        pkg.setHoldEndDate(null);

        TreatmentPackage saved = treatmentPackageRepository.save(pkg);
        LOG.info("🔄 Kích hoạt lại phác đồ ID: {} về trạng thái ACTIVE", pkg.getId());
        return ResponseEntity.ok(saved);
    }

    /**
     * {@code GET /treatment-packages/:id/goals} : Lấy danh sách mục tiêu điều trị.
     */
    @GetMapping("/treatment-packages/{id}/goals")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR', 'ROLE_PATIENT', 'ROLE_ADMIN')")
    public ResponseEntity<List<TreatmentGoal>> getPackageGoals(@PathVariable("id") Long id) {
        TreatmentPackage pkg = treatmentPackageRepository
            .findById(id)
            .orElseThrow(() -> new BadRequestAlertException("Không tìm thấy phác đồ điều trị", ENTITY_NAME, "idnotfound"));
        checkPackageAccess(pkg);

        List<TreatmentGoal> goals = treatmentGoalRepository.findByTreatmentPackage_IdOrderByPriorityDescTargetDateAsc(id);
        return ResponseEntity.ok(goals);
    }

    /**
     * {@code POST /treatment-packages/:id/goals} : Tạo mục tiêu điều trị mới.
     */
    @PostMapping("/treatment-packages/{id}/goals")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR', 'ROLE_ADMIN')")
    public ResponseEntity<TreatmentGoal> createGoal(
        @PathVariable("id") Long id,
        @Valid @RequestBody TreatmentGoal goal
    ) throws URISyntaxException {
        TreatmentPackage pkg = treatmentPackageRepository
            .findById(id)
            .orElseThrow(() -> new BadRequestAlertException("Không tìm thấy phác đồ điều trị", ENTITY_NAME, "idnotfound"));
        checkDoctorOrAdminOwnership(pkg);

        goal.setTreatmentPackage(pkg);
        if (goal.getProgressPercent() == null) {
            goal.setProgressPercent(0);
        }
        if (goal.getStatus() == null) {
            goal.setStatus("IN_PROGRESS");
        }
        TreatmentGoal result = treatmentGoalRepository.save(goal);
        return ResponseEntity.created(new URI("/api/treatment-packages/" + id + "/goals/" + result.getId())).body(result);
    }

    /**
     * {@code PUT /treatment-packages/:id/goals/:goalId} : Cập nhật mục tiêu điều trị.
     */
    @PutMapping("/treatment-packages/{id}/goals/{goalId}")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR', 'ROLE_ADMIN')")
    public ResponseEntity<TreatmentGoal> updateGoal(
        @PathVariable("id") Long id,
        @PathVariable("goalId") Long goalId,
        @Valid @RequestBody TreatmentGoal goalDetails
    ) {
        TreatmentPackage pkg = treatmentPackageRepository
            .findById(id)
            .orElseThrow(() -> new BadRequestAlertException("Không tìm thấy phác đồ điều trị", ENTITY_NAME, "idnotfound"));
        checkDoctorOrAdminOwnership(pkg);

        TreatmentGoal goal = treatmentGoalRepository
            .findById(goalId)
            .orElseThrow(() -> new BadRequestAlertException("Không tìm thấy mục tiêu", "treatmentGoal", "idnotfound"));

        goal.setTitle(goalDetails.getTitle());
        goal.setDescription(goalDetails.getDescription());
        goal.setTargetDate(goalDetails.getTargetDate());
        goal.setProgressPercent(goalDetails.getProgressPercent());
        goal.setPriority(goalDetails.getPriority());
        goal.setStatus(goalDetails.getStatus());

        if (goalDetails.getProgressPercent() != null && goalDetails.getProgressPercent() >= 100) {
            goal.setStatus("ACHIEVED");
            if (goal.getAchievedDate() == null) {
                goal.setAchievedDate(Instant.now());
            }
        }

        TreatmentGoal result = treatmentGoalRepository.save(goal);
        return ResponseEntity.ok(result);
    }

    /**
     * {@code DELETE /treatment-packages/:id/goals/:goalId} : Xóa mục tiêu điều trị.
     */
    @DeleteMapping("/treatment-packages/{id}/goals/{goalId}")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR', 'ROLE_ADMIN')")
    public ResponseEntity<Void> deleteGoal(@PathVariable("id") Long id, @PathVariable("goalId") Long goalId) {
        TreatmentPackage pkg = treatmentPackageRepository
            .findById(id)
            .orElseThrow(() -> new BadRequestAlertException("Không tìm thấy phác đồ điều trị", ENTITY_NAME, "idnotfound"));
        checkDoctorOrAdminOwnership(pkg);

        treatmentGoalRepository.deleteById(goalId);
        return ResponseEntity.noContent().build();
    }

    /**
     * {@code GET /treatment-packages/:id/sessions} : Lấy danh sách buổi tham vấn trong phác đồ.
     */
    @GetMapping("/treatment-packages/{id}/sessions")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR', 'ROLE_PATIENT', 'ROLE_ADMIN')")
    public ResponseEntity<List<TreatmentSession>> getPackageSessions(@PathVariable("id") Long id) {
        TreatmentPackage pkg = treatmentPackageRepository
            .findById(id)
            .orElseThrow(() -> new BadRequestAlertException("Không tìm thấy phác đồ điều trị", ENTITY_NAME, "idnotfound"));
        checkPackageAccess(pkg);

        List<TreatmentSession> sessions = treatmentSessionRepository.findByTreatmentPackage_IdOrderBySessionNumberAsc(id);
        return ResponseEntity.ok(sessions);
    }

    /**
     * {@code POST /treatment-packages/:id/sessions} : Bác sĩ ghi nhận buổi tham vấn điều trị.
     */
    @PostMapping("/treatment-packages/{id}/sessions")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR', 'ROLE_ADMIN')")
    public ResponseEntity<TreatmentSession> createSession(
        @PathVariable("id") Long id,
        @Valid @RequestBody TreatmentSession session
    ) throws URISyntaxException {
        TreatmentPackage pkg = treatmentPackageRepository
            .findById(id)
            .orElseThrow(() -> new BadRequestAlertException("Không tìm thấy phác đồ điều trị", ENTITY_NAME, "idnotfound"));
        checkDoctorOrAdminOwnership(pkg);

        session.setTreatmentPackage(pkg);
        if (session.getCreatedAt() == null) {
            session.setCreatedAt(Instant.now());
        }
        if (session.getStatus() == null) {
            session.setStatus("COMPLETED");
        }

        // Tự động đánh số buổi tiếp theo nếu chưa có
        if (session.getSessionNumber() == null) {
            long count = treatmentSessionRepository.countByTreatmentPackage_Id(id);
            session.setSessionNumber((int) count + 1);
        }

        TreatmentSession result = treatmentSessionRepository.save(session);
        return ResponseEntity.created(new URI("/api/treatment-packages/" + id + "/sessions/" + result.getId())).body(result);
    }

    /**
     * {@code GET /treatment-packages/:id/progress} : Thống kê chỉ số tiến độ được tính toán trực tiếp ở tầng Database.
     */
    @GetMapping("/treatment-packages/{id}/progress")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR', 'ROLE_PATIENT', 'ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> getTreatmentProgressSummary(@PathVariable("id") Long id) {
        TreatmentPackage pkg = treatmentPackageRepository
            .findById(id)
            .orElseThrow(() -> new BadRequestAlertException("Không tìm thấy phác đồ điều trị", ENTITY_NAME, "idnotfound"));
        checkPackageAccess(pkg);

        int totalSessions = pkg.getSessionQuantity() != null ? pkg.getSessionQuantity() : 0;
        int remainingSessions = pkg.getRemainingSessions() != null ? pkg.getRemainingSessions() : 0;
        int completedSessions = Math.max(0, totalSessions - remainingSessions);

        double sessionProgressPercent = totalSessions > 0 ? (double) (completedSessions * 100) / totalSessions : 0.0;

        // DB level aggregates
        long totalGoals = treatmentGoalRepository.countByTreatmentPackage_Id(id);
        long completedGoals = treatmentGoalRepository.countByTreatmentPackage_IdAndStatus(id, "ACHIEVED");
        Double avgGoalProgress = treatmentGoalRepository.getAverageProgressPercentByPackageId(id);
        double goalProgressPercent = avgGoalProgress != null ? Math.round(avgGoalProgress * 100.0) / 100.0 : 0.0;

        long recordedSessions = treatmentSessionRepository.countByTreatmentPackage_Id(id);

        Map<String, Object> progress = new java.util.LinkedHashMap<>();
        progress.put("packageId", id);
        progress.put("packageName", pkg.getName() != null ? pkg.getName() : "Phác đồ điều trị");
        progress.put("status", pkg.getStatus() != null ? pkg.getStatus().name() : "UNKNOWN");
        progress.put("isHoldRequested", Boolean.TRUE.equals(pkg.getIsHoldRequested()));
        progress.put("holdStartDate", pkg.getHoldStartDate());
        progress.put("holdEndDate", pkg.getHoldEndDate());
        progress.put("totalHoldDays", pkg.getTotalHoldDays() != null ? pkg.getTotalHoldDays() : 0);
        progress.put("totalSessions", totalSessions);
        progress.put("remainingSessions", remainingSessions);
        progress.put("completedSessions", completedSessions);
        progress.put("sessionProgressPercent", Math.round(sessionProgressPercent * 10.0) / 10.0);
        progress.put("totalGoals", totalGoals);
        progress.put("completedGoals", completedGoals);
        progress.put("goalProgressPercent", goalProgressPercent);
        progress.put("recordedSessions", recordedSessions);

        return ResponseEntity.ok(progress);
    }

    /**
     * {@code POST /treatment-packages/deduct-session} : Internal endpoint to deduct 1 session for a patient upon booking/completing appointment.
     */
    @PostMapping("/treatment-packages/deduct-session")
    @PreAuthorize("hasAnyAuthority('ROLE_PATIENT', 'ROLE_DOCTOR', 'ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> deductSessionForPatient(
        @RequestParam(name = "patientId") Long patientId,
        @RequestParam(name = "doctorId", required = false) Long doctorId
    ) {
        boolean isAdmin = SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.ADMIN);
        if (!isAdmin) {
            if (SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.PATIENT)) {
                PatientProfile patient = getCurrentPatient();
                if (!patient.getId().equals(patientId)) {
                    throw new BadRequestAlertException("Không có quyền trừ buổi cho bệnh nhân khác", ENTITY_NAME, "forbidden");
                }
            } else if (SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.DOCTOR)) {
                DoctorProfile doctor = getCurrentDoctor();
                if (doctorId != null && !doctor.getId().equals(doctorId)) {
                    throw new BadRequestAlertException("Không có quyền thao tác với bác sĩ khác", ENTITY_NAME, "forbidden");
                }
            }
        }

        List<TreatmentPackage> activePackages = null;
        if (doctorId != null) {
            activePackages = treatmentPackageRepository.findActivePackagesByPatientAndDoctor(patientId, doctorId);
        }
        if (activePackages == null || activePackages.isEmpty()) {
            activePackages = treatmentPackageRepository.findActivePackagesByPatient(patientId);
        }

        if (activePackages != null && !activePackages.isEmpty()) {
            TreatmentPackage target = activePackages.get(0);
            int rem = target.getRemainingSessions() != null ? target.getRemainingSessions() : 0;
            if (rem > 0) {
                target.setRemainingSessions(rem - 1);
                if (rem - 1 == 0) {
                    target.setStatus(TreatmentPackageStatus.COMPLETED);
                }
                treatmentPackageRepository.save(target);
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "packageId", target.getId(),
                    "packageName", target.getName(),
                    "remainingSessions", target.getRemainingSessions(),
                    "totalSessions", target.getSessionQuantity()
                ));
            }
        }
        return ResponseEntity.ok(Map.of("success", false, "message", "No active package available"));
    }
}

