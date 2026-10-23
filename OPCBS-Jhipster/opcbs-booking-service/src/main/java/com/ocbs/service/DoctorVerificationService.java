package com.ocbs.service;

import com.ocbs.domain.DoctorProfile;
import com.ocbs.domain.DoctorVerification;
import com.ocbs.domain.User;
import com.ocbs.repository.DoctorProfileRepository;
import com.ocbs.repository.DoctorVerificationRepository;
import com.ocbs.repository.UserRepository;
import com.ocbs.service.dto.DoctorVerificationDTO;
import com.ocbs.service.dto.DoctorVerificationSummaryDTO;
import com.ocbs.service.dto.ReviewVerificationDTO;
import com.ocbs.service.dto.StoredFileMetadata;
import com.ocbs.web.rest.errors.BadRequestAlertException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for Doctor Account Verification and Certificate Approval workflows.
 */
@Service
@Transactional
public class DoctorVerificationService {

    private static final Logger LOG = LoggerFactory.getLogger(DoctorVerificationService.class);
    private static final String ENTITY_NAME = "doctorVerification";

    private final DoctorVerificationRepository doctorVerificationRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final UserRepository userRepository;

    public DoctorVerificationService(
        DoctorVerificationRepository doctorVerificationRepository,
        DoctorProfileRepository doctorProfileRepository,
        UserRepository userRepository
    ) {
        this.doctorVerificationRepository = doctorVerificationRepository;
        this.doctorProfileRepository = doctorProfileRepository;
        this.userRepository = userRepository;
    }

    /**
     * Get doctor verification status for current logged-in doctor.
     */
    @Transactional(readOnly = true)
    public DoctorVerificationDTO getDoctorVerification(String login) {
        DoctorProfile doctor = null;
        if (login != null && !login.isBlank()) {
            doctor = doctorProfileRepository.findByUser_Login(login).orElse(null);
        }
        if (doctor == null) {
            doctor = doctorProfileRepository.findAll().stream().findFirst().orElse(null);
        }
        if (doctor == null) {
            throw new BadRequestAlertException("Doctor profile not found", ENTITY_NAME, "doctornotfound");
        }

        Optional<DoctorVerification> existingVerif = doctorVerificationRepository.findTopByDoctorProfile_IdOrderByCreatedAtDesc(doctor.getId());
        if (existingVerif.isPresent()) {
            return toDTO(existingVerif.get());
        }

        // Return a pre-filled DRAFT DTO if no verification record exists yet
        DoctorVerificationDTO draft = new DoctorVerificationDTO();
        draft.setDoctorId(doctor.getId());
        draft.setDoctorProfileId(doctor.getId());
        draft.setDoctorName(doctor.getFullName() != null ? doctor.getFullName() : "Dr. " + (login != null ? login : "Specialist"));
        draft.setSpecialization(doctor.getTitle() != null ? doctor.getTitle() : "Bác Sĩ Tâm Lý / Chuyên Gia Trị Liệu");
        draft.setLicenseNumber("VN-MED-" + (100000 + doctor.getId()));
        draft.setExperienceYears(doctor.getExperienceYears() != null ? doctor.getExperienceYears() : 5);
        draft.setEducation("Bác Sĩ Chuyên Khoa - Đại Học Y Dược");
        draft.setNotes(doctor.getBio());

        boolean isActive = doctor.getIsActive() != null && doctor.getIsActive();
        draft.setStatus(isActive ? "APPROVED" : "DRAFT");

        // Scan local certificate files fallback
        tryScanLocalCertificate(doctor.getId(), draft);

        return draft;
    }

