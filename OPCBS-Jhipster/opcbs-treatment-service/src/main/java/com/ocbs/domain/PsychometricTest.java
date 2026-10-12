package com.ocbs.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Table(name = "psychometric_test")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class PsychometricTest extends AbstractAuditingEntity<Long> implements Serializable {

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
    @Column(name = "description")
    private String description;

    @NotNull
    @Size(max = 50)
    @Column(name = "test_type", length = 50, nullable = false)
    private String testType; // "DASS21", "PHQ9", "GAD7", "CUSTOM"

    @Size(max = 100)
    @Column(name = "category", length = 100)
    private String category; // "Depression", "Anxiety", "Stress", "General Wellbeing"

    @Lob
    @Column(name = "purpose")
    private String purpose;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "user", "specialization" }, allowSetters = true)
    private DoctorProfile doctor; // null for system tests

    @Lob
    @Column(name = "score_ranges_json")
    private String scoreRangesJson;

    @Size(max = 500)
    @Column(name = "source_url", length = 500)
    private String sourceUrl;

    @NotNull
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @OneToMany(mappedBy = "test", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("questionNumber ASC")
    @JsonIgnoreProperties(value = { "test" }, allowSetters = true)
    private List<PsychometricQuestion> questions = new ArrayList<>();

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTestType() {
        return testType;
    }

    public void setTestType(String testType) {
        this.testType = testType;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public DoctorProfile getDoctor() {
        return doctor;
    }

    public void setDoctor(DoctorProfile doctor) {
        this.doctor = doctor;
    }

    public String getScoreRangesJson() {
        return scoreRangesJson;
    }

    public void setScoreRangesJson(String scoreRangesJson) {
        this.scoreRangesJson = scoreRangesJson;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public List<PsychometricQuestion> getQuestions() {
        return questions;
    }

    public void setQuestions(List<PsychometricQuestion> questions) {
        this.questions = questions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PsychometricTest)) {
            return false;
        }
        return id != null && id.equals(((PsychometricTest) o).id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
