package com.ocbs.service;

import com.ocbs.domain.UserEventOutbox;
import com.ocbs.event.UserEvent;
import com.ocbs.repository.UserEventOutboxRepository;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
@ConditionalOnProperty(name = "application.kafka.enabled", havingValue = "true")
public class UserEventOutboxRelay {
    private static final Logger LOG = LoggerFactory.getLogger(UserEventOutboxRelay.class);
    private final UserEventOutboxRepository repository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    @Value("${application.kafka.enabled:false}") private boolean enabled;
    @Value("${application.outbox.max-retries:8}") private int maxRetries;

    public UserEventOutboxRelay(UserEventOutboxRepository repository, KafkaTemplate<String, Object> kafkaTemplate, ObjectMapper objectMapper) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${application.outbox.relay-interval-ms:1000}")
    public void relay() {
        if (!enabled) return;
        for (UserEventOutbox row : repository.findReady(Instant.now(), maxRetries, PageRequest.of(0, 50))) {
            try {
                UserEvent event = objectMapper.readValue(row.getPayload(), UserEvent.class);
                kafkaTemplate.send(com.ocbs.event.UserEventProducer.USER_TOPIC, row.getAggregateKey(), event).get(5, TimeUnit.SECONDS);
                row.setStatus("SENT");
                row.setSentAt(Instant.now());
                row.setNextRetryAt(null);
                row.setErrorMessage(null);
                repository.save(row);
            } catch (Exception ex) {
                int attempt = row.getRetryCount() + 1;
                row.setRetryCount(attempt);
                row.setStatus("FAILED");
                row.setNextRetryAt(attempt >= maxRetries ? null : Instant.now().plusSeconds(Math.min(300, 1L << Math.min(attempt, 8))));
                String message = ex.getMessage();
                row.setErrorMessage(message != null && message.length() > 950 ? message.substring(0, 950) : message);
                repository.save(row);
                LOG.error("User event outbox publish failed (eventId={}, attempt={}/{}).", row.getEventId(), attempt, maxRetries, ex);
            }
        }
    }
}
