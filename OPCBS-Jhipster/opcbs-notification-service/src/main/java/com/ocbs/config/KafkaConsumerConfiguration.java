package com.ocbs.config;

import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

@Configuration
@EnableKafka
@ConditionalOnProperty(name = "application.kafka.enabled", havingValue = "true", matchIfMissing = false)
public class KafkaConsumerConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaConsumerConfiguration.class);
    public static final String APPOINTMENT_DLT_TOPIC = "opcbs-appointment-events.DLT";
    public static final String PAYMENT_DLT_TOPIC = "opcbs-payment-events.DLT";
    public static final String USER_DLT_TOPIC = "opcbs-user-events.DLT";

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        return consumerFactoryFor(com.ocbs.event.AppointmentEvent.class);
    }

    @Bean
    public ConsumerFactory<String, Object> paymentConsumerFactory() {
        return consumerFactoryFor(com.ocbs.event.PaymentEvent.class);
    }

    @Bean
    public ConsumerFactory<String, Object> userConsumerFactory() {
        return consumerFactoryFor(com.ocbs.event.UserEvent.class);
    }

    private ConsumerFactory<String, Object> consumerFactoryFor(Class<?> eventType) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "notification-service-group");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        // Deserializer configuration with ErrorHandlingDeserializer
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class.getName());
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class.getName());

        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.ocbs.*");
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, eventType.getName());

        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ProducerFactory<String, Object> dltProducerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, Object> dltKafkaTemplate() {
        return new KafkaTemplate<>(dltProducerFactory());
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
        @Qualifier("consumerFactory") ConsumerFactory<String, Object> consumerFactory,
        KafkaTemplate<String, Object> dltKafkaTemplate
    ) {
        return listenerFactory(consumerFactory, dltKafkaTemplate, APPOINTMENT_DLT_TOPIC, "Appointment");
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> paymentKafkaListenerContainerFactory(
        @Qualifier("paymentConsumerFactory") ConsumerFactory<String, Object> paymentConsumerFactory,
        KafkaTemplate<String, Object> dltKafkaTemplate
    ) {
        return listenerFactory(paymentConsumerFactory, dltKafkaTemplate, PAYMENT_DLT_TOPIC, "Payment");
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> userKafkaListenerContainerFactory(
        @Qualifier("userConsumerFactory") ConsumerFactory<String, Object> userConsumerFactory,
        KafkaTemplate<String, Object> dltKafkaTemplate
    ) {
        return listenerFactory(userConsumerFactory, dltKafkaTemplate, USER_DLT_TOPIC, "User");
    }

    private ConcurrentKafkaListenerContainerFactory<String, Object> listenerFactory(
        ConsumerFactory<String, Object> consumerFactory,
        KafkaTemplate<String, Object> dltKafkaTemplate,
        String dltTopic,
        String streamName
    ) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);

        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(dltKafkaTemplate,
            (record, ex) -> {
                LOG.error("🚨 [Notification {} DLT] Forwarding poisoned event from partition {} to topic '{}' due to: {}",
                    streamName, record.partition(), dltTopic, ex.getMessage());
                return new TopicPartition(dltTopic, record.partition());
            });
        // Ensure failed DLT publish does NOT mark message as successfully handled
        recoverer.setFailIfSendResultIsError(true);

        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(3);
        backOff.setInitialInterval(1000L);
        backOff.setMultiplier(2.0);
        backOff.setMaxInterval(8000L);
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);
        errorHandler.addNotRetryableExceptions(IllegalArgumentException.class);
        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }

    @Bean
    public NewTopic appointmentEventsDltTopic() {
        return TopicBuilder.name(APPOINTMENT_DLT_TOPIC)
            .partitions(3)
            .replicas(1)
            .build();
    }

    @Bean
    public NewTopic paymentEventsDltTopic() {
        return TopicBuilder.name(PAYMENT_DLT_TOPIC).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic userEventsDltTopic() {
        return TopicBuilder.name(USER_DLT_TOPIC).partitions(3).replicas(1).build();
    }
}
