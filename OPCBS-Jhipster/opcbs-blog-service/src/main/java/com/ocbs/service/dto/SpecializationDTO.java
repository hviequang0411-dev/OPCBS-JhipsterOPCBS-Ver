package com.ocbs.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A DTO for the {@link com.ocbs.domain.Specialization} entity.
 */
@Schema(description = "Danh mục chuyên khoa y tế (ví dụ: Tâm lý, Thần kinh, Nhi khoa, Đa khoa...)")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class SpecializationDTO implements Serializable {

    private Long id;

    @NotNull
    @Size(max = 160)
    private String name;

    @Size(max = 500)
    private String description;

    private Set<DoctorProfileDTO> doctorses = new HashSet<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Set<DoctorProfileDTO> getDoctorses() {
        return doctorses;
    }

    public void setDoctorses(Set<DoctorProfileDTO> doctorses) {
        this.doctorses = doctorses;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SpecializationDTO)) {
            return false;
        }

        SpecializationDTO specializationDTO = (SpecializationDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, specializationDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "SpecializationDTO{" +
            "id=" + getId() +
            ", name='" + getName() + "'" +
            ", description='" + getDescription() + "'" +
            ", doctorses=" + getDoctorses() +
            "}";
    }
}
