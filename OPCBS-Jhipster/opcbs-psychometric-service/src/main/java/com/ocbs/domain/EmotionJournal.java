package com.ocbs.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.math.BigDecimal;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Table(name = "emotion_journal")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class EmotionJournal extends AbstractAuditingEntity<Long> implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull
    @Size(max = 255)
    @Column(name = "title", length = 255, nullable = false)
    private String title;

    @Lob
    @Column(name = "content")
    private String content;

    @NotNull
    @Column(name = "mood_scale", nullable = false)
    private Integer moodScale = 3; // 1: Very Bad -> 5: Very Good

    @NotNull
    @Column(name = "stress_scale", nullable = false)
    private Integer stressScale = 3; // 1: Very Low -> 5: Very High

    @Column(name = "sleep_hours", precision = 4, scale = 1)
    private BigDecimal sleepHours;

    @Column(name = "depression_scale")
    private Integer depressionScale; // 1: Very Low -> 5: Very High

    @NotNull
    @Column(name = "is_shared", nullable = false)
    private Boolean isShared = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "user" }, allowSetters = true)
    private PatientProfile patient;

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getMoodScale() {
        return moodScale;
    }

    public void setMoodScale(Integer moodScale) {
        this.moodScale = moodScale;
    }

    public Integer getStressScale() {
        return stressScale;
    }

    public void setStressScale(Integer stressScale) {
        this.stressScale = stressScale;
    }

    public BigDecimal getSleepHours() {
        return sleepHours;
    }

    public void setSleepHours(BigDecimal sleepHours) {
        this.sleepHours = sleepHours;
    }

    public Integer getDepressionScale() {
        return depressionScale;
    }

    public void setDepressionScale(Integer depressionScale) {
        this.depressionScale = depressionScale;
    }

    public Boolean getIsShared() {
        return isShared;
    }

    public void setIsShared(Boolean isShared) {
        this.isShared = isShared;
    }

    public PatientProfile getPatient() {
        return patient;
    }

    public void setPatient(PatientProfile patient) {
        this.patient = patient;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EmotionJournal)) {
            return false;
        }
        return id != null && id.equals(((EmotionJournal) o).id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
