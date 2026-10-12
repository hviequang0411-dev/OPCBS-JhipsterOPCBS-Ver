package com.ocbs.service.criteria;

import com.ocbs.domain.enumeration.AppointmentStatus;
import com.ocbs.domain.enumeration.ConsultationMode;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link com.ocbs.domain.Appointment} entity. This class is used
 * in {@link com.ocbs.web.rest.AppointmentResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /appointments?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class AppointmentCriteria implements Serializable, Criteria {

    /**
     * Class for filtering ConsultationMode
     */
    public static class ConsultationModeFilter extends Filter<ConsultationMode> {

        public ConsultationModeFilter() {}

        public ConsultationModeFilter(ConsultationModeFilter filter) {
            super(filter);
        }

        @Override
        public ConsultationModeFilter copy() {
            return new ConsultationModeFilter(this);
        }
    }

    /**
     * Class for filtering AppointmentStatus
     */
    public static class AppointmentStatusFilter extends Filter<AppointmentStatus> {

        public AppointmentStatusFilter() {}

        public AppointmentStatusFilter(AppointmentStatusFilter filter) {
            super(filter);
        }

        @Override
        public AppointmentStatusFilter copy() {
            return new AppointmentStatusFilter(this);
        }
    }

    @Serial
    private static final long serialVersionUID = 1L;

    private LongFilter id;

    private StringFilter bookingCode;

    private InstantFilter scheduledStart;

    private InstantFilter scheduledEnd;

    private ConsultationModeFilter consultationMode;

    private AppointmentStatusFilter status;

    private StringFilter reason;

    private StringFilter cancellationReason;

    private InstantFilter createdAt;

    private InstantFilter updatedAt;

    private LongFilter patientId;

    private LongFilter doctorId;

    private LongFilter slotId;

    private Boolean distinct;

    public AppointmentCriteria() {}

    public AppointmentCriteria(AppointmentCriteria other) {
        this.id = other.optionalId().map(LongFilter::copy).orElse(null);
        this.bookingCode = other.optionalBookingCode().map(StringFilter::copy).orElse(null);
        this.scheduledStart = other.optionalScheduledStart().map(InstantFilter::copy).orElse(null);
        this.scheduledEnd = other.optionalScheduledEnd().map(InstantFilter::copy).orElse(null);
        this.consultationMode = other.optionalConsultationMode().map(ConsultationModeFilter::copy).orElse(null);
        this.status = other.optionalStatus().map(AppointmentStatusFilter::copy).orElse(null);
        this.reason = other.optionalReason().map(StringFilter::copy).orElse(null);
        this.cancellationReason = other.optionalCancellationReason().map(StringFilter::copy).orElse(null);
        this.createdAt = other.optionalCreatedAt().map(InstantFilter::copy).orElse(null);
        this.updatedAt = other.optionalUpdatedAt().map(InstantFilter::copy).orElse(null);
        this.patientId = other.optionalPatientId().map(LongFilter::copy).orElse(null);
        this.doctorId = other.optionalDoctorId().map(LongFilter::copy).orElse(null);
        this.slotId = other.optionalSlotId().map(LongFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public AppointmentCriteria copy() {
        return new AppointmentCriteria(this);
    }

    public LongFilter getId() {
        return id;
    }

    public Optional<LongFilter> optionalId() {
        return Optional.ofNullable(id);
    }

    public LongFilter id() {
        if (id == null) {
            setId(new LongFilter());
        }
        return id;
    }

    public void setId(LongFilter id) {
        this.id = id;
    }

    public StringFilter getBookingCode() {
        return bookingCode;
    }

    public Optional<StringFilter> optionalBookingCode() {
        return Optional.ofNullable(bookingCode);
    }

    public StringFilter bookingCode() {
        if (bookingCode == null) {
            setBookingCode(new StringFilter());
        }
        return bookingCode;
    }

    public void setBookingCode(StringFilter bookingCode) {
        this.bookingCode = bookingCode;
    }

    public InstantFilter getScheduledStart() {
        return scheduledStart;
    }

    public Optional<InstantFilter> optionalScheduledStart() {
        return Optional.ofNullable(scheduledStart);
    }

    public InstantFilter scheduledStart() {
        if (scheduledStart == null) {
            setScheduledStart(new InstantFilter());
        }
        return scheduledStart;
    }

    public void setScheduledStart(InstantFilter scheduledStart) {
        this.scheduledStart = scheduledStart;
    }

    public InstantFilter getScheduledEnd() {
        return scheduledEnd;
    }

    public Optional<InstantFilter> optionalScheduledEnd() {
        return Optional.ofNullable(scheduledEnd);
    }

    public InstantFilter scheduledEnd() {
        if (scheduledEnd == null) {
            setScheduledEnd(new InstantFilter());
        }
        return scheduledEnd;
    }

    public void setScheduledEnd(InstantFilter scheduledEnd) {
        this.scheduledEnd = scheduledEnd;
    }

    public ConsultationModeFilter getConsultationMode() {
        return consultationMode;
    }

    public Optional<ConsultationModeFilter> optionalConsultationMode() {
        return Optional.ofNullable(consultationMode);
    }

    public ConsultationModeFilter consultationMode() {
        if (consultationMode == null) {
            setConsultationMode(new ConsultationModeFilter());
        }
        return consultationMode;
    }

    public void setConsultationMode(ConsultationModeFilter consultationMode) {
        this.consultationMode = consultationMode;
    }

    public AppointmentStatusFilter getStatus() {
        return status;
    }

    public Optional<AppointmentStatusFilter> optionalStatus() {
        return Optional.ofNullable(status);
    }

    public AppointmentStatusFilter status() {
        if (status == null) {
            setStatus(new AppointmentStatusFilter());
        }
        return status;
    }

    public void setStatus(AppointmentStatusFilter status) {
        this.status = status;
    }

    public StringFilter getReason() {
        return reason;
    }

    public Optional<StringFilter> optionalReason() {
        return Optional.ofNullable(reason);
    }

    public StringFilter reason() {
        if (reason == null) {
            setReason(new StringFilter());
        }
        return reason;
    }

    public void setReason(StringFilter reason) {
        this.reason = reason;
    }

    public StringFilter getCancellationReason() {
        return cancellationReason;
    }

    public Optional<StringFilter> optionalCancellationReason() {
        return Optional.ofNullable(cancellationReason);
    }

    public StringFilter cancellationReason() {
        if (cancellationReason == null) {
            setCancellationReason(new StringFilter());
        }
        return cancellationReason;
    }

    public void setCancellationReason(StringFilter cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    public InstantFilter getCreatedAt() {
        return createdAt;
    }

    public Optional<InstantFilter> optionalCreatedAt() {
        return Optional.ofNullable(createdAt);
    }

    public InstantFilter createdAt() {
        if (createdAt == null) {
            setCreatedAt(new InstantFilter());
        }
        return createdAt;
    }

    public void setCreatedAt(InstantFilter createdAt) {
        this.createdAt = createdAt;
    }

    public InstantFilter getUpdatedAt() {
        return updatedAt;
    }

    public Optional<InstantFilter> optionalUpdatedAt() {
        return Optional.ofNullable(updatedAt);
    }

    public InstantFilter updatedAt() {
        if (updatedAt == null) {
            setUpdatedAt(new InstantFilter());
        }
        return updatedAt;
    }

    public void setUpdatedAt(InstantFilter updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LongFilter getPatientId() {
        return patientId;
    }

    public Optional<LongFilter> optionalPatientId() {
        return Optional.ofNullable(patientId);
    }

    public LongFilter patientId() {
        if (patientId == null) {
            setPatientId(new LongFilter());
        }
        return patientId;
    }

    public void setPatientId(LongFilter patientId) {
        this.patientId = patientId;
    }

    public LongFilter getDoctorId() {
        return doctorId;
    }

    public Optional<LongFilter> optionalDoctorId() {
        return Optional.ofNullable(doctorId);
    }

    public LongFilter doctorId() {
        if (doctorId == null) {
            setDoctorId(new LongFilter());
        }
        return doctorId;
    }

    public void setDoctorId(LongFilter doctorId) {
        this.doctorId = doctorId;
    }

    public LongFilter getSlotId() {
        return slotId;
    }

    public Optional<LongFilter> optionalSlotId() {
        return Optional.ofNullable(slotId);
    }

    public LongFilter slotId() {
        if (slotId == null) {
            setSlotId(new LongFilter());
        }
        return slotId;
    }

    public void setSlotId(LongFilter slotId) {
        this.slotId = slotId;
    }

    public Boolean getDistinct() {
        return distinct;
    }

    public Optional<Boolean> optionalDistinct() {
        return Optional.ofNullable(distinct);
    }

    public Boolean distinct() {
        if (distinct == null) {
            setDistinct(true);
        }
        return distinct;
    }

    public void setDistinct(Boolean distinct) {
        this.distinct = distinct;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final AppointmentCriteria that = (AppointmentCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(bookingCode, that.bookingCode) &&
            Objects.equals(scheduledStart, that.scheduledStart) &&
            Objects.equals(scheduledEnd, that.scheduledEnd) &&
            Objects.equals(consultationMode, that.consultationMode) &&
            Objects.equals(status, that.status) &&
            Objects.equals(reason, that.reason) &&
            Objects.equals(cancellationReason, that.cancellationReason) &&
            Objects.equals(createdAt, that.createdAt) &&
            Objects.equals(updatedAt, that.updatedAt) &&
            Objects.equals(patientId, that.patientId) &&
            Objects.equals(doctorId, that.doctorId) &&
            Objects.equals(slotId, that.slotId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            id,
            bookingCode,
            scheduledStart,
            scheduledEnd,
            consultationMode,
            status,
            reason,
            cancellationReason,
            createdAt,
            updatedAt,
            patientId,
            doctorId,
            slotId,
            distinct
        );
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "AppointmentCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalBookingCode().map(f -> "bookingCode=" + f + ", ").orElse("") +
            optionalScheduledStart().map(f -> "scheduledStart=" + f + ", ").orElse("") +
            optionalScheduledEnd().map(f -> "scheduledEnd=" + f + ", ").orElse("") +
            optionalConsultationMode().map(f -> "consultationMode=" + f + ", ").orElse("") +
            optionalStatus().map(f -> "status=" + f + ", ").orElse("") +
            optionalReason().map(f -> "reason=" + f + ", ").orElse("") +
            optionalCancellationReason().map(f -> "cancellationReason=" + f + ", ").orElse("") +
            optionalCreatedAt().map(f -> "createdAt=" + f + ", ").orElse("") +
            optionalUpdatedAt().map(f -> "updatedAt=" + f + ", ").orElse("") +
            optionalPatientId().map(f -> "patientId=" + f + ", ").orElse("") +
            optionalDoctorId().map(f -> "doctorId=" + f + ", ").orElse("") +
            optionalSlotId().map(f -> "slotId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
