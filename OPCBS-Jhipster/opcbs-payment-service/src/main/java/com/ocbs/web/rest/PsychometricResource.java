package com.ocbs.web.rest;

import com.ocbs.domain.*;
import com.ocbs.repository.*;
import com.ocbs.security.SecurityUtils;
import com.ocbs.web.rest.errors.BadRequestAlertException;
import jakarta.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api")
@Transactional
public class PsychometricResource {

    private static final Logger LOG = LoggerFactory.getLogger(PsychometricResource.class);
    private static final String ENTITY_NAME = "psychometricTest";

    private final PsychometricTestRepository testRepository;
    private final PsychometricQuestionRepository questionRepository;
    private final PsychometricSubmissionRepository submissionRepository;
    private final PsychometricAnswerRepository answerRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PsychometricResource(
        PsychometricTestRepository testRepository,
        PsychometricQuestionRepository questionRepository,
        PsychometricSubmissionRepository submissionRepository,
        PsychometricAnswerRepository answerRepository,
        PatientProfileRepository patientProfileRepository,
        DoctorProfileRepository doctorProfileRepository
    ) {
        this.testRepository = testRepository;
        this.questionRepository = questionRepository;
        this.submissionRepository = submissionRepository;
        this.answerRepository = answerRepository;
        this.patientProfileRepository = patientProfileRepository;
        this.doctorProfileRepository = doctorProfileRepository;
    }

