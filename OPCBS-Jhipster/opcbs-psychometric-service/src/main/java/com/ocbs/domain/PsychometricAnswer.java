package com.ocbs.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Table(name = "psychometric_answer")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class PsychometricAnswer extends AbstractAuditingEntity<Long> implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    private PsychometricSubmission submission;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "test" }, allowSetters = true)
    private PsychometricQuestion question;

    @NotNull
    @Column(name = "score", nullable = false)
    private Integer score = 0;

    @Lob
    @Column(name = "answer_text")
    private String answerText;

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PsychometricSubmission getSubmission() {
        return submission;
    }

    public void setSubmission(PsychometricSubmission submission) {
        this.submission = submission;
    }

    public PsychometricQuestion getQuestion() {
        return question;
    }

    public void setQuestion(PsychometricQuestion question) {
        this.question = question;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public String getAnswerText() {
        return answerText;
    }

    public void setAnswerText(String answerText) {
        this.answerText = answerText;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PsychometricAnswer)) {
            return false;
        }
        return id != null && id.equals(((PsychometricAnswer) o).id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
