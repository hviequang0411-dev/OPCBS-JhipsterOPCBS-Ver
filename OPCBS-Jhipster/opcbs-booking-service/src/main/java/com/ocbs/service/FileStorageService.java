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
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
            LOG.info("📁 [File Storage] Initialized upload storage directory at: {}", this.fileStorageLocation);
        } catch (Exception ex) {
            LOG.error("Could not create upload directory: {}", this.fileStorageLocation, ex);
            throw new RuntimeException("Could not create upload directory: " + uploadDir, ex);
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

    private String getFileExtension(String fileName) {
        int lastIndex = fileName.lastIndexOf('.');
        if (lastIndex == -1) {
            return "";
        }
        return fileName.substring(lastIndex);
    }
}