    private PatientProfile getCurrentPatient() {
        String login = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new BadRequestAlertException("User not authenticated", ENTITY_NAME, "unauthenticated"));
        return patientProfileRepository
            .findByUser_Login(login)
            .orElseThrow(() -> new BadRequestAlertException("Patient profile not found", ENTITY_NAME, "patientnotfound"));
    }

    private DoctorProfile getCurrentDoctor() {
        String login = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new BadRequestAlertException("User not authenticated", ENTITY_NAME, "unauthenticated"));
        return doctorProfileRepository
            .findByUser_Login(login)
            .orElseThrow(() -> new BadRequestAlertException("Doctor profile not found", ENTITY_NAME, "doctornotfound"));
    }

    @GetMapping("/psychometrics/tests")
    public ResponseEntity<List<PsychometricTest>> getAllActiveTests() {
        List<PsychometricTest> tests = testRepository.findAllActive();
        return ResponseEntity.ok(tests);
    }

    @GetMapping("/psychometrics/tests/{id}")
    public ResponseEntity<PsychometricTest> getTestWithQuestions(@PathVariable("id") Long id) {
        PsychometricTest test = testRepository
            .findOneWithQuestions(id)
            .orElseThrow(() -> new BadRequestAlertException("Test not found", ENTITY_NAME, "idnotfound"));
        return ResponseEntity.ok(test);
    }

    public static class SubmitAnswerVM {
        public Long questionId;
        public Integer score;
        public String answerText;
    }

    public static class SubmissionRequestVM {
        public Long assignedSubmissionId;
        public Long appointmentId;
        public List<SubmitAnswerVM> answers;
    }

    @PostMapping("/psychometrics/tests/{id}/submit")
    @PreAuthorize("hasAuthority('ROLE_PATIENT')")
    public ResponseEntity<PsychometricSubmission> submitTest(
        @PathVariable("id") Long id,
        @RequestBody SubmissionRequestVM request
    ) throws Exception {
        PsychometricTest test = testRepository
            .findOneWithQuestions(id)
            .orElseThrow(() -> new BadRequestAlertException("Test not found", ENTITY_NAME, "idnotfound"));

        PatientProfile patient = getCurrentPatient();
        List<PsychometricQuestion> questions = test.getQuestions();
        Map<Long, PsychometricQuestion> questionMap = new HashMap<>();
        for (PsychometricQuestion q : questions) {
            questionMap.put(q.getId(), q);
        }

        int totalScore = 0;
        Map<String, Integer> subscores = new HashMap<>();
        List<PsychometricAnswer> answerEntities = new ArrayList<>();

        PsychometricSubmission submission;
        if (request.assignedSubmissionId != null) {
            submission = submissionRepository.findById(request.assignedSubmissionId).orElse(new PsychometricSubmission());
        } else {
            submission = new PsychometricSubmission();
        }

        submission.setTest(test);
        submission.setPatient(patient);
        submission.setStatus("COMPLETED");
        submission.setCompletedAt(Instant.now());

        if (request.answers != null) {
            for (SubmitAnswerVM a : request.answers) {
                PsychometricQuestion q = questionMap.get(a.questionId);
                int score = a.score != null ? a.score : 0;
                totalScore += score;

                if (q != null && q.getCategory() != null) {
                    String cat = q.getCategory().trim().toLowerCase();
                    subscores.put(cat, subscores.getOrDefault(cat, 0) + score);
                }

                PsychometricAnswer ans = new PsychometricAnswer();
                ans.setSubmission(submission);
                ans.setQuestion(q);
                ans.setScore(score);
                ans.setAnswerText(a.answerText);
                answerEntities.add(ans);
            }
        }

        submission.setTotalScore(totalScore);

        // Scoring and clinical interpretation logic
        String type = test.getTestType() != null ? test.getTestType().toUpperCase() : "";
        Map<String, Object> scoreData = new HashMap<>();
        String interpretation = "";

        if (type.contains("DASS")) {
            // DASS-21 scales are multiplied by 2
            int depRaw = subscores.getOrDefault("depression", 0);
            int anxRaw = subscores.getOrDefault("anxiety", 0);
            int strRaw = subscores.getOrDefault("stress", 0);

            int depFinal = depRaw * 2;
            int anxFinal = anxRaw * 2;
            int strFinal = strRaw * 2;

            scoreData.put("depressionScore", depFinal);
            scoreData.put("anxietyScore", anxFinal);
            scoreData.put("stressScore", strFinal);

            String depLevel = depFinal <= 9 ? "Bình thường" : depFinal <= 13 ? "Nhẹ" : depFinal <= 20 ? "Vừa" : depFinal <= 27 ? "Nặng" : "Rất nặng";
            String anxLevel = anxFinal <= 7 ? "Bình thường" : anxFinal <= 9 ? "Nhẹ" : anxFinal <= 14 ? "Vừa" : anxFinal <= 19 ? "Nặng" : "Rất nặng";
            String strLevel = strFinal <= 14 ? "Bình thường" : strFinal <= 18 ? "Nhẹ" : strFinal <= 25 ? "Vừa" : strFinal <= 33 ? "Nặng" : "Rất nặng";

            scoreData.put("depressionLevel", depLevel);
            scoreData.put("anxietyLevel", anxLevel);
            scoreData.put("stressLevel", strLevel);

            interpretation = String.format("Trầm cảm: %s (%d điểm) | Lo âu: %s (%d điểm) | Căng thẳng (Stress): %s (%d điểm)",
                depLevel, depFinal, anxLevel, anxFinal, strLevel, strFinal);

        } else if (type.contains("PHQ")) {
            // PHQ-9
            scoreData.put("totalScore", totalScore);
            String level = totalScore <= 4 ? "Tối thiểu / Không trầm cảm" : totalScore <= 9 ? "Trầm cảm nhẹ" : totalScore <= 14 ? "Trầm cảm vừa" : totalScore <= 19 ? "Trầm cảm mức độ nặng vừa" : "Trầm cảm nặng";
            scoreData.put("severityLevel", level);
            interpretation = String.format("Mức độ: %s (Tổng điểm: %d/27)", level, totalScore);

        } else if (type.contains("GAD")) {
            // GAD-7
            scoreData.put("totalScore", totalScore);
            String level = totalScore <= 4 ? "Tối thiểu / Bình thường" : totalScore <= 9 ? "Lo âu nhẹ" : totalScore <= 14 ? "Lo âu trung bình" : "Lo âu nặng";
            scoreData.put("severityLevel", level);
            interpretation = String.format("Mức độ: %s (Tổng điểm: %d/21)", level, totalScore);

        } else {
            scoreData.put("totalScore", totalScore);
            interpretation = "Tổng điểm đạt được: " + totalScore;
        }

        submission.setScoreDataJson(objectMapper.writeValueAsString(scoreData));
        submission.setInterpretation(interpretation);

        PsychometricSubmission savedSubmission = submissionRepository.save(submission);

        for (PsychometricAnswer ans : answerEntities) {
            ans.setSubmission(savedSubmission);
            answerRepository.save(ans);
        }

        return ResponseEntity.ok(savedSubmission);
    }

    @GetMapping("/patient/psychometrics/my-submissions")
    @PreAuthorize("hasAuthority('ROLE_PATIENT')")
    public ResponseEntity<List<PsychometricSubmission>> getMySubmissions() {
        PatientProfile patient = getCurrentPatient();
        List<PsychometricSubmission> list = submissionRepository.findAllByPatientId(patient.getId());
        return ResponseEntity.ok(list);
    }

    @GetMapping("/psychometrics/submissions/{id}")
    public ResponseEntity<PsychometricSubmission> getSubmissionDetails(@PathVariable("id") Long id) {
        PsychometricSubmission sub = submissionRepository
            .findOneWithDetails(id)
            .orElseThrow(() -> new BadRequestAlertException("Submission not found", ENTITY_NAME, "idnotfound"));

        List<PsychometricAnswer> answers = answerRepository.findAllBySubmissionId(id);
        sub.setAnswers(answers);
        return ResponseEntity.ok(sub);
    }

    public static class AssignTestVM {
        public Long testId;
        public Long patientId;
        public Instant dueDate;
        public String doctorNotes;
    }

    @PostMapping("/doctor/psychometrics/assign")
    @PreAuthorize("hasAuthority('ROLE_DOCTOR')")
    public ResponseEntity<PsychometricSubmission> assignTestToPatient(@RequestBody AssignTestVM vm) {
        DoctorProfile doctor = getCurrentDoctor();
        PsychometricTest test = testRepository
            .findById(vm.testId)
            .orElseThrow(() -> new BadRequestAlertException("Test not found", ENTITY_NAME, "testnotfound"));

        PatientProfile patient = patientProfileRepository
            .findById(vm.patientId)
            .orElseThrow(() -> new BadRequestAlertException("Patient not found", ENTITY_NAME, "patientnotfound"));

        PsychometricSubmission submission = new PsychometricSubmission();
        submission.setTest(test);
        submission.setPatient(patient);
        submission.setAssignedByDoctor(doctor);
        submission.setDueDate(vm.dueDate);
        submission.setDoctorNotes(vm.doctorNotes);
        submission.setStatus("ASSIGNED");
        submission.setTotalScore(0);
        submission.setInterpretation("Được bác sĩ " + doctor.getUser().getLastName() + " chỉ định làm bài");
        submission.setScoreDataJson("{}");

        PsychometricSubmission saved = submissionRepository.save(submission);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/doctor/patients/{patientId}/psychometrics")
    @PreAuthorize("hasAuthority('ROLE_DOCTOR')")
    public ResponseEntity<List<PsychometricSubmission>> getPatientPsychometricsForDoctor(@PathVariable("patientId") Long patientId) {
        List<PsychometricSubmission> list = submissionRepository.findAllByPatientId(patientId);
        return ResponseEntity.ok(list);
    }
}
