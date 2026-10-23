package com.ocbs.service;

import com.ocbs.config.RabbitMqConfiguration;
import com.ocbs.domain.NotificationInbox;
import com.ocbs.repository.NotificationInboxRepository;
import com.ocbs.service.dto.EmailTaskDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Worker Consumer for Asynchronous Email Delivery from RabbitMQ Queue.
 * Queue: opcbs.email.dispatch.queue
 * DLQ: opcbs.email.dlq
 *
 * Guarantees:
 * - Rate-limited delivery via consumer prefetch.
 * - Idempotency guard: checks NotificationInbox to avoid duplicate email transmissions on broker retry.
 * - Unrecoverable or exceeded retry tasks are routed to DLQ for operator inspection without stalling the worker.
 */
@Service
@ConditionalOnProperty(name = "application.rabbitmq.enabled", havingValue = "true", matchIfMissing = false)
public class EmailTaskConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(EmailTaskConsumer.class);
    private static final String WORKER_GROUP = "email-worker-group";
    private static final int MAX_WORKER_RETRIES = 3;

    private final MailService mailService;
    private final NotificationInboxRepository inboxRepository;

    public EmailTaskConsumer(MailService mailService, NotificationInboxRepository inboxRepository) {
        this.mailService = mailService;
        this.inboxRepository = inboxRepository;
    }

    @RabbitListener(
        queues = RabbitMqConfiguration.EMAIL_QUEUE,
        containerFactory = "rabbitListenerContainerFactory"
    )
    public void processEmailTask(EmailTaskDTO task) {
        if (task == null || task.getTaskId() == null || task.getTo() == null || task.getTo().isBlank() ||
            !task.getTo().contains("@") || task.getSubject() == null || task.getContent() == null) {
            LOG.warn("⚠️ [Email Worker] Received invalid email task or malformed recipient. Rejecting to DLQ: {}", task);
            throw new org.springframework.amqp.AmqpRejectAndDontRequeueException("Invalid email task payload");
        }

        String idempotencyKey = task.getIdempotencyKey() != null
            ? task.getIdempotencyKey()
            : "EMAIL_" + task.getTaskId();

        // 1. Idempotency Check
        if (inboxRepository.existsByConsumerGroupAndIdempotencyKey(WORKER_GROUP, idempotencyKey)) {
            LOG.info("⚡ [Email Idempotency Guard] Email task key '{}' already dispatched. Skipping duplicate send.",
                idempotencyKey);
            return;
        }

        LOG.info("📥 [Email Worker PROCESSING] Task ID '{}' to '{}' (Category: '{}')",
            task.getTaskId(), task.getTo(), task.getEmailCategory());

        try {
            sendWithBoundedRetry(task);

            // 3. Record Successful Inbox Record
            NotificationInbox inbox = new NotificationInbox(
                task.getTaskId(),
                idempotencyKey,
                "EMAIL_SENT",
                WORKER_GROUP
            );
            inboxRepository.save(inbox);

            LOG.info("✅ [Email Worker CONFIRMED] Task ID '{}' delivered to '{}'", task.getTaskId(), task.getTo());

        } catch (Exception ex) {
            LOG.error("❌ [Email Worker DLQ] Task ID '{}' to '{}' failed after bounded SMTP retries. Routing to '{}'",
                task.getTaskId(), task.getTo(), RabbitMqConfiguration.EMAIL_DLQ, ex);
            throw new org.springframework.amqp.AmqpRejectAndDontRequeueException(
                "Email delivery failed after bounded retries. Routing to DLQ.", ex
            );
        }
    }

    private void sendWithBoundedRetry(EmailTaskDTO task) {
        for (int attempt = 1; attempt <= MAX_WORKER_RETRIES; attempt++) {
            try {
                // Wait for SMTP completion before the listener returns and AUTO-ACKs the message.
                mailService.sendEmailAndWait(
                    task.getTo(),
                    task.getSubject(),
                    task.getContent(),
                    task.isMultipart(),
                    task.isHtml()
                );
                return;
            } catch (Exception ex) {
                if (attempt == MAX_WORKER_RETRIES) {
                    throw new IllegalStateException("SMTP delivery exhausted retries", ex);
                }
                LOG.warn("SMTP delivery attempt {}/{} failed for taskId='{}'; retrying",
                    attempt, MAX_WORKER_RETRIES, task.getTaskId());
                try {
                    Thread.sleep(1000L << (attempt - 1));
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted while retrying SMTP delivery", interrupted);
                }
            }
        }
    }
}
