package com.ocbs.event;

import com.ocbs.domain.TreatmentInbox;
import com.ocbs.domain.TreatmentPackage;
import com.ocbs.domain.TreatmentSession;
import com.ocbs.domain.enumeration.TreatmentPackageStatus;
import com.ocbs.repository.TreatmentInboxRepository;
import com.ocbs.repository.TreatmentPackageRepository;
import com.ocbs.repository.TreatmentSessionRepository;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Kafka Consumer in Treatment Service.
 * Listens to appointment events and safely deducts remaining sessions from patient's active treatment package.
 *
 * Guaranteed Features:
 * - Persistent Idempotency via TreatmentInbox per (appointmentId, eventType) and eventId.
 * - Strict doctor matching: When event has doctorId, ONLY packages belonging to patientId + doctorId are matched (NO fallback to other doctors).
 * - Safe missing doctorId handling: standalone/ambiguous appointments are not erroneously deducted.
 * - Pessimistic DB Row Locking (SELECT FOR UPDATE) preventing concurrent double deduction or negative remaining sessions.
 * - Schema version validation: Non-supported event versions and invalid event payloads trigger Kafka DLT.
 * - Atomic execution: inbox record and package deduction are committed together.
 */
@Service
public class AppointmentEventConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(AppointmentEventConsumer.class);
    public static final String CONSUMER_GROUP = "treatment-service-group";

    private static final Set<String> SUPPORTED_EVENT_TYPES = Set.of(
        "APPOINTMENT_BOOKED",
        "APPOINTMENT_CONFIRMED",
        "APPOINTMENT_ACCEPTED",
        "APPOINTMENT_STARTED",
        "APPOINTMENT_REJECTED",
        "APPOINTMENT_CANCELLED",
        "APPOINTMENT_COMPLETED"
    );

    private final TreatmentPackageRepository treatmentPackageRepository;
    private final TreatmentSessionRepository treatmentSessionRepository;
    private final TreatmentInboxRepository treatmentInboxRepository;

    public AppointmentEventConsumer(
        TreatmentPackageRepository treatmentPackageRepository,
        TreatmentSessionRepository treatmentSessionRepository,
        TreatmentInboxRepository treatmentInboxRepository
    ) {
        this.treatmentPackageRepository = treatmentPackageRepository;
        this.treatmentSessionRepository = treatmentSessionRepository;
        this.treatmentInboxRepository = treatmentInboxRepository;
    }

    @KafkaListener(
        topics = "opcbs-appointment-events",
        groupId = CONSUMER_GROUP,
        autoStartup = "${application.kafka.enabled:false}"
    )
    @Transactional
    public void handleAppointmentEvent(AppointmentEvent event) {
        // 1. Strict Validation: Null check & Schema contract validation
        validateEvent(event);

        LOG.info("📥 [Treatment Service Kafka] Received Event: Type='{}', ID='{}', Version='{}', BookingCode='{}', PatientId={}, DoctorId={}, ApptId={}",
            event.getEventType(), event.getEventId(), event.getEventVersion(), event.getBookingCode(),
            event.getPatientId(), event.getDoctorId(), event.getAppointmentId());

        String eventType = event.getEventType().toUpperCase();
        Long appointmentId = event.getAppointmentId();

        // 2. Business Filter: Deduct sessions ONLY for completed appointments
        if (!"APPOINTMENT_COMPLETED".equals(eventType)) {
            // Check idempotency for this non-completion event
            if (treatmentInboxRepository.existsByEventId(event.getEventId()) ||
                treatmentInboxRepository.existsByAppointmentIdAndEventType(appointmentId, eventType)) {
                LOG.info("⚡ [Treatment Service Idempotency] Non-completion event ID '{}' (Type: '{}') already recorded. Skipping.",
                    event.getEventId(), eventType);
                return;
            }

            // Intentionally skip session deduction for non-completion event and record inbox
            recordInbox(event, eventType, "SKIPPED_NOT_COMPLETED");
            LOG.info("ℹ️ [Treatment Service] Event type '{}' is valid but intentionally not applicable for package session deduction (Appt #{}, Event ID '{}'). Recorded inbox.",
                eventType, appointmentId, event.getEventId());
            return;
        }

        // 3. APPOINTMENT_COMPLETED specific validation
        if (event.getPatientId() == null) {
            LOG.error("❌ [Treatment Service] APPOINTMENT_COMPLETED event is missing mandatory patientId. Forwarding to DLT (Event ID: '{}', Appt: #{})",
                event.getEventId(), appointmentId);
            throw new IllegalArgumentException("APPOINTMENT_COMPLETED event is missing mandatory patientId for appointment #" + appointmentId);
        }

        // 4. Persistent Idempotency Check via TreatmentInbox & TreatmentSession
        if (treatmentInboxRepository.existsByAppointmentIdAndEventType(appointmentId, "APPOINTMENT_COMPLETED")) {
            LOG.info("⚡ [Treatment Service Idempotency] Appointment ID {} already processed completion in inbox. Idempotent skip.", appointmentId);
            return;
        }

        if (event.getEventId() != null && treatmentInboxRepository.existsByEventId(event.getEventId())) {
            LOG.info("⚡ [Treatment Service Idempotency] Event ID '{}' already recorded in inbox. Idempotent skip.", event.getEventId());
            return;
        }

        if (treatmentSessionRepository.existsByAppointmentId(appointmentId)) {
            LOG.info("⚡ [Treatment Service Idempotency] Appointment ID {} already linked to completed session. Recording inbox and skipping deduction.",
                appointmentId);
            recordInbox(event, "APPOINTMENT_COMPLETED", "PROCESSED_ALREADY_LINKED");
            return;
        }

        // 5. Perform Safe Deduction with Strict Doctor Matching & Pessimistic Locking
        deductPackageSessionWithLock(event);
    }

    /**
     * Deducts 1 session from patient's active treatment package using pessimistic write lock.
     * Enforces strict matching between patientId and doctorId without cross-doctor fallback.
     */
    private void deductPackageSessionWithLock(AppointmentEvent event) {
        Long patientId = event.getPatientId();
        Long doctorId = event.getDoctorId();
        Long appointmentId = event.getAppointmentId();
        String bookingCode = event.getBookingCode();

        List<TreatmentPackage> candidatePackages = null;
        if (doctorId != null) {
            // STRICT MATCH: Only select package matching patientId and doctorId
            candidatePackages = treatmentPackageRepository.findActivePackagesByPatientAndDoctor(patientId, doctorId);
            // CRITICAL: DO NOT fallback to packages of another doctor
        } else {
            // When doctorId is missing, check if there's an unambiguous clinic-wide package
            List<TreatmentPackage> patientPackages = treatmentPackageRepository.findActivePackagesByPatient(patientId);
            if (patientPackages != null && patientPackages.size() == 1 && patientPackages.get(0).getDoctor() == null) {
                candidatePackages = patientPackages;
            } else {
                LOG.warn("⚠️ [Treatment Package] Event for appointment #{} is missing doctorId and package cannot be safely determined. Skipping deduction (Event ID: '{}').",
                    appointmentId, event.getEventId());
                recordInbox(event, "APPOINTMENT_COMPLETED", "SKIPPED_AMBIGUOUS_DOCTOR");
                return;
            }
        }

        if (candidatePackages != null && !candidatePackages.isEmpty()) {
            TreatmentPackage candidate = candidatePackages.get(0);

            // Acquire Pessimistic Write Lock on the target package row
            TreatmentPackage targetPackage = treatmentPackageRepository.findByIdForUpdate(candidate.getId())
                .orElse(candidate);

            int currentRemaining = targetPackage.getRemainingSessions() != null ? targetPackage.getRemainingSessions() : 0;

            if (currentRemaining > 0 && targetPackage.getStatus() == TreatmentPackageStatus.ACTIVE) {
                // Check expiration
                if (targetPackage.getExpirationDate() != null && targetPackage.getExpirationDate().isBefore(Instant.now())) {
                    LOG.warn("⚠️ [Treatment Package] Package ID #{} has expired at {}. Marking EXPIRED.",
                        targetPackage.getId(), targetPackage.getExpirationDate());
                    targetPackage.setStatus(TreatmentPackageStatus.EXPIRED);
                    treatmentPackageRepository.save(targetPackage);
                    recordInbox(event, "APPOINTMENT_COMPLETED", "SKIPPED_PACKAGE_EXPIRED");
                    return;
                }

                int newRemaining = currentRemaining - 1;
                targetPackage.setRemainingSessions(newRemaining);
                LOG.info("⚡ [Treatment Package] Deducted 1 session for Package ID: {}, Name: '{}', BookingCode: '{}'. Remaining: {}/{}",
                    targetPackage.getId(), targetPackage.getName(), bookingCode, newRemaining, targetPackage.getSessionQuantity());

                if (newRemaining <= 0) {
                    targetPackage.setStatus(TreatmentPackageStatus.COMPLETED);
                    LOG.info("🏁 [Treatment Package] Package ID: {} has 0 sessions remaining. Marked as COMPLETED!", targetPackage.getId());
                }

                treatmentPackageRepository.save(targetPackage);

                // Create TreatmentSession record linked to the appointment
                TreatmentSession sessionRecord = new TreatmentSession();
                sessionRecord.setTreatmentPackage(targetPackage);
                sessionRecord.setAppointmentId(appointmentId);
                int totalSessions = targetPackage.getSessionQuantity() != null ? targetPackage.getSessionQuantity() : 1;
                sessionRecord.setSessionNumber(Math.max(1, totalSessions - newRemaining));
                sessionRecord.setTitle("Buổi khám hoàn tất - Mã " + (bookingCode != null ? bookingCode : appointmentId));
                sessionRecord.setStatus("COMPLETED");
                sessionRecord.setCreatedAt(Instant.now());
                sessionRecord.setUpdatedAt(Instant.now());
                treatmentSessionRepository.save(sessionRecord);
                LOG.info("✅ [Treatment Session] Created completed session record for Appointment ID: {}", appointmentId);

                recordInbox(event, "APPOINTMENT_COMPLETED", "DEDUCTED");
            } else {
                LOG.info("ℹ️ [Treatment Package] Package ID #{} has no remaining sessions (current: {}) or inactive status.",
                    targetPackage.getId(), currentRemaining);
                recordInbox(event, "APPOINTMENT_COMPLETED", "SKIPPED_EXHAUSTED_PACKAGE");
            }
        } else {
            LOG.info("ℹ️ [Treatment Package] No active package found for Patient ID: {} and Doctor ID: {} (Event ID: '{}', Appt: #{}). Not applicable for package deduction.",
                patientId, doctorId, event.getEventId(), appointmentId);
            recordInbox(event, "APPOINTMENT_COMPLETED", "SKIPPED_NO_PACKAGE");
        }
    }

    private void recordInbox(AppointmentEvent event, String eventType, String status) {
        String eventId = event.getEventId() != null ? event.getEventId() : java.util.UUID.randomUUID().toString();
        String idempotencyKey = event.getIdempotencyKey() != null
            ? event.getIdempotencyKey()
            : "APPT_" + (event.getBookingCode() != null ? event.getBookingCode() : event.getAppointmentId()) + "_" + eventType;

        TreatmentInbox inbox = new TreatmentInbox(
            eventId,
            event.getAppointmentId(),
            eventType,
            status,
            idempotencyKey,
            CONSUMER_GROUP
        );
        treatmentInboxRepository.save(inbox);
        LOG.info("💾 [Treatment Service] Recorded TreatmentInbox for appointment #{} (Event ID '{}', Type: '{}', Status: '{}')",
            event.getAppointmentId(), eventId, eventType, status);
    }

    private void validateEvent(AppointmentEvent event) {
        if (event == null) {
            LOG.error("❌ [Treatment Service] Received null event record. Triggering DLT.");
            throw new IllegalArgumentException("Received null event record. Cannot process.");
        }

        if (event.getEventId() == null || event.getEventId().isBlank()) {
            LOG.error("❌ [Treatment Service] Event is missing mandatory eventId. Triggering DLT.");
            throw new IllegalArgumentException("Event is missing mandatory eventId");
        }

        String version = event.getEventVersion();
        if (version == null || !version.startsWith("1.")) {
            LOG.error("❌ [Treatment Service] Unsupported event contract version '{}' for event ID '{}'. Will trigger DLT.",
                version, event.getEventId());
            throw new IllegalArgumentException("Unsupported event contract version: " + version + ". Supported versions: 1.x");
        }

        if (event.getEventType() == null || event.getEventType().isBlank()) {
            LOG.error("❌ [Treatment Service] Event ID '{}' is missing mandatory eventType. Triggering DLT.", event.getEventId());
            throw new IllegalArgumentException("Event is missing mandatory eventType");
        }

        if (!SUPPORTED_EVENT_TYPES.contains(event.getEventType().toUpperCase())) {
            LOG.error("❌ [Treatment Service] Unrecognized eventType '{}' for event ID '{}'. Triggering DLT.",
                event.getEventType(), event.getEventId());
            throw new IllegalArgumentException("Unrecognized event type: " + event.getEventType());
        }

        if (event.getAppointmentId() == null) {
            LOG.error("❌ [Treatment Service] Event ID '{}' is missing mandatory appointmentId. Triggering DLT.", event.getEventId());
            throw new IllegalArgumentException("Event is missing mandatory appointmentId");
        }
    }
}
