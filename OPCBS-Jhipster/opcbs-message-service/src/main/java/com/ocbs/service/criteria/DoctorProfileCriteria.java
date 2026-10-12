package com.ocbs.service.criteria;

import com.ocbs.domain.enumeration.Gender;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link com.ocbs.domain.DoctorProfile} entity. This class is used
 * in {@link com.ocbs.web.rest.DoctorProfileResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /doctor-profiles?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class DoctorProfileCriteria implements Serializable, Criteria {

    /**
     * Class for filtering Gender
     */
    public static class GenderFilter extends Filter<Gender> {

        public GenderFilter() {}

        public GenderFilter(GenderFilter filter) {
            super(filter);
        }

        @Override
        public GenderFilter copy() {
            return new GenderFilter(this);
        }
    }

    @Serial
    private static final long serialVersionUID = 1L;

    private LongFilter id;

    private StringFilter fullName;

    private StringFilter title;

    private IntegerFilter experienceYears;

    private BigDecimalFilter consultationFee;

    private StringFilter phone;

    private GenderFilter gender;

    private BooleanFilter isActive;

    private LongFilter userId;

    private LongFilter specializationsId;

    private Boolean distinct;

    public DoctorProfileCriteria() {}

    public DoctorProfileCriteria(DoctorProfileCriteria other) {
        this.id = other.optionalId().map(LongFilter::copy).orElse(null);
        this.fullName = other.optionalFullName().map(StringFilter::copy).orElse(null);
        this.title = other.optionalTitle().map(StringFilter::copy).orElse(null);
        this.experienceYears = other.optionalExperienceYears().map(IntegerFilter::copy).orElse(null);
        this.consultationFee = other.optionalConsultationFee().map(BigDecimalFilter::copy).orElse(null);
        this.phone = other.optionalPhone().map(StringFilter::copy).orElse(null);
        this.gender = other.optionalGender().map(GenderFilter::copy).orElse(null);
        this.isActive = other.optionalIsActive().map(BooleanFilter::copy).orElse(null);
        this.userId = other.optionalUserId().map(LongFilter::copy).orElse(null);
        this.specializationsId = other.optionalSpecializationsId().map(LongFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public DoctorProfileCriteria copy() {
        return new DoctorProfileCriteria(this);
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

    public StringFilter getFullName() {
        return fullName;
    }

    public Optional<StringFilter> optionalFullName() {
        return Optional.ofNullable(fullName);
    }

    public StringFilter fullName() {
        if (fullName == null) {
            setFullName(new StringFilter());
        }
        return fullName;
    }

    public void setFullName(StringFilter fullName) {
        this.fullName = fullName;
    }

    public StringFilter getTitle() {
        return title;
    }

    public Optional<StringFilter> optionalTitle() {
        return Optional.ofNullable(title);
    }

    public StringFilter title() {
        if (title == null) {
            setTitle(new StringFilter());
        }
        return title;
    }

    public void setTitle(StringFilter title) {
        this.title = title;
    }

    public IntegerFilter getExperienceYears() {
        return experienceYears;
    }

    public Optional<IntegerFilter> optionalExperienceYears() {
        return Optional.ofNullable(experienceYears);
    }

    public IntegerFilter experienceYears() {
        if (experienceYears == null) {
            setExperienceYears(new IntegerFilter());
        }
        return experienceYears;
    }

    public void setExperienceYears(IntegerFilter experienceYears) {
        this.experienceYears = experienceYears;
    }

    public BigDecimalFilter getConsultationFee() {
        return consultationFee;
    }

    public Optional<BigDecimalFilter> optionalConsultationFee() {
        return Optional.ofNullable(consultationFee);
    }

    public BigDecimalFilter consultationFee() {
        if (consultationFee == null) {
            setConsultationFee(new BigDecimalFilter());
        }
        return consultationFee;
    }

    public void setConsultationFee(BigDecimalFilter consultationFee) {
        this.consultationFee = consultationFee;
    }

    public StringFilter getPhone() {
        return phone;
    }

    public Optional<StringFilter> optionalPhone() {
        return Optional.ofNullable(phone);
    }

    public StringFilter phone() {
        if (phone == null) {
            setPhone(new StringFilter());
        }
        return phone;
    }

    public void setPhone(StringFilter phone) {
        this.phone = phone;
    }

    public GenderFilter getGender() {
        return gender;
    }

    public Optional<GenderFilter> optionalGender() {
        return Optional.ofNullable(gender);
    }

    public GenderFilter gender() {
        if (gender == null) {
            setGender(new GenderFilter());
        }
        return gender;
    }

    public void setGender(GenderFilter gender) {
        this.gender = gender;
    }

    public BooleanFilter getIsActive() {
        return isActive;
    }

    public Optional<BooleanFilter> optionalIsActive() {
        return Optional.ofNullable(isActive);
    }

    public BooleanFilter isActive() {
        if (isActive == null) {
            setIsActive(new BooleanFilter());
        }
        return isActive;
    }

    public void setIsActive(BooleanFilter isActive) {
        this.isActive = isActive;
    }

    public LongFilter getUserId() {
        return userId;
    }

    public Optional<LongFilter> optionalUserId() {
        return Optional.ofNullable(userId);
    }

    public LongFilter userId() {
        if (userId == null) {
            setUserId(new LongFilter());
        }
        return userId;
    }

    public void setUserId(LongFilter userId) {
        this.userId = userId;
    }

    public LongFilter getSpecializationsId() {
        return specializationsId;
    }

    public Optional<LongFilter> optionalSpecializationsId() {
        return Optional.ofNullable(specializationsId);
    }

    public LongFilter specializationsId() {
        if (specializationsId == null) {
            setSpecializationsId(new LongFilter());
        }
        return specializationsId;
    }

    public void setSpecializationsId(LongFilter specializationsId) {
        this.specializationsId = specializationsId;
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
        final DoctorProfileCriteria that = (DoctorProfileCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(fullName, that.fullName) &&
            Objects.equals(title, that.title) &&
            Objects.equals(experienceYears, that.experienceYears) &&
            Objects.equals(consultationFee, that.consultationFee) &&
            Objects.equals(phone, that.phone) &&
            Objects.equals(gender, that.gender) &&
            Objects.equals(isActive, that.isActive) &&
            Objects.equals(userId, that.userId) &&
            Objects.equals(specializationsId, that.specializationsId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            id,
            fullName,
            title,
            experienceYears,
            consultationFee,
            phone,
            gender,
            isActive,
            userId,
            specializationsId,
            distinct
        );
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "DoctorProfileCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalFullName().map(f -> "fullName=" + f + ", ").orElse("") +
            optionalTitle().map(f -> "title=" + f + ", ").orElse("") +
            optionalExperienceYears().map(f -> "experienceYears=" + f + ", ").orElse("") +
            optionalConsultationFee().map(f -> "consultationFee=" + f + ", ").orElse("") +
            optionalPhone().map(f -> "phone=" + f + ", ").orElse("") +
            optionalGender().map(f -> "gender=" + f + ", ").orElse("") +
            optionalIsActive().map(f -> "isActive=" + f + ", ").orElse("") +
            optionalUserId().map(f -> "userId=" + f + ", ").orElse("") +
            optionalSpecializationsId().map(f -> "specializationsId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
