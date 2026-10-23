package com.ocbs.web.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.ocbs.domain.*;
import com.ocbs.repository.*;
import com.ocbs.security.AuthoritiesConstants;
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
    private final com.ocbs.event.PsychometricEventProducer psychometricEventProducer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PsychometricResource(
        PsychometricTestRepository testRepository,
        PsychometricQuestionRepository questionRepository,
        PsychometricSubmissionRepository submissionRepository,
        PsychometricAnswerRepository answerRepository,
        PatientProfileRepository patientProfileRepository,
        DoctorProfileRepository doctorProfileRepository,
        com.ocbs.event.PsychometricEventProducer psychometricEventProducer
    ) {
        this.testRepository = testRepository;
        this.questionRepository = questionRepository;
        this.submissionRepository = submissionRepository;
        this.answerRepository = answerRepository;
        this.patientProfileRepository = patientProfileRepository;
        this.doctorProfileRepository = doctorProfileRepository;
        this.psychometricEventProducer = psychometricEventProducer;
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

        // Check for Severe / High-Risk Score and produce Clinical Alert Event
        boolean isHighRisk = false;
        String riskSeverity = "Bình thường";

        if (type.contains("DASS")) {
            int depFinal = (int) scoreData.getOrDefault("depressionScore", 0);
            int anxFinal = (int) scoreData.getOrDefault("anxietyScore", 0);
            int strFinal = (int) scoreData.getOrDefault("stressScore", 0);

            if (depFinal >= 21 || anxFinal >= 15 || strFinal >= 26) {
                isHighRisk = true;
                riskSeverity = (depFinal >= 28 || anxFinal >= 20 || strFinal >= 34) ? "Rất nặng" : "Nặng";
            }
        } else if (type.contains("PHQ")) {
            if (totalScore >= 15) {
                isHighRisk = true;
                riskSeverity = totalScore >= 20 ? "Trầm cảm nặng" : "Trầm cảm mức độ nặng vừa";
            }
        } else if (type.contains("GAD")) {
            if (totalScore >= 15) {
                isHighRisk = true;
                riskSeverity = "Lo âu nặng";
            }
        } else {
            if (totalScore >= 20) {
                isHighRisk = true;
                riskSeverity = "Nguy cơ cao";
            }
        }

        if (isHighRisk) {
            com.ocbs.event.PsychometricAlertEvent alertEvent = new com.ocbs.event.PsychometricAlertEvent();
            alertEvent.setSubmissionId(savedSubmission.getId());
            alertEvent.setTestId(test.getId());
            alertEvent.setTestTitle(test.getTitle());
            alertEvent.setTestType(test.getTestType());
            alertEvent.setPatientId(patient.getId());
            alertEvent.setPatientName(patient.getFullName() != null ? patient.getFullName() : (patient.getUser() != null ? patient.getUser().getLogin() : "Bệnh nhân"));
            alertEvent.setPatientPhone(patient.getPhone());
            if (submission.getAssignedByDoctor() != null) {
                alertEvent.setDoctorId(submission.getAssignedByDoctor().getId());
            }
            alertEvent.setSeverityLevel(riskSeverity);
            alertEvent.setTotalScore(totalScore);
            alertEvent.setInterpretation(interpretation);
            alertEvent.setAlertMessage(String.format("CẢNH BÁO LÂM SÀNG: Bệnh nhân '%s' có kết quả đánh giá '%s' ở mức '%s' (%s). Cần bác sĩ đánh giá khẩn cấp!",
                alertEvent.getPatientName(), test.getTitle(), riskSeverity, interpretation));

            psychometricEventProducer.publishSevereScoreAlert(alertEvent);
        }

        return ResponseEntity.ok(savedSubmission);
    }

    @GetMapping("/patient/psychometrics/my-submissions")
    @PreAuthorize("hasAuthority('ROLE_PATIENT')")
    @Transactional(readOnly = true)
    public ResponseEntity<List<PsychometricSubmission>> getMySubmissions() {
        PatientProfile patient = getCurrentPatient();
        List<PsychometricSubmission> list = submissionRepository.findAllByPatientId(patient.getId());
        return ResponseEntity.ok(list);
    }

    @GetMapping("/psychometrics/submissions/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_PATIENT', 'ROLE_DOCTOR', 'ROLE_ADMIN')")
    @Transactional(readOnly = true)
    public ResponseEntity<PsychometricSubmission> getSubmissionDetails(@PathVariable("id") Long id) {
        try {
            String login = SecurityUtils.getCurrentUserLogin()
                .orElseThrow(() -> new BadRequestAlertException("User not authenticated", ENTITY_NAME, "unauthenticated"));

            PsychometricSubmission sub = submissionRepository
                .findOneWithDetails(id)
                .orElseThrow(() -> new BadRequestAlertException("Submission not found", ENTITY_NAME, "idnotfound"));

            boolean isAdmin = SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.ADMIN);
            if (!isAdmin) {
                if (SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.PATIENT)) {
                    PatientProfile patient = getCurrentPatient();
                    if (sub.getPatient() == null || !sub.getPatient().getId().equals(patient.getId())) {
                        throw new BadRequestAlertException("Bạn không có quyền xem kết quả trắc nghiệm này", ENTITY_NAME, "forbidden");
                    }
                } else if (SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.DOCTOR)) {
                    DoctorProfile doctor = getCurrentDoctor();
                    boolean isAssignedDoctor = sub.getAssignedByDoctor() != null && sub.getAssignedByDoctor().getId().equals(doctor.getId());
                    if (!isAssignedDoctor) {
                        throw new BadRequestAlertException("Bạn không có quyền xem kết quả trắc nghiệm này do chưa có quan hệ điều trị hoặc chỉ định trực tiếp", ENTITY_NAME, "forbidden");
                    }
                } else {
                    throw new BadRequestAlertException("Không có quyền truy cập", ENTITY_NAME, "forbidden");
                }
            }

            LOG.info("AUDIT: User '{}' viewed psychometric submission ID {}", login, id);

            List<PsychometricAnswer> answers = answerRepository.findAllBySubmissionId(id);
            sub.setAnswers(answers);
            return ResponseEntity.ok(sub);
        } catch (Exception ex) {
            LOG.error("LỖI truy vấn chi tiết kết quả trắc nghiệm ID {}: {}", id, ex.getMessage(), ex);
            throw ex;
        }
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
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR', 'ROLE_ADMIN')")
    public ResponseEntity<List<PsychometricSubmission>> getPatientPsychometricsForDoctor(@PathVariable("patientId") Long patientId) {
        String login = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new BadRequestAlertException("User not authenticated", ENTITY_NAME, "unauthenticated"));

        boolean isAdmin = SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.ADMIN);
        List<PsychometricSubmission> list;
        if (isAdmin) {
            list = submissionRepository.findAllByPatientId(patientId);
        } else {
            DoctorProfile doctor = getCurrentDoctor();
            list = submissionRepository.findAllByPatientId(patientId).stream()
                .filter(s -> s.getAssignedByDoctor() != null && s.getAssignedByDoctor().getId().equals(doctor.getId()))
                .toList();
        }

        LOG.info("AUDIT: User '{}' accessed psychometrics for patient ID {}", login, patientId);
        return ResponseEntity.ok(list);
    }

    /**
     * {@code GET /admin/psychometrics/tests} : Admin gets all tests with their questions.
     */
    @GetMapping("/admin/psychometrics/tests")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<PsychometricTest>> getAllTestsForAdmin() {
        List<PsychometricTest> list = testRepository.findAllWithQuestions();
        return ResponseEntity.ok(list);
    }

    /**
     * {@code GET /admin/psychometrics/stats} : Admin gets psychometrics analytics stats.
     */
    @GetMapping("/admin/psychometrics/stats")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> getPsychometricStats() {
        List<PsychometricTest> allTests = testRepository.findAll();
        long totalTests = allTests.size();
        long activeTests = allTests.stream().filter(t -> Boolean.TRUE.equals(t.getIsActive())).count();
        long totalQuestions = questionRepository.count();
        long totalSubmissions = submissionRepository.count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalTests", totalTests);
        stats.put("activeTests", activeTests);
        stats.put("inactiveTests", totalTests - activeTests);
        stats.put("totalQuestions", totalQuestions);
        stats.put("totalSubmissions", totalSubmissions);
        return ResponseEntity.ok(stats);
    }

    /**
     * {@code POST /admin/psychometrics/tests} : Admin creates a new test and its questions.
     */
    @PostMapping("/admin/psychometrics/tests")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<PsychometricTest> createTestForAdmin(@Valid @RequestBody PsychometricTest test) throws URISyntaxException {
        if (test.getId() != null) {
            throw new BadRequestAlertException("A new test cannot already have an ID", ENTITY_NAME, "idexists");
        }
        if (test.getQuestions() != null) {
            for (PsychometricQuestion q : test.getQuestions()) {
                q.setTest(test);
            }
        }
        PsychometricTest result = testRepository.save(test);
        return ResponseEntity.created(new URI("/api/admin/psychometrics/tests/" + result.getId())).body(result);
    }

    /**
     * {@code PUT /admin/psychometrics/tests/:id} : Admin updates a test and questions.
     */
    @PutMapping("/admin/psychometrics/tests/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<PsychometricTest> updateTestForAdmin(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody PsychometricTest test
    ) {
        if (test.getId() == null || !Objects.equals(id, test.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }
        if (!testRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        PsychometricTest existing = testRepository.findOneWithQuestions(id)
            .orElseThrow(() -> new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound"));

        existing.setTitle(test.getTitle());
        existing.setDescription(test.getDescription());
        existing.setTestType(test.getTestType());
        existing.setCategory(test.getCategory());
        existing.setPurpose(test.getPurpose());
        existing.setScoreRangesJson(test.getScoreRangesJson());
        existing.setSourceUrl(test.getSourceUrl());
        existing.setIsActive(test.getIsActive());

        if (test.getQuestions() != null) {
            existing.getQuestions().clear();
            for (PsychometricQuestion q : test.getQuestions()) {
                q.setTest(existing);
                existing.getQuestions().add(q);
            }
        }

        PsychometricTest result = testRepository.save(existing);
        return ResponseEntity.ok(result);
    }

    /**
     * {@code PATCH /admin/psychometrics/tests/:id/toggle-active} : Toggle test active status.
     */
    @PatchMapping("/admin/psychometrics/tests/{id}/toggle-active")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<PsychometricTest> toggleTestActive(@PathVariable("id") Long id) {
        PsychometricTest test = testRepository.findById(id)
            .orElseThrow(() -> new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound"));

        test.setIsActive(!Boolean.TRUE.equals(test.getIsActive()));
        PsychometricTest result = testRepository.save(test);
        return ResponseEntity.ok(result);
    }

    /**
     * {@code DELETE /admin/psychometrics/tests/:id} : Admin deletes a test.
     */
    @DeleteMapping("/admin/psychometrics/tests/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> deleteTestForAdmin(@PathVariable("id") Long id) {
        if (!testRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }
        testRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
