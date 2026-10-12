package com.ocbs.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Table(name = "psychometric_question")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class PsychometricQuestion extends AbstractAuditingEntity<Long> implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "questions" }, allowSetters = true)
    private PsychometricTest test;

    @NotNull
    @Lob
    @Column(name = "question_text", nullable = false)
    private String questionText;

    @NotNull
    @Column(name = "question_number", nullable = false)
    private Integer questionNumber;

    @Size(max = 100)
    @Column(name = "category", length = 100)
    private String category; // "Depression", "Anxiety", "Stress"

    @NotNull
    @Size(max = 50)
    @Column(name = "question_type", length = 50, nullable = false)
    private String questionType = "Rating0To3"; // "Rating0To3", "Rating1To5", "MultipleChoice", "YesNo"

    @Lob
    @Column(name = "options_json")
    private String optionsJson;

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PsychometricTest getTest() {
        return test;
    }

    public void setTest(PsychometricTest test) {
        this.test = test;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public Integer getQuestionNumber() {
        return questionNumber;
    }

    public void setQuestionNumber(Integer questionNumber) {
        this.questionNumber = questionNumber;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getQuestionType() {
        return questionType;
    }

    public void setQuestionType(String questionType) {
        this.questionType = questionType;
    }

    public String getOptionsJson() {
        return optionsJson;
    }

    public void setOptionsJson(String optionsJson) {
        this.optionsJson = optionsJson;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PsychometricQuestion)) {
            return false;
        }
        return id != null && id.equals(((PsychometricQuestion) o).id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
