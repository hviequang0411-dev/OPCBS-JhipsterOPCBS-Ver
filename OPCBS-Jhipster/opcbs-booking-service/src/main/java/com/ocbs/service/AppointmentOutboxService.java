package com.ocbs.service;

import com.ocbs.domain.Appointment;
import com.ocbs.domain.OutboxEvent;
import com.ocbs.domain.enumeration.OutboxStatus;
import com.ocbs.event.AppointmentEvent;
import com.ocbs.repository.OutboxEventRepository;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Service to record domain events into the Transactional Outbox table
 * within the SAME database transaction as the appointment status change.
 */
@Service
public class AppointmentOutboxService {

    private static final Logger LOG = LoggerFactory.getLogger(AppointmentOutboxService.class);

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public AppointmentOutboxService(OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Records an appointment lifecycle event into the outbox table.
     * Guaranteed to commit or rollback atomically with the caller's business transaction.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public OutboxEvent recordAppointmentEvent(
        Appointment appointment,
        String eventType,
        String previousStatus,
        String newStatus,
        String reason
    ) {
        if (appointment == null || appointment.getId() == null) {
            throw new IllegalArgumentException("Appointment and appointment ID must not be null when recording outbox event");
        }

        Long patientId = appointment.getPatient() != null ? appointment.getPatient().getId() : null;
        Long doctorId = appointment.getDoctor() != null ? appointment.getDoctor().getId() : null;

        AppointmentEvent event = new AppointmentEvent(
            appointment.getId(),
            appointment.getBookingCode(),
            patientId,
            doctorId,
            eventType,
            previousStatus,
            newStatus
        );

        try {
            if (appointment.getPatient() != null) {
                event.setPatientName(appointment.getPatient().getFullName());
                if (appointment.getPatient().getUser() != null) {
                    event.setPatientLogin(appointment.getPatient().getUser().getLogin());
                    event.setPatientEmail(appointment.getPatient().getUser().getEmail());
                }
            }
            if (appointment.getDoctor() != null) {
                event.setDoctorName(appointment.getDoctor().getFullName());
                if (appointment.getDoctor().getUser() != null) {
                    event.setDoctorLogin(appointment.getDoctor().getUser().getLogin());
                    event.setDoctorEmail(appointment.getDoctor().getUser().getEmail());
                }
            }
        } catch (Exception ex) {
            LOG.debug("Could not resolve optional participant names/logins/emails for outbox event: {}", ex.getMessage());
        }
        event.setReason(reason);

        String jsonPayload;
        try {
            jsonPayload = objectMapper.writeValueAsString(event);
        } catch (JacksonException e) {
            LOG.error("Failed to serialize AppointmentEvent for appointment #{}: {}", appointment.getId(), e.getMessage());
            throw new IllegalStateException("Failed to serialize appointment event payload", e);
        }

        OutboxEvent outbox = new OutboxEvent();
        outbox.setEventId(event.getEventId());
        outbox.setEventType(event.getEventType());
        outbox.setEventVersion(event.getEventVersion());
        outbox.setAggregateType("APPOINTMENT");
        outbox.setAggregateId(appointment.getId());
        outbox.setIdempotencyKey(event.getIdempotencyKey());
        outbox.setPayload(jsonPayload);
        outbox.setStatus(OutboxStatus.PENDING);
        outbox.setRetryCount(0);
        outbox.setCreatedAt(Instant.now());

        OutboxEvent saved = outboxEventRepository.save(outbox);
        LOG.info("📦 [Transactional Outbox] Enqueued Event ID '{}' (Type: '{}', Appt: #{}, Code: '{}') in DB transaction.",
            saved.getEventId(), saved.getEventType(), saved.getAggregateId(), event.getBookingCode());
        return saved;
    }
}
