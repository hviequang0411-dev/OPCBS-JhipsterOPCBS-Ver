package com.ocbs.web.rest;

import com.ocbs.domain.DoctorProfile;
import com.ocbs.domain.PatientProfile;
import com.ocbs.domain.TreatmentPackage;
import com.ocbs.domain.enumeration.TreatmentPackageStatus;
import com.ocbs.repository.DoctorProfileRepository;
import com.ocbs.repository.PatientProfileRepository;
import com.ocbs.repository.TreatmentPackageRepository;
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

    public TreatmentPackageResource(
        TreatmentPackageRepository treatmentPackageRepository,
        DoctorProfileRepository doctorProfileRepository,
        PatientProfileRepository patientProfileRepository
    ) {
        this.treatmentPackageRepository = treatmentPackageRepository;
        this.doctorProfileRepository = doctorProfileRepository;
        this.patientProfileRepository = patientProfileRepository;
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
     * {@code POST /treatment-packages/deduct-session} : Internal endpoint to deduct 1 session for a patient upon booking/completing appointment.
     */
    @PostMapping("/treatment-packages/deduct-session")
    @PreAuthorize("hasAnyAuthority('ROLE_PATIENT', 'ROLE_DOCTOR', 'ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> deductSessionForPatient(
        @RequestParam(name = "patientId") Long patientId,
        @RequestParam(name = "doctorId", required = false) Long doctorId
    ) {
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

