package com.ocbs.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.ocbs.domain.DoctorProfile;
import com.ocbs.domain.Notification;
import com.ocbs.domain.NotificationInbox;
import com.ocbs.domain.PatientProfile;
import com.ocbs.domain.User;
import com.ocbs.repository.DoctorProfileRepository;
import com.ocbs.repository.NotificationInboxRepository;
import com.ocbs.repository.NotificationRepository;
import com.ocbs.repository.PatientProfileRepository;
import com.ocbs.repository.UserRepository;
import com.ocbs.service.EmailTaskProducer;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationEventConsumerTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationInboxRepository notificationInboxRepository;

    @Mock
    private DoctorProfileRepository doctorProfileRepository;

    @Mock
    private PatientProfileRepository patientProfileRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailTaskProducer emailTaskProducer;

    private NotificationEventConsumer notificationEventConsumer;

    @BeforeEach
    void setUp() {
        notificationEventConsumer = new NotificationEventConsumer(
            notificationRepository,
            notificationInboxRepository,
            doctorProfileRepository,
            patientProfileRepository,
            userRepository,
            emailTaskProducer
        );
    }

    @Test
    @DisplayName("1. Gửi thông báo bằng cách tra cứu ID bệnh nhân và bác sĩ từ repository, không hardcode login giả")
    void shouldSendNotificationUsingDynamicLookupFromRepository() {
        AppointmentEvent event = new AppointmentEvent(
            101L, "BK-101", 88L, 99L,
            "APPOINTMENT_BOOKED", null, "PENDING"
        );

        User doctorUser = new User();
        doctorUser.setLogin("doctor_lan");
        DoctorProfile doctor = new DoctorProfile();
        doctor.setId(99L);
        doctor.setFullName("BS. Nguyen Thi Lan");
        doctor.setUser(doctorUser);

        User patientUser = new User();
        patientUser.setLogin("patient_nam");
        PatientProfile patient = new PatientProfile();
        patient.setId(88L);
        patient.setFullName("Tran Van Nam");
        patient.setUser(patientUser);

        when(notificationInboxRepository.existsByEventId(event.getEventId())).thenReturn(false);
        when(notificationInboxRepository.existsByConsumerGroupAndIdempotencyKey(any(), any())).thenReturn(false);
        when(doctorProfileRepository.findOneWithToOneRelationships(99L)).thenReturn(Optional.of(doctor));
        when(patientProfileRepository.findOneWithToOneRelationships(88L)).thenReturn(Optional.of(patient));

        notificationEventConsumer.handleAppointmentEvent(event);

        ArgumentCaptor<Notification> notifCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(2)).save(notifCaptor.capture());

        List<Notification> capturedNotifications = notifCaptor.getAllValues();
        assertThat(capturedNotifications).extracting(Notification::getUserLogin)
            .containsExactlyInAnyOrder("doctor_lan", "patient_nam");

        verify(notificationInboxRepository, times(1)).save(any(NotificationInbox.class));
    }

    @Test
    @DisplayName("2. Privacy: Thông báo hủy lịch KHÔNG làm lộ nội dung lý do riêng tư trong tin nhắn thông báo")
    void shouldNotLeakSensitiveReasonInCancellationNotification() {
        AppointmentEvent event = new AppointmentEvent(
            103L, "BK-103", 88L, 99L,
            "APPOINTMENT_CANCELLED", "CONFIRMED", "CANCELLED"
        );
        event.setReason("Lý do bệnh án rất nhạy cảm"); // Legacy/accidental field

        User doctorUser = new User();
        doctorUser.setLogin("doctor_lan");
        DoctorProfile doctor = new DoctorProfile();
        doctor.setUser(doctorUser);

        User patientUser = new User();
        patientUser.setLogin("patient_nam");
        PatientProfile patient = new PatientProfile();
        patient.setUser(patientUser);

        when(notificationInboxRepository.existsByEventId(event.getEventId())).thenReturn(false);
        when(notificationInboxRepository.existsByConsumerGroupAndIdempotencyKey(any(), any())).thenReturn(false);
        when(doctorProfileRepository.findOneWithToOneRelationships(99L)).thenReturn(Optional.of(doctor));
        when(patientProfileRepository.findOneWithToOneRelationships(88L)).thenReturn(Optional.of(patient));

        notificationEventConsumer.handleAppointmentEvent(event);

        ArgumentCaptor<Notification> notifCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(2)).save(notifCaptor.capture());

        List<Notification> notifications = notifCaptor.getAllValues();
        // Không chứa nội dung lý do nhạy cảm
        assertThat(notifications).noneMatch(n -> n.getMessage().contains("Lý do bệnh án rất nhạy cảm"));
        assertThat(notifications).allMatch(n -> n.getMessage().contains("Lịch khám mã BK-103 đã được hủy."));
    }

    @Test
    @DisplayName("3. Idempotency: Không tạo thông báo trùng nếu eventId hoặc idempotencyKey đã có trong inbox")
    void shouldNotCreateDuplicateNotificationsWhenEventAlreadyProcessed() {
        AppointmentEvent event = new AppointmentEvent(
            102L, "BK-102", 88L, 99L,
            "APPOINTMENT_ACCEPTED", "PENDING", "ACCEPTED"
        );

        when(notificationInboxRepository.existsByEventId(event.getEventId())).thenReturn(true);

        notificationEventConsumer.handleAppointmentEvent(event);

        verify(notificationRepository, never()).save(any());
        verify(notificationInboxRepository, never()).save(any());
    }

    @Test
    @DisplayName("4. Khi không tìm thấy người dùng trong DB, không tự sinh login giả dạng doctor{id} hay patient{id}")
    void shouldNotCreateArtificialLoginsWhenUserNotFound() {
        AppointmentEvent event = new AppointmentEvent(
            104L, "BK-104", 88L, 99L,
            "APPOINTMENT_ACCEPTED", "PENDING", "ACCEPTED"
        );

        when(notificationInboxRepository.existsByEventId(event.getEventId())).thenReturn(false);
        when(notificationInboxRepository.existsByConsumerGroupAndIdempotencyKey(any(), any())).thenReturn(false);
        // Không tìm thấy trong DB
        when(patientProfileRepository.findOneWithToOneRelationships(88L)).thenReturn(Optional.empty());

        notificationEventConsumer.handleAppointmentEvent(event);

        // Không tạo notification cho login giả mạo
        verify(notificationRepository, never()).save(any());
        // Vẫn ghi nhận inbox là đã xử lý
        verify(notificationInboxRepository, times(1)).save(any(NotificationInbox.class));
    }

    @Test
    @DisplayName("5. Schema validation: Event null ném IllegalArgumentException (DLT)")
    void shouldThrowExceptionForNullEvent() {
        assertThatThrownBy(() -> notificationEventConsumer.handleAppointmentEvent(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Received null event record");
    }

    @Test
    @DisplayName("6. Schema validation: Unsupported eventVersion ném IllegalArgumentException (DLT)")
    void shouldThrowExceptionForUnsupportedEventVersion() {
        AppointmentEvent event = new AppointmentEvent(
            105L, "BK-105", 88L, 99L,
            "APPOINTMENT_BOOKED", null, "PENDING"
        );
        event.setEventVersion("2.0");

        assertThatThrownBy(() -> notificationEventConsumer.handleAppointmentEvent(event))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsupported event contract version");

        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("7. Schema validation: Unrecognized eventType ném IllegalArgumentException (DLT)")
    void shouldThrowExceptionForUnrecognizedEventType() {
        AppointmentEvent event = new AppointmentEvent(
            106L, "BK-106", 88L, 99L,
            "INVALID_TYPE_XYZ", null, "PENDING"
        );

        assertThatThrownBy(() -> notificationEventConsumer.handleAppointmentEvent(event))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unrecognized event type");

        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("8. Không nuốt exception: khi database lỗi, exception được ném ra để Spring Kafka retry/DLT")
    void shouldPropagateExceptionWhenDatabaseErrorOccurs() {
        AppointmentEvent event = new AppointmentEvent(
            107L, "BK-107", 88L, 99L,
            "APPOINTMENT_ACCEPTED", "PENDING", "ACCEPTED"
        );

        User patientUser = new User();
        patientUser.setLogin("patient_nam");
        PatientProfile patient = new PatientProfile();
        patient.setUser(patientUser);

        when(notificationInboxRepository.existsByEventId(any())).thenReturn(false);
        when(notificationInboxRepository.existsByConsumerGroupAndIdempotencyKey(any(), any())).thenReturn(false);
        when(patientProfileRepository.findOneWithToOneRelationships(88L)).thenReturn(Optional.of(patient));
        when(notificationRepository.save(any())).thenThrow(new RuntimeException("Database connection timeout"));

        assertThatThrownBy(() -> notificationEventConsumer.handleAppointmentEvent(event))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Database connection timeout");
    }
}
