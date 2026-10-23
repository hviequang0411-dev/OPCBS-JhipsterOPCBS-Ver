package com.ocbs.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.ocbs.domain.DoctorProfile;
import com.ocbs.domain.TreatmentInbox;
import com.ocbs.domain.TreatmentPackage;
import com.ocbs.domain.TreatmentSession;
import com.ocbs.domain.enumeration.TreatmentPackageStatus;
import com.ocbs.repository.TreatmentInboxRepository;
import com.ocbs.repository.TreatmentPackageRepository;
import com.ocbs.repository.TreatmentSessionRepository;
import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TreatmentAppointmentEventConsumerTest {

    @Mock
    private TreatmentPackageRepository treatmentPackageRepository;

    @Mock
    private TreatmentSessionRepository treatmentSessionRepository;

    @Mock
    private TreatmentInboxRepository treatmentInboxRepository;

    private AppointmentEventConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new AppointmentEventConsumer(
            treatmentPackageRepository,
            treatmentSessionRepository,
            treatmentInboxRepository
        );
    }

    @Test
    @DisplayName("1. Ngăn trừ nhầm gói: Không có package của bác sĩ trong event nhưng có package của bác sĩ khác -> KHÔNG trừ gói đó")
    void shouldNotDeductOtherDoctorPackageWhenMatchingDoctorPackageNotFound() {
        // Event với bác sĩ ID = 20L
        AppointmentEvent completedEvent = new AppointmentEvent(
            201L, "BK-201", 10L, 20L,
            "APPOINTMENT_COMPLETED", "IN_PROGRESS", "COMPLETED"
        );

        when(treatmentInboxRepository.existsByAppointmentIdAndEventType(201L, "APPOINTMENT_COMPLETED")).thenReturn(false);
        when(treatmentInboxRepository.existsByEventId(completedEvent.getEventId())).thenReturn(false);
        when(treatmentSessionRepository.existsByAppointmentId(201L)).thenReturn(false);

        // Bác sĩ 20 không có package nào với bệnh nhân 10
        when(treatmentPackageRepository.findActivePackagesByPatientAndDoctor(10L, 20L))
            .thenReturn(Collections.emptyList());

        consumer.handleAppointmentEvent(completedEvent);

        // KHÔNG BAO GIỜ fallback gọi findActivePackagesByPatient(10L) để lấy gói bác sĩ khác!
        verify(treatmentPackageRepository, never()).findActivePackagesByPatient(10L);
        verify(treatmentPackageRepository, never()).save(any());
        verify(treatmentSessionRepository, never()).save(any());

        // Ghi inbox với status rõ ràng SKIPPED_NO_PACKAGE để không xử lý lại
        ArgumentCaptor<TreatmentInbox> captor = ArgumentCaptor.forClass(TreatmentInbox.class);
        verify(treatmentInboxRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("SKIPPED_NO_PACKAGE");
        assertThat(captor.getValue().getEventType()).isEqualTo("APPOINTMENT_COMPLETED");
    }

    @Test
    @DisplayName("2. Có package đúng bệnh nhân và bác sĩ: Trừ đúng 1 buổi và cập nhật COMPLETED khi hết buổi")
    void shouldDeductOneSessionForExactPatientAndDoctorMatch() {
        AppointmentEvent completedEvent = new AppointmentEvent(
            202L, "BK-202", 10L, 20L,
            "APPOINTMENT_COMPLETED", "IN_PROGRESS", "COMPLETED"
        );

        when(treatmentInboxRepository.existsByAppointmentIdAndEventType(202L, "APPOINTMENT_COMPLETED")).thenReturn(false);
        when(treatmentInboxRepository.existsByEventId(completedEvent.getEventId())).thenReturn(false);
        when(treatmentSessionRepository.existsByAppointmentId(202L)).thenReturn(false);

        TreatmentPackage pkg = new TreatmentPackage();
        pkg.setId(77L);
        pkg.setName("Gói Trị Liệu Trầm Cảm");
        pkg.setSessionQuantity(5);
        pkg.setRemainingSessions(1);
        pkg.setStatus(TreatmentPackageStatus.ACTIVE);
        pkg.setExpirationDate(Instant.now().plusSeconds(86400));

        when(treatmentPackageRepository.findActivePackagesByPatientAndDoctor(10L, 20L))
            .thenReturn(Collections.singletonList(pkg));
        when(treatmentPackageRepository.findByIdForUpdate(77L))
            .thenReturn(Optional.of(pkg));

        consumer.handleAppointmentEvent(completedEvent);

        assertThat(pkg.getRemainingSessions()).isEqualTo(0);
        assertThat(pkg.getStatus()).isEqualTo(TreatmentPackageStatus.COMPLETED);

        verify(treatmentPackageRepository, times(1)).save(pkg);
        verify(treatmentSessionRepository, times(1)).save(any(TreatmentSession.class));

        ArgumentCaptor<TreatmentInbox> captor = ArgumentCaptor.forClass(TreatmentInbox.class);
        verify(treatmentInboxRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("DEDUCTED");
    }

    @Test
    @DisplayName("3. Idempotency: Gửi lặp cùng event hoặc appointment đã hoàn tất không trừ thêm buổi")
    void shouldNotDeductTwiceForDuplicateEvent() {
        AppointmentEvent completedEvent = new AppointmentEvent(
            203L, "BK-203", 10L, 20L,
            "APPOINTMENT_COMPLETED", "IN_PROGRESS", "COMPLETED"
        );

        when(treatmentInboxRepository.existsByAppointmentIdAndEventType(203L, "APPOINTMENT_COMPLETED")).thenReturn(true);

        consumer.handleAppointmentEvent(completedEvent);

        verify(treatmentPackageRepository, never()).findActivePackagesByPatientAndDoctor(any(), any());
        verify(treatmentPackageRepository, never()).findByIdForUpdate(any());
        verify(treatmentPackageRepository, never()).save(any());
        verify(treatmentSessionRepository, never()).save(any());
    }

    @Test
    @DisplayName("4. Event hợp lệ nhưng không phải APPOINTMENT_COMPLETED: Ghi inbox chủ đích và KHÔNG trừ buổi")
    void shouldRecordInboxAndSkipDeductionForNonCompletedEvent() {
        AppointmentEvent bookedEvent = new AppointmentEvent(
            204L, "BK-204", 10L, 20L,
            "APPOINTMENT_BOOKED", null, "PENDING"
        );

        when(treatmentInboxRepository.existsByEventId(bookedEvent.getEventId())).thenReturn(false);
        when(treatmentInboxRepository.existsByAppointmentIdAndEventType(204L, "APPOINTMENT_BOOKED")).thenReturn(false);

        consumer.handleAppointmentEvent(bookedEvent);

        verify(treatmentPackageRepository, never()).findActivePackagesByPatientAndDoctor(any(), any());
        verify(treatmentPackageRepository, never()).save(any());
        verify(treatmentSessionRepository, never()).save(any());

        ArgumentCaptor<TreatmentInbox> captor = ArgumentCaptor.forClass(TreatmentInbox.class);
        verify(treatmentInboxRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo("APPOINTMENT_BOOKED");
        assertThat(captor.getValue().getStatus()).isEqualTo("SKIPPED_NOT_COMPLETED");
    }

    @Test
    @DisplayName("5. Schema validation: Event null ném IllegalArgumentException (DLT)")
    void shouldThrowExceptionForNullEvent() {
        assertThatThrownBy(() -> consumer.handleAppointmentEvent(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Received null event record");
    }

    @Test
    @DisplayName("6. Schema validation: Unsupported eventVersion ném IllegalArgumentException (DLT)")
    void shouldThrowExceptionForUnsupportedEventVersion() {
        AppointmentEvent event = new AppointmentEvent(
            205L, "BK-205", 10L, 20L,
            "APPOINTMENT_COMPLETED", "IN_PROGRESS", "COMPLETED"
        );
        event.setEventVersion("2.0");

        assertThatThrownBy(() -> consumer.handleAppointmentEvent(event))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsupported event contract version");

        verify(treatmentInboxRepository, never()).save(any());
    }

    @Test
    @DisplayName("7. Schema validation: Event thiếu field bắt buộc (patientId null trong COMPLETED) ném IllegalArgumentException (DLT)")
    void shouldThrowExceptionWhenCompletedEventMissingPatientId() {
        AppointmentEvent event = new AppointmentEvent(
            206L, "BK-206", null, 20L,
            "APPOINTMENT_COMPLETED", "IN_PROGRESS", "COMPLETED"
        );

        assertThatThrownBy(() -> consumer.handleAppointmentEvent(event))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("missing mandatory patientId");

        verify(treatmentInboxRepository, never()).save(any());
    }

    @Test
    @DisplayName("8. Schema validation: Unrecognized eventType ném IllegalArgumentException (DLT)")
    void shouldThrowExceptionForUnrecognizedEventType() {
        AppointmentEvent event = new AppointmentEvent(
            207L, "BK-207", 10L, 20L,
            "UNKNOWN_LIFECYCLE_ACTION", "PENDING", "ACTIVE"
        );

        assertThatThrownBy(() -> consumer.handleAppointmentEvent(event))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unrecognized event type");

        verify(treatmentInboxRepository, never()).save(any());
    }
}
