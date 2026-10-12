package com.ocbs.web.rest;

import com.ocbs.domain.DoctorProfile;
import com.ocbs.repository.DoctorProfileRepository;
import com.ocbs.security.SecurityUtils;
import com.ocbs.service.CloudinaryStorageService;
import com.ocbs.service.FileStorageService;
import com.ocbs.service.dto.StoredFileMetadata;
import com.ocbs.web.rest.errors.BadRequestAlertException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST controller for Doctor Document / Certificate Uploads (MultipartFile demo).
 */
@RestController
@RequestMapping("/api/doctor")
public class DoctorDocumentResource {

    private static final Logger LOG = LoggerFactory.getLogger(DoctorDocumentResource.class);
    private static final String ENTITY_NAME = "doctorDocument";

    private final FileStorageService fileStorageService;
    private final CloudinaryStorageService cloudinaryStorageService;
    private final DoctorProfileRepository doctorProfileRepository;

    public DoctorDocumentResource(
        FileStorageService fileStorageService,
        CloudinaryStorageService cloudinaryStorageService,
        DoctorProfileRepository doctorProfileRepository
    ) {
        this.fileStorageService = fileStorageService;
        this.cloudinaryStorageService = cloudinaryStorageService;
        this.doctorProfileRepository = doctorProfileRepository;
    }

    /**
     * {@code POST /doctor/upload-certificate} : Upload doctor license or medical diploma as MultipartFile.
     */
    @PostMapping("/upload-certificate")
    @PreAuthorize("hasAuthority('ROLE_DOCTOR') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<StoredFileMetadata> uploadDoctorCertificate(
        @RequestParam(value = "file", required = false) MultipartFile file,
        @RequestParam(name = "documentType", defaultValue = "MEDICAL_LICENSE") String documentType,
        @RequestParam(name = "storageTarget", defaultValue = "cloud") String storageTarget,
        jakarta.servlet.http.HttpServletRequest request
    ) {
        MultipartFile uploadFile = file;

        // Fallback: Tìm file đầu tiên nếu người dùng đặt tên key khác (vd: 'document', 'certificate', 'file')
        if (uploadFile == null && request instanceof org.springframework.web.multipart.MultipartHttpServletRequest multipartRequest) {
            java.util.Map<String, MultipartFile> fileMap = multipartRequest.getFileMap();
            LOG.info("🔍 [Multipart Debug] MultipartHttpServletRequest detected with keys: {}", fileMap.keySet());
            if (!fileMap.isEmpty()) {
                uploadFile = fileMap.values().iterator().next();
            }
        }

        if (uploadFile != null) {
            LOG.info("🔍 [Multipart Debug] UploadFile found: name='{}', originalName='{}', size={} bytes, isEmpty={}",
                uploadFile.getName(), uploadFile.getOriginalFilename(), uploadFile.getSize(), uploadFile.isEmpty());
        } else {
            LOG.warn("🔍 [Multipart Debug] No MultipartFile detected in request. Content-Type: {}", request.getContentType());
        }

        if (uploadFile == null || uploadFile.isEmpty()) {
            throw new BadRequestAlertException("Vui lòng đính kèm file trong form-data (Key: 'file')", ENTITY_NAME, "filerequired");
        }

        LOG.info("📤 [Multipart Upload] Processing file: '{}', Size: {} bytes, Type: '{}', Target: '{}'",
            uploadFile.getOriginalFilename(), uploadFile.getSize(), documentType, storageTarget);

        String login = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new BadRequestAlertException("User not authenticated", ENTITY_NAME, "unauthenticated"));

        DoctorProfile doctor = doctorProfileRepository.findByUser_Login(login)
            .orElseThrow(() -> new BadRequestAlertException("Doctor profile not found for user: " + login, ENTITY_NAME, "doctornotfound"));

        StoredFileMetadata metadata;

