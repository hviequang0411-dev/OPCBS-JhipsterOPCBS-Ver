package com.ocbs.web.rest;

import com.ocbs.security.AuthoritiesConstants;
import com.ocbs.security.SecurityUtils;
import com.ocbs.service.DoctorVerificationService;
import com.ocbs.service.dto.DoctorVerificationDTO;
import com.ocbs.service.dto.DoctorVerificationSummaryDTO;
import com.ocbs.service.dto.ReviewVerificationDTO;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for Admin Doctor Verification and Practice Certificate Approval (Duyệt chứng chỉ bác sĩ).
 * References the C# OPCBS verification moderation workflow.
 */
@RestController
@RequestMapping("/api/admin/doctor-verifications")
@PreAuthorize("hasAuthority('" + AuthoritiesConstants.ADMIN + "')")
public class AdminDoctorVerificationResource {

    private static final Logger LOG = LoggerFactory.getLogger(AdminDoctorVerificationResource.class);

    private final DoctorVerificationService doctorVerificationService;

    public AdminDoctorVerificationResource(DoctorVerificationService doctorVerificationService) {
        this.doctorVerificationService = doctorVerificationService;
    }

    /**
     * {@code GET /api/admin/doctor-verifications} : Get paginated list of doctor verification requests.
     */
    @GetMapping("")
    public ResponseEntity<Page<DoctorVerificationDTO>> getAllVerifications(
        @RequestParam(name = "status", required = false) String status,
        @RequestParam(name = "search", required = false) String search,
        Pageable pageable
    ) {
        LOG.info("📋 [Admin Verifications] Fetching verifications - status: '{}', search: '{}', pageable: {}", status, search, pageable);
        Page<DoctorVerificationDTO> page = doctorVerificationService.getAllVerifications(status, search, pageable);
        return ResponseEntity.ok(page);
    }

    /**
     * {@code GET /api/admin/doctor-verifications/summary} : Get verification counts grouped by status.
     */
    @GetMapping("/summary")
    public ResponseEntity<DoctorVerificationSummaryDTO> getVerificationSummary() {
        DoctorVerificationSummaryDTO summary = doctorVerificationService.getVerificationSummary();
        return ResponseEntity.ok(summary);
    }

    /**
     * {@code GET /api/admin/doctor-verifications/:id} : Get detail of a single verification request.
     */
    @GetMapping("/{id}")
    public ResponseEntity<DoctorVerificationDTO> getVerificationById(@PathVariable("id") Long id) {
        LOG.info("🔍 [Admin Verifications] Getting verification detail for ID: {}", id);
        DoctorVerificationDTO dto = doctorVerificationService.getVerificationById(id);
        return ResponseEntity.ok(dto);
    }

    /**
     * {@code PUT /api/admin/doctor-verifications/:id/review} : Review verification application.
     */
    @PutMapping("/{id}/review")
    public ResponseEntity<DoctorVerificationDTO> reviewVerification(
        @PathVariable("id") Long id,
        @RequestBody ReviewVerificationDTO reviewDto
    ) {
        String adminLogin = SecurityUtils.getCurrentUserLogin().orElse("admin");
        LOG.info("⚖️ [Admin Verifications] Reviewing ID: {}, Action: {}, Admin: {}", id, reviewDto != null ? reviewDto.getAction() : "null", adminLogin);
        DoctorVerificationDTO result = doctorVerificationService.reviewVerification(id, reviewDto, adminLogin);
        return ResponseEntity.ok(result);
    }

    /**
     * {@code POST /api/admin/doctor-verifications/:id/approve} : Approve doctor verification application.
     */
    @PostMapping("/{id}/approve")
    public ResponseEntity<DoctorVerificationDTO> approveVerification(@PathVariable("id") Long id) {
        String adminLogin = SecurityUtils.getCurrentUserLogin().orElse("admin");
        LOG.info("✅ [Admin Verifications] Explicit Approve for ID: {}, Admin: {}", id, adminLogin);
        ReviewVerificationDTO dto = new ReviewVerificationDTO("APPROVE", true, null);
        DoctorVerificationDTO result = doctorVerificationService.reviewVerification(id, dto, adminLogin);
        return ResponseEntity.ok(result);
    }

    /**
     * {@code POST /api/admin/doctor-verifications/:id/reject} : Reject doctor verification application.
     */
    @PostMapping("/{id}/reject")
    public ResponseEntity<DoctorVerificationDTO> rejectVerification(
        @PathVariable("id") Long id,
        @RequestBody Map<String, String> payload
    ) {
        String adminLogin = SecurityUtils.getCurrentUserLogin().orElse("admin");
        String reason = payload != null ? payload.getOrDefault("reason", payload.get("rejectionReason")) : null;
        LOG.warn("❌ [Admin Verifications] Explicit Reject for ID: {}, Reason: {}, Admin: {}", id, reason, adminLogin);
        ReviewVerificationDTO dto = new ReviewVerificationDTO("REJECT", false, reason);
        DoctorVerificationDTO result = doctorVerificationService.reviewVerification(id, dto, adminLogin);
        return ResponseEntity.ok(result);
    }

    /**
     * {@code POST /api/admin/doctor-verifications/:id/request-info} : Request additional info/documents.
     */
    @PostMapping("/{id}/request-info")
    public ResponseEntity<DoctorVerificationDTO> requestAdditionalInfo(
        @PathVariable("id") Long id,
        @RequestBody Map<String, String> payload
    ) {
        String adminLogin = SecurityUtils.getCurrentUserLogin().orElse("admin");
        String reason = payload != null ? payload.getOrDefault("reason", payload.get("rejectionReason")) : null;
        LOG.info("ℹ️ [Admin Verifications] Request Info for ID: {}, Note: {}, Admin: {}", id, reason, adminLogin);
        ReviewVerificationDTO dto = new ReviewVerificationDTO("REQUEST_INFO", false, reason);
        DoctorVerificationDTO result = doctorVerificationService.reviewVerification(id, dto, adminLogin);
        return ResponseEntity.ok(result);
    }
}
