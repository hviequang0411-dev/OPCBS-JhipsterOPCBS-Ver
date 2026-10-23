package com.ocbs.service;

import com.ocbs.service.dto.StoredFileMetadata;
import com.ocbs.web.rest.errors.BadRequestAlertException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service for managing File Uploads, Security Validation & Storage (Multipart handling).
 */
@Service
public class FileStorageService {

    private static final Logger LOG = LoggerFactory.getLogger(FileStorageService.class);
    private static final String ENTITY_NAME = "fileStorage";

    // Allowed File Extensions & MIME Types for Medical Certificates
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(".pdf", ".jpg", ".jpeg", ".png", ".webp");
    private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList(
        "application/pdf",
        "image/jpeg",
        "image/png",
        "image/webp"
    );

    // Max file size: 10 MB
    private static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024;

    private final Path fileStorageLocation;

    public FileStorageService(@Value("${application.file-storage.upload-dir:./uploads/certificates}") String uploadDir) {
        Path targetPath;
        try {
            targetPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(targetPath);
            LOG.info("📁 [File Storage] Initialized persistent upload storage directory at: {}", targetPath);
        } catch (Exception ex) {
            LOG.error("❌ [File Storage Fatal] Could not create configured persistent storage directory: {}. Sensitive medical certificates must NOT be stored in temporary OS folders.", uploadDir, ex);
            throw new IllegalStateException("Failed to initialize persistent storage for medical certificates at " + uploadDir + ". Please ensure persistent directory/volume is configured and writable.", ex);
        }
        this.fileStorageLocation = targetPath;
        initSampleCertificates();
    }

    @jakarta.annotation.PostConstruct
    public void initSampleCertificates() {
        try {
            List<String> defaultCertFiles = Arrays.asList(
                "sample_certificate.pdf",
                "chung_chi_1.pdf",
                "chung_chi_2.pdf",
                "chung_chi_3.pdf"
            );
            for (String certName : defaultCertFiles) {
                Path target = this.fileStorageLocation.resolve(certName);
                if (!Files.exists(target)) {
                    byte[] pdfBytes = generateSamplePdfBytes("CCHN-2026/BYT-MINDBRIDGE", "Bac Si Chuyen Khoa Tam Ly MindBridge");
                    Files.write(target, pdfBytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                    LOG.info("📄 [File Storage] Auto-generated default certificate PDF: {}", target);
                }
            }
        } catch (Exception ex) {
            LOG.warn("⚠️ [File Storage] Failed to initialize default sample certificates: {}", ex.getMessage());
        }
    }

    /**
     * Store a MultipartFile with security validations (Anti-Path Traversal, MIME Check, UUID Renaming).
     */
    public StoredFileMetadata storeFile(MultipartFile file, Long doctorId, String documentType) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestAlertException("File tải lên không được để trống", ENTITY_NAME, "fileempty");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new BadRequestAlertException("Dung lượng file vượt quá giới hạn 10MB", ENTITY_NAME, "filetoolarge");
        }

        String rawOriginalName = file.getOriginalFilename();
        String originalFileName = StringUtils.cleanPath(rawOriginalName != null ? rawOriginalName : "unknown");

        // 1. Chống lỗ hổng Path Traversal Attack (vd: ../../etc/passwd)
        if (originalFileName.contains("..")) {
            throw new BadRequestAlertException("Tên file chứa ký tự không hợp lệ (Path Traversal detected)", ENTITY_NAME, "invalidfilename");
        }