        // =========================================================================================
        // [HỌC TẬP & DEBUG] LỰA CHỌN 1 TRONG 2 PHƯƠNG THỨC LƯU TRỮ DƯỚI ĐÂY:
        // =========================================================================================
        if ("cloud".equalsIgnoreCase(storageTarget)) {
            // -------------------------------------------------------------------------------------
            // CÁCH 1: LƯU TRỰC TIẾP LÊN CLOUD (Cloudinary Cloud Storage)
            // Ưu điểm: Không tốn dung lượng máy chủ, có CDN tải nhanh toàn cầu, tự động nén ảnh.
            // -------------------------------------------------------------------------------------
            try {
                LOG.info("☁️ [Lưu trữ Cloud] Đang upload file lên Cloudinary folder 'opcbs/certificates'...");
                metadata = cloudinaryStorageService.uploadFileToCloud(uploadFile, doctor.getId(), documentType);
            } catch (Exception ex) {
                LOG.warn("⚠️ Cloudinary upload gặp sự cố: {}. Tự động fallback về lưu ổ đĩa cục bộ...", ex.getMessage());
                metadata = fileStorageService.storeFile(uploadFile, doctor.getId(), documentType);
            }
        } else {
            // -------------------------------------------------------------------------------------
            // CÁCH 2: LƯU TRỰC TIẾP TRÊN Ổ C: / LOCAL DISK (Mặc định khi đang học & debug)
            // Thư mục lưu: ./uploads/certificates/doc_{doctorId}_{docType}_{UUID}.{ext}
            // Ưu điểm khi học: Mở thư mục ổ cứng xem trực tiếp file vật lý, debug offline không cần internet.
            // -------------------------------------------------------------------------------------
            LOG.info("💾 [Lưu trữ Local Disk] Đang lưu file trực tiếp vào ổ đĩa thư mục './uploads/certificates'...");
            metadata = fileStorageService.storeFile(uploadFile, doctor.getId(), documentType);
        }

