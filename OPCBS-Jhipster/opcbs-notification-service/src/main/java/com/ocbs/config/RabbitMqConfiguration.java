package com.ocbs.config;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ Configuration for Asynchronous Task Worker Queues in Notification Service.
 *
 * Architecture Role:
 * - RabbitMQ is chosen specifically for discrete task/command queues (e.g. SMTP email delivery, background jobs)
 *   requiring fine-grained acknowledgement (ACK/NACK), competing worker consumption, rate-limiting (prefetch count),
 *   and dead-letter queue routing without partition-blocking semantics.
 */
@Configuration
@ConditionalOnProperty(name = "application.rabbitmq.enabled", havingValue = "true", matchIfMissing = false)
public class RabbitMqConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(RabbitMqConfiguration.class);

    // Exchanges
    public static final String DIRECT_EXCHANGE = "opcbs.direct.exchange";
    public static final String DEAD_LETTER_EXCHANGE = "opcbs.dlx.exchange";

    // Queues
    public static final String EMAIL_QUEUE = "opcbs.email.dispatch.queue";
    public static final String EMAIL_DLQ = "opcbs.email.dlq";

    // Routing Keys
    public static final String EMAIL_ROUTING_KEY = "opcbs.email.dispatch";
    public static final String EMAIL_DLQ_ROUTING_KEY = "email.dlq";

    @Bean
    public DirectExchange directExchange() {
        return new DirectExchange(DIRECT_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DEAD_LETTER_EXCHANGE, true, false);
    }

    @Bean
    public Queue emailDispatchQueue() {
        Map<String, Object> args = new HashMap<>();
        // Configure Dead Letter Exchange for failed deliveries after retries
        args.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
        args.put("x-dead-letter-routing-key", EMAIL_DLQ_ROUTING_KEY);
        // Message TTL in queue: 24 hours (86,400,000 ms)
        args.put("x-message-ttl", 86400000);
        return new Queue(EMAIL_QUEUE, true, false, false, args);
    }

    @Bean
    public Queue emailDeadLetterQueue() {
        return new Queue(EMAIL_DLQ, true, false, false);
    }

    @Bean
    public Binding emailQueueBinding(Queue emailDispatchQueue, DirectExchange directExchange) {
        return BindingBuilder.bind(emailDispatchQueue).to(directExchange).with(EMAIL_ROUTING_KEY);
    }

    @Bean
    public Binding emailDlqBinding(Queue emailDeadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(emailDeadLetterQueue).to(deadLetterExchange).with(EMAIL_DLQ_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        template.setMandatory(true);
        template.setConfirmCallback((correlation, ack, cause) -> {
            if (!ack) {
                LOG.error("RabbitMQ broker rejected email task publish, correlationId='{}', cause='{}'",
                    correlation != null ? correlation.getId() : "unknown", cause);
            }
        });
        LOG.info("🚀 [RabbitMQ Configuration] RabbitTemplate configured with JSON converter for task routing.");
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
        ConnectionFactory connectionFactory,
        MessageConverter jsonMessageConverter
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);
        // Worker queue rate limiting: prefetch 10 messages per worker instance to prevent SMTP flooding
        factory.setPrefetchCount(10);
        factory.setAcknowledgeMode(AcknowledgeMode.AUTO);
        factory.setDefaultRequeueRejected(false); // Reject poisoned messages to DLQ instead of looping
        return factory;
    }
}
