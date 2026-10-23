package com.ocbs.web.rest;

import com.ocbs.domain.Notification;
import com.ocbs.repository.NotificationRepository;
import com.ocbs.security.SecurityUtils;
import com.ocbs.web.rest.errors.BadRequestAlertException;
import jakarta.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@Transactional
public class NotificationResource {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationResource.class);
    private static final String ENTITY_NAME = "notification";

    private final NotificationRepository notificationRepository;
    private final com.ocbs.service.EmailTaskProducer emailTaskProducer;
    private final com.ocbs.service.MailService mailService;
    private final com.ocbs.repository.UserRepository userRepository;

    public NotificationResource(
        NotificationRepository notificationRepository,
        com.ocbs.service.EmailTaskProducer emailTaskProducer,
        com.ocbs.service.MailService mailService,
        com.ocbs.repository.UserRepository userRepository
    ) {
        this.notificationRepository = notificationRepository;
        this.emailTaskProducer = emailTaskProducer;
        this.mailService = mailService;
        this.userRepository = userRepository;
    }

    /**
     * {@code GET /notifications} : Get all notifications for the current authenticated user.
     */
    @GetMapping("/notifications")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Notification>> getCurrentUserNotifications(
        @RequestParam(name = "unreadOnly", defaultValue = "false") boolean unreadOnly
    ) {
        String login = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new BadRequestAlertException("User not authenticated", ENTITY_NAME, "unauthenticated"));

        List<Notification> list = unreadOnly
            ? notificationRepository.findAllByUserLoginAndIsReadFalseOrderByCreatedDateDesc(login)
            : notificationRepository.findAllByUserLoginOrderByCreatedDateDesc(login);

        return ResponseEntity.ok(list);
    }

    /**
     * {@code GET /notifications/unread-count} : Count unread notifications for current user.
     */
    @GetMapping("/notifications/unread-count")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Long>> getUnreadCount() {
        String login = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new BadRequestAlertException("User not authenticated", ENTITY_NAME, "unauthenticated"));

        long count = notificationRepository.countByUserLoginAndIsReadFalse(login);
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    /**
     * {@code PUT /notifications/:id/read} : Mark a single notification as read.
     */
    @PutMapping("/notifications/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Notification> markAsRead(@PathVariable("id") Long id) {
        String login = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new BadRequestAlertException("User not authenticated", ENTITY_NAME, "unauthenticated"));

        Notification notification = notificationRepository.findById(id)
            .orElseThrow(() -> new BadRequestAlertException("Notification not found", ENTITY_NAME, "idnotfound"));

        if (!login.equalsIgnoreCase(notification.getUserLogin()) && !SecurityUtils.hasCurrentUserAnyOfAuthorities("ROLE_ADMIN")) {
            throw new BadRequestAlertException("Access denied to this notification", ENTITY_NAME, "forbidden");
        }

        notification.setIsRead(true);
        notification.setReadAt(Instant.now());
        Notification result = notificationRepository.save(notification);
        return ResponseEntity.ok(result);
    }

    /**
     * {@code PUT /notifications/read-all} : Mark all notifications as read for current user.
     */
    @PutMapping("/notifications/read-all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> markAllAsRead() {
        String login = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new BadRequestAlertException("User not authenticated", ENTITY_NAME, "unauthenticated"));

        int updated = notificationRepository.markAllAsReadForUser(login);
        return ResponseEntity.ok(Map.of("success", true, "updatedCount", updated));
    }

    /**
     * {@code POST /notifications} : Send a notification (Admin or Internal Service).
     */
    @PostMapping("/notifications")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_DOCTOR')")
    public ResponseEntity<Notification> sendNotification(@Valid @RequestBody Notification notification) throws URISyntaxException {
        if (notification.getId() != null) {
            throw new BadRequestAlertException("A new notification cannot already have an ID", ENTITY_NAME, "idexists");
        }
        if (notification.getIsRead() == null) {
            notification.setIsRead(false);
        }

        Notification result = notificationRepository.save(notification);
        LOG.info("📢 [Notification Service] Sent notification ID: {} to user: '{}'", result.getId(), result.getUserLogin());
        return ResponseEntity.created(new URI("/api/notifications/" + result.getId())).body(result);
    }

    /**
     * {@code POST /notifications/test-email} : Diagnostic endpoint to verify RabbitMQ + Spring Mail dispatch.
     */
    @PostMapping("/notifications/test-email")
    public ResponseEntity<Map<String, Object>> sendTestEmail(
        @RequestParam(name = "to", required = false) String targetEmail,
        @RequestParam(name = "sync", defaultValue = "false") boolean sync
    ) {
        String recipient = (targetEmail != null && !targetEmail.isBlank())
            ? targetEmail
            : SecurityUtils.getCurrentUserLogin().flatMap(userRepository::findOneByLogin).map(com.ocbs.domain.User::getEmail).orElse("quanghvhe161464@fpt.edu.vn");

        String subject = "[MindBridge] Email Thử Nghiệm Hệ Thống Thông Báo";
        String content = """
            <div style="font-family: Arial, sans-serif; max-width: 580px; margin: auto; padding: 24px; border: 1px solid #e2e8f0; border-radius: 12px; background: #ffffff;">
                <div style="background: linear-gradient(135deg, #0f766e 0%, #0d9488 100%); padding: 20px; border-radius: 8px; color: #ffffff; text-align: center;">
                    <h2 style="margin: 0; font-size: 20px;">MindBridge Notification System</h2>
                    <p style="margin: 4px 0 0 0; font-size: 13px; opacity: 0.9;">Kiểm thử đường truyền email qua RabbitMQ Worker &amp; Gmail SMTP</p>
                </div>
                <div style="padding: 24px 8px;">
                    <p style="font-size: 15px; color: #1e293b;">Xin chào,</p>
                    <p style="font-size: 14px; color: #475569; line-height: 1.6;">
                        Hệ thống gửi email của dịch vụ <strong>Notification Service</strong> đang hoạt động ổn định và sẵn sàng xử lý các thông báo lịch hẹn, đơn thuốc và thanh toán.
                    </p>
                    <div style="background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 14px 18px; margin: 20px 0;">
                        <div style="font-size: 13px; color: #64748b;">Trạng thái kiểm tra:</div>
                        <div style="font-size: 16px; font-weight: bold; color: #16a34a; margin-top: 4px;">● SẴN SÀNG HOẠT ĐỘNG (HEALTHY)</div>
                    </div>
                </div>
                <div style="text-align: center; font-size: 12px; color: #94a3b8; border-top: 1px solid #f1f5f9; padding-top: 16px;">
                    © 2026 MindBridge OPCBS. Gửi tự động từ RabbitMQ Worker Queue.
                </div>
            </div>
            """;

        if (sync) {
            mailService.sendEmailAndWait(recipient, subject, content, false, true);
            return ResponseEntity.ok(Map.of(
                "status", "SENT_SYNC",
                "recipient", recipient,
                "message", "Email sent synchronously via JavaMailSender"
            ));
        } else {
            com.ocbs.service.dto.EmailTaskDTO task = new com.ocbs.service.dto.EmailTaskDTO(
                recipient, subject, content, false, true, "DIAGNOSTIC_TEST"
            );
            emailTaskProducer.enqueueEmailTask(task);
            return ResponseEntity.ok(Map.of(
                "status", "ENQUEUED_RABBITMQ",
                "recipient", recipient,
                "queue", "opcbs.email.dispatch.queue",
                "message", "Email task enqueued to RabbitMQ worker"
            ));
        }
    }
}
