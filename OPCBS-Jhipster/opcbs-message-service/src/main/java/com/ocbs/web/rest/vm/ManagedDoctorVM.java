package com.ocbs.web.rest.vm;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

/**
 * View Model for registering a Doctor account with professional details.
 */
public class ManagedDoctorVM implements Serializable {

    @NotBlank
    @Size(min = 1, max = 50)
    private String login;

    @NotBlank
    @Email
    @Size(min = 5, max = 254)
    private String email;

    @NotBlank
    @Size(min = 4, max = 100)
    private String password;

    @NotBlank
    @Size(max = 160)
    private String fullName;

    @Size(max = 30)
    private String phone;

    @Size(max = 100)
    private String title;

    private String bio;

    @Min(0)
    private Integer experienceYears;

    @DecimalMin("0")
    private BigDecimal consultationFee;

    private Set<Long> specializationIds = new HashSet<>();

    public ManagedDoctorVM() {}

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public Integer getExperienceYears() {
        return experienceYears;
    }

    public void setExperienceYears(Integer experienceYears) {
        this.experienceYears = experienceYears;
    }

    public BigDecimal getConsultationFee() {
        return consultationFee;
    }

    public void setConsultationFee(BigDecimal consultationFee) {
        this.consultationFee = consultationFee;
    }

    public Set<Long> getSpecializationIds() {
        return specializationIds;
    }

    public void setSpecializationIds(Set<Long> specializationIds) {
        this.specializationIds = specializationIds;
    }
}
