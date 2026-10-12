package com.ocbs.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.ocbs.service.dto.StoredFileMetadata;
import com.ocbs.web.rest.errors.BadRequestAlertException;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service for uploading files and doctor certificates to Cloudinary Cloud Storage.
 */
@Service
public class CloudinaryStorageService {

    private static final Logger LOG = LoggerFactory.getLogger(CloudinaryStorageService.class);
    private static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024; // 10MB
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "jpg", "jpeg", "png", "webp");

    private final Cloudinary cloudinary;

    public CloudinaryStorageService(
        @Value("${application.cloudinary.cloud-name:jqhp5g23}") String cloudName,
        @Value("${application.cloudinary.api-key:136888838371782}") String apiKey,
        @Value("${application.cloudinary.api-secret:utJ16iRSPhJE3xaHVsDDmvExZ9E}") String apiSecret
    ) {
        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
            "cloud_name", cloudName,
            "api_key", apiKey,
            "api_secret", apiSecret,
            "secure", true
        ));
        LOG.info("☁️ [Cloudinary Storage] Initialized Cloudinary client with Cloud Name: '{}'", cloudName);
    }

    /**
     * Upload MultipartFile to Cloudinary Cloud Storage.
     */
    public StoredFileMetadata uploadFileToCloud(MultipartFile file, Long doctorId, String documentType) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestAlertException("File upload không được để trống", "cloudinaryStorage", "filenotfound");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new BadRequestAlertException("Dung lượng file vượt quá giới hạn 10MB", "cloudinaryStorage", "filesizeexceeded");
        }

        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "document.pdf";
        String extension = getFileExtension(originalFilename).toLowerCase();

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BadRequestAlertException("Định dạng file không được hỗ trợ (" + extension + ")", "cloudinaryStorage", "invalidfileextension");
        }

        String publicId = "doc_" + (doctorId != null ? doctorId : 0) + "_" + (documentType != null ? documentType : "CERTIFICATE") + "_" + UUID.randomUUID();

        try {
            LOG.info("☁️ [Cloudinary Upload] Uploading '{}' ({} bytes) to Cloudinary folder 'opcbs/certificates'...",
                originalFilename, file.getSize());

            Map<?, ?> uploadParams = ObjectUtils.asMap(
                "public_id", publicId,
                "folder", "opcbs/certificates",
                "resource_type", "auto",
                "overwrite", true
            );

            Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), uploadParams);
            String secureUrl = (String) result.get("secure_url");
            String format = (String) result.get("format");
            Number bytes = (Number) result.get("bytes");

            LOG.info("✅ [Cloudinary Upload SUCCESS] Cloud URL: {}", secureUrl);

            return new StoredFileMetadata(
                publicId + (format != null ? "." + format : ""),
                originalFilename,
                secureUrl,
                file.getContentType() != null ? file.getContentType() : "application/octet-stream",
                bytes != null ? bytes.longValue() : file.getSize(),
                documentType,
                doctorId,
                Instant.now()
            );
        } catch (IOException ex) {
            LOG.error("❌ [Cloudinary Upload ERROR] Failed to upload file to Cloudinary: {}", ex.getMessage(), ex);
            throw new BadRequestAlertException("Lỗi trong quá trình upload file lên Cloudinary: " + ex.getMessage(), "cloudinaryStorage", "uploaderror");
        }
    }

    /**
     * Upload byte array directly to Cloudinary Cloud Storage.
     */
    public StoredFileMetadata uploadBytesToCloud(
        byte[] fileBytes,
        String originalFileName,
        String contentType,
        Long doctorId,
        String documentType
    ) {
        if (fileBytes == null || fileBytes.length == 0) {
            throw new BadRequestAlertException("Dữ liệu byte không được để trống", "cloudinaryStorage", "emptybytes");
        }

        if (fileBytes.length > MAX_FILE_SIZE_BYTES) {
            throw new BadRequestAlertException("Dung lượng file vượt quá giới hạn 10MB", "cloudinaryStorage", "filesizeexceeded");
        }

        String publicId = "doc_" + (doctorId != null ? doctorId : 0) + "_" + (documentType != null ? documentType : "CERTIFICATE") + "_" + UUID.randomUUID();

        try {
            LOG.info("☁️ [Cloudinary Upload] Uploading byte payload ({} bytes) to Cloudinary...", fileBytes.length);

            Map<?, ?> uploadParams = ObjectUtils.asMap(
                "public_id", publicId,
                "folder", "opcbs/certificates",
                "resource_type", "auto",
                "overwrite", true
            );

            Map<?, ?> result = cloudinary.uploader().upload(fileBytes, uploadParams);
            String secureUrl = (String) result.get("secure_url");
            String format = (String) result.get("format");
            Number bytes = (Number) result.get("bytes");

            LOG.info("✅ [Cloudinary Upload SUCCESS] Cloud URL: {}", secureUrl);

            return new StoredFileMetadata(
                publicId + (format != null ? "." + format : ""),
                originalFileName != null ? originalFileName : "certificate.pdf",
                secureUrl,
                contentType != null ? contentType : "application/pdf",
                bytes != null ? bytes.longValue() : fileBytes.length,
                documentType,
                doctorId,
                Instant.now()
            );
        } catch (IOException ex) {
            LOG.error("❌ [Cloudinary Upload ERROR] Failed to upload byte array to Cloudinary: {}", ex.getMessage(), ex);
            throw new BadRequestAlertException("Lỗi upload lên Cloudinary: " + ex.getMessage(), "cloudinaryStorage", "uploaderror");
        }
    }

    /**
     * Upload Avatar image directly to Cloudinary Cloud Storage folder 'opcbs/avatars'.
     */
    public String uploadAvatarToCloud(MultipartFile file, String userLogin) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestAlertException("File ảnh đại diện không được để trống", "cloudinaryStorage", "filenotfound");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new BadRequestAlertException("Dung lượng file ảnh vượt quá giới hạn 10MB", "cloudinaryStorage", "filesizeexceeded");
        }

        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "avatar.jpg";
        String extension = getFileExtension(originalFilename).toLowerCase();

        if (!Set.of("jpg", "jpeg", "png", "webp", "gif").contains(extension)) {
            throw new BadRequestAlertException("Định dạng file ảnh không được hỗ trợ (" + extension + ")", "cloudinaryStorage", "invalidfileextension");
        }

        String publicId = "avatar_" + (userLogin != null ? userLogin : "user") + "_" + UUID.randomUUID().toString().substring(0, 8);

        try {
            LOG.info("☁️ [Cloudinary Avatar Upload] Uploading avatar for '{}' ({} bytes) to folder 'opcbs/avatars'...",
                userLogin, file.getSize());

            Map<?, ?> uploadParams = ObjectUtils.asMap(
                "public_id", publicId,
                "folder", "opcbs/avatars",
                "resource_type", "image",
                "overwrite", true
            );

            Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), uploadParams);
            String secureUrl = (String) result.get("secure_url");

            LOG.info("✅ [Cloudinary Avatar SUCCESS] Cloud URL: {}", secureUrl);
            return secureUrl;
        } catch (IOException ex) {
            LOG.error("❌ [Cloudinary Avatar ERROR] Failed to upload avatar: {}", ex.getMessage(), ex);
            throw new BadRequestAlertException("Lỗi upload ảnh lên Cloudinary: " + ex.getMessage(), "cloudinaryStorage", "uploaderror");
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }
}

