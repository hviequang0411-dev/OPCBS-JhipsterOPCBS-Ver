package com.ocbs.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import tools.jackson.databind.ObjectMapper;
import com.ocbs.domain.OutboxEvent;
import com.ocbs.domain.enumeration.OutboxStatus;
import com.ocbs.event.AppointmentEvent;
import com.ocbs.repository.OutboxEventRepository;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OutboxRelayServiceTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private ObjectMapper objectMapper;
    private OutboxRelayService outboxRelayService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        outboxRelayService = new OutboxRelayService(outboxEventRepository, kafkaTemplate, objectMapper);
        ReflectionTestUtils.setField(outboxRelayService, "kafkaEnabled", true);
        ReflectionTestUtils.setField(outboxRelayService, "maxRetries", 3);
        ReflectionTestUtils.setField(outboxRelayService, "batchSize", 10);
        ReflectionTestUtils.setField(outboxRelayService, "initialBackoffMs", 1000L);
        ReflectionTestUtils.setField(outboxRelayService, "maxBackoffMs", 8000L);
        ReflectionTestUtils.setField(outboxRelayService, "backoffMultiplier", 2.0);
    }

    @Test
    @DisplayName("1. Relay chỉ đánh dấu SENT sau khi nhận được xác nhận (ACK) từ Kafka broker")
    void shouldMarkOutboxAsSentOnlyWhenBrokerAcknowledges() throws Exception {
        AppointmentEvent event = new AppointmentEvent(
            100L, "BK-100", 10L, 20L, "APPOINTMENT_ACCEPTED", "PENDING", "ACCEPTED"
        );
        String payload = objectMapper.writeValueAsString(event);

        OutboxEvent outbox = new OutboxEvent();
        outbox.setId(1L);
        outbox.setEventId(event.getEventId());
        outbox.setEventType("APPOINTMENT_ACCEPTED");
        outbox.setAggregateId(100L);
        outbox.setIdempotencyKey(event.getIdempotencyKey());
        outbox.setPayload(payload);
        outbox.setStatus(OutboxStatus.PENDING);
        outbox.setRetryCount(0);

        when(outboxEventRepository.countExceededMaxRetries(anyInt())).thenReturn(0L);
        when(outboxEventRepository.findReadyToDispatchEvents(any(Instant.class), eq(3), any()))
            .thenReturn(Collections.singletonList(outbox));
        when(outboxEventRepository.findById(1L)).thenReturn(Optional.of(outbox));

        RecordMetadata metadata = new RecordMetadata(
            new TopicPartition(OutboxRelayService.APPOINTMENT_TOPIC, 0),
            0, 0, System.currentTimeMillis(), 0, 0
        );
        SendResult<String, Object> sendResult = new SendResult<>(null, metadata);
        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(sendResult);

        when(kafkaTemplate.send(eq(OutboxRelayService.APPOINTMENT_TOPIC), eq("100"), any(AppointmentEvent.class)))
            .thenReturn(future);

        outboxRelayService.relayPendingEvents();

        assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.SENT);
        assertThat(outbox.getSentAt()).isNotNull();
        assertThat(outbox.getNextRetryAt()).isNull();
        verify(outboxEventRepository, times(1)).save(outbox);
    }

    @Test
    @DisplayName("2. Relay chuyển FAILED, tăng retryCount và tính toán next_retry_at theo exponential backoff khi Kafka lỗi")
    void shouldMarkOutboxAsFailedWithBackoffWhenBrokerThrowsException() throws Exception {
        AppointmentEvent event = new AppointmentEvent(
            200L, "BK-200", 10L, 20L, "APPOINTMENT_BOOKED", null, "PENDING"
        );
        String payload = objectMapper.writeValueAsString(event);

        OutboxEvent outbox = new OutboxEvent();
        outbox.setId(2L);
        outbox.setEventId(event.getEventId());
        outbox.setEventType("APPOINTMENT_BOOKED");
        outbox.setAggregateId(200L);
        outbox.setIdempotencyKey(event.getIdempotencyKey());
        outbox.setPayload(payload);
        outbox.setStatus(OutboxStatus.PENDING);
        outbox.setRetryCount(0);

        when(outboxEventRepository.countExceededMaxRetries(anyInt())).thenReturn(0L);
        when(outboxEventRepository.findReadyToDispatchEvents(any(Instant.class), eq(3), any()))
            .thenReturn(Collections.singletonList(outbox));
        when(outboxEventRepository.findById(2L)).thenReturn(Optional.of(outbox));

        CompletableFuture<SendResult<String, Object>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Kafka broker connection timeout"));

        when(kafkaTemplate.send(eq(OutboxRelayService.APPOINTMENT_TOPIC), eq("200"), any(AppointmentEvent.class)))
            .thenReturn(failedFuture);

        Instant beforeDispatch = Instant.now();
        outboxRelayService.relayPendingEvents();

        assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(outbox.getRetryCount()).isEqualTo(1);
        assertThat(outbox.getErrorMessage()).contains("Kafka broker connection timeout");
        assertThat(outbox.getSentAt()).isNull();
        assertThat(outbox.getNextRetryAt()).isNotNull();
        // retry 1 backoff = 1000ms
        assertThat(outbox.getNextRetryAt()).isAfterOrEqualTo(beforeDispatch.plusMillis(900));
        verify(outboxEventRepository, times(1)).save(outbox);
    }

    @Test
    @DisplayName("3. Exponential backoff tăng dần nhưng không vượt quá maxBackoffMs")
    void shouldCalculateBackoffWithinLimits() {
        // initialBackoffMs = 1000, multiplier = 2.0, maxBackoffMs = 8000
        assertThat(outboxRelayService.calculateBackoffMs(1)).isEqualTo(1000L);
        assertThat(outboxRelayService.calculateBackoffMs(2)).isEqualTo(2000L);
        assertThat(outboxRelayService.calculateBackoffMs(3)).isEqualTo(4000L);
        assertThat(outboxRelayService.calculateBackoffMs(4)).isEqualTo(8000L);
        // Exceeds maxBackoffMs -> capped at 8000
        assertThat(outboxRelayService.calculateBackoffMs(5)).isEqualTo(8000L);
        assertThat(outboxRelayService.calculateBackoffMs(10)).isEqualTo(8000L);
    }

    @Test
    @DisplayName("4. Relay tiếp tục dispatch các event còn lại trong batch khi một event gặp lỗi")
    void shouldContinueDispatchingWhenOneEventFails() throws Exception {
        AppointmentEvent event1 = new AppointmentEvent(301L, "BK-301", 10L, 20L, "APPOINTMENT_BOOKED", null, "PENDING");
        AppointmentEvent event2 = new AppointmentEvent(302L, "BK-302", 11L, 21L, "APPOINTMENT_BOOKED", null, "PENDING");

        OutboxEvent outbox1 = new OutboxEvent();
        outbox1.setId(10L);
        outbox1.setEventId(event1.getEventId());
        outbox1.setAggregateId(301L);
        outbox1.setPayload(objectMapper.writeValueAsString(event1));
        outbox1.setStatus(OutboxStatus.PENDING);

        OutboxEvent outbox2 = new OutboxEvent();
        outbox2.setId(20L);
        outbox2.setEventId(event2.getEventId());
        outbox2.setAggregateId(302L);
        outbox2.setPayload(objectMapper.writeValueAsString(event2));
        outbox2.setStatus(OutboxStatus.PENDING);

        when(outboxEventRepository.countExceededMaxRetries(anyInt())).thenReturn(0L);
        when(outboxEventRepository.findReadyToDispatchEvents(any(Instant.class), eq(3), any()))
            .thenReturn(Arrays.asList(outbox1, outbox2));
        when(outboxEventRepository.findById(10L)).thenReturn(Optional.of(outbox1));
        when(outboxEventRepository.findById(20L)).thenReturn(Optional.of(outbox2));

        // Event 1 fails
        CompletableFuture<SendResult<String, Object>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Transient failure"));
        when(kafkaTemplate.send(eq(OutboxRelayService.APPOINTMENT_TOPIC), eq("301"), any(AppointmentEvent.class)))
            .thenReturn(failedFuture);

        // Event 2 succeeds
        RecordMetadata metadata = new RecordMetadata(
            new TopicPartition(OutboxRelayService.APPOINTMENT_TOPIC, 0),
            0, 1, System.currentTimeMillis(), 0, 0
        );
        when(kafkaTemplate.send(eq(OutboxRelayService.APPOINTMENT_TOPIC), eq("302"), any(AppointmentEvent.class)))
            .thenReturn(CompletableFuture.completedFuture(new SendResult<>(null, metadata)));

        outboxRelayService.relayPendingEvents();

        assertThat(outbox1.getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(outbox2.getStatus()).isEqualTo(OutboxStatus.SENT);
    }

    @Test
    @DisplayName("5. Hỗ trợ resetForReplay giữ nguyên eventId và idempotencyKey để tránh lặp ở consumer")
    void shouldSafelyResetForReplay() {
        OutboxEvent outbox = new OutboxEvent();
        outbox.setId(99L);
        outbox.setEventId("original-uuid-123");
        outbox.setStatus(OutboxStatus.FAILED);
        outbox.setRetryCount(5);
        outbox.setNextRetryAt(Instant.now().plusSeconds(60));
        outbox.setErrorMessage("Broker unvailable");

        when(outboxEventRepository.findById(99L)).thenReturn(Optional.of(outbox));

        outboxRelayService.resetForReplay(99L);

        assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(outbox.getRetryCount()).isEqualTo(0);
        assertThat(outbox.getNextRetryAt()).isNull();
        assertThat(outbox.getErrorMessage()).isNull();
        // eventId remains unchanged
        assertThat(outbox.getEventId()).isEqualTo("original-uuid-123");
        verify(outboxEventRepository, times(1)).save(outbox);
    }

    @Test
    @DisplayName("6. Không gửi event khi application.kafka.enabled=false")
    void shouldNotRelayWhenKafkaDisabled() {
        ReflectionTestUtils.setField(outboxRelayService, "kafkaEnabled", false);

        outboxRelayService.relayPendingEvents();

        verify(outboxEventRepository, never()).findReadyToDispatchEvents(any(), anyInt(), any());
        verify(kafkaTemplate, never()).send(any(), any(), any());
    }
}
