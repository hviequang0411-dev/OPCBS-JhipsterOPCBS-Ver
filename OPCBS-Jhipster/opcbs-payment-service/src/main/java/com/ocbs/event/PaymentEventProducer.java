package com.ocbs.event;

import com.ocbs.domain.PaymentEventOutbox;
import com.ocbs.repository.PaymentEventOutboxRepository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Persists payment events with payment state; a relay publishes only committed outbox rows. */
@Service
public class PaymentEventProducer {
    private static final Logger LOG = LoggerFactory.getLogger(PaymentEventProducer.class);
    public static final String PAYMENT_TOPIC = "opcbs-payment-events";

    private final PaymentEventOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public PaymentEventProducer(PaymentEventOutboxRepository outboxRepository, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void publishPaymentEvent(PaymentEvent event) {
        if (event == null || event.getEventType() == null || event.getSubscriptionId() == null) {
            throw new IllegalArgumentException("Payment event requires eventType and subscriptionId");
        }
        try {
            PaymentEventOutbox outbox = new PaymentEventOutbox();
            outbox.setEventId(event.getEventId());
            outbox.setEventType(event.getEventType());
            outbox.setAggregateKey(String.valueOf(event.getSubscriptionId()));
            outbox.setPayload(objectMapper.writeValueAsString(event));
            outbox.setStatus("PENDING");
            outboxRepository.save(outbox);
            LOG.info("📦 [Payment Event Outbox] Stored eventId='{}', type='{}' in payment transaction",
                event.getEventId(), event.getEventType());
        } catch (JacksonException ex) {
            throw new IllegalStateException("Could not serialize payment domain event", ex);
        }
    }
}
