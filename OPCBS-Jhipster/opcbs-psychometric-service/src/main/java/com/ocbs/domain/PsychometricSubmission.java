package com.ocbs.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Table(name = "psychometric_submission")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class PsychometricSubmission extends AbstractAuditingEntity<Long> implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "questions" }, allowSetters = true)
    private PsychometricTest test;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "user" }, allowSetters = true)
    private PatientProfile patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "user", "specialization" }, allowSetters = true)
    private DoctorProfile assignedByDoctor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "doctor", "patient" }, allowSetters = true)
    private Appointment appointment;

    @NotNull
    @Column(name = "total_score", nullable = false)
    private Integer totalScore = 0;

    @Lob
    @Column(name = "score_data_json")
    private String scoreDataJson; // e.g. {"depression": 14, "anxiety": 8, "stress": 12}

    @Lob
    @Column(name = "interpretation")
    private String interpretation;

    @Lob
    @Column(name = "doctor_notes")
    private String doctorNotes;

    @Column(name = "due_date")
    private Instant dueDate;

    @NotNull
    @Size(max = 50)
    @Column(name = "status", length = 50, nullable = false)
    private String status = "COMPLETED"; // ASSIGNED, IN_PROGRESS, COMPLETED, EXPIRED

    @Column(name = "completed_at")
    private Instant completedAt;

    @OneToMany(mappedBy = "submission")
    @JsonIgnoreProperties(value = { "submission" }, allowSetters = true)
    private List<PsychometricAnswer> answers = new ArrayList<>();

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

    public PatientProfile getPatient() {
        return patient;
    }

    public void setPatient(PatientProfile patient) {
        this.patient = patient;
    }

    public DoctorProfile getAssignedByDoctor() {
        return assignedByDoctor;
    }

    public void setAssignedByDoctor(DoctorProfile assignedByDoctor) {
        this.assignedByDoctor = assignedByDoctor;
    }

    public Appointment getAppointment() {
        return appointment;
    }

    public void setAppointment(Appointment appointment) {
        this.appointment = appointment;
    }

    public Integer getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(Integer totalScore) {
        this.totalScore = totalScore;
    }

    public String getScoreDataJson() {
        return scoreDataJson;
    }

    public void setScoreDataJson(String scoreDataJson) {
        this.scoreDataJson = scoreDataJson;
    }

    public String getInterpretation() {
        return interpretation;
    }

    public void setInterpretation(String interpretation) {
        this.interpretation = interpretation;
    }

    public String getDoctorNotes() {
        return doctorNotes;
    }

    public void setDoctorNotes(String doctorNotes) {
        this.doctorNotes = doctorNotes;
    }

    public Instant getDueDate() {
        return dueDate;
    }

    public void setDueDate(Instant dueDate) {
        this.dueDate = dueDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public List<PsychometricAnswer> getAnswers() {
        return answers;
    }

    public void setAnswers(List<PsychometricAnswer> answers) {
        this.answers = answers;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PsychometricSubmission)) {
            return false;
        }
        return id != null && id.equals(((PsychometricSubmission) o).id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
