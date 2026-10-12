package com.ocbs.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service to publish user registration and authentication lifecycle events to Kafka.
 */
@Service
public class UserEventProducer {

    private static final Logger LOG = LoggerFactory.getLogger(UserEventProducer.class);
    public static final String USER_TOPIC = "opcbs-user-events";

    public void publishUserEvent(UserEvent event) {
        LOG.info("🚀 [Kafka Producer] Publishing User Event to topic '{}': EventType='{}', Login='{}', Role='{}', Email='{}'",
            USER_TOPIC, event.getEventType(), event.getLogin(), event.getRole(), event.getEmail());
    }
}
