package com.ocbs.event;

import com.ocbs.domain.UserEventOutbox;
import com.ocbs.repository.UserEventOutboxRepository;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/** Records account events transactionally; the relay publishes them after commit. */
@Service
public class UserEventProducer {
    private static final Logger LOG = LoggerFactory.getLogger(UserEventProducer.class);
    public static final String USER_TOPIC = "opcbs-user-events";

    private final UserEventOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public UserEventProducer(UserEventOutboxRepository outboxRepository, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void publishUserEvent(UserEvent event) {
        if (event == null || event.getUserId() == null || event.getEventType() == null) {
            throw new IllegalArgumentException("User event requires userId and eventType");
        }
        try {
            UserEventOutbox outbox = new UserEventOutbox();
            outbox.setEventId(event.getEventId());
            outbox.setEventType(event.getEventType());
            outbox.setAggregateKey(String.valueOf(event.getUserId()));
            outbox.setPayload(objectMapper.writeValueAsString(event));
            outbox.setStatus("PENDING");
            outbox.setCreatedAt(Instant.now());
            outboxRepository.save(outbox);
            LOG.info("📦 [User Event Outbox] Stored eventId='{}', type='{}' in account transaction",
                event.getEventId(), event.getEventType());
        } catch (JacksonException ex) {
            throw new IllegalStateException("Could not serialize user domain event", ex);
        }
    }
}
