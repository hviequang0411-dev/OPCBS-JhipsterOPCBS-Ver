package com.ocbs.service.dto;

import java.io.Serializable;

/**
 * Request payload for reviewing doctor verification application (Admin review).
 */
public class ReviewVerificationDTO implements Serializable {

    private String action; // "APPROVE", "REJECT", "REQUEST_INFO"
    private Boolean approved;
    private String rejectionReason;

    public ReviewVerificationDTO() {}

    public ReviewVerificationDTO(String action, Boolean approved, String rejectionReason) {
        this.action = action;
        this.approved = approved;
        this.rejectionReason = rejectionReason;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Boolean getApproved() {
        return approved;
    }

    public void setApproved(Boolean approved) {
        this.approved = approved;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }
}
