package com.ocbs.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.ocbs.domain.enumeration.Gender;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

/**
 * Hồ sơ bác sĩ (mở rộng từ JHipster User)
 */
@Entity
@Table(name = "doctor_profile")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class DoctorProfile implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull
    @Size(max = 160)
    @Column(name = "full_name", length = 160, nullable = false)
    private String fullName;

    @Size(max = 100)
    @Column(name = "title", length = 100)
    private String title;

    @Lob
    @Column(name = "bio")
    private String bio;

    @Min(value = 0)
    @Column(name = "experience_years")
    private Integer experienceYears;

    @DecimalMin(value = "0")
    @Column(name = "consultation_fee", precision = 21, scale = 2)
    private BigDecimal consultationFee;

    @Size(max = 30)
    @Column(name = "phone", length = 30)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender")
    private Gender gender;

    @NotNull
    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @NotNull
    @JoinColumn(unique = true)
    private User user;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "rel_doctor_profile__specializations",
        joinColumns = @JoinColumn(name = "doctor_profile_id"),
        inverseJoinColumns = @JoinColumn(name = "specializations_id")
    )
    @JsonIgnoreProperties(value = { "doctorses" }, allowSetters = true)
    private Set<Specialization> specializationses = new HashSet<>();

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public DoctorProfile id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFullName() {
        return this.fullName;
    }

    public DoctorProfile fullName(String fullName) {
        this.setFullName(fullName);
        return this;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getTitle() {
        return this.title;
    }

    public DoctorProfile title(String title) {
        this.setTitle(title);
        return this;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBio() {
        return this.bio;
    }

    public DoctorProfile bio(String bio) {
        this.setBio(bio);
        return this;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public Integer getExperienceYears() {
        return this.experienceYears;
    }

    public DoctorProfile experienceYears(Integer experienceYears) {
        this.setExperienceYears(experienceYears);
        return this;
    }

    public void setExperienceYears(Integer experienceYears) {
        this.experienceYears = experienceYears;
    }

    public BigDecimal getConsultationFee() {
        return this.consultationFee;
    }

    public DoctorProfile consultationFee(BigDecimal consultationFee) {
        this.setConsultationFee(consultationFee);
        return this;
    }

    public void setConsultationFee(BigDecimal consultationFee) {
        this.consultationFee = consultationFee;
    }

    public String getPhone() {
        return this.phone;
    }

    public DoctorProfile phone(String phone) {
        this.setPhone(phone);
        return this;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Gender getGender() {
        return this.gender;
    }

    public DoctorProfile gender(Gender gender) {
        this.setGender(gender);
        return this;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public Boolean getIsActive() {
        return this.isActive;
    }

    public DoctorProfile isActive(Boolean isActive) {
        this.setIsActive(isActive);
        return this;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public User getUser() {
        return this.user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public DoctorProfile user(User user) {
        this.setUser(user);
        return this;
    }

    public Set<Specialization> getSpecializationses() {
        return this.specializationses;
    }

    public void setSpecializationses(Set<Specialization> specializations) {
        this.specializationses = specializations;
    }

    public DoctorProfile specializationses(Set<Specialization> specializations) {
        this.setSpecializationses(specializations);
        return this;
    }

    public DoctorProfile addSpecializations(Specialization specialization) {
        this.specializationses.add(specialization);
        return this;
    }

    public DoctorProfile removeSpecializations(Specialization specialization) {
        this.specializationses.remove(specialization);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DoctorProfile)) {
            return false;
        }
        return getId() != null && getId().equals(((DoctorProfile) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "DoctorProfile{" +
            "id=" + getId() +
            ", fullName='" + getFullName() + "'" +
            ", title='" + getTitle() + "'" +
            ", bio='" + getBio() + "'" +
            ", experienceYears=" + getExperienceYears() +
            ", consultationFee=" + getConsultationFee() +
            ", phone='" + getPhone() + "'" +
            ", gender='" + getGender() + "'" +
            ", isActive='" + getIsActive() + "'" +
            "}";
    }
}