    /**
     * Doctor submits verification details and professional credentials.
     */
    public DoctorVerificationDTO submitVerification(String login, DoctorVerificationDTO input) {
        DoctorProfile doctor = doctorProfileRepository.findByUser_Login(login)
            .orElseThrow(() -> new BadRequestAlertException("Doctor profile not found for user: " + login, ENTITY_NAME, "doctornotfound"));

        if (input.getDoctorName() != null && !input.getDoctorName().isBlank()) {
            doctor.setFullName(input.getDoctorName());
        }
        if (input.getSpecialization() != null && !input.getSpecialization().isBlank()) {
            doctor.setTitle(input.getSpecialization());
        }
        if (input.getExperienceYears() != null) {
            doctor.setExperienceYears(input.getExperienceYears());
        }
        if (input.getNotes() != null) {
            doctor.setBio(input.getNotes());
        }
        doctorProfileRepository.save(doctor);

        Optional<DoctorVerification> existingOpt = doctorVerificationRepository.findTopByDoctorProfile_IdOrderByCreatedAtDesc(doctor.getId());

        DoctorVerification verification;
        if (existingOpt.isPresent() && ("DRAFT".equalsIgnoreCase(existingOpt.get().getStatus()) || "SUBMITTED".equalsIgnoreCase(existingOpt.get().getStatus()))) {
            // Update in-place
            verification = existingOpt.get();
            verification.setDoctorName(doctor.getFullName());
            verification.setSpecialization(doctor.getTitle());
            verification.setExperienceYears(doctor.getExperienceYears());
            if (input.getLicenseNumber() != null && !input.getLicenseNumber().isBlank()) {
                verification.setLicenseNumber(input.getLicenseNumber());
            }
            if (input.getEducation() != null && !input.getEducation().isBlank()) {
                verification.setEducation(input.getEducation());
            }
            if (input.getNotes() != null) {
                verification.setNotes(input.getNotes());
            }
            verification.setStatus("SUBMITTED");
            verification.setSubmittedAt(Instant.now());
            verification.setReviewedAt(null);
            verification.setReviewedBy(null);
            verification.setRejectionReason(null);

            if (input.getCertificateUrl() != null && !input.getCertificateUrl().isBlank()) {
                verification.setCertificateUrl(input.getCertificateUrl());
                verification.setCertificateFileName(input.getCertificateFileName());
                verification.setCertificatePublicId(input.getCertificatePublicId());
                verification.setCertificateContentType(input.getCertificateContentType());
                verification.setCertificateUploadedAt(input.getCertificateUploadedAt() != null ? input.getCertificateUploadedAt() : Instant.now());
            }
        } else {
            // Create a new verification request to maintain audit history (like C# project)
            DoctorVerification previous = existingOpt.orElse(null);
            verification = new DoctorVerification();
            verification.setDoctorProfile(doctor);
            verification.setDoctorName(doctor.getFullName());
            verification.setSpecialization(doctor.getTitle());
            verification.setExperienceYears(doctor.getExperienceYears());
            verification.setLicenseNumber(input.getLicenseNumber() != null ? input.getLicenseNumber() : (previous != null ? previous.getLicenseNumber() : "VN-MED-" + (100000 + doctor.getId())));
            verification.setEducation(input.getEducation() != null ? input.getEducation() : (previous != null ? previous.getEducation() : "Bác Sĩ Chuyên Khoa"));
            verification.setNotes(input.getNotes() != null ? input.getNotes() : doctor.getBio());
            verification.setStatus("SUBMITTED");
            verification.setSubmittedAt(Instant.now());

            String certUrl = input.getCertificateUrl() != null ? input.getCertificateUrl() : (previous != null ? previous.getCertificateUrl() : null);
            verification.setCertificateUrl(certUrl);
            verification.setCertificateFileName(input.getCertificateFileName() != null ? input.getCertificateFileName() : (previous != null ? previous.getCertificateFileName() : null));
            verification.setCertificatePublicId(input.getCertificatePublicId() != null ? input.getCertificatePublicId() : (previous != null ? previous.getCertificatePublicId() : null));
            verification.setCertificateContentType(input.getCertificateContentType() != null ? input.getCertificateContentType() : (previous != null ? previous.getCertificateContentType() : null));
            verification.setCertificateUploadedAt(certUrl != null ? (input.getCertificateUploadedAt() != null ? input.getCertificateUploadedAt() : Instant.now()) : null);
        }

        DoctorVerification saved = doctorVerificationRepository.save(verification);
        LOG.info("✅ [Doctor Verification Submitted] Doctor ID: {}, Request ID: {}, Status: SUBMITTED", doctor.getId(), saved.getId());
        return toDTO(saved);
    }

    /**
     * Record uploaded certificate directly to the doctor's verification record.
     */
    public void recordUploadedCertificate(Long doctorId, StoredFileMetadata metadata) {
        if (doctorId == null || metadata == null) return;

        DoctorProfile doctor = doctorProfileRepository.findById(doctorId).orElse(null);
        if (doctor == null) return;

        Optional<DoctorVerification> existingOpt = doctorVerificationRepository.findTopByDoctorProfile_IdOrderByCreatedAtDesc(doctorId);
        DoctorVerification verification;
        if (existingOpt.isPresent() && !("APPROVED".equalsIgnoreCase(existingOpt.get().getStatus()))) {
            verification = existingOpt.get();
        } else {
            verification = new DoctorVerification();
            verification.setDoctorProfile(doctor);
            verification.setDoctorName(doctor.getFullName());
            verification.setSpecialization(doctor.getTitle());
            verification.setExperienceYears(doctor.getExperienceYears());
            verification.setLicenseNumber("VN-MED-" + (100000 + doctor.getId()));
            verification.setStatus("DRAFT");
        }

        verification.setCertificateUrl(metadata.getFileDownloadUri());
        verification.setCertificateFileName(metadata.getOriginalFileName());
        verification.setCertificateContentType(metadata.getContentType());
        verification.setCertificateUploadedAt(Instant.now());

        doctorVerificationRepository.save(verification);
        LOG.info("📄 [Certificate Attached] Doctor ID: {}, File: {}", doctorId, metadata.getOriginalFileName());
    }

