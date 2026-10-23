package com.ocbs.event;

import com.ocbs.domain.Notification;
import com.ocbs.domain.NotificationInbox;
import com.ocbs.repository.NotificationInboxRepository;
import com.ocbs.repository.NotificationRepository;
import com.ocbs.service.EmailTaskProducer;
import com.ocbs.service.dto.EmailTaskDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Kafka Consumer in Notification Service for User Lifecycle Events.
 * Topic: opcbs-user-events
 * Group: notification-user-group
 */
@Service
public class UserEventConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(UserEventConsumer.class);
    public static final String CONSUMER_GROUP = "notification-user-group";

    private final NotificationRepository notificationRepository;
    private final NotificationInboxRepository inboxRepository;
    private final EmailTaskProducer emailTaskProducer;

    public UserEventConsumer(
        NotificationRepository notificationRepository,
        NotificationInboxRepository inboxRepository,
        EmailTaskProducer emailTaskProducer
    ) {
        this.notificationRepository = notificationRepository;
        this.inboxRepository = inboxRepository;
        this.emailTaskProducer = emailTaskProducer;
    }

    @KafkaListener(
        topics = "opcbs-user-events",
        groupId = CONSUMER_GROUP,
        containerFactory = "userKafkaListenerContainerFactory",
        autoStartup = "${application.kafka.enabled:false}"
    )
    @Transactional
    public void handleUserEvent(UserEvent event) {
        if (event == null || event.getEventId() == null || event.getLogin() == null || event.getEventType() == null ||
            event.getEventVersion() == null || !event.getEventVersion().startsWith("1.")) {
            throw new IllegalArgumentException("User event is null or has an invalid eventId, login, eventType, or version");
        }

        if (!java.util.Set.of("USER_REGISTERED", "USER_ACTIVATED").contains(event.getEventType().toUpperCase(java.util.Locale.ROOT))) {
            throw new IllegalArgumentException("Unsupported user event type: " + event.getEventType());
        }

        LOG.info("📥 [User Notification Kafka] Received: Event ID '{}', Type='{}', Login='{}', Role='{}'",
            event.getEventId(), event.getEventType(), event.getLogin(), event.getRole());

        String idempotencyKey = event.getIdempotencyKey() != null
            ? event.getIdempotencyKey()
            : "USER_" + event.getLogin() + "_" + event.getEventType();

        if (inboxRepository.existsByEventId(event.getEventId()) ||
            inboxRepository.existsByConsumerGroupAndIdempotencyKey(CONSUMER_GROUP, idempotencyKey)) {
            LOG.info("⚡ [User Notification Idempotency] Event '{}' already processed for group '{}'. Skipping duplicate.",
                event.getEventId(), CONSUMER_GROUP);
            return;
        }

        if ("USER_REGISTERED".equalsIgnoreCase(event.getEventType()) || "USER_ACTIVATED".equalsIgnoreCase(event.getEventType())) {
            Notification notif = new Notification();
            notif.setUserLogin(event.getLogin());
            String displayName = event.getFullName() != null && !event.getFullName().isBlank() ? event.getFullName() : event.getLogin();
            boolean activated = "USER_ACTIVATED".equalsIgnoreCase(event.getEventType());
            notif.setTitle(activated ? "🎉 Tài khoản đã được kích hoạt" : "📩 Hoàn tất kích hoạt tài khoản");
            notif.setMessage(activated
                ? "Xin chào " + displayName + ", tài khoản MindBridge của bạn đã được kích hoạt thành công."
                : "Xin chào " + displayName + ", hãy kiểm tra email để hoàn tất kích hoạt tài khoản MindBridge.");
            notif.setNotificationType("SYSTEM");
            notif.setRelatedEntityId(event.getUserId());
            notif.setRelatedEntityType("User");
            notif.setActionUrl("/account/profile");
            notif.setChannel("IN_APP");
            notif.setDeliveryStatus("DELIVERED");
            notificationRepository.save(notif);
            LOG.info("✅ [User Notification] Created welcome in-app notification for '{}'", event.getLogin());

            // Keep activation OTP delivery in Auth; this queues only a non-sensitive welcome email after activation.
            if (activated && event.getEmail() != null && !event.getEmail().isBlank()) {
                String name = event.getFullName() != null && !event.getFullName().isBlank() ? event.getFullName() : event.getLogin();
                EmailTaskDTO task = new EmailTaskDTO(
                    event.getEmail(),
                    "[MindBridge] Tài khoản đã được kích hoạt",
                    "<p>Xin chào " + org.springframework.web.util.HtmlUtils.htmlEscape(name) +
                        ", tài khoản MindBridge của bạn đã được kích hoạt.</p>",
                    false,
                    true,
                    "ACCOUNT_ACTIVATED"
                );
                task.setIdempotencyKey("EMAIL_USER_ACTIVATED_" + event.getUserId());
                emailTaskProducer.enqueueEmailTask(task);
            }
        }

        NotificationInbox inbox = new NotificationInbox(
            event.getEventId(),
            idempotencyKey,
            event.getEventType(),
            CONSUMER_GROUP
        );
        inboxRepository.save(inbox);
    }
}
