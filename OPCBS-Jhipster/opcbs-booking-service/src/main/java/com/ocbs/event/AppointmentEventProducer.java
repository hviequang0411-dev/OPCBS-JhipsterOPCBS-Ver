package com.ocbs.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Service to publish appointment events.
 * Supports real Apache Kafka broker publishing when available,
 * and gracefully records events in Standalone Mode when the broker is not configured.
 */
@Service
public class AppointmentEventProducer {

    private static final Logger LOG = LoggerFactory.getLogger(AppointmentEventProducer.class);
    public static final String APPOINTMENT_TOPIC = "opcbs-appointment-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public AppointmentEventProducer(@Autowired(required = false) KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        if (kafkaTemplate != null) {
            LOG.info("🚀 [Kafka Producer] KafkaTemplate is ACTIVE. Real event publishing enabled for topic '{}'", APPOINTMENT_TOPIC);
        } else {
            LOG.info("ℹ️ [Event Dispatcher] Operating in Standalone Mode (Kafka broker is not active). Events are logged internally without network publication.");
        }
    }

    public void publishAppointmentEvent(AppointmentEvent event) {
        if (org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()) {
            LOG.debug("Enqueuing AppointmentEvent dispatch after database transaction commit: BookingCode='{}'", event.getBookingCode());
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        dispatch(event);
                    }
                }
            );
        } else {
            dispatch(event);
        }
    }

    private void dispatch(AppointmentEvent event) {
        if (kafkaTemplate != null) {
            try {
                kafkaTemplate.send(APPOINTMENT_TOPIC, event.getBookingCode(), event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            LOG.error("❌ [Kafka Producer ERROR] Failed to send event to Kafka broker topic '{}': {}", APPOINTMENT_TOPIC, ex.getMessage(), ex);
                        } else {
                            LOG.info("🚀 [Kafka Producer CONFIRMED] Event acknowledged by broker. Topic='{}', Partition={}, Offset={}, BookingCode='{}'",
                                APPOINTMENT_TOPIC, result.getRecordMetadata().partition(), result.getRecordMetadata().offset(), event.getBookingCode());
                        }
                    });
            } catch (Exception ex) {
                LOG.error("❌ [Kafka Producer ERROR] Synchronous failure initiating Kafka send: {}", ex.getMessage(), ex);
            }
        } else {
            // Standalone mode: Log clearly that no broker is published to
            LOG.info("📋 [Event Dispatcher - Standalone Mode] Recorded AppointmentEvent (broker disabled): Type='{}', Code='{}', Doctor='{}', Patient='{}'",
                event.getEventType(), event.getBookingCode(), event.getDoctorName(), event.getPatientName());
        }
    }
}