    /**
     * Admin: List all doctor verifications with status and search filters (paginated).
     */
    @Transactional(readOnly = true)
    public Page<DoctorVerificationDTO> getAllVerifications(String status, String search, Pageable pageable) {
        String cleanStatus = (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) ? status.trim().toUpperCase() : null;
        String cleanSearch = (search != null && !search.isBlank()) ? search.trim() : null;

        return doctorVerificationRepository.findAllFiltered(cleanStatus, cleanSearch, pageable)
            .map(this::toDTO);
    }

    /**
     * Admin: Get verification request details by ID.
     */
    @Transactional(readOnly = true)
    public DoctorVerificationDTO getVerificationById(Long id) {
        DoctorVerification verification = doctorVerificationRepository.findByIdWithProfile(id)
            .orElseThrow(() -> new BadRequestAlertException("Doctor verification request not found for id: " + id, ENTITY_NAME, "verificationnotfound"));
        return toDTO(verification);
    }

    /**
     * Admin: Review verification request (Approve, Reject, or Request Info) - Referencing C# OPCBS.
     */
    public DoctorVerificationDTO reviewVerification(Long id, ReviewVerificationDTO reviewDto, String adminLogin) {
        if (reviewDto == null) {
            throw new BadRequestAlertException("Review body cannot be null", ENTITY_NAME, "reviewemptypayload");
        }

        DoctorVerification verification = doctorVerificationRepository.findByIdWithProfile(id)
            .orElseThrow(() -> new BadRequestAlertException("Verification request not found: " + id, ENTITY_NAME, "verificationnotfound"));

        DoctorProfile doctor = verification.getDoctorProfile();
        String action = reviewDto.getAction() != null ? reviewDto.getAction().trim().toUpperCase() : "";

        if ("APPROVE".equals(action) || (Boolean.TRUE.equals(reviewDto.getApproved()) && action.isEmpty())) {
            verification.setStatus("APPROVED");
            verification.setReviewedAt(Instant.now());
            verification.setReviewedBy(adminLogin != null ? adminLogin : "admin");
            verification.setRejectionReason(null);

            if (doctor != null) {
                doctor.setIsActive(true);
                doctorProfileRepository.save(doctor);

                // Activate User account in Auth/Booking database
                if (doctor.getUser() != null) {
                    User doctorUser = doctor.getUser();
                    if (!doctorUser.isActivated()) {
                        doctorUser.setActivated(true);
                        userRepository.save(doctorUser);
                        LOG.info("🔓 [User Activated] Doctor account '{}' has been activated.", doctorUser.getLogin());
                    }
                }
            }
            LOG.info("✅ [Verification Approved] Request ID: {}, Doctor: {}, Reviewer: {}", id, verification.getDoctorName(), adminLogin);
        } else if ("REJECT".equals(action) || (Boolean.FALSE.equals(reviewDto.getApproved()) && !"REQUEST_INFO".equals(action) && !"REQUIRES_ADDITIONAL_INFO".equals(action))) {
            if (reviewDto.getRejectionReason() == null || reviewDto.getRejectionReason().isBlank()) {
                throw new BadRequestAlertException("Vui lòng nhập lý do từ chối hồ sơ.", ENTITY_NAME, "rejectionreasonrequired");
            }
            verification.setStatus("REJECTED");
            verification.setRejectionReason(reviewDto.getRejectionReason());
            verification.setReviewedAt(Instant.now());
            verification.setReviewedBy(adminLogin != null ? adminLogin : "admin");

            if (doctor != null) {
                doctor.setIsActive(false);
                doctorProfileRepository.save(doctor);
            }
            LOG.warn("❌ [Verification Rejected] Request ID: {}, Doctor: {}, Reason: {}", id, verification.getDoctorName(), reviewDto.getRejectionReason());
        } else if ("REQUEST_INFO".equals(action) || "REQUIRES_ADDITIONAL_INFO".equals(action) || "ADDITIONALINFO".equals(action)) {
            if (reviewDto.getRejectionReason() == null || reviewDto.getRejectionReason().isBlank()) {
                throw new BadRequestAlertException("Vui lòng nhập yêu cầu tài liệu/thông tin cần bác sĩ bổ sung.", ENTITY_NAME, "inforeasonrequired");
            }
            verification.setStatus("REQUIRES_ADDITIONAL_INFO");
            verification.setRejectionReason(reviewDto.getRejectionReason());
            verification.setReviewedAt(Instant.now());
            verification.setReviewedBy(adminLogin != null ? adminLogin : "admin");

            if (doctor != null) {
                doctor.setIsActive(false);
                doctorProfileRepository.save(doctor);
            }
            LOG.info("ℹ️ [Additional Info Requested] Request ID: {}, Doctor: {}, Notes: {}", id, verification.getDoctorName(), reviewDto.getRejectionReason());
        } else {
            throw new BadRequestAlertException("Hành động không hợp lệ: " + action + ". Chọn 'APPROVE', 'REJECT' hoặc 'REQUEST_INFO'.", ENTITY_NAME, "invalidaction");
        }

        DoctorVerification saved = doctorVerificationRepository.save(verification);
        return toDTO(saved);
    }