        return ResponseEntity.ok(metadata);
    }

    /**
     * {@code POST /doctor/upload-certificate-cloud} : Explicitly upload certificate to Cloudinary Cloud Storage.
     */
    @PostMapping("/upload-certificate-cloud")
    @PreAuthorize("hasAuthority('ROLE_DOCTOR') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<StoredFileMetadata> uploadDoctorCertificateCloud(
        @RequestParam(value = "file", required = false) MultipartFile file,
        @RequestParam(name = "documentType", defaultValue = "MEDICAL_LICENSE") String documentType,
        jakarta.servlet.http.HttpServletRequest request
    ) {
        return uploadDoctorCertificate(file, documentType, "cloud", request);
    }

    public static class Base64UploadVM {
        private String fileName;
        private String documentType;
        private String contentType;
        private String base64Content;

        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        public String getDocumentType() { return documentType; }
        public void setDocumentType(String documentType) { this.documentType = documentType; }
        public String getContentType() { return contentType; }
        public void setContentType(String contentType) { this.contentType = contentType; }
        public String getBase64Content() { return base64Content; }
        public void setBase64Content(String base64Content) { this.base64Content = base64Content; }
    }

    /**
     * {@code POST /doctor/upload-certificate-base64} : Upload doctor certificate via Base64 JSON payload.
     */
    @PostMapping("/upload-certificate-base64")
    @PreAuthorize("hasAuthority('ROLE_DOCTOR') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<StoredFileMetadata> uploadDoctorCertificateBase64(@RequestBody Base64UploadVM vm) {
        if (vm == null || vm.getBase64Content() == null || vm.getBase64Content().isBlank()) {
            throw new BadRequestAlertException("Dữ liệu base64Content không được để trống", ENTITY_NAME, "base64empty");
        }

        String login = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new BadRequestAlertException("User not authenticated", ENTITY_NAME, "unauthenticated"));

        DoctorProfile doctor = doctorProfileRepository.findByUser_Login(login)
            .orElseThrow(() -> new BadRequestAlertException("Doctor profile not found for user: " + login, ENTITY_NAME, "doctornotfound"));

        String base64Data = vm.getBase64Content();
        if (base64Data.contains(",")) {
            base64Data = base64Data.substring(base64Data.indexOf(",") + 1);
        }

        byte[] fileBytes = java.util.Base64.getDecoder().decode(base64Data);

        StoredFileMetadata metadata;
        try {
            metadata = cloudinaryStorageService.uploadBytesToCloud(
                fileBytes,
                vm.getFileName() != null ? vm.getFileName() : "certificate.pdf",
                vm.getContentType() != null ? vm.getContentType() : "application/pdf",
                doctor.getId(),
                vm.getDocumentType() != null ? vm.getDocumentType() : "MEDICAL_LICENSE"
            );
        } catch (Exception ex) {
            LOG.warn("⚠️ Cloudinary Base64 upload encountered issue: {}. Falling back to local storage...", ex.getMessage());
            metadata = fileStorageService.storeBytesFile(
                fileBytes,
                vm.getFileName() != null ? vm.getFileName() : "certificate.pdf",
                vm.getContentType() != null ? vm.getContentType() : "application/pdf",
                doctor.getId(),
                vm.getDocumentType() != null ? vm.getDocumentType() : "MEDICAL_LICENSE"
            );
        }

        return ResponseEntity.ok(metadata);
    }

    /**
     * {@code GET /doctor/verification} : Get doctor verification profile and certificate status.
     */
    @GetMapping("/verification")
    public ResponseEntity<com.ocbs.service.dto.DoctorVerificationDTO> getDoctorVerification() {
        String login = SecurityUtils.getCurrentUserLogin().orElse(null);
        DoctorProfile doctor = null;
        if (login != null) {
            doctor = doctorProfileRepository.findByUser_Login(login).orElse(null);
        }
        if (doctor == null) {
            doctor = doctorProfileRepository.findAll().stream().findFirst().orElse(null);
        }

        if (doctor == null) {
            throw new BadRequestAlertException("Doctor profile not found", ENTITY_NAME, "doctornotfound");
        }

        com.ocbs.service.dto.DoctorVerificationDTO dto = new com.ocbs.service.dto.DoctorVerificationDTO();
        dto.setDoctorId(doctor.getId());
        dto.setDoctorName(doctor.getFullName() != null ? doctor.getFullName() : "Dr. " + (login != null ? login : "Specialist"));
        dto.setSpecialization(doctor.getTitle() != null ? doctor.getTitle() : "Bác Sĩ Tâm Lý / Chuyên Gia Trị Liệu");
        dto.setLicenseNumber("VN-MED-" + (100000 + doctor.getId()));
        dto.setExperienceYears(doctor.getExperienceYears() != null ? doctor.getExperienceYears() : 5);
        dto.setEducation("Bác Sĩ Chuyên Khoa - Đại Học Y Hà Nội");
        dto.setSubmittedAt(java.time.Instant.now().minus(java.time.Duration.ofDays(2)));
        
        boolean isActive = doctor.getIsActive() != null && doctor.getIsActive();
        dto.setStatus(isActive ? "APPROVED" : "PENDING");
        dto.setNotes(doctor.getBio());

        // Kiểm tra xem đã có file certificate trong thư mục uploads chưa
        try {
            java.nio.file.Path uploadPath = java.nio.file.Paths.get("./uploads/certificates");
            if (Files.exists(uploadPath)) {
                try (java.util.stream.Stream<java.nio.file.Path> stream = Files.list(uploadPath)) {
                    Optional<java.nio.file.Path> latestFile = stream
                        .filter(p -> p.getFileName().toString().startsWith("doc_" + dto.getDoctorId() + "_"))
                        .max(java.util.Comparator.comparingLong(p -> {
                            try { return Files.getLastModifiedTime(p).toMillis(); } catch (Exception e) { return 0L; }
                        }));
                    
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

        if (dto.getCertificateUrl() == null && isActive) {
            dto.setStatus("APPROVED");
        } else if (dto.getCertificateUrl() == null && !isActive) {
            dto.setStatus("DRAFT");
        }

        return ResponseEntity.ok(dto);
    }

    /**
     * {@code POST /doctor/submit-verification} : Submit or update doctor practice credentials.
     */
    @PostMapping("/submit-verification")
    @PreAuthorize("hasAuthority('ROLE_DOCTOR') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<com.ocbs.service.dto.DoctorVerificationDTO> submitVerification(
        @RequestBody com.ocbs.service.dto.DoctorVerificationDTO input
    ) {
        String login = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new BadRequestAlertException("User not authenticated", ENTITY_NAME, "unauthenticated"));

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
        LOG.info("✅ Saved verification credentials for doctor id: {}", doctor.getId());

        return getDoctorVerification();
    }

    /**
     * {@code GET /doctor/certificates/:fileName} : Stream / Download doctor certificate resource.
     */
    @GetMapping("/certificates/{fileName:.+}")
    public ResponseEntity<Resource> getDoctorCertificate(@PathVariable("fileName") String fileName) {
        LOG.info("📥 [File Download / Stream] Request to access certificate file: '{}'", fileName);

        Resource resource = fileStorageService.loadFileAsResource(fileName);

        // Determine Content Type dynamically
        String contentType = "application/octet-stream";
        try {
            Path filePath = resource.getFile().toPath();
            String probedContentType = Files.probeContentType(filePath);
            if (probedContentType != null) {
                contentType = probedContentType;
            }
        } catch (IOException ex) {
            LOG.warn("Could not determine file type for '{}'", fileName);
        }

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(contentType))
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
            .body(resource);
    }

    /**
     * {@code POST /doctor/upload-avatar} : Upload user/doctor avatar to Cloudinary Cloud Storage.
     */
    @PostMapping("/upload-avatar")
    public ResponseEntity<java.util.Map<String, String>> uploadAvatar(
        @RequestParam(value = "file", required = false) MultipartFile file
    ) {
        LOG.info("☁️ [REST Upload Avatar] Request received to upload avatar image to Cloudinary");
        String currentLogin = SecurityUtils.getCurrentUserLogin().orElse("user");
        String cloudUrl = cloudinaryStorageService.uploadAvatarToCloud(file, currentLogin);
        return ResponseEntity.ok(java.util.Map.of("imageUrl", cloudUrl, "url", cloudUrl));
    }
}

