package com.ocbs.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * Danh mục chuyên khoa y tế (ví dụ: Tâm lý, Thần kinh, Nhi khoa, Đa khoa...)
 */
@Entity
@Table(name = "specialization")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Specialization implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull
    @Size(max = 160)
    @Column(name = "name", length = 160, nullable = false, unique = true)
    private String name;

    @Size(max = 500)
    @Column(name = "description", length = 500)
    private String description;

    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "specializationses")
    @JsonIgnoreProperties(value = { "user", "specializationses" }, allowSetters = true)
    private Set<DoctorProfile> doctorses = new HashSet<>();

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public Specialization id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public Specialization name(String name) {
        this.setName(name);
        return this;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return this.description;
    }

    public Specialization description(String description) {
        this.setDescription(description);
        return this;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Set<DoctorProfile> getDoctorses() {
        return this.doctorses;
    }

    public void setDoctorses(Set<DoctorProfile> doctorProfiles) {
        if (this.doctorses != null) {
            this.doctorses.forEach(i -> i.removeSpecializations(this));
        }
        if (doctorProfiles != null) {
            doctorProfiles.forEach(i -> i.addSpecializations(this));
        }
        this.doctorses = doctorProfiles;
    }

    public Specialization doctorses(Set<DoctorProfile> doctorProfiles) {
        this.setDoctorses(doctorProfiles);
        return this;
    }

    public Specialization addDoctors(DoctorProfile doctorProfile) {
        this.doctorses.add(doctorProfile);
        doctorProfile.getSpecializationses().add(this);
        return this;
    }

    public Specialization removeDoctors(DoctorProfile doctorProfile) {
        this.doctorses.remove(doctorProfile);
        doctorProfile.getSpecializationses().remove(this);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Specialization)) {
            return false;
        }
        return getId() != null && getId().equals(((Specialization) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Specialization{" +
            "id=" + getId() +
            ", name='" + getName() + "'" +
            ", description='" + getDescription() + "'" +
            "}";
    }
}
