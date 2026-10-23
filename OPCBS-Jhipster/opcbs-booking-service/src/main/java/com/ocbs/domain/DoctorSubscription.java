package com.ocbs.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Doctor Subscription entity in Booking Service (Read-Only reference to shared DB).
 */
@Entity
@Table(name = "doctor_subscription")
public class DoctorSubscription extends AbstractAuditingEntity<Long> implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "doctor_id")
    private Long doctorId;

    @NotNull
    @Column(name = "doctor_login", nullable = false, length = 100)
    private String doctorLogin;

    @Column(name = "service_package_id")
    private Long servicePackageId;

    @Column(name = "service_package_name", length = 150)
    private String servicePackageName;

    @NotNull
    @Column(name = "status", nullable = false, length = 50)
    private String status = "PENDING_PAYMENT"; // PENDING_PAYMENT, ACTIVE, EXPIRED, CANCELLED

    @Column(name = "start_date")
    private Instant startDate;

    @Column(name = "expiration_date")
    private Instant expirationDate;

    @Column(name = "price", precision = 21, scale = 2)
    private BigDecimal price;

    @Column(name = "transaction_code", length = 100)
    private String transactionCode;

    public DoctorSubscription() {}

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(Long doctorId) {
        this.doctorId = doctorId;
    }

    public String getDoctorLogin() {
        return doctorLogin;
    }

    public void setDoctorLogin(String doctorLogin) {
        this.doctorLogin = doctorLogin;
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

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getTransactionCode() {
        return transactionCode;
    }

    public void setTransactionCode(String transactionCode) {
        this.transactionCode = transactionCode;
    }
}
