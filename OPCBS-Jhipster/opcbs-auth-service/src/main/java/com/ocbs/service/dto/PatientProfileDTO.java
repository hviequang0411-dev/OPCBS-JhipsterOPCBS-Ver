package com.ocbs.service.dto;

import com.ocbs.domain.enumeration.Gender;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

/**
 * A DTO for the {@link com.ocbs.domain.PatientProfile} entity.
 */
@Schema(description = "Hồ sơ bệnh nhân (mở rộng từ JHipster User)")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class PatientProfileDTO implements Serializable {

    private Long id;

    @NotNull
    @Size(max = 160)
    private String fullName;

    @NotNull
    @Size(max = 30)
    private String phone;

    private LocalDate dateOfBirth;

    private Gender gender;

    @Size(max = 255)
    private String address;

    private String medicalHistory;

    @Size(max = 500)
    private String allergies;

    @Size(max = 30)
    private String emergencyContactPhone;

    @Size(max = 10)
    private String bloodGroup;

    @NotNull
    private UserDTO user;

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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getMedicalHistory() {
        return medicalHistory;
    }

    public void setMedicalHistory(String medicalHistory) {
        this.medicalHistory = medicalHistory;
    }

    public String getAllergies() {
        return allergies;
    }

    public void setAllergies(String allergies) {
        this.allergies = allergies;
    }

    public String getEmergencyContactPhone() {
        return emergencyContactPhone;
    }

    public void setEmergencyContactPhone(String emergencyContactPhone) {
        this.emergencyContactPhone = emergencyContactPhone;
    }

    public String getBloodGroup() {
        return bloodGroup;
    }

    public void setBloodGroup(String bloodGroup) {
        this.bloodGroup = bloodGroup;
    }

    public UserDTO getUser() {
        return user;
    }

    public void setUser(UserDTO user) {
        this.user = user;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PatientProfileDTO)) {
            return false;
        }

        PatientProfileDTO patientProfileDTO = (PatientProfileDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, patientProfileDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "PatientProfileDTO{" +
            "id=" + getId() +
            ", fullName='" + getFullName() + "'" +
            ", phone='" + getPhone() + "'" +
            ", dateOfBirth='" + getDateOfBirth() + "'" +
            ", gender='" + getGender() + "'" +
            ", address='" + getAddress() + "'" +
            ", medicalHistory='" + getMedicalHistory() + "'" +
            ", user=" + getUser() +
            "}";
    }
}
