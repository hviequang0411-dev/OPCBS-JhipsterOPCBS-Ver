package com.ocbs.service;

import com.ocbs.domain.OutboxEvent;
import com.ocbs.domain.enumeration.OutboxStatus;
import com.ocbs.event.AppointmentEvent;
import com.ocbs.repository.OutboxEventRepository;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * Background Relay Worker to publish PENDING/FAILED events from the Outbox table
 * to Apache Kafka.
 *
 * Design constraints:
 * - Does NOT hold database transaction open while waiting for network Kafka ACK.
 * - Marks event as SENT ONLY AFTER explicit broker confirmation.
 * - Exponential backoff with configurable upper limit, preventing retry storms.
 * - Only queries events whose next_retry_at has arrived.
 * - Failed events exceeding maxRetries remain in FAILED status for operator investigation and safe replay.
 * - Continues dispatching remaining events in the batch even if one event fails.
 */
@Service
public class OutboxRelayService {

    private static final Logger LOG = LoggerFactory.getLogger(OutboxRelayService.class);
    public static final String APPOINTMENT_TOPIC = "opcbs-appointment-events";

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${application.kafka.enabled:false}")
    private boolean kafkaEnabled;

    @Value("${application.outbox.max-retries:5}")
    private int maxRetries;

    @Value("${application.outbox.batch-size:50}")
    private int batchSize;

    @Value("${application.outbox.initial-backoff-ms:1000}")
    private long initialBackoffMs;

    @Value("${application.outbox.max-backoff-ms:60000}")
    private long maxBackoffMs;

    @Value("${application.outbox.backoff-multiplier:2.0}")
    private double backoffMultiplier;

