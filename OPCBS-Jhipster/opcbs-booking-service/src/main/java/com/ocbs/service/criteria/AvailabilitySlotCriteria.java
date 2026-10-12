package com.ocbs.service.criteria;

import com.ocbs.domain.enumeration.ConsultationMode;
import com.ocbs.domain.enumeration.SlotStatus;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link com.ocbs.domain.AvailabilitySlot} entity. This class is used
 * in {@link com.ocbs.web.rest.AvailabilitySlotResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /availability-slots?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class AvailabilitySlotCriteria implements Serializable, Criteria {

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
     * Class for filtering SlotStatus
     */
    public static class SlotStatusFilter extends Filter<SlotStatus> {

        public SlotStatusFilter() {}

        public SlotStatusFilter(SlotStatusFilter filter) {
            super(filter);
        }

        @Override
        public SlotStatusFilter copy() {
            return new SlotStatusFilter(this);
        }
    }

    @Serial
    private static final long serialVersionUID = 1L;

    private LongFilter id;

    private InstantFilter startTime;

    private InstantFilter endTime;

    private ConsultationModeFilter consultationMode;

    private SlotStatusFilter status;

    private StringFilter notes;

    private LongFilter doctorId;

    private Boolean distinct;

    public AvailabilitySlotCriteria() {}

    public AvailabilitySlotCriteria(AvailabilitySlotCriteria other) {
        this.id = other.optionalId().map(LongFilter::copy).orElse(null);
        this.startTime = other.optionalStartTime().map(InstantFilter::copy).orElse(null);
        this.endTime = other.optionalEndTime().map(InstantFilter::copy).orElse(null);
        this.consultationMode = other.optionalConsultationMode().map(ConsultationModeFilter::copy).orElse(null);
        this.status = other.optionalStatus().map(SlotStatusFilter::copy).orElse(null);
        this.notes = other.optionalNotes().map(StringFilter::copy).orElse(null);
        this.doctorId = other.optionalDoctorId().map(LongFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public AvailabilitySlotCriteria copy() {
        return new AvailabilitySlotCriteria(this);
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

    public InstantFilter getStartTime() {
        return startTime;
    }

    public Optional<InstantFilter> optionalStartTime() {
        return Optional.ofNullable(startTime);
    }

    public InstantFilter startTime() {
        if (startTime == null) {
            setStartTime(new InstantFilter());
        }
        return startTime;
    }

    public void setStartTime(InstantFilter startTime) {
        this.startTime = startTime;
    }

    public InstantFilter getEndTime() {
        return endTime;
    }

    public Optional<InstantFilter> optionalEndTime() {
        return Optional.ofNullable(endTime);
    }

    public InstantFilter endTime() {
        if (endTime == null) {
            setEndTime(new InstantFilter());
        }
        return endTime;
    }

    public void setEndTime(InstantFilter endTime) {
        this.endTime = endTime;
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

    public SlotStatusFilter getStatus() {
        return status;
    }

    public Optional<SlotStatusFilter> optionalStatus() {
        return Optional.ofNullable(status);
    }

    public SlotStatusFilter status() {
        if (status == null) {
            setStatus(new SlotStatusFilter());
        }
        return status;
    }

    public void setStatus(SlotStatusFilter status) {
        this.status = status;
    }

    public StringFilter getNotes() {
        return notes;
    }

    public Optional<StringFilter> optionalNotes() {
        return Optional.ofNullable(notes);
    }

    public StringFilter notes() {
        if (notes == null) {
            setNotes(new StringFilter());
        }
        return notes;
    }

    public void setNotes(StringFilter notes) {
        this.notes = notes;
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
        final AvailabilitySlotCriteria that = (AvailabilitySlotCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(startTime, that.startTime) &&
            Objects.equals(endTime, that.endTime) &&
            Objects.equals(consultationMode, that.consultationMode) &&
            Objects.equals(status, that.status) &&
            Objects.equals(notes, that.notes) &&
            Objects.equals(doctorId, that.doctorId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, startTime, endTime, consultationMode, status, notes, doctorId, distinct);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "AvailabilitySlotCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalStartTime().map(f -> "startTime=" + f + ", ").orElse("") +
            optionalEndTime().map(f -> "endTime=" + f + ", ").orElse("") +
            optionalConsultationMode().map(f -> "consultationMode=" + f + ", ").orElse("") +
            optionalStatus().map(f -> "status=" + f + ", ").orElse("") +
            optionalNotes().map(f -> "notes=" + f + ", ").orElse("") +
            optionalDoctorId().map(f -> "doctorId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
