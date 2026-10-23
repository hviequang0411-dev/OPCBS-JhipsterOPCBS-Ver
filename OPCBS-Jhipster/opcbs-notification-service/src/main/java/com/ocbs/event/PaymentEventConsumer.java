package com.ocbs.event;

import com.ocbs.domain.DoctorProfile;
import com.ocbs.domain.Notification;
import com.ocbs.domain.NotificationInbox;
import com.ocbs.repository.DoctorProfileRepository;
import com.ocbs.repository.NotificationInboxRepository;
import com.ocbs.repository.NotificationRepository;
import com.ocbs.service.EmailTaskProducer;
import com.ocbs.service.dto.EmailTaskDTO;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Kafka Consumer in Notification Service for Payment and Subscription Events.
 * Topic: opcbs-payment-events
 * Group: notification-payment-group
 *
 * Guarantees:
 * - Persistent idempotency check via NotificationInbox.
 * - Creates in-app notification for the paying doctor.
 * - Dispatches asynchronous receipt/confirmation email via RabbitMQ worker queue.
 * - Strictly avoids leaking credit card numbers or banking secrets.
 */
@Service
public class PaymentEventConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(PaymentEventConsumer.class);
    public static final String CONSUMER_GROUP = "notification-payment-group";

    private final NotificationRepository notificationRepository;
    private final NotificationInboxRepository inboxRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final com.ocbs.repository.UserRepository userRepository;
    private final EmailTaskProducer emailTaskProducer;

    public PaymentEventConsumer(
        NotificationRepository notificationRepository,
        NotificationInboxRepository inboxRepository,
        DoctorProfileRepository doctorProfileRepository,
        com.ocbs.repository.UserRepository userRepository,
        EmailTaskProducer emailTaskProducer
    ) {
        this.notificationRepository = notificationRepository;
        this.inboxRepository = inboxRepository;
        this.doctorProfileRepository = doctorProfileRepository;
        this.userRepository = userRepository;
        this.emailTaskProducer = emailTaskProducer;
    }

    @KafkaListener(
        topics = "opcbs-payment-events",
        groupId = CONSUMER_GROUP,
        containerFactory = "paymentKafkaListenerContainerFactory",
        autoStartup = "${application.kafka.enabled:false}"
    )
    @Transactional
    public void handlePaymentEvent(PaymentEvent event) {
        if (event == null || event.getEventId() == null || event.getEventType() == null ||
            event.getEventVersion() == null || !event.getEventVersion().startsWith("1.")) {
            throw new IllegalArgumentException("Payment event is null or has an invalid eventId, eventType, or version");
        }

        String eventType = event.getEventType().toUpperCase(java.util.Locale.ROOT);
        if (!java.util.Set.of("PAYMENT_SUCCESS", "PAYMENT_COMPLETED", "PAYMENT_FAILED", "PAYMENT_REFUNDED").contains(eventType)) {
            throw new IllegalArgumentException("Unsupported payment event type: " + eventType);
        }

        LOG.info("📥 [Payment Notification Kafka] Received: Event ID '{}', Type='{}', TxnCode='{}', Doctor='{}', Amount={}",
            event.getEventId(), event.getEventType(), event.getTransactionCode(), event.getDoctorLogin(), event.getAmount());

        String idempotencyKey = event.getIdempotencyKey() != null
            ? event.getIdempotencyKey()
            : "PAY_" + event.getTransactionCode() + "_" + event.getEventType();

        // 1. Idempotency Check
        if (inboxRepository.existsByEventId(event.getEventId()) ||
            inboxRepository.existsByConsumerGroupAndIdempotencyKey(CONSUMER_GROUP, idempotencyKey)) {
            LOG.info("⚡ [Payment Notification Idempotency] Event '{}' already processed for group '{}'. Skipping duplicate.",
                event.getEventId(), CONSUMER_GROUP);
            return;
        }

        String doctorLogin = event.getDoctorLogin();
        String doctorEmail = null;
        String doctorName = "Bác sĩ";

        // Lookup doctor details dynamically from database
        if (doctorLogin != null) {
            DoctorProfile doctorProfile = doctorProfileRepository.findByUser_Login(doctorLogin).orElse(null);
            if (doctorProfile != null) {
                if (doctorProfile.getFullName() != null && !doctorProfile.getFullName().isBlank()) {
                    doctorName = doctorProfile.getFullName();
                }
                if (doctorProfile.getUser() != null && doctorProfile.getUser().getEmail() != null) {
                    doctorEmail = doctorProfile.getUser().getEmail();
                }
            }
            if (doctorEmail == null || doctorEmail.isBlank()) {
                doctorEmail = userRepository.findOneByLogin(doctorLogin).map(com.ocbs.domain.User::getEmail).orElse(null);
            }
            if ((doctorEmail == null || doctorEmail.isBlank()) && doctorLogin.contains("@")) {
                doctorEmail = doctorLogin;
            }
        }

        // 2. Process by Event Type
        if ("PAYMENT_SUCCESS".equals(eventType) || "PAYMENT_COMPLETED".equals(eventType)) {
            // In-App Notification
            if (doctorLogin != null) {
                Notification notif = new Notification();
                notif.setUserLogin(doctorLogin);
                notif.setTitle("🎉 Kích Hoạt Gói Dịch Vụ Thành Công!");
                String expireDateStr = event.getExpirationDate() != null
                    ? DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault()).format(event.getExpirationDate())
                    : "theo quy định";
                notif.setMessage("Gói dịch vụ '" + event.getServicePackageName() +
                    "' đã được kích hoạt thành công. Hạn sử dụng đến: " + expireDateStr + ".");
                notif.setNotificationType("PAYMENT");
                notif.setRelatedEntityId(event.getSubscriptionId());
                notif.setRelatedEntityType("DoctorSubscription");
                notif.setActionUrl("/doctor/subscription");
                notif.setChannel("IN_APP");
                notif.setDeliveryStatus("DELIVERED");
                notificationRepository.save(notif);
                LOG.info("✅ [Payment Notification] Created in-app notification for doctor '{}'", doctorLogin);
            }

            // Asynchronous Email Dispatch via RabbitMQ Worker Queue
            if (doctorEmail != null && !doctorEmail.isBlank()) {
                String subject = "[MindBridge] Xác Nhận Kích Hoạt Gói Dịch Vụ Bác Sĩ";
                String htmlBody = """
                    <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto; padding: 20px; border: 1px solid #e2e8f0; border-radius: 8px;">
                      <h2 style="color: #0f766e;">Xác Nhận Kích Hoạt Gói Dịch Vụ</h2>
                      <p>Kính gửi <strong>%s</strong>,</p>
                      <p>Hệ thống MindBridge trân trọng thông báo giao dịch thanh toán gói dịch vụ của Quý Bác sĩ đã được ghi nhận thành công.</p>
                      <table style="width: 100%%; border-collapse: collapse; margin: 16px 0;">
                        <tr><td style="padding: 8px; border-bottom: 1px solid #e2e8f0;">Mã giao dịch:</td><td style="padding: 8px; border-bottom: 1px solid #e2e8f0; font-weight: bold;">%s</td></tr>
                        <tr><td style="padding: 8px; border-bottom: 1px solid #e2e8f0;">Gói dịch vụ:</td><td style="padding: 8px; border-bottom: 1px solid #e2e8f0; font-weight: bold;">%s</td></tr>
                        <tr><td style="padding: 8px; border-bottom: 1px solid #e2e8f0;">Số tiền:</td><td style="padding: 8px; border-bottom: 1px solid #e2e8f0; font-weight: bold;">%s VNĐ</td></tr>
                        <tr><td style="padding: 8px; border-bottom: 1px solid #e2e8f0;">Trạng thái:</td><td style="padding: 8px; border-bottom: 1px solid #e2e8f0; color: #16a34a; font-weight: bold;">ĐÃ KÍCH HOẠT</td></tr>
                      </table>
                      <p>Chúc Quý Bác sĩ có trải nghiệm tuyệt vời cùng MindBridge!</p>
                    </div>
                    """.formatted(doctorName, event.getTransactionCode(), event.getServicePackageName(), event.getAmount());

                EmailTaskDTO emailTask = new EmailTaskDTO(
                    doctorEmail,
                    subject,
                    htmlBody,
                    false,
                    true,
                    "PAYMENT_INVOICE"
                );
                emailTask.setIdempotencyKey("EMAIL_PAY_" + event.getTransactionCode());
                emailTaskProducer.enqueueEmailTask(emailTask);
            } else {
                LOG.warn("⚠️ [Payment Notification] Could not resolve email for doctor '{}'. Skipping payment confirmation email.", doctorLogin);
            }
        } else if ("PAYMENT_FAILED".equals(eventType)) {
            if (doctorLogin != null) {
                Notification notif = new Notification();
                notif.setUserLogin(doctorLogin);
                notif.setTitle("⚠️ Thanh Toán Gói Dịch Vụ Không Thành Công");
                notif.setMessage("Giao dịch mã " + event.getTransactionCode() + " cho gói '" +
                    event.getServicePackageName() + "' chưa hoàn tất. Vui lòng thử lại.");
                notif.setNotificationType("PAYMENT");
                notif.setRelatedEntityId(event.getSubscriptionId());
                notif.setRelatedEntityType("DoctorSubscription");
                notif.setActionUrl("/doctor/subscription");
                notif.setChannel("IN_APP");
                notif.setDeliveryStatus("DELIVERED");
                notificationRepository.save(notif);
            }
        } else {
            if (doctorLogin != null) {
                Notification notif = new Notification();
                notif.setUserLogin(doctorLogin);
                notif.setTitle("↩️ Giao dịch đã được hoàn tiền");
                notif.setMessage("Giao dịch mã " + event.getTransactionCode() + " đã được hoàn tiền.");
                notif.setNotificationType("PAYMENT");
                notif.setRelatedEntityId(event.getSubscriptionId());
                notif.setRelatedEntityType("DoctorSubscription");
                notif.setActionUrl("/doctor/subscription");
                notif.setChannel("IN_APP");
                notif.setDeliveryStatus("DELIVERED");
                notificationRepository.save(notif);
            }
        }

        // 3. Record Inbox Record
        NotificationInbox inbox = new NotificationInbox(
            event.getEventId(),
            idempotencyKey,
            event.getEventType(),
            CONSUMER_GROUP
        );
        inboxRepository.save(inbox);
        LOG.info("💾 [Payment Notification] Recorded NotificationInbox for event '{}'", event.getEventId());
    }
}
