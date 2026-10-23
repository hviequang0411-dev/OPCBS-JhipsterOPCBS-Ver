package com.ocbs.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Standardized Domain Event for Payment and Doctor Subscription Lifecycle.
 * Contract Version: 1.0
 * Topics: opcbs-payment-events
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("eventId")
    private String eventId = UUID.randomUUID().toString();

    @JsonProperty("eventVersion")
    private String eventVersion = "1.0";

    @JsonProperty("eventType")
    private String eventType; // PAYMENT_COMPLETED, PAYMENT_FAILED

    @JsonProperty("occurredAt")
    private Instant occurredAt = Instant.now();

    @JsonProperty("aggregateId")
    private Long aggregateId;

    @JsonProperty("idempotencyKey")
    private String idempotencyKey;

    @JsonProperty("correlationId")
    private String correlationId;

    @JsonProperty("transactionCode")
    private String transactionCode;

    @JsonProperty("subscriptionId")
    private Long subscriptionId;

    @JsonProperty("doctorLogin")
    private String doctorLogin;

    @JsonProperty("servicePackageId")
    private Long servicePackageId;

    @JsonProperty("servicePackageName")
    private String servicePackageName;

    @JsonProperty("amount")
    private BigDecimal amount;

    @JsonProperty("paymentStatus")
    private String paymentStatus; // SUCCESS, FAILED

    @JsonProperty("paidAt")
    private Instant paidAt;

    @JsonProperty("expirationDate")
    private Instant expirationDate;

    @JsonProperty("durationDays")
    private Integer durationDays;

    public PaymentEvent() {}

    public PaymentEvent(
        String transactionCode,
        Long subscriptionId,
        String doctorLogin,
        Long servicePackageId,
        String servicePackageName,
        BigDecimal amount,
        String paymentStatus,
        Instant paidAt,
        Instant expirationDate,
        Integer durationDays
    ) {
        this.eventId = UUID.randomUUID().toString();
        this.eventVersion = "1.0";
        this.eventType = "SUCCESS".equalsIgnoreCase(paymentStatus) ? "PAYMENT_COMPLETED" : "PAYMENT_FAILED";
        this.occurredAt = Instant.now();
        this.transactionCode = transactionCode;
        this.subscriptionId = subscriptionId;
        this.aggregateId = subscriptionId;
        this.doctorLogin = doctorLogin;
        this.servicePackageId = servicePackageId;
        this.servicePackageName = servicePackageName;
        this.amount = amount;
        this.paymentStatus = paymentStatus;
        this.paidAt = paidAt;
        this.expirationDate = expirationDate;
        this.durationDays = durationDays;
        this.idempotencyKey = "PAY_" + transactionCode + "_" + this.eventType;
        this.correlationId = "CORR_PAY_" + transactionCode;
    }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getEventVersion() { return eventVersion; }
    public void setEventVersion(String eventVersion) { this.eventVersion = eventVersion; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; }

    public Long getAggregateId() { return aggregateId != null ? aggregateId : subscriptionId; }
    public void setAggregateId(Long aggregateId) {
        this.aggregateId = aggregateId;
        if (this.subscriptionId == null) {
            this.subscriptionId = aggregateId;
        }
    }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public String getCorrelationId() { return correlationId != null ? correlationId : eventId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }

    public String getTransactionCode() { return transactionCode; }
    public void setTransactionCode(String transactionCode) { this.transactionCode = transactionCode; }

    public Long getSubscriptionId() { return subscriptionId; }
    public void setSubscriptionId(Long subscriptionId) {
        this.subscriptionId = subscriptionId;
        if (this.aggregateId == null) {
            this.aggregateId = subscriptionId;
        }
    }

    public String getDoctorLogin() { return doctorLogin; }
    public void setDoctorLogin(String doctorLogin) { this.doctorLogin = doctorLogin; }

    public Long getServicePackageId() { return servicePackageId; }
    public void setServicePackageId(Long servicePackageId) { this.servicePackageId = servicePackageId; }

    public String getServicePackageName() { return servicePackageName; }
    public void setServicePackageName(String servicePackageName) { this.servicePackageName = servicePackageName; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public Instant getPaidAt() { return paidAt; }
    public void setPaidAt(Instant paidAt) { this.paidAt = paidAt; }

    public Instant getExpirationDate() { return expirationDate; }
    public void setExpirationDate(Instant expirationDate) { this.expirationDate = expirationDate; }

    public Integer getDurationDays() { return durationDays; }
    public void setDurationDays(Integer durationDays) { this.durationDays = durationDays; }
}