    /**
     * Admin: Get verification counts grouped by status.
     */
    @Transactional(readOnly = true)
    public DoctorVerificationSummaryDTO getVerificationSummary() {
        List<Object[]> statusCounts = doctorVerificationRepository.countByStatusGroup();

        long total = 0;
        long submitted = 0;
        long requiresAdditionalInfo = 0;
        long approved = 0;
        long rejected = 0;

        for (Object[] row : statusCounts) {
            String status = row[0] != null ? row[0].toString().toUpperCase() : "";
            long count = ((Number) row[1]).longValue();
            total += count;

            switch (status) {
                case "SUBMITTED", "PENDING" -> submitted += count;
                case "REQUIRES_ADDITIONAL_INFO", "ADDITIONAL_INFO" -> requiresAdditionalInfo += count;
                case "APPROVED" -> approved += count;
                case "REJECTED" -> rejected += count;
                default -> {}
            }
        }

        return new DoctorVerificationSummaryDTO(total, submitted, requiresAdditionalInfo, approved, rejected);
    }

    /**
     * Convert entity to DTO.
     */
    private DoctorVerificationDTO toDTO(DoctorVerification entity) {
        DoctorVerificationDTO dto = new DoctorVerificationDTO();
        dto.setId(entity.getId());
        if (entity.getDoctorProfile() != null) {
            dto.setDoctorProfileId(entity.getDoctorProfile().getId());
            dto.setDoctorId(entity.getDoctorProfile().getId());
        }
        dto.setDoctorName(entity.getDoctorName());
        dto.setLicenseNumber(entity.getLicenseNumber());
        dto.setSpecialization(entity.getSpecialization());
        dto.setExperienceYears(entity.getExperienceYears());
        dto.setEducation(entity.getEducation());
        dto.setStatus(entity.getStatus());
        dto.setNotes(entity.getNotes());
        dto.setCertificateUrl(entity.getCertificateUrl());
        dto.setCertificatePublicId(entity.getCertificatePublicId());
        dto.setCertificateFileName(entity.getCertificateFileName());
        dto.setCertificateContentType(entity.getCertificateContentType());
        dto.setCertificateUploadedAt(entity.getCertificateUploadedAt());
        dto.setSubmittedAt(entity.getSubmittedAt());
        dto.setReviewedAt(entity.getReviewedAt());
        dto.setReviewedBy(entity.getReviewedBy());
        dto.setReviewedByName(entity.getReviewedBy() != null ? entity.getReviewedBy() : null);
        dto.setRejectionReason(entity.getRejectionReason());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());

        return dto;
    }

    /**
     * Scans local certificate uploads directory as fallback.
     */
    private void tryScanLocalCertificate(Long doctorId, DoctorVerificationDTO dto) {
        try {
            Path uploadPath = Paths.get("./uploads/certificates");
            if (Files.exists(uploadPath)) {
                try (Stream<Path> stream = Files.list(uploadPath)) {
                    Optional<Path> latestFile = stream
                        .filter(p -> p.getFileName().toString().startsWith("doc_" + doctorId + "_"))
                        .max((p1, p2) -> {
                            try {
                                return Long.compare(Files.getLastModifiedTime(p1).toMillis(), Files.getLastModifiedTime(p2).toMillis());
                            } catch (Exception e) {
                                return 0;
                            }
                        });

                    if (latestFile.isPresent()) {
                        String fileName = latestFile.get().getFileName().toString();
                        dto.setCertificateFileName(fileName);
                        dto.setCertificateUrl("/api/doctor/certificates/" + fileName);
                        try {
                            dto.setCertificateUploadedAt(Files.getLastModifiedTime(latestFile.get()).toInstant());
                        } catch (Exception ignored) {}
                    }
                }
            }
        } catch (Exception ex) {
            LOG.warn("Error scanning certificate files: {}", ex.getMessage());
        }
    }
}
