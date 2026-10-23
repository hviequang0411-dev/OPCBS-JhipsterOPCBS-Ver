package com.ocbs.service.dto;

import java.io.Serializable;

/**
 * Summary metrics for Doctor Verification Queue (Admin overview).
 */
public class DoctorVerificationSummaryDTO implements Serializable {

    private long total;
    private long submitted;
    private long requiresAdditionalInfo;
    private long approved;
    private long rejected;

    public DoctorVerificationSummaryDTO() {}

    public DoctorVerificationSummaryDTO(long total, long submitted, long requiresAdditionalInfo, long approved, long rejected) {
        this.total = total;
        this.submitted = submitted;
        this.requiresAdditionalInfo = requiresAdditionalInfo;
        this.approved = approved;
        this.rejected = rejected;
    }

    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }

    public long getSubmitted() { return submitted; }
    public void setSubmitted(long submitted) { this.submitted = submitted; }

    public long getRequiresAdditionalInfo() { return requiresAdditionalInfo; }
    public void setRequiresAdditionalInfo(long requiresAdditionalInfo) { this.requiresAdditionalInfo = requiresAdditionalInfo; }

    public long getApproved() { return approved; }
    public void setApproved(long approved) { this.approved = approved; }

    public long getRejected() { return rejected; }
    public void setRejected(long rejected) { this.rejected = rejected; }
}
