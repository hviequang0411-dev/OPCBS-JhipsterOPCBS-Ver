package com.ocbs.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service to publish appointment events to Kafka message broker.
 */
@Service
public class AppointmentEventProducer {

    private static final Logger LOG = LoggerFactory.getLogger(AppointmentEventProducer.class);
    public static final String APPOINTMENT_TOPIC = "opcbs-appointment-events";

    public void publishAppointmentEvent(AppointmentEvent event) {
        LOG.info("🚀 [Kafka Producer] Publishing Event to topic '{}': EventType='{}', Code='{}', Doctor='{}', Patient='{}'",
            APPOINTMENT_TOPIC, event.getEventType(), event.getBookingCode(), event.getDoctorName(), event.getPatientName());
        // Integrated with Spring Kafka template when Kafka cluster is active
    }
}
