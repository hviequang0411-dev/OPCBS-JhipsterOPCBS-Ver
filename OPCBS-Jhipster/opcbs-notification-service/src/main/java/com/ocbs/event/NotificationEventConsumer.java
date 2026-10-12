package com.ocbs.event;

import com.ocbs.domain.Notification;
import com.ocbs.repository.NotificationRepository;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Kafka Consumer in Notification Service.
 * Automatically generates in-app notifications for patients and doctors when appointment events occur.
 */
@Service
public class NotificationEventConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationEventConsumer.class);

    private final NotificationRepository notificationRepository;

    public NotificationEventConsumer(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @KafkaListener(topics = "opcbs-appointment-events", groupId = "notification-service-group")
    @Transactional
    public void handleAppointmentEvent(AppointmentEvent event) {
        LOG.info("📥 [Notification Service Kafka] Processing Event: Type='{}', BookingCode='{}', Patient='{}', Doctor='{}'",
            event.getEventType(), event.getBookingCode(), event.getPatientName(), event.getDoctorName());

        try {
            switch (event.getEventType()) {
                case "APPOINTMENT_BOOKED" -> {
                    // 1. Thông báo cho Bác sĩ
                    Notification doctorNotif = new Notification();
                    doctorNotif.setUserLogin("doctor1"); // fallback/mapped
                    doctorNotif.setTitle("📅 Có Lịch Khám Mới Chờ Duyệt!");
                    doctorNotif.setMessage("Bệnh nhân " + (event.getPatientName() != null ? event.getPatientName() : "vãng lai") +
                        " vừa đặt lịch khám mã " + event.getBookingCode() + ". Vui lòng vào duyệt lịch.");
                    doctorNotif.setNotificationType("APPOINTMENT");
                    doctorNotif.setRelatedEntityId(event.getAppointmentId());
                    doctorNotif.setRelatedEntityType("Appointment");
                    doctorNotif.setActionUrl("/appointment");
                    notificationRepository.save(doctorNotif);

                    // 2. Thông báo cho Bệnh nhân
                    Notification patientNotif = new Notification();
                    patientNotif.setUserLogin("patient1");
                    patientNotif.setTitle("✅ Đặt Lịch Khám Thành Công!");
                    patientNotif.setMessage("Lịch hẹn mã " + event.getBookingCode() + " với Bác sĩ " +
                        (event.getDoctorName() != null ? event.getDoctorName() : "") + " đã được tiếp nhận và đang chờ bác sĩ xác nhận.");
                    patientNotif.setNotificationType("APPOINTMENT");
                    patientNotif.setRelatedEntityId(event.getAppointmentId());
                    patientNotif.setRelatedEntityType("Appointment");
                    patientNotif.setActionUrl("/appointment");
                    notificationRepository.save(patientNotif);
                }
                case "APPOINTMENT_ACCEPTED" -> {
                    Notification patientNotif = new Notification();
                    patientNotif.setUserLogin("patient1");
                    patientNotif.setTitle("🎉 Bác Sĩ Đã Chấp Nhận Lịch Khám!");
                    patientNotif.setMessage("Bác sĩ " + (event.getDoctorName() != null ? event.getDoctorName() : "") +
                        " đã xác nhận lịch hẹn " + event.getBookingCode() + ". Vui lòng tham gia đúng giờ hẹn.");
                    patientNotif.setNotificationType("APPOINTMENT");
                    patientNotif.setRelatedEntityId(event.getAppointmentId());
                    patientNotif.setRelatedEntityType("Appointment");
                    patientNotif.setActionUrl("/appointment");
                    notificationRepository.save(patientNotif);
                }
                case "APPOINTMENT_COMPLETED" -> {
                    Notification patientNotif = new Notification();
                    patientNotif.setUserLogin("patient1");
                    patientNotif.setTitle("📋 Hồ Sơ Bệnh Án & Lời Dặn Của Bác Sĩ");
                    patientNotif.setMessage("Ca khám " + event.getBookingCode() + " đã hoàn tất. Bác sĩ đã lưu hồ sơ bệnh án và phác đồ điều trị của bạn.");
                    patientNotif.setNotificationType("CLINICAL");
                    patientNotif.setRelatedEntityId(event.getAppointmentId());
                    patientNotif.setRelatedEntityType("Appointment");
                    patientNotif.setActionUrl("/patient/clinical-records");
                    notificationRepository.save(patientNotif);
                }
            }
        } catch (Exception ex) {
            LOG.error("❌ Error processing notification event: {}", ex.getMessage(), ex);
        }
    }
}