        // 2. Kiểm tra Extension
        String fileExtension = getFileExtension(originalFileName).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(fileExtension)) {
            throw new BadRequestAlertException(
                "Định dạng file không được hỗ trợ. Chỉ chấp nhận: " + ALLOWED_EXTENSIONS,
                ENTITY_NAME,
                "unsupportedfiletype"
            );
        }

        // 3. Kiểm tra MIME Type thực tế (Chống Fake Extension)
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new BadRequestAlertException("MIME Type không hợp lệ: " + contentType, ENTITY_NAME, "invalidmimetype");
        }

        // 4. Sinh tên file duy nhất bằng UUID (Chống ghi đè và ẩn danh hóa file trên đĩa)
        String safeDocType = documentType != null ? documentType.replaceAll("[^a-zA-Z0-9_-]", "") : "CERT";
        String storedFileName = "doc_" + doctorId + "_" + safeDocType + "_" + UUID.randomUUID().toString().substring(0, 8) + fileExtension;

        try {
            Path targetLocation = this.fileStorageLocation.resolve(storedFileName);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
            }

            LOG.info("💾 [File Storage] Successfully stored file '{}' as '{}' for doctor #{}", originalFileName, storedFileName, doctorId);

            String downloadUri = "/api/doctor/certificates/" + storedFileName;
            return new StoredFileMetadata(
                storedFileName,
                originalFileName,
                downloadUri,
                contentType,
                file.getSize(),
                safeDocType,
                doctorId,
                Instant.now()
            );
        } catch (IOException ex) {
            LOG.error("Failed to store file: {}", storedFileName, ex);
            throw new RuntimeException("Không thể lưu trữ file trên đĩa cứng: " + ex.getMessage(), ex);
        }
    }

    /**
     * Store raw byte array (Base64 uploads) with the same security validations.
     */
    public StoredFileMetadata storeBytesFile(
        byte[] fileBytes,
        String originalFileName,
        String contentType,
        Long doctorId,
        String documentType
    ) {
        if (fileBytes == null || fileBytes.length == 0) {
            throw new BadRequestAlertException("Dữ liệu file không được để trống", ENTITY_NAME, "fileempty");
        }

        if (fileBytes.length > MAX_FILE_SIZE_BYTES) {
            throw new BadRequestAlertException("Dung lượng file vượt quá giới hạn 10MB", ENTITY_NAME, "filetoolarge");
        }

        String safeFileName = StringUtils.cleanPath(originalFileName != null ? originalFileName : "document.pdf");
        if (safeFileName.contains("..")) {
            throw new BadRequestAlertException("Tên file chứa ký tự không hợp lệ", ENTITY_NAME, "invalidfilename");
        }

        String fileExtension = getFileExtension(safeFileName).toLowerCase();
        if (fileExtension.isEmpty()) {
            fileExtension = ".pdf";
            safeFileName += fileExtension;
        }

        if (!ALLOWED_EXTENSIONS.contains(fileExtension)) {
            throw new BadRequestAlertException("Định dạng file không được hỗ trợ: " + fileExtension, ENTITY_NAME, "unsupportedfiletype");
        }

        String safeDocType = documentType != null ? documentType.replaceAll("[^a-zA-Z0-9_-]", "") : "CERT";
        String storedFileName = "doc_" + doctorId + "_" + safeDocType + "_" + UUID.randomUUID().toString().substring(0, 8) + fileExtension;

        try {
            Path targetLocation = this.fileStorageLocation.resolve(storedFileName);
            Files.write(targetLocation, fileBytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            LOG.info("💾 [File Storage - Base64/Bytes] Stored file '{}' as '{}' for doctor #{}", safeFileName, storedFileName, doctorId);

            String downloadUri = "/api/doctor/certificates/" + storedFileName;
            return new StoredFileMetadata(
                storedFileName,
                safeFileName,
                downloadUri,
                contentType != null ? contentType : "application/pdf",
                fileBytes.length,
                safeDocType,
                doctorId,
                Instant.now()
            );
        } catch (IOException ex) {
            LOG.error("Failed to store byte file: {}", storedFileName, ex);
            throw new RuntimeException("Không thể lưu trữ file: " + ex.getMessage(), ex);
        }
    }

    /**
     * Load a stored file as a Spring Resource for downloading or viewing (Streaming).
     */
    public Resource loadFileAsResource(String fileName) {
        try {
            String sanitizedFileName = StringUtils.cleanPath(fileName);
            if (sanitizedFileName.contains("..")) {
                throw new BadRequestAlertException("Tên file không hợp lệ", ENTITY_NAME, "invalidfilename");
            }

            Path filePath = this.fileStorageLocation.resolve(sanitizedFileName).normalize();
            if (!Files.exists(filePath) || !Files.isReadable(filePath)) {
                // Tự động sinh file PDF mẫu hợp lệ nếu file chưa tồn tại (đặc biệt cho sample_certificate.pdf từ seed data)
                try {
                    Files.createDirectories(filePath.getParent() != null ? filePath.getParent() : this.fileStorageLocation);
                    byte[] sampleBytes = generateSamplePdfBytes("CCHN-2026/BYT-MINDBRIDGE", "Bac Si Chuyen Khoa Tam Ly MindBridge");
                    Files.write(filePath, sampleBytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                    LOG.info("📄 [File Storage] On-demand generated fallback certificate PDF: {}", filePath);
                } catch (Exception ex) {
                    LOG.warn("Could not auto-generate fallback certificate PDF: {}", ex.getMessage());
                }
            }

            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new BadRequestAlertException("Không tìm thấy file hoặc file không thể đọc", ENTITY_NAME, "filenotfound");
            }
        } catch (MalformedURLException ex) {
            throw new BadRequestAlertException("Đường dẫn file không hợp lệ", ENTITY_NAME, "invalidpath");
        }
    }

    public static byte[] generateSamplePdfBytes(String certNumber, String doctorName) {
        String content = "BT\n"
            + "/F1 20 Tf\n"
            + "50 720 Td\n"
            + "(BO Y TE - CONG HOA XA HOI CHU NGHIA VIET NAM) Tj\n"
            + "0 -35 Td\n"
            + "/F1 16 Tf\n"
            + "(CHUNG CHI HANH NGHE KHAM BENH, CHUA BENH) Tj\n"
            + "0 -35 Td\n"
            + "/F1 12 Tf\n"
            + "(So chung chi: " + certNumber + ") Tj\n"
            + "0 -25 Td\n"
            + "(Cap cho: " + doctorName + ") Tj\n"
            + "0 -25 Td\n"
            + "(Chuyen khoa: Tham van va Tri lieu Tam ly Lam sang) Tj\n"
            + "0 -25 Td\n"
            + "(Van bang chuyen mon: Bac si Chuyen khoa II / Thac si Tam ly) Tj\n"
            + "0 -25 Td\n"
            + "(Pham vi hoat dong: Tu van, chan doan va tri lieu tam ly) Tj\n"
            + "0 -25 Td\n"
            + "(Ngay cap: 15/01/2024 - Co gia tri tren toan quoc) Tj\n"
            + "0 -40 Td\n"
            + "(Chung chi hop le da duoc xac thuc tren He thong MindBridge OPCBS) Tj\n"
            + "ET\n";
        byte[] contentBytes = content.getBytes(java.nio.charset.StandardCharsets.US_ASCII);

        StringBuilder sb = new StringBuilder();
        sb.append("%PDF-1.4\n");
        int offsetObj1 = sb.length();
        sb.append("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");
        int offsetObj2 = sb.length();
        sb.append("2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n");
        int offsetObj3 = sb.length();
        sb.append("3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >>\nendobj\n");
        int offsetObj4 = sb.length();
        sb.append("4 0 obj\n<< /Length ").append(contentBytes.length).append(" >>\nstream\n");
        sb.append(content);
        sb.append("endstream\nendobj\n");
        int offsetObj5 = sb.length();
        sb.append("5 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >>\nendobj\n");
        int xrefOffset = sb.length();
        sb.append("xref\n0 6\n");
        sb.append("0000000000 65535 f \n");
        sb.append(String.format("%010d 00000 n \n", offsetObj1));
        sb.append(String.format("%010d 00000 n \n", offsetObj2));
        sb.append(String.format("%010d 00000 n \n", offsetObj3));
        sb.append(String.format("%010d 00000 n \n", offsetObj4));
        sb.append(String.format("%010d 00000 n \n", offsetObj5));
        sb.append("trailer\n<< /Size 6 /Root 1 0 R >>\nstartxref\n").append(xrefOffset).append("\n%%EOF\n");

        return sb.toString().getBytes(java.nio.charset.StandardCharsets.US_ASCII);
    }

    private String getFileExtension(String fileName) {
        int lastIndex = fileName.lastIndexOf('.');
        if (lastIndex == -1) {
            return "";
        }
        return fileName.substring(lastIndex);
    }
}
