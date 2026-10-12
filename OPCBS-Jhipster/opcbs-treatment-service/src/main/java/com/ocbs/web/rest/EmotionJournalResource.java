package com.ocbs.web.rest;

import com.ocbs.domain.DoctorProfile;
import com.ocbs.domain.EmotionJournal;
import com.ocbs.domain.PatientProfile;
import com.ocbs.repository.DoctorProfileRepository;
import com.ocbs.repository.EmotionJournalRepository;
import com.ocbs.repository.PatientProfileRepository;
import com.ocbs.security.SecurityUtils;
import com.ocbs.web.rest.errors.BadRequestAlertException;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@Transactional
public class EmotionJournalResource {

    private static final Logger LOG = LoggerFactory.getLogger(EmotionJournalResource.class);
    private static final String ENTITY_NAME = "emotionJournal";

    private final EmotionJournalRepository emotionJournalRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final DoctorProfileRepository doctorProfileRepository;

    public EmotionJournalResource(
        EmotionJournalRepository emotionJournalRepository,
        PatientProfileRepository patientProfileRepository,
        DoctorProfileRepository doctorProfileRepository
    ) {
        this.emotionJournalRepository = emotionJournalRepository;
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

    @GetMapping("/patient/journals")
    @PreAuthorize("hasAuthority('ROLE_PATIENT')")
    public ResponseEntity<List<EmotionJournal>> getMyJournals() {
        PatientProfile patient = getCurrentPatient();
        List<EmotionJournal> list = emotionJournalRepository.findAllByPatientId(patient.getId());
        return ResponseEntity.ok(list);
    }

    @PostMapping("/patient/journals")
    @PreAuthorize("hasAuthority('ROLE_PATIENT')")
    public ResponseEntity<EmotionJournal> createJournal(@Valid @RequestBody EmotionJournal journal) throws URISyntaxException {
        if (journal.getId() != null) {
            throw new BadRequestAlertException("A new journal cannot already have an ID", ENTITY_NAME, "idexists");
        }
        PatientProfile patient = getCurrentPatient();
        journal.setPatient(patient);
        if (journal.getIsShared() == null) {
            journal.setIsShared(false);
        }
        EmotionJournal result = emotionJournalRepository.save(journal);
        return ResponseEntity.created(new URI("/api/patient/journals/" + result.getId())).body(result);
    }

    @PutMapping("/patient/journals/{id}")
    @PreAuthorize("hasAuthority('ROLE_PATIENT')")
    public ResponseEntity<EmotionJournal> updateJournal(
        @PathVariable("id") Long id,
        @Valid @RequestBody EmotionJournal journal
    ) {
        PatientProfile patient = getCurrentPatient();
        EmotionJournal existing = emotionJournalRepository
            .findById(id)
            .orElseThrow(() -> new BadRequestAlertException("Journal not found", ENTITY_NAME, "idnotfound"));

        if (!existing.getPatient().getId().equals(patient.getId())) {
            throw new BadRequestAlertException("Unauthorized to modify this journal", ENTITY_NAME, "forbidden");
        }

        existing.setTitle(journal.getTitle());
        existing.setContent(journal.getContent());
        existing.setMoodScale(journal.getMoodScale());
        existing.setStressScale(journal.getStressScale());
        existing.setSleepHours(journal.getSleepHours());
        existing.setDepressionScale(journal.getDepressionScale());
        existing.setIsShared(journal.getIsShared() != null ? journal.getIsShared() : false);

        EmotionJournal result = emotionJournalRepository.save(existing);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/patient/journals/{id}")
    @PreAuthorize("hasAuthority('ROLE_PATIENT')")
    public ResponseEntity<Void> deleteJournal(@PathVariable("id") Long id) {
        PatientProfile patient = getCurrentPatient();
        EmotionJournal existing = emotionJournalRepository
            .findById(id)
            .orElseThrow(() -> new BadRequestAlertException("Journal not found", ENTITY_NAME, "idnotfound"));

        if (!existing.getPatient().getId().equals(patient.getId())) {
            throw new BadRequestAlertException("Unauthorized to delete this journal", ENTITY_NAME, "forbidden");
        }

        emotionJournalRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/patient/journals/stats")
    @PreAuthorize("hasAuthority('ROLE_PATIENT')")
    public ResponseEntity<Map<String, Object>> getJournalStats() {
        PatientProfile patient = getCurrentPatient();
        List<EmotionJournal> list = emotionJournalRepository.findAllByPatientId(patient.getId());

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalEntries", list.size());

        if (list.isEmpty()) {
            stats.put("averageMood", 0);
            stats.put("averageStress", 0);
            stats.put("averageSleepHours", 0);
            stats.put("moodDistribution", Collections.emptyMap());
            return ResponseEntity.ok(stats);
        }

        double sumMood = 0;
        double sumStress = 0;
        double sumSleep = 0;
        int sleepCount = 0;
        Map<Integer, Integer> moodCounts = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            moodCounts.put(i, 0);
        }

        for (EmotionJournal ej : list) {
            if (ej.getMoodScale() != null) {
                sumMood += ej.getMoodScale();
                moodCounts.put(ej.getMoodScale(), moodCounts.getOrDefault(ej.getMoodScale(), 0) + 1);
            }
            if (ej.getStressScale() != null) {
                sumStress += ej.getStressScale();
            }
            if (ej.getSleepHours() != null) {
                sumSleep += ej.getSleepHours().doubleValue();
                sleepCount++;
            }
        }

        stats.put("averageMood", Math.round((sumMood / list.size()) * 10.0) / 10.0);
        stats.put("averageStress", Math.round((sumStress / list.size()) * 10.0) / 10.0);
        stats.put("averageSleepHours", sleepCount > 0 ? Math.round((sumSleep / sleepCount) * 10.0) / 10.0 : 0);
        stats.put("moodDistribution", moodCounts);

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/doctor/patients/{patientId}/shared-journals")
    @PreAuthorize("hasAuthority('ROLE_DOCTOR')")
    public ResponseEntity<List<EmotionJournal>> getSharedJournalsForDoctor(@PathVariable("patientId") Long patientId) {
        // Doctor views shared journals of patient
        List<EmotionJournal> list = emotionJournalRepository.findSharedByPatientId(patientId);
        return ResponseEntity.ok(list);
    }
}
