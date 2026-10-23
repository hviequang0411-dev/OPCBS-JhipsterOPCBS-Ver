package com.ocbs.service.dto;

import java.io.Serializable;
import java.time.Instant;

/**
 * DTO for Doctor Subscription Quota and Benefits status.
 */
public class DoctorSubscriptionQuotaDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private boolean hasActiveSubscription;
    private Long subscriptionId;
    private Long servicePackageId;
    private String servicePackageName;
    private String status;
    private Instant startDate;
    private Instant expirationDate;
    private Long daysRemaining;
    private Integer maxPatientCapacity;
    private Long currentPatientCount;
    private Long remainingPatientCapacity;
    private Integer maxDailySlotsCapacity;
    private String message;

    public DoctorSubscriptionQuotaDTO() {}

    public boolean isHasActiveSubscription() {
        return hasActiveSubscription;
    }

    public void setHasActiveSubscription(boolean hasActiveSubscription) {
        this.hasActiveSubscription = hasActiveSubscription;
    }

    public Long getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(Long subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public Long getServicePackageId() {
        return servicePackageId;
    }

    public void setServicePackageId(Long servicePackageId) {
        this.servicePackageId = servicePackageId;
    }

    public String getServicePackageName() {
        return servicePackageName;
    }

    public void setServicePackageName(String servicePackageName) {
        this.servicePackageName = servicePackageName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getStartDate() {
        return startDate;
    }

    public void setStartDate(Instant startDate) {
        this.startDate = startDate;
    }

    public Instant getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(Instant expirationDate) {
        this.expirationDate = expirationDate;
    }

    public Long getDaysRemaining() {
        return daysRemaining;
    }

    public void setDaysRemaining(Long daysRemaining) {
        this.daysRemaining = daysRemaining;
    }

    public Integer getMaxPatientCapacity() {
        return maxPatientCapacity;
    }

    public void setMaxPatientCapacity(Integer maxPatientCapacity) {
        this.maxPatientCapacity = maxPatientCapacity;
    }

    public Long getCurrentPatientCount() {
        return currentPatientCount;
    }

    public void setCurrentPatientCount(Long currentPatientCount) {
        this.currentPatientCount = currentPatientCount;
    }

    public Long getRemainingPatientCapacity() {
        return remainingPatientCapacity;
    }

    public void setRemainingPatientCapacity(Long remainingPatientCapacity) {
        this.remainingPatientCapacity = remainingPatientCapacity;
    }

    public Integer getMaxDailySlotsCapacity() {
        return maxDailySlotsCapacity;
    }

    public void setMaxDailySlotsCapacity(Integer maxDailySlotsCapacity) {
        this.maxDailySlotsCapacity = maxDailySlotsCapacity;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
