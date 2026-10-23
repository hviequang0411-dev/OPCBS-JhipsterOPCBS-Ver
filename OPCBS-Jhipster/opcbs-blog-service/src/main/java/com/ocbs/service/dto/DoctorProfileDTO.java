package com.ocbs.service.dto;

import com.ocbs.domain.enumeration.Gender;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Lob;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A DTO for the {@link com.ocbs.domain.DoctorProfile} entity.
 */
@Schema(description = "Hồ sơ bác sĩ (mở rộng từ JHipster User)")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class DoctorProfileDTO implements Serializable {

    private Long id;

    @NotNull
    @Size(max = 160)
    private String fullName;

    @Size(max = 100)
    private String title;

    @Lob
    private String bio;

    @Min(value = 0)
    private Integer experienceYears;

    @DecimalMin(value = "0")
    private BigDecimal consultationFee;

    @Size(max = 30)
    private String phone;

    private Gender gender;

    @NotNull
    private Boolean isActive;

    @NotNull
    private UserDTO user;

    private Set<SpecializationDTO> specializationses = new HashSet<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public UserDTO getUser() {
        return user;
    }

    public void setUser(UserDTO user) {
        this.user = user;
    }

    public Set<SpecializationDTO> getSpecializationses() {
        return specializationses;
    }

    public void setSpecializationses(Set<SpecializationDTO> specializationses) {
        this.specializationses = specializationses;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DoctorProfileDTO)) {
            return false;
        }

        DoctorProfileDTO doctorProfileDTO = (DoctorProfileDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, doctorProfileDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "DoctorProfileDTO{" +
            "id=" + getId() +
            ", fullName='" + getFullName() + "'" +
            ", title='" + getTitle() + "'" +
            ", bio='" + getBio() + "'" +
            ", experienceYears=" + getExperienceYears() +
            ", consultationFee=" + getConsultationFee() +
            ", phone='" + getPhone() + "'" +
            ", gender='" + getGender() + "'" +
            ", isActive='" + getIsActive() + "'" +
            ", user=" + getUser() +
            ", specializationses=" + getSpecializationses() +
            "}";
    }
}
