package com.ocbs.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service to consume and process appointment events asynchronously from Kafka.
 */
@Service
public class AppointmentEventConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(AppointmentEventConsumer.class);

    public void handleAppointmentEvent(AppointmentEvent event) {
        LOG.info("📥 [Kafka Consumer] Received Event from topic '{}': EventType='{}', AppointmentId={}, Patient='{}'",
            "opcbs-appointment-events", event.getEventType(), event.getAppointmentId(), event.getPatientName());
        
        switch (event.getEventType()) {
            case "APPOINTMENT_BOOKED" -> LOG.info("📋 [Clinical Service] Prepared preliminary intake chart for appointment {}", event.getBookingCode());
            case "APPOINTMENT_ACCEPTED" -> LOG.info("✅ [Clinical Service] Appointment {} confirmed by doctor. Ready for clinical consultation.", event.getBookingCode());
            case "APPOINTMENT_COMPLETED" -> LOG.info("🎉 [Clinical Service] Consultation finished. Medical records finalized for patient ID {}", event.getPatientId());
            default -> LOG.debug("Unhandled event type: {}", event.getEventType());
        }
    }
}
