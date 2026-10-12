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

    public NotificationResource(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
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
}
