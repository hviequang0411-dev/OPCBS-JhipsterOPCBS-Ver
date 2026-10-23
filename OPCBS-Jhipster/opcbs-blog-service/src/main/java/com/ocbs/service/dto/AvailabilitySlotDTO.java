package com.ocbs.service.dto;

import com.ocbs.domain.enumeration.ConsultationMode;
import com.ocbs.domain.enumeration.SlotStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * A DTO for the {@link com.ocbs.domain.AvailabilitySlot} entity.
 */
@Schema(description = "Khung giờ rảnh của bác sĩ để nhận lịch khám")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class AvailabilitySlotDTO implements Serializable {

    private Long id;

    @NotNull
    private Instant startTime;

    @NotNull
    private Instant endTime;

    @NotNull
    private ConsultationMode consultationMode;

    @NotNull
    private SlotStatus status;

    @Size(max = 255)
    private String notes;

    @NotNull
    private DoctorProfileDTO doctor;

    private Long appointmentId;

    private String appointmentBookingCode;

    private String patientName;

    private String patientPhone;

    private com.ocbs.domain.enumeration.AppointmentStatus appointmentStatus;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(Long appointmentId) {
        this.appointmentId = appointmentId;
    }

    public String getAppointmentBookingCode() {
        return appointmentBookingCode;
    }

    public void setAppointmentBookingCode(String appointmentBookingCode) {
        this.appointmentBookingCode = appointmentBookingCode;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getPatientPhone() {
        return patientPhone;
    }

    public void setPatientPhone(String patientPhone) {
        this.patientPhone = patientPhone;
    }

    public com.ocbs.domain.enumeration.AppointmentStatus getAppointmentStatus() {
        return appointmentStatus;
    }

    public void setAppointmentStatus(com.ocbs.domain.enumeration.AppointmentStatus appointmentStatus) {
        this.appointmentStatus = appointmentStatus;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }

    public ConsultationMode getConsultationMode() {
        return consultationMode;
    }

    public void setConsultationMode(ConsultationMode consultationMode) {
        this.consultationMode = consultationMode;
    }

    public SlotStatus getStatus() {
        return status;
    }

    public void setStatus(SlotStatus status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public DoctorProfileDTO getDoctor() {
        return doctor;
    }

    public void setDoctor(DoctorProfileDTO doctor) {
        this.doctor = doctor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AvailabilitySlotDTO)) {
            return false;
        }

        AvailabilitySlotDTO availabilitySlotDTO = (AvailabilitySlotDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, availabilitySlotDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "AvailabilitySlotDTO{" +
            "id=" + getId() +
            ", startTime='" + getStartTime() + "'" +
            ", endTime='" + getEndTime() + "'" +
            ", consultationMode='" + getConsultationMode() + "'" +
            ", status='" + getStatus() + "'" +
            ", notes='" + getNotes() + "'" +
            ", doctor=" + getDoctor() +
            "}";
    }
}