    public OutboxRelayService(
        OutboxEventRepository outboxEventRepository,
        @Autowired(required = false) KafkaTemplate<String, Object> kafkaTemplate,
        ObjectMapper objectMapper
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${application.outbox.relay-interval-ms:2000}")
    public void relayPendingEvents() {
        if (!kafkaEnabled || kafkaTemplate == null) {
            return; // Standalone or test mode: do not poll when Kafka is disabled
        }

        Instant now = Instant.now();

        // 1. Backlog & Poisoned Event Monitoring
        long deadLetterCount = outboxEventRepository.countExceededMaxRetries(maxRetries);
        if (deadLetterCount > 0) {
            LOG.warn("⚠️ [Outbox Backlog] Found {} outbox events that have exceeded max retries ({}) requiring operator review.",
                deadLetterCount, maxRetries);
        }

        // 2. Fetch only events ready to dispatch (PENDING or FAILED where nextRetryAt <= now)
        List<OutboxEvent> readyEvents = outboxEventRepository.findReadyToDispatchEvents(
            now,
            maxRetries,
            PageRequest.of(0, batchSize)
        );

        if (readyEvents.isEmpty()) {
            return;
        }

        LOG.debug("🔄 [Outbox Relay] Found {} ready outbox events to dispatch to Kafka.", readyEvents.size());

        for (OutboxEvent outbox : readyEvents) {
            try {
                dispatchSingleOutboxEvent(outbox);
            } catch (Exception e) {
                LOG.error("❌ [Outbox Relay Unexpected] Error processing event ID '{}': {}", outbox.getEventId(), e.getMessage());
            }
        }
    }

    /**
     * Dispatches a single outbox record to Kafka without holding an open DB transaction.
     */
    public void dispatchSingleOutboxEvent(OutboxEvent outbox) {
        try {
            AppointmentEvent event = objectMapper.readValue(outbox.getPayload(), AppointmentEvent.class);
            String partitionKey = String.valueOf(event.getAppointmentId());

            LOG.debug("🚀 [Outbox Relay] Sending Event ID '{}' (Key: '{}') to topic '{}'...",
                event.getEventId(), partitionKey, APPOINTMENT_TOPIC);

            // Synchronously wait for broker acknowledgement with a 5-second timeout
            SendResult<String, Object> sendResult = kafkaTemplate.send(APPOINTMENT_TOPIC, partitionKey, event)
                .get(5, TimeUnit.SECONDS);

            // Mark as SENT only after explicit broker ACK
            markAsSent(outbox.getId(), sendResult.getRecordMetadata().partition(), sendResult.getRecordMetadata().offset());

            LOG.info("✅ [Outbox Relay CONFIRMED] Event '{}' acknowledged by Kafka: Topic='{}', Partition={}, Offset={}",
                event.getEventId(), APPOINTMENT_TOPIC, sendResult.getRecordMetadata().partition(), sendResult.getRecordMetadata().offset());

        } catch (Exception ex) {
            String errorMsg = ex.getMessage();
            if (errorMsg != null && errorMsg.length() > 950) {
                errorMsg = errorMsg.substring(0, 950) + "...";
            }
            LOG.warn("⚠️ [Outbox Relay RETRY] Failed to dispatch Event ID '{}': {}", outbox.getEventId(), errorMsg);
            markAsFailed(outbox.getId(), errorMsg);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markAsSent(Long outboxId, int partition, long offset) {
        outboxEventRepository.findById(outboxId).ifPresent(outbox -> {
            outbox.setStatus(OutboxStatus.SENT);
            outbox.setSentAt(Instant.now());
            outbox.setNextRetryAt(null);
            outboxEventRepository.save(outbox);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markAsFailed(Long outboxId, String errorMessage) {
        outboxEventRepository.findById(outboxId).ifPresent(outbox -> {
            int retries = outbox.getRetryCount() != null ? outbox.getRetryCount() + 1 : 1;
            outbox.setRetryCount(retries);
            Instant now = Instant.now();
            outbox.setLastRetryAt(now);
            outbox.setErrorMessage(errorMessage);
            outbox.setStatus(OutboxStatus.FAILED);

            // Calculate exponential backoff capped by maxBackoffMs
            long backoffMs = calculateBackoffMs(retries);
            outbox.setNextRetryAt(now.plusMillis(backoffMs));

            if (retries >= maxRetries) {
                LOG.error("🚨 [Outbox Max Retries Exceeded] Event ID '{}' (Appt #{}) has failed {} times. Kept as FAILED for operator inspection.",
                    outbox.getEventId(), outbox.getAggregateId(), retries);
            } else {
                LOG.info("⏳ [Outbox Backoff] Scheduled retry #{} for Event ID '{}' in {}ms (at {})",
                    retries, outbox.getEventId(), backoffMs, outbox.getNextRetryAt());
            }

            outboxEventRepository.save(outbox);
        });
    }

    /**
     * Operator reconciliation helper: Resets a failed outbox event for replay.
     * Retains original eventId and idempotencyKey to prevent duplicate side-effects on consumers.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void resetForReplay(Long outboxId) {
        outboxEventRepository.findById(outboxId).ifPresent(outbox -> {
            outbox.setStatus(OutboxStatus.PENDING);
            outbox.setRetryCount(0);
            outbox.setNextRetryAt(null);
            outbox.setErrorMessage(null);
            outboxEventRepository.save(outbox);
            LOG.info("🔄 [Outbox Operator] Event ID '{}' reset to PENDING for manual replay.", outbox.getEventId());
        });
    }

    public long calculateBackoffMs(int retryCount) {
        if (retryCount <= 1) {
            return initialBackoffMs;
        }
        double rawBackoff = initialBackoffMs * Math.pow(backoffMultiplier, retryCount - 1);
        return Math.min(maxBackoffMs, (long) rawBackoff);
    }

    // Setters for unit testing configuration
    public void setKafkaEnabled(boolean kafkaEnabled) { this.kafkaEnabled = kafkaEnabled; }
    public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
    public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
    public void setInitialBackoffMs(long initialBackoffMs) { this.initialBackoffMs = initialBackoffMs; }
    public void setMaxBackoffMs(long maxBackoffMs) { this.maxBackoffMs = maxBackoffMs; }
    public void setBackoffMultiplier(double backoffMultiplier) { this.backoffMultiplier = backoffMultiplier; }
}
