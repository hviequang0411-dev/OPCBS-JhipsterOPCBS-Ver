package com.ocbs.service;

import com.ocbs.config.RabbitMqConfiguration;
import com.ocbs.service.dto.EmailTaskDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.concurrent.TimeUnit;

/**
 * Producer for Asynchronous Email Tasks.
 * Routes discrete email jobs to RabbitMQ worker queue for rate-limited, retried execution.
 * Gracefully executes locally via MailService when RabbitMQ broker is disabled.
 */
@Service
public class EmailTaskProducer {

    private static final Logger LOG = LoggerFactory.getLogger(EmailTaskProducer.class);

    private final RabbitTemplate rabbitTemplate;
    private final MailService mailService;

    @Value("${application.rabbitmq.enabled:false}")
    private boolean rabbitmqEnabled;

    public EmailTaskProducer(
        ObjectProvider<RabbitTemplate> rabbitTemplateProvider,
        MailService mailService
    ) {
        this.rabbitTemplate = rabbitTemplateProvider.getIfAvailable();
        this.mailService = mailService;
        if (this.rabbitTemplate != null) {
            LOG.info("🚀 [Email Task Producer] RabbitMQ is ACTIVE. Email tasks will route to queue '{}'",
                RabbitMqConfiguration.EMAIL_QUEUE);
        } else {
            LOG.info("ℹ️ [Email Task Producer] Operating in Direct Mode (RabbitMQ disabled). Emails dispatched locally via MailService.");
        }
    }

    public void enqueueEmailTask(EmailTaskDTO task) {
        if (task == null || task.getTo() == null || task.getTo().isBlank()) {
            LOG.warn("⚠️ [Email Task] Rejected invalid email task with missing recipient");
            return;
        }

        if (rabbitmqEnabled && rabbitTemplate != null) {
            try {
                LOG.info("📤 [RabbitMQ Worker Queue] Enqueuing Email Task ID '{}' for recipient '{}' (Subject: '{}')",
                    task.getTaskId(), task.getTo(), task.getSubject());

                CorrelationData correlation = new CorrelationData(task.getTaskId());
                rabbitTemplate.convertAndSend(
                    RabbitMqConfiguration.DIRECT_EXCHANGE,
                    RabbitMqConfiguration.EMAIL_ROUTING_KEY,
                    task,
                    correlation
                );
                CorrelationData.Confirm confirm = correlation.getFuture().get(5, TimeUnit.SECONDS);
                if (!confirm.isAck()) {
                    throw new IllegalStateException("RabbitMQ did not confirm email task: " + confirm.getReason());
                }
            } catch (Exception ex) {
                LOG.error("❌ [RabbitMQ Queue Error] Failed to publish email task to RabbitMQ: {}", ex.getMessage(), ex);
                throw new IllegalStateException("RabbitMQ email task publish failed", ex);
            }
        } else if (rabbitmqEnabled) {
            throw new IllegalStateException("RabbitMQ is enabled but RabbitTemplate is unavailable");
        } else {
            fallbackDirectSend(task);
        }
    }

    private void fallbackDirectSend(EmailTaskDTO task) {
        LOG.debug("📨 [Direct Mail Dispatch] Delivering email directly to '{}' (Subject: '{}')", task.getTo(), task.getSubject());
        mailService.sendEmail(
            task.getTo(),
            task.getSubject(),
            task.getContent(),
            task.isMultipart(),
            task.isHtml()
        );
    }
}
