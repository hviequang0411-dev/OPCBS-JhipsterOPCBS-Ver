package com.ocbs.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.ocbs.domain.enumeration.ConsultationMode;
import com.ocbs.domain.enumeration.SlotStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

/**
 * Khung giờ rảnh của bác sĩ để nhận lịch khám
 */
@Entity
@Table(name = "availability_slot")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class AvailabilitySlot implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull
    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @NotNull
    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "consultation_mode", nullable = false)
    private ConsultationMode consultationMode;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SlotStatus status;

    @Size(max = 255)
    @Column(name = "notes", length = 255)
    private String notes;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "user", "specializationses" }, allowSetters = true)
    private DoctorProfile doctor;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public AvailabilitySlot id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Instant getStartTime() {
        return this.startTime;
    }

    public AvailabilitySlot startTime(Instant startTime) {
        this.setStartTime(startTime);
        return this;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }

    public Instant getEndTime() {
        return this.endTime;
    }

    public AvailabilitySlot endTime(Instant endTime) {
        this.setEndTime(endTime);
        return this;
    }

    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }

    public ConsultationMode getConsultationMode() {
        return this.consultationMode;
    }

    public AvailabilitySlot consultationMode(ConsultationMode consultationMode) {
        this.setConsultationMode(consultationMode);
        return this;
    }

    public void setConsultationMode(ConsultationMode consultationMode) {
        this.consultationMode = consultationMode;
    }

    public SlotStatus getStatus() {
        return this.status;
    }

    public AvailabilitySlot status(SlotStatus status) {
        this.setStatus(status);
        return this;
    }

    public void setStatus(SlotStatus status) {
        this.status = status;
    }

    public String getNotes() {
        return this.notes;
    }

    public AvailabilitySlot notes(String notes) {
        this.setNotes(notes);
        return this;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public DoctorProfile getDoctor() {
        return this.doctor;
    }

    public void setDoctor(DoctorProfile doctorProfile) {
        this.doctor = doctorProfile;
    }

    public AvailabilitySlot doctor(DoctorProfile doctorProfile) {
        this.setDoctor(doctorProfile);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AvailabilitySlot)) {
            return false;
        }
        return getId() != null && getId().equals(((AvailabilitySlot) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "AvailabilitySlot{" +
            "id=" + getId() +
            ", startTime='" + getStartTime() + "'" +
            ", endTime='" + getEndTime() + "'" +
            ", consultationMode='" + getConsultationMode() + "'" +
            ", status='" + getStatus() + "'" +
            ", notes='" + getNotes() + "'" +
            "}";
    }
}
