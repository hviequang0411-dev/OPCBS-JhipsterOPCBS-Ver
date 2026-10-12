package com.ocbs.event;

import com.ocbs.domain.TreatmentPackage;
import com.ocbs.domain.enumeration.TreatmentPackageStatus;
import com.ocbs.repository.TreatmentPackageRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Kafka Consumer in Treatment Service.
 * Listens to appointment events and automatically deducts remaining sessions from patient's active treatment package.
 */
@Service
public class AppointmentEventConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(AppointmentEventConsumer.class);

    private final TreatmentPackageRepository treatmentPackageRepository;

    public AppointmentEventConsumer(TreatmentPackageRepository treatmentPackageRepository) {
        this.treatmentPackageRepository = treatmentPackageRepository;
    }

    @KafkaListener(topics = "opcbs-appointment-events", groupId = "treatment-service-group")
    @Transactional
    public void handleAppointmentEvent(AppointmentEvent event) {
        LOG.info("📥 [Treatment Service Kafka] Received AppointmentEvent: Type='{}', BookingCode='{}', PatientId={}, DoctorId={}",
            event.getEventType(), event.getBookingCode(), event.getPatientId(), event.getDoctorId());

        if ("APPOINTMENT_BOOKED".equalsIgnoreCase(event.getEventType()) || "APPOINTMENT_COMPLETED".equalsIgnoreCase(event.getEventType())) {
            if (event.getPatientId() != null) {
                deductPackageSession(event.getPatientId(), event.getDoctorId(), event.getBookingCode());
            }
        }
    }

    /**
     * Deduct 1 session from patient's active treatment package.
     * Prioritizes package with the specific doctor, falls back to any active package for the patient.
     */
    public void deductPackageSession(Long patientId, Long doctorId, String bookingCode) {
        List<TreatmentPackage> activePackages = null;
        if (doctorId != null) {
            activePackages = treatmentPackageRepository.findActivePackagesByPatientAndDoctor(patientId, doctorId);
        }
        if (activePackages == null || activePackages.isEmpty()) {
            activePackages = treatmentPackageRepository.findActivePackagesByPatient(patientId);
        }

        if (activePackages != null && !activePackages.isEmpty()) {
            TreatmentPackage targetPackage = activePackages.get(0);
            int currentRemaining = targetPackage.getRemainingSessions() != null ? targetPackage.getRemainingSessions() : 0;

            if (currentRemaining > 0) {
                int newRemaining = currentRemaining - 1;
                targetPackage.setRemainingSessions(newRemaining);
                LOG.info("⚡ [Treatment Package] Deducted 1 session for Package ID: {}, Name: '{}', BookingCode: '{}'. Remaining: {}/{}",
                    targetPackage.getId(), targetPackage.getName(), bookingCode, newRemaining, targetPackage.getSessionQuantity());

                if (newRemaining <= 0) {
                    targetPackage.setStatus(TreatmentPackageStatus.COMPLETED);
                    LOG.info("🏁 [Treatment Package] Package ID: {} has 0 sessions remaining. Marked as COMPLETED!", targetPackage.getId());
                }

                treatmentPackageRepository.save(targetPackage);
            }
        } else {
            LOG.debug("ℹ️ [Treatment Package] No active package found for Patient ID: {} to deduct session.", patientId);
        }
    }
}
