package com.ocbs.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import tools.jackson.databind.ObjectMapper;
import com.ocbs.domain.Appointment;
import com.ocbs.domain.DoctorProfile;
import com.ocbs.domain.OutboxEvent;
import com.ocbs.domain.PatientProfile;
import com.ocbs.domain.User;
import com.ocbs.domain.enumeration.AppointmentStatus;
import com.ocbs.domain.enumeration.OutboxStatus;
import com.ocbs.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AppointmentOutboxServiceTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    private ObjectMapper objectMapper;
    private AppointmentOutboxService appointmentOutboxService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        appointmentOutboxService = new AppointmentOutboxService(outboxEventRepository, objectMapper);
    }

    @Test
    @DisplayName("1. Lưu outbox event với ID định danh ổn định, KHÔNG lộ tên, login hay ghi chú nhạy cảm")
    void shouldRecordOutboxEventWithIdentifiersOnlyAndPrivacyCompliance() {
        User patientUser = new User();
        patientUser.setId(10L);
        patientUser.setLogin("patient_quang");

        PatientProfile patient = new PatientProfile();
        patient.setId(100L);
        patient.setFullName("Nguyen Van Quang");
        patient.setUser(patientUser);

        User doctorUser = new User();
        doctorUser.setId(20L);
        doctorUser.setLogin("doctor_minh");

        DoctorProfile doctor = new DoctorProfile();
        doctor.setId(200L);
        doctor.setFullName("BS. Tran Minh");
        doctor.setUser(doctorUser);

        Appointment appointment = new Appointment();
        appointment.setId(5001L);
        appointment.setBookingCode("BK-5001");
        appointment.setStatus(AppointmentStatus.ACCEPTED);
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);

        when(outboxEventRepository.save(any(OutboxEvent.class))).thenAnswer(invocation -> {
            OutboxEvent saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        OutboxEvent result = appointmentOutboxService.recordAppointmentEvent(
            appointment,
            "APPOINTMENT_ACCEPTED",
            "PENDING",
            "ACCEPTED",
            "Sensitive consultation reason"
        );

        assertThat(result).isNotNull();
        assertThat(result.getEventId()).isNotBlank();
        assertThat(result.getEventType()).isEqualTo("APPOINTMENT_ACCEPTED");
        assertThat(result.getAggregateId()).isEqualTo(5001L);
        assertThat(result.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(result.getRetryCount()).isEqualTo(0);

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository, times(1)).save(captor.capture());
        OutboxEvent captured = captor.getValue();

        // Must contain stable IDs & workflow state
        assertThat(captured.getPayload()).contains("\"appointmentId\":5001");
        assertThat(captured.getPayload()).contains("\"bookingCode\":\"BK-5001\"");
        assertThat(captured.getPayload()).contains("\"patientId\":100");
        assertThat(captured.getPayload()).contains("\"doctorId\":200");
        assertThat(captured.getPayload()).contains("\"eventType\":\"APPOINTMENT_ACCEPTED\"");
        assertThat(captured.getPayload()).contains("\"eventVersion\":\"1.0\"");

        // Must NOT leak personal names, logins or private reasons in event payload
        assertThat(captured.getPayload()).doesNotContain("patient_quang");
        assertThat(captured.getPayload()).doesNotContain("doctor_minh");
        assertThat(captured.getPayload()).doesNotContain("Nguyen Van Quang");
        assertThat(captured.getPayload()).doesNotContain("BS. Tran Minh");
        assertThat(captured.getPayload()).doesNotContain("Sensitive consultation reason");
    }

    @Test
    @DisplayName("2. Thất bại nếu đối tượng Appointment hoặc appointmentId null")
    void shouldThrowExceptionWhenAppointmentIdIsNull() {
        Appointment appointment = new Appointment();
        appointment.setId(null);

        assertThatThrownBy(() -> appointmentOutboxService.recordAppointmentEvent(
            appointment,
            "APPOINTMENT_BOOKED",
            null,
            "PENDING",
            null
        )).isInstanceOf(IllegalArgumentException.class);

        verify(outboxEventRepository, never()).save(any());
    }
}
