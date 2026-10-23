package com.ocbs.event;

import com.ocbs.domain.DoctorProfile;
import com.ocbs.domain.Notification;
import com.ocbs.domain.NotificationInbox;
import com.ocbs.domain.PatientProfile;
import com.ocbs.repository.DoctorProfileRepository;
import com.ocbs.repository.NotificationInboxRepository;
import com.ocbs.repository.NotificationRepository;
import com.ocbs.repository.PatientProfileRepository;
import com.ocbs.repository.UserRepository;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Kafka Consumer in Notification Service.
 * Automatically generates in-app notifications for patients and doctors when appointment events occur.
 *
 * Guaranteed Features:
 * - Persistent Idempotency via NotificationInbox (no duplicate notifications upon retry).
 * - Privacy-compliant dynamic recipient resolution via DoctorProfileRepository / PatientProfileRepository.
 *   Never generates artificial logins ('patient1', 'doctor1') when lookup fails.
 * - Does not leak sensitive private reasons or medical notes in notification messages or logs.
 * - Strict contract validation: null events, invalid version, unknown types, or missing mandatory fields trigger Kafka DLT.
 */
@Service
public class NotificationEventConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationEventConsumer.class);
    public static final String CONSUMER_GROUP = "notification-service-group";

    private static final Set<String> SUPPORTED_EVENT_TYPES = Set.of(
        "APPOINTMENT_BOOKED",
        "APPOINTMENT_CONFIRMED",
        "APPOINTMENT_ACCEPTED",
        "APPOINTMENT_STARTED",
        "APPOINTMENT_REJECTED",
        "APPOINTMENT_CANCELLED",
        "APPOINTMENT_COMPLETED",
        "APPOINTMENT_RESCHEDULED"
    );

    private final NotificationRepository notificationRepository;
    private final NotificationInboxRepository notificationInboxRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final UserRepository userRepository;
    private final com.ocbs.service.EmailTaskProducer emailTaskProducer;

    @org.springframework.beans.factory.annotation.Value("${jhipster.mail.base-url:${application.base-url:http://localhost}}")
    private String baseUrl = "http://localhost";

    public NotificationEventConsumer(
        NotificationRepository notificationRepository,
        NotificationInboxRepository notificationInboxRepository,
        DoctorProfileRepository doctorProfileRepository,
        PatientProfileRepository patientProfileRepository,
        UserRepository userRepository,
        com.ocbs.service.EmailTaskProducer emailTaskProducer
    ) {
        this.notificationRepository = notificationRepository;
        this.notificationInboxRepository = notificationInboxRepository;
        this.doctorProfileRepository = doctorProfileRepository;
        this.patientProfileRepository = patientProfileRepository;
        this.userRepository = userRepository;
        this.emailTaskProducer = emailTaskProducer;
    }

    @KafkaListener(
        topics = "opcbs-appointment-events",
        groupId = CONSUMER_GROUP,
        autoStartup = "${application.kafka.enabled:false}"
    )
    @Transactional
    public void handleAppointmentEvent(AppointmentEvent event) {
        // 1. Strict Validation: Null check, Schema contract, Supported version and types
        validateEvent(event);

        LOG.info("📥 [Notification Service Kafka] Processing Event: Type='{}', ID='{}', ApptId={}, BookingCode='{}', PatientId={}, DoctorId={}",
            event.getEventType(), event.getEventId(), event.getAppointmentId(), event.getBookingCode(),
            event.getPatientId(), event.getDoctorId());

        String eventType = event.getEventType().toUpperCase();

        // 2. Check Idempotency via NotificationInbox
        String idempotencyKey = event.getIdempotencyKey() != null
            ? event.getIdempotencyKey()
            : "APPT_" + event.getBookingCode() + "_" + eventType;

        if (event.getEventId() != null && notificationInboxRepository.existsByEventId(event.getEventId())) {
            LOG.info("⚡ [Notification Idempotency] Event ID '{}' already processed. Skipping duplicate dispatch.", event.getEventId());
            return;
        }

        if (notificationInboxRepository.existsByConsumerGroupAndIdempotencyKey(CONSUMER_GROUP, idempotencyKey)) {
            LOG.info("⚡ [Notification Idempotency] IdempotencyKey '{}' already processed for group '{}'. Skipping duplicate.",
                idempotencyKey, CONSUMER_GROUP);
            return;
        }

        // 3. Resolve Dynamic Recipients via Local Repositories using Entity IDs
        RecipientInfo doctorRecipient = resolveDoctorRecipient(event);
        RecipientInfo patientRecipient = resolvePatientRecipient(event);
        validateRecipients(eventType, patientRecipient, doctorRecipient);

        // 4. Process Business Logic according to Event Type
        switch (eventType) {
            case "APPOINTMENT_BOOKED" -> {
                if (doctorRecipient.login != null) {
                    Notification doctorNotif = new Notification();
                    doctorNotif.setUserLogin(doctorRecipient.login);
                    doctorNotif.setTitle("📅 Có Lịch Khám Mới Chờ Duyệt!");
                    String patientDisplayName = patientRecipient.name != null ? patientRecipient.name : "Bệnh nhân";
                    doctorNotif.setMessage(patientDisplayName + " vừa đặt lịch khám mã " + event.getBookingCode() + ". Vui lòng vào duyệt lịch.");
                    doctorNotif.setNotificationType("APPOINTMENT");
                    doctorNotif.setRelatedEntityId(event.getAppointmentId());
                    doctorNotif.setRelatedEntityType("Appointment");
                    doctorNotif.setActionUrl("/appointment");
                    doctorNotif.setChannel("IN_APP");
                    doctorNotif.setDeliveryStatus("DELIVERED");
                    notificationRepository.save(doctorNotif);
                    LOG.info("✅ [Notification Service] In-App booking notification created for doctor '{}'", doctorRecipient.login);
                }

                if (patientRecipient.login != null) {
                    Notification patientNotif = new Notification();
                    patientNotif.setUserLogin(patientRecipient.login);
                    patientNotif.setTitle("✅ Đặt Lịch Khám Thành Công!");
                    String doctorDisplayName = doctorRecipient.name != null ? (" với " + doctorRecipient.name) : "";
                    patientNotif.setMessage("Lịch hẹn mã " + event.getBookingCode() + doctorDisplayName +
                        " đã được tiếp nhận và đang chờ bác sĩ xác nhận.");
                    patientNotif.setNotificationType("APPOINTMENT");
                    patientNotif.setRelatedEntityId(event.getAppointmentId());
                    patientNotif.setRelatedEntityType("Appointment");
                    patientNotif.setActionUrl("/appointment");
                    patientNotif.setChannel("IN_APP");
                    patientNotif.setDeliveryStatus("DELIVERED");
                    notificationRepository.save(patientNotif);
                    LOG.info("✅ [Notification Service] In-App booking notification created for patient '{}'", patientRecipient.login);
                }

                String bookingCode = event.getBookingCode();
                if (patientRecipient.email != null && !patientRecipient.email.isBlank()) {
                    enqueueAppointmentEmail(
                        patientRecipient.email,
                        patientRecipient.name,
                        bookingCode,
                        "[MindBridge] Đã tiếp nhận yêu cầu đặt lịch " + bookingCode,
                        "Yêu Cầu Đặt Lịch Đã Được Tiếp Nhận",
                        "#0284c7",
                        "#e0f2fe",
                        "Lịch hẹn của bạn đã được hệ thống ghi nhận thành công và đang chờ bác sĩ xác nhận khung giờ khám.",
                        "/appointment",
                        "EMAIL_APPT_BOOKED_PATIENT_" + bookingCode
                    );
                }
                if (doctorRecipient.email != null && !doctorRecipient.email.isBlank()) {
                    enqueueAppointmentEmail(
                        doctorRecipient.email,
                        doctorRecipient.name,
                        bookingCode,
                        "[MindBridge] Có yêu cầu đặt lịch mới " + bookingCode,
                        "Yêu Cầu Đặt Lịch Mới Cần Duyệt",
                        "#0f766e",
                        "#ccfbf1",
                        "Bệnh nhân " + (patientRecipient.name != null ? patientRecipient.name : "") + " vừa đặt lịch hẹn mới. Vui lòng vào hệ thống kiểm tra và xác nhận.",
                        "/doctor/appointments",
                        "EMAIL_APPT_BOOKED_DOCTOR_" + bookingCode
                    );
                }
            }
            case "APPOINTMENT_ACCEPTED", "APPOINTMENT_CONFIRMED" -> {
                String doctorDisplayName = doctorRecipient.name != null ? ("Bác sĩ " + doctorRecipient.name) : "Bác sĩ";
                if (patientRecipient.login != null) {
                    Notification patientNotif = new Notification();
                    patientNotif.setUserLogin(patientRecipient.login);
                    patientNotif.setTitle("🎉 Bác Sĩ Đã Chấp Nhận Lịch Khám!");
                    patientNotif.setMessage(doctorDisplayName + " đã xác nhận lịch hẹn mã " + event.getBookingCode() + ". Vui lòng tham gia đúng giờ hẹn.");
                    patientNotif.setNotificationType("APPOINTMENT");
                    patientNotif.setRelatedEntityId(event.getAppointmentId());
                    patientNotif.setRelatedEntityType("Appointment");
                    patientNotif.setActionUrl("/appointment");
                    patientNotif.setChannel("IN_APP");
                    patientNotif.setDeliveryStatus("DELIVERED");
                    notificationRepository.save(patientNotif);
                    LOG.info("✅ [Notification Service] In-App acceptance notification sent to patient '{}'", patientRecipient.login);
                }

                if (patientRecipient.email != null && !patientRecipient.email.isBlank()) {
                    enqueueAppointmentEmail(
                        patientRecipient.email,
                        patientRecipient.name,
                        event.getBookingCode(),
                        "[MindBridge] Lịch khám mã " + event.getBookingCode() + " đã được xác nhận!",
                        "Lịch Khám Đã Được Xác Nhận",
                        "#16a34a",
                        "#dcfce7",
                        doctorDisplayName + " đã xác nhận lịch hẹn của bạn. Bạn vui lòng chuẩn bị tham gia buổi tư vấn đúng thời gian đã hẹn.",
                        "/appointment",
                        "EMAIL_APPT_ACCEPT_" + event.getBookingCode()
                    );
                }
                if (doctorRecipient.email != null && !doctorRecipient.email.isBlank()) {
                    enqueueAppointmentEmail(
                        doctorRecipient.email,
                        doctorRecipient.name,
                        event.getBookingCode(),
                        "[MindBridge] Xác nhận lịch khám mã " + event.getBookingCode(),
                        "Lịch Khám Đã Được Xác Nhận",
                        "#16a34a",
                        "#dcfce7",
                        "Bạn đã xác nhận ca khám mã " + event.getBookingCode() + " với bệnh nhân " + (patientRecipient.name != null ? patientRecipient.name : "") + ".",
                        "/doctor/appointments",
                        "EMAIL_APPT_ACCEPT_DR_" + event.getBookingCode()
                    );
                }
            }
            case "APPOINTMENT_STARTED" -> {
                if (patientRecipient.login != null) {
                    Notification patientNotif = new Notification();
                    patientNotif.setUserLogin(patientRecipient.login);
                    patientNotif.setTitle("Ca khám đang bắt đầu");
                    patientNotif.setMessage("Bác sĩ đã bắt đầu phiên tư vấn cho lịch hẹn mã " + event.getBookingCode() + ". Vui lòng chuẩn bị tham gia.");
                    patientNotif.setNotificationType("APPOINTMENT");
                    patientNotif.setRelatedEntityId(event.getAppointmentId());
                    patientNotif.setRelatedEntityType("Appointment");
                    patientNotif.setActionUrl("/appointment");
                    patientNotif.setChannel("IN_APP");
                    patientNotif.setDeliveryStatus("DELIVERED");
                    notificationRepository.save(patientNotif);
                }

                if (patientRecipient.email != null && !patientRecipient.email.isBlank()) {
                    enqueueAppointmentEmail(
                        patientRecipient.email,
                        patientRecipient.name,
                        event.getBookingCode(),
                        "[MindBridge] Ca khám " + event.getBookingCode() + " đang diễn ra",
                        "Ca Khám Đang Diễn Ra",
                        "#d97706",
                        "#fef3c7",
                        "Bác sĩ đã bắt đầu phiên tham vấn cho lịch khám mã " + event.getBookingCode() + ". Vui lòng vào phòng tư vấn trực tuyến để bắt đầu ca khám.",
                        "/appointment",
                        "EMAIL_APPT_STARTED_" + event.getBookingCode()
                    );
                }
            }
            case "APPOINTMENT_REJECTED" -> {
                if (patientRecipient.login != null) {
                    Notification patientNotif = new Notification();
                    patientNotif.setUserLogin(patientRecipient.login);
                    patientNotif.setTitle("Lịch hẹn đã bị từ chối");
                    patientNotif.setMessage("Bác sĩ đã từ chối lịch hẹn mã " + event.getBookingCode() + ". Bạn có thể chọn thời gian hoặc bác sĩ khác.");
                    patientNotif.setNotificationType("APPOINTMENT");
                    patientNotif.setRelatedEntityId(event.getAppointmentId());
                    patientNotif.setRelatedEntityType("Appointment");
                    patientNotif.setActionUrl("/appointment");
                    patientNotif.setChannel("IN_APP");
                    patientNotif.setDeliveryStatus("DELIVERED");
                    notificationRepository.save(patientNotif);
                }

                if (patientRecipient.email != null && !patientRecipient.email.isBlank()) {
                    enqueueAppointmentEmail(
                        patientRecipient.email,
                        patientRecipient.name,
                        event.getBookingCode(),
                        "[MindBridge] Lịch hẹn mã " + event.getBookingCode() + " không thể thực hiện",
                        "Lịch Hẹn Bị Từ Chối",
                        "#dc2626",
                        "#fee2e2",
                        "Bác sĩ không thể tiếp nhận lịch hẹn mã " + event.getBookingCode() + ". Bạn có thể đăng nhập MindBridge để chọn khung giờ hoặc bác sĩ khác phù hợp hơn.",
                        "/appointment",
                        "EMAIL_APPT_REJECT_" + event.getBookingCode()
                    );
                }
            }
            case "APPOINTMENT_CANCELLED" -> {
                if (patientRecipient.login != null) {
                    Notification patientNotif = new Notification();
                    patientNotif.setUserLogin(patientRecipient.login);
                    patientNotif.setTitle("⚠️ Lịch Hẹn Đã Bị Hủy");
                    patientNotif.setMessage("Lịch khám mã " + event.getBookingCode() + " đã được hủy.");
                    patientNotif.setNotificationType("APPOINTMENT");
                    patientNotif.setRelatedEntityId(event.getAppointmentId());
                    patientNotif.setRelatedEntityType("Appointment");
                    patientNotif.setActionUrl("/appointment");
                    patientNotif.setChannel("IN_APP");
                    patientNotif.setDeliveryStatus("DELIVERED");
                    notificationRepository.save(patientNotif);
                }
                if (doctorRecipient.login != null) {
                    Notification doctorNotif = new Notification();
                    doctorNotif.setUserLogin(doctorRecipient.login);
                    doctorNotif.setTitle("⚠️ Lịch Hẹn Đã Bị Hủy");
                    doctorNotif.setMessage("Lịch khám mã " + event.getBookingCode() + " đã được hủy.");
                    doctorNotif.setNotificationType("APPOINTMENT");
                    doctorNotif.setRelatedEntityId(event.getAppointmentId());
                    doctorNotif.setRelatedEntityType("Appointment");
                    doctorNotif.setActionUrl("/doctor/appointments");
                    doctorNotif.setChannel("IN_APP");
                    doctorNotif.setDeliveryStatus("DELIVERED");
                    notificationRepository.save(doctorNotif);
                }

                if (patientRecipient.email != null && !patientRecipient.email.isBlank()) {
                    enqueueAppointmentEmail(
                        patientRecipient.email,
                        patientRecipient.name,
                        event.getBookingCode(),
                        "[MindBridge] Thông báo hủy lịch khám mã " + event.getBookingCode(),
                        "Lịch Khám Đã Bị Hủy",
                        "#dc2626",
                        "#fee2e2",
                        "Lịch khám mã " + event.getBookingCode() + " của bạn đã được hủy trên hệ thống. Nếu có nhu cầu tiếp tục tư vấn, bạn có thể đăng nhập để đặt lịch mới.",
                        "/appointment",
                        "EMAIL_APPT_CANCEL_PATIENT_" + event.getBookingCode()
                    );
                }
                if (doctorRecipient.email != null && !doctorRecipient.email.isBlank()) {
                    enqueueAppointmentEmail(
                        doctorRecipient.email,
                        doctorRecipient.name,
                        event.getBookingCode(),
                        "[MindBridge] Lịch khám mã " + event.getBookingCode() + " đã hủy",
                        "Lịch Khám Đã Bị Hủy",
                        "#dc2626",
                        "#fee2e2",
                        "Lịch khám mã " + event.getBookingCode() + " với bệnh nhân " + (patientRecipient.name != null ? patientRecipient.name : "") + " đã được hủy. Khung giờ khám đã được giải phóng.",
                        "/doctor/appointments",
                        "EMAIL_APPT_CANCEL_DOCTOR_" + event.getBookingCode()
                    );
                }
                LOG.info("✅ [Notification Service] In-App & Email cancellation notifications dispatched for booking '{}'", event.getBookingCode());
            }
            case "APPOINTMENT_COMPLETED" -> {
                if (patientRecipient.login != null) {
                    Notification patientNotif = new Notification();
                    patientNotif.setUserLogin(patientRecipient.login);
                    patientNotif.setTitle("📋 Hồ Sơ Bệnh Án & Lời Dặn Của Bác Sĩ");
                    patientNotif.setMessage("Ca khám mã " + event.getBookingCode() + " đã hoàn tất. Bác sĩ đã lưu hồ sơ bệnh án và phác đồ điều trị của bạn.");
                    patientNotif.setNotificationType("CLINICAL");
                    patientNotif.setRelatedEntityId(event.getAppointmentId());
                    patientNotif.setRelatedEntityType("Appointment");
                    patientNotif.setActionUrl("/patient/clinical-records");
                    patientNotif.setChannel("IN_APP");
                    patientNotif.setDeliveryStatus("DELIVERED");
                    notificationRepository.save(patientNotif);
                    LOG.info("✅ [Notification Service] In-App clinical completion notification sent to patient '{}'", patientRecipient.login);
                }

                if (patientRecipient.email != null && !patientRecipient.email.isBlank()) {
                    enqueueAppointmentEmail(
                        patientRecipient.email,
                        patientRecipient.name,
                        event.getBookingCode(),
                        "[MindBridge] Ca khám mã " + event.getBookingCode() + " đã hoàn tất - Hồ sơ bệnh án & lời dặn",
                        "Ca Khám Đã Hoàn Tất",
                        "#7c3aed",
                        "#ede9fe",
                        "Ca khám mã " + event.getBookingCode() + " đã kết thúc thành công. Bác sĩ đã cập nhật chẩn đoán lâm sàng, phác đồ điều trị và bài tập phục hồi cho bạn.",
                        "/patient/clinical-records",
                        "EMAIL_APPT_COMPLETED_" + event.getBookingCode()
                    );
                }
            }
            case "APPOINTMENT_RESCHEDULED" -> {
                if (patientRecipient.login != null) {
                    Notification patientNotif = new Notification();
                    patientNotif.setUserLogin(patientRecipient.login);
                    patientNotif.setTitle("🔄 Lịch Hẹn Đã Được Dời Khung Giờ Mới");
                    patientNotif.setMessage("Lịch khám mã " + event.getBookingCode() + " đã được dời sang thời gian mới. Vui lòng xem chi tiết.");
                    patientNotif.setNotificationType("APPOINTMENT");
                    patientNotif.setRelatedEntityId(event.getAppointmentId());
                    patientNotif.setRelatedEntityType("Appointment");
                    patientNotif.setActionUrl("/appointment");
                    patientNotif.setChannel("IN_APP");
                    patientNotif.setDeliveryStatus("DELIVERED");
                    notificationRepository.save(patientNotif);
                }
                if (doctorRecipient.login != null) {
                    Notification doctorNotif = new Notification();
                    doctorNotif.setUserLogin(doctorRecipient.login);
                    doctorNotif.setTitle("🔄 Lịch Hẹn Đã Được Dời Khung Giờ Mới");
                    doctorNotif.setMessage("Lịch khám mã " + event.getBookingCode() + " đã được cập nhật lịch hẹn mới.");
                    doctorNotif.setNotificationType("APPOINTMENT");
                    doctorNotif.setRelatedEntityId(event.getAppointmentId());
                    doctorNotif.setRelatedEntityType("Appointment");
                    doctorNotif.setActionUrl("/doctor/appointments");
                    doctorNotif.setChannel("IN_APP");
                    doctorNotif.setDeliveryStatus("DELIVERED");
                    notificationRepository.save(doctorNotif);
                }

                if (patientRecipient.email != null && !patientRecipient.email.isBlank()) {
                    enqueueAppointmentEmail(
                        patientRecipient.email,
                        patientRecipient.name,
                        event.getBookingCode(),
                        "[MindBridge] Lịch hẹn " + event.getBookingCode() + " đã được dời lịch",
                        "Lịch Hẹn Đã Dời Sang Khung Giờ Mới",
                        "#0284c7",
                        "#e0f2fe",
                        "Lịch khám mã " + event.getBookingCode() + " của bạn đã được dời sang khung giờ mới thành công. Vui lòng đăng nhập để kiểm tra thời gian cụ thể.",
                        "/appointment",
                        "EMAIL_APPT_RESCHEDULE_PATIENT_" + event.getBookingCode()
                    );
                }
                if (doctorRecipient.email != null && !doctorRecipient.email.isBlank()) {
                    enqueueAppointmentEmail(
                        doctorRecipient.email,
                        doctorRecipient.name,
                        event.getBookingCode(),
                        "[MindBridge] Lịch hẹn " + event.getBookingCode() + " đã được dời lịch",
                        "Lịch Hẹn Đã Dời Sang Khung Giờ Mới",
                        "#0284c7",
                        "#e0f2fe",
                        "Lịch khám mã " + event.getBookingCode() + " đã được cập nhật thời gian mới trên lịch làm việc của bạn.",
                        "/doctor/appointments",
                        "EMAIL_APPT_RESCHEDULE_DOCTOR_" + event.getBookingCode()
                    );
                }
            }
            default -> LOG.info("ℹ️ [Notification Service] Event type '{}' is valid but intentionally has no notification template. Recorded inbox.", eventType);
        }

        // 5. Record Inbox Entity within the SAME Database Transaction
        NotificationInbox inbox = new NotificationInbox(
            event.getEventId() != null ? event.getEventId() : java.util.UUID.randomUUID().toString(),
            idempotencyKey,
            eventType,
            CONSUMER_GROUP
        );
        notificationInboxRepository.save(inbox);
        LOG.info("💾 [Notification Service] Saved NotificationInbox for event '{}'", inbox.getEventId());
    }

    private void enqueueAppointmentEmail(
        String to,
        String recipientName,
        String bookingCode,
        String subject,
        String statusTitle,
        String statusBadgeColor,
        String statusBadgeBg,
        String safeMessage,
        String actionUrl,
        String idempotencyKey
    ) {
        String escapedName = org.springframework.web.util.HtmlUtils.htmlEscape(
            recipientName != null && !recipientName.isBlank() ? recipientName : "Quý khách"
        );
        String escapedCode = org.springframework.web.util.HtmlUtils.htmlEscape(bookingCode);
        String escapedTitle = org.springframework.web.util.HtmlUtils.htmlEscape(statusTitle);
        String escapedMessage = org.springframework.web.util.HtmlUtils.htmlEscape(safeMessage);

        String domain = baseUrl != null && !baseUrl.isBlank() ? baseUrl : "http://localhost";
        if (domain.endsWith("/")) {
            domain = domain.substring(0, domain.length() - 1);
        }
        String targetAction = actionUrl != null ? actionUrl : "/appointment";
        String fullActionUrl = targetAction.startsWith("http") ? targetAction : (targetAction.startsWith("/") ? domain + targetAction : domain + "/" + targetAction);

        String html = "<!DOCTYPE html><html><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                "<style>" +
                "body{margin:0;padding:24px;background-color:#f1f5f9;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif;color:#1e293b;line-height:1.6;}" +
                ".email-wrap{max-width:580px;margin:0 auto;background:#ffffff;border-radius:16px;overflow:hidden;box-shadow:0 4px 12px rgba(0,0,0,0.06);border:1px solid #e2e8f0;}" +
                ".email-head{background:linear-gradient(135deg,#0f766e 0%,#0d9488 100%);color:#ffffff;padding:32px 28px;text-align:center;}" +
                ".brand-title{font-size:24px;font-weight:800;letter-spacing:-0.5px;margin:0 0 6px 0;}" +
                ".brand-sub{font-size:13px;opacity:0.9;margin:0;text-transform:uppercase;letter-spacing:1px;font-weight:600;}" +
                ".email-body{padding:32px 28px;}" +
                ".status-pill{display:inline-block;padding:8px 18px;border-radius:9999px;font-size:13px;font-weight:700;letter-spacing:0.3px;margin-bottom:20px;background:" + statusBadgeBg + ";color:" + statusBadgeColor + ";}" +
                ".greeting{font-size:17px;font-weight:700;color:#0f172a;margin:0 0 14px 0;}" +
                ".message-text{font-size:15px;color:#334155;margin:0 0 24px 0;}" +
                ".info-card{background:#f8fafc;border:1px solid #e2e8f0;border-radius:12px;padding:16px 20px;margin-bottom:26px;}" +
                ".info-row{display:flex;justify-content:space-between;padding:8px 0;font-size:14px;border-bottom:1px dashed #cbd5e1;}" +
                ".info-row:last-child{border-bottom:none;}" +
                ".info-lbl{color:#64748b;font-weight:600;}" +
                ".info-val{color:#0f172a;font-weight:700;}" +
                ".btn-cta{display:inline-block;background:#0f766e;color:#ffffff !important;text-decoration:none;padding:14px 32px;border-radius:10px;font-weight:700;font-size:15px;text-align:center;}" +
                ".email-footer{background:#f8fafc;padding:24px;text-align:center;font-size:12px;color:#64748b;border-top:1px solid #e2e8f0;}" +
                "</style></head><body>" +
                "<div class=\"email-wrap\">" +
                "<div class=\"email-head\"><h1 class=\"brand-title\">MindBridge Health Care</h1><p class=\"brand-sub\">Hệ Thống Y Tế &amp; Tham Vấn Tâm Lý Trực Tuyến</p></div>" +
                "<div class=\"email-body\">" +
                "<div class=\"status-pill\">● " + escapedTitle + "</div>" +
                "<p class=\"greeting\">Kính gửi " + escapedName + ",</p>" +
                "<p class=\"message-text\">" + escapedMessage + "</p>" +
                "<div class=\"info-card\">" +
                "<div class=\"info-row\"><span class=\"info-lbl\">Mã Lịch Hẹn:</span><span class=\"info-val\">#" + escapedCode + "</span></div>" +
                "<div class=\"info-row\"><span class=\"info-lbl\">Trạng Thái:</span><span class=\"info-val\" style=\"color:" + statusBadgeColor + ";\">" + escapedTitle + "</span></div>" +
                "<div class=\"info-row\"><span class=\"info-lbl\">Thời Gian Ghi Nhận:</span><span class=\"info-val\">" + java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(java.time.ZoneId.of("Asia/Ho_Chi_Minh")).format(java.time.Instant.now()) + "</span></div>" +
                "</div>" +
                "<div style=\"text-align:center;margin-top:20px;\">" +
                "<a href=\"" + fullActionUrl + "\" class=\"btn-cta\">Đăng Nhập Xem Chi Tiết Lịch</a>" +
                "</div></div>" +
                "<div class=\"email-footer\"><p style=\"margin:0 0 6px 0;\">© 2026 MindBridge OPCBS. Mọi thông tin y tế được bảo mật tuyệt đối.</p>" +
                "<p style=\"margin:0;\">Email này được gửi tự động qua hệ thống hàng đợi RabbitMQ Worker.</p></div></div>" +
                "</body></html>";

            com.ocbs.service.dto.EmailTaskDTO task = new com.ocbs.service.dto.EmailTaskDTO(
                to, subject, html, false, true, "APPOINTMENT_STATUS_UPDATE"
            );
            task.setIdempotencyKey(idempotencyKey);
            emailTaskProducer.enqueueEmailTask(task);
            LOG.info("📤 [Notification Service] Enqueued email via RabbitMQ for '{}' (Subject: '{}')", to, subject);
        }

    private RecipientInfo resolveDoctorRecipient(AppointmentEvent event) {
        String login = null;
        String name = null;
        String email = event.getDoctorEmail();

        if (event.getDoctorId() != null) {
            DoctorProfile doctor = doctorProfileRepository.findOneWithToOneRelationships(event.getDoctorId()).orElse(null);
            if (doctor != null) {
                if (doctor.getUser() != null) {
                    if (login == null) login = doctor.getUser().getLogin();
                    if (email == null || email.isBlank()) email = doctor.getUser().getEmail();
                }
                if (name == null) name = doctor.getFullName();
            }
        }

        // Legacy fallback during transitional phase
        if (login == null && event.getDoctorLogin() != null && !event.getDoctorLogin().isBlank()) {
            login = event.getDoctorLogin();
        }
        if (name == null && event.getDoctorName() != null && !event.getDoctorName().isBlank()) {
            name = event.getDoctorName();
        }

        if ((email == null || email.isBlank()) && login != null) {
            email = userRepository.findOneByLogin(login).map(com.ocbs.domain.User::getEmail).orElse(null);
        }

        if ((email == null || email.isBlank()) && login != null && login.contains("@")) {
            email = login;
        }

        if (email == null || email.isBlank()) {
            LOG.warn("⚠️ [Notification Service] Could not resolve email address for doctor (id={}, login='{}', name='{}')",
                event.getDoctorId(), login, name);
        }

        return new RecipientInfo(login, name, email);
    }

    private RecipientInfo resolvePatientRecipient(AppointmentEvent event) {
        String login = null;
        String name = null;
        String email = event.getPatientEmail();

        if (event.getPatientId() != null) {
            PatientProfile patient = patientProfileRepository.findOneWithToOneRelationships(event.getPatientId()).orElse(null);
            if (patient != null) {
                if (patient.getUser() != null) {
                    if (login == null) login = patient.getUser().getLogin();
                    if (email == null || email.isBlank()) email = patient.getUser().getEmail();
                }
                if (name == null) name = patient.getFullName();
            }
        }

        // Legacy fallback during transitional phase
        if (login == null && event.getPatientLogin() != null && !event.getPatientLogin().isBlank()) {
            login = event.getPatientLogin();
        }
        if (name == null && event.getPatientName() != null && !event.getPatientName().isBlank()) {
            name = event.getPatientName();
        }

        if ((email == null || email.isBlank()) && login != null) {
            email = userRepository.findOneByLogin(login).map(com.ocbs.domain.User::getEmail).orElse(null);
        }

        if ((email == null || email.isBlank()) && login != null && login.contains("@")) {
            email = login;
        }

        if (email == null || email.isBlank()) {
            LOG.warn("⚠️ [Notification Service] Could not resolve email address for patient (id={}, login='{}', name='{}')",
                event.getPatientId(), login, name);
        }

        return new RecipientInfo(login, name, email);
    }

    private void validateRecipients(String eventType, RecipientInfo patient, RecipientInfo doctor) {
        boolean patientRequired = true;
        boolean doctorRequired = "APPOINTMENT_BOOKED".equals(eventType) || "APPOINTMENT_CANCELLED".equals(eventType);
        if ((patientRequired && (patient.login == null || patient.login.isBlank())) ||
            (doctorRequired && (doctor.login == null || doctor.login.isBlank()))) {
            // Do not commit the inbox for a valid event whose recipient lookup failed; retries can recover transient DB issues.
            throw new IllegalStateException("Could not resolve required appointment notification recipient(s) for " + eventType);
        }
    }

    private void validateEvent(AppointmentEvent event) {
        if (event == null) {
            LOG.error("❌ [Notification Service] Received null event record. Triggering DLT.");
            throw new IllegalArgumentException("Received null event record. Cannot process.");
        }

        if (event.getEventId() == null || event.getEventId().isBlank()) {
            LOG.error("❌ [Notification Service] Event is missing mandatory eventId. Triggering DLT.");
            throw new IllegalArgumentException("Event is missing mandatory eventId");
        }

        String version = event.getEventVersion();
        if (version == null || !version.startsWith("1.")) {
            LOG.error("❌ [Notification Service] Unsupported event contract version '{}' for event ID '{}'. Will trigger DLT.",
                version, event.getEventId());
            throw new IllegalArgumentException("Unsupported event contract version: " + version + ". Supported versions: 1.x");
        }

        if (event.getEventType() == null || event.getEventType().isBlank()) {
            LOG.error("❌ [Notification Service] Event ID '{}' is missing mandatory eventType. Triggering DLT.", event.getEventId());
            throw new IllegalArgumentException("Event is missing mandatory eventType");
        }

        if (!SUPPORTED_EVENT_TYPES.contains(event.getEventType().toUpperCase())) {
            LOG.error("❌ [Notification Service] Unrecognized eventType '{}' for event ID '{}'. Triggering DLT.",
                event.getEventType(), event.getEventId());
            throw new IllegalArgumentException("Unrecognized event type: " + event.getEventType());
        }

        if (event.getAppointmentId() == null) {
            LOG.error("❌ [Notification Service] Event ID '{}' is missing mandatory appointmentId. Triggering DLT.", event.getEventId());
            throw new IllegalArgumentException("Event is missing mandatory appointmentId");
        }

        if (event.getBookingCode() == null || event.getBookingCode().isBlank()) {
            LOG.error("❌ [Notification Service] Event ID '{}' is missing mandatory bookingCode. Triggering DLT.", event.getEventId());
            throw new IllegalArgumentException("Event is missing mandatory bookingCode");
        }
    }

    private static class RecipientInfo {
        final String login;
        final String name;
        final String email;

        RecipientInfo(String login, String name, String email) {
            this.login = login;
            this.name = name;
            this.email = email;
        }
    }
}
