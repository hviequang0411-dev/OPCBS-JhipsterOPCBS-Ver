package com.ocbs.service.dto;

import java.io.Serializable;
import java.time.Instant;

/**
 * DTO for storing file upload metadata (Multipart response).
 */
public class StoredFileMetadata implements Serializable {

    private String fileName;
    private String originalFileName;
    private String fileDownloadUri;
    private String contentType;
    private long sizeBytes;
    private String documentType;
    private Long doctorId;
    private Instant uploadedAt;

    public StoredFileMetadata() {}

    public StoredFileMetadata(
        String fileName,
        String originalFileName,
        String fileDownloadUri,
        String contentType,
        long sizeBytes,
        String documentType,
        Long doctorId,
        Instant uploadedAt
    ) {
        this.fileName = fileName;
        this.originalFileName = originalFileName;
        this.fileDownloadUri = fileDownloadUri;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.documentType = documentType;
        this.doctorId = doctorId;
        this.uploadedAt = uploadedAt;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }

    public String getFileDownloadUri() {
        return fileDownloadUri;
    }

    public void setFileDownloadUri(String fileDownloadUri) {
        this.fileDownloadUri = fileDownloadUri;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public Long getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(Long doctorId) {
        this.doctorId = doctorId;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(Instant uploadedAt) {
        this.uploadedAt = uploadedAt;
    }
}
