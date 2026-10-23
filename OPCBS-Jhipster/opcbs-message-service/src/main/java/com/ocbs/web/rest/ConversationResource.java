package com.ocbs.web.rest;

import com.ocbs.domain.ChatMessage;
import com.ocbs.domain.Conversation;
import com.ocbs.domain.DoctorProfile;
import com.ocbs.domain.PatientProfile;
import com.ocbs.domain.User;
import com.ocbs.repository.ChatMessageRepository;
import com.ocbs.repository.ConversationRepository;
import com.ocbs.repository.DoctorProfileRepository;
import com.ocbs.repository.PatientProfileRepository;
import com.ocbs.repository.UserRepository;
import com.ocbs.security.SecurityUtils;
import com.ocbs.service.ChatMessageService;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing Conversations and ChatMessages.
 * Strictly verifies user authorization against conversation participants and enforces caller identity.
 */
@RestController
@RequestMapping("/api")
@Transactional
public class ConversationResource {

    private static final Logger LOG = LoggerFactory.getLogger(ConversationResource.class);

    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final ChatMessageService chatMessageService;
    private final org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    public ConversationResource(
        ConversationRepository conversationRepository,
        ChatMessageRepository chatMessageRepository,
        UserRepository userRepository,
        DoctorProfileRepository doctorProfileRepository,
        PatientProfileRepository patientProfileRepository,
        ChatMessageService chatMessageService,
        org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate
    ) {
        this.conversationRepository = conversationRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.userRepository = userRepository;
        this.doctorProfileRepository = doctorProfileRepository;
        this.patientProfileRepository = patientProfileRepository;
        this.chatMessageService = chatMessageService;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * {@code GET  /conversations} : get all conversations for current logged-in user.
     * Only returns conversations where user is patient or doctor, or all if admin.
     * Does NOT fall back to all conversations when user has none.
     */
    @GetMapping("/conversations")
    public List<Map<String, Object>> getMyConversations() {
        LOG.debug("REST request to get my conversations");
        User currentUser = getCurrentAuthenticatedUser();
        if (currentUser == null) {
            return Collections.emptyList();
        }

        boolean isAdmin = currentUser.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getName()));
        Long userId = currentUser.getId();

        List<Conversation> conversations;
        if (isAdmin) {
            conversations = conversationRepository.findAll(Sort.by(Sort.Direction.DESC, "lastMessageTime"));
        } else {
            Set<Long> myIds = getParticipantIdsForUser(currentUser);
            conversations = conversationRepository.findAllByParticipantIds(myIds);
        }

        // No fallback to findAll() - users without conversations receive an empty list
        List<Map<String, Object>> result = new ArrayList<>();

        for (Conversation conv : conversations) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", conv.getId());
            map.put("patientId", conv.getPatientId());
            map.put("patientName", conv.getPatientName() != null ? conv.getPatientName() : "Bệnh nhân #" + conv.getPatientId());
            map.put("doctorId", conv.getDoctorId());
            map.put("doctorName", conv.getDoctorName() != null ? conv.getDoctorName() : "Bác sĩ #" + conv.getDoctorId());
            map.put("appointmentId", conv.getAppointmentId());
            map.put("treatmentPackageId", conv.getTreatmentPackageId());
            map.put("status", conv.getStatus());
            map.put("lastMessage", conv.getLastMessage());
            map.put("lastMessageTime", conv.getLastMessageTime());
            map.put("createdDate", conv.getCreatedDate());

            long unread = chatMessageRepository.countByConversationIdAndSenderIdNotAndIsReadFalse(conv.getId(), userId);
            map.put("unreadCount", unread);

            result.add(map);
        }

        return result;
    }

    /**
     * {@code GET  /conversations/:id} : get conversation by id with authorization check.
     */
    @GetMapping("/conversations/{id}")
    public ResponseEntity<Conversation> getConversation(@PathVariable Long id) {
        LOG.debug("REST request to get Conversation : {}", id);
        User currentUser = getCurrentAuthenticatedUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Optional<Conversation> convOpt = conversationRepository.findById(id);
        if (convOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Conversation conv = convOpt.get();
        if (!isParticipantOrAdmin(conv, currentUser)) {
            LOG.warn("🚨 [Security Alert] User '{}' (ID: {}) attempted to access unauthorized conversation #{}",
                currentUser.getLogin(), currentUser.getId(), id);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(conv);
    }

    /**
     * {@code GET  /conversations/:id/messages} : get all messages for a conversation with authorization check.
     * Optionally filters messages newer than sinceId for incremental synchronization after reconnect.
     */
    @GetMapping("/conversations/{id}/messages")
    public ResponseEntity<List<ChatMessage>> getMessages(
        @PathVariable Long id,
        @RequestParam(required = false) Long sinceId
    ) {
        LOG.debug("REST request to get messages for conversation : {}, sinceId: {}", id, sinceId);
        User currentUser = getCurrentAuthenticatedUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Optional<Conversation> convOpt = conversationRepository.findById(id);
        if (convOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Conversation conv = convOpt.get();
        if (!isParticipantOrAdmin(conv, currentUser)) {
            LOG.warn("🚨 [Security Alert] User '{}' (ID: {}) attempted to view messages of unauthorized conversation #{}",
                currentUser.getLogin(), currentUser.getId(), id);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // Auto mark as read for current user
        chatMessageRepository.markMessagesAsRead(id, currentUser.getId());

        List<ChatMessage> messages;
        if (sinceId != null && sinceId > 0) {
            messages = chatMessageRepository.findByConversationIdAndIdGreaterThanOrderByCreatedDateAsc(id, sinceId);
        } else {
            messages = chatMessageRepository.findByConversationIdOrderByCreatedDateAsc(id);
        }
        return ResponseEntity.ok(messages);
    }

    /**
     * {@code POST  /conversations/:id/messages} : send a new message in a conversation.
     * Sender identity is strictly derived from JWT principal, client sender params are ignored.
     * Enforces database-backed idempotency matching the WebSocket flow.
     */
    @PostMapping("/conversations/{id}/messages")
    public ResponseEntity<ChatMessage> sendMessage(@PathVariable Long id, @RequestBody ChatMessage chatMessage) throws URISyntaxException {
        LOG.debug("REST request to send message in conversation : {}", id);
        User currentUser = getCurrentAuthenticatedUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Optional<Conversation> convOpt = conversationRepository.findById(id);
        if (convOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Conversation conv = convOpt.get();
        if (!isParticipantOrAdmin(conv, currentUser)) {
            LOG.warn("🚨 [Security Alert] User '{}' (ID: {}) attempted to send message to unauthorized conversation #{}",
                currentUser.getLogin(), currentUser.getId(), id);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        if (chatMessage.getContent() == null || chatMessage.getContent().trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        ChatMessageService.SaveMessageResult result = chatMessageService.saveMessageWithResult(
            id, currentUser.getLogin(), chatMessage.getContent(), chatMessage.getClientMessageId()
        );
        ChatMessage saved = result.getMessage();

        // Broadcast to WebSocket topic only for newly created messages (avoid duplicate notifications)
        if (result.isNew()) {
            try {
                messagingTemplate.convertAndSend("/topic/conversations/" + id, saved);
            } catch (Exception e) {
                LOG.warn("Failed to broadcast message #{} to STOMP topic: {}", saved.getId(), e.getMessage());
            }
            return ResponseEntity.created(new URI("/api/conversations/" + id + "/messages/" + saved.getId())).body(saved);
        } else {
            LOG.info("⚡ [IDEMPOTENCY] Replayed existing message #{} via REST fallback for clientMessageId '{}'",
                saved.getId(), chatMessage.getClientMessageId());
            return ResponseEntity.ok(saved);
        }
    }

    /**
     * {@code POST  /conversations/start} : start or get existing conversation.
     * Participant ID for the caller is strictly bound to currentUser (preventing impersonation).
     * Automatically resolves Profile IDs and User IDs to guarantee bidirectional conversation sharing.
     */
    @PostMapping("/conversations/start")
    public ResponseEntity<Conversation> startConversation(@RequestBody Map<String, Object> request) throws URISyntaxException {
        LOG.debug("REST request to start or get conversation : {}", request);
        User currentUser = getCurrentAuthenticatedUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        boolean isDoc = currentUser.getAuthorities().stream().anyMatch(a -> "ROLE_DOCTOR".equals(a.getName()));
        boolean isAdmin = currentUser.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getName()));

        String currentFullName = ((currentUser.getFirstName() != null ? currentUser.getFirstName() : "") + " " +
                                  (currentUser.getLastName() != null ? currentUser.getLastName() : "")).trim();
        if (currentFullName.isEmpty()) {
            currentFullName = currentUser.getLogin();
        }

        Long reqPatientId = request.get("patientId") != null ? Long.valueOf(request.get("patientId").toString()) : null;
        Long reqDoctorId = request.get("doctorId") != null ? Long.valueOf(request.get("doctorId").toString()) : null;
        String reqPatientName = request.get("patientName") != null ? request.get("patientName").toString() : null;
        String reqDoctorName = request.get("doctorName") != null ? request.get("doctorName").toString() : null;
        Long appointmentId = request.get("appointmentId") != null ? Long.valueOf(request.get("appointmentId").toString()) : null;
        Long treatmentPackageId = request.get("treatmentPackageId") != null ? Long.valueOf(request.get("treatmentPackageId").toString()) : null;
        String initialMessage = request.get("initialMessage") != null ? request.get("initialMessage").toString() : null;

        Set<Long> candidateDoctorIds = new HashSet<>();
        Set<Long> candidatePatientIds = new HashSet<>();

        Long standardDoctorId = null;
        Long standardPatientId = null;
        String finalDoctorName = null;
        String finalPatientName = null;

        if (isAdmin) {
            if (reqPatientId == null || reqDoctorId == null) {
                return ResponseEntity.badRequest().build();
            }
            candidateDoctorIds.add(reqDoctorId);
            candidatePatientIds.add(reqPatientId);
            standardDoctorId = reqDoctorId;
            standardPatientId = reqPatientId;
            finalDoctorName = reqDoctorName != null ? reqDoctorName : "Bác sĩ #" + reqDoctorId;
            finalPatientName = reqPatientName != null ? reqPatientName : "Bệnh nhân #" + reqPatientId;
        } else if (isDoc) {
            // Caller is Doctor
            candidateDoctorIds.add(currentUser.getId());
            standardDoctorId = currentUser.getId();
            finalDoctorName = currentFullName;

            Optional<DoctorProfile> currentDocProf = doctorProfileRepository.findByUser_Login(currentUser.getLogin());
            if (currentDocProf.isPresent()) {
                DoctorProfile dp = currentDocProf.get();
                candidateDoctorIds.add(dp.getId());
                standardDoctorId = dp.getId();
                if (dp.getFullName() != null && !dp.getFullName().trim().isEmpty()) {
                    finalDoctorName = dp.getFullName();
                }
            }

            if (reqPatientId == null) {
                return ResponseEntity.badRequest().build();
            }
            candidatePatientIds.add(reqPatientId);
            standardPatientId = reqPatientId;
            finalPatientName = reqPatientName != null ? reqPatientName : "Bệnh nhân #" + reqPatientId;

            // Resolve patient profile if exists
            Optional<PatientProfile> targetPatProf = patientProfileRepository.findById(reqPatientId);
            if (targetPatProf.isPresent()) {
                PatientProfile pp = targetPatProf.get();
                if (pp.getUser() != null) {
                    candidatePatientIds.add(pp.getUser().getId());
                }
                if (pp.getFullName() != null && !pp.getFullName().trim().isEmpty()) {
                    finalPatientName = pp.getFullName();
                }
            } else {
                // If reqPatientId was a user id, find profile
                Optional<PatientProfile> ppOpt = patientProfileRepository.findAll().stream()
                    .filter(p -> p.getUser() != null && p.getUser().getId().equals(reqPatientId))
                    .findFirst();
                if (ppOpt.isPresent()) {
                    PatientProfile pp = ppOpt.get();
                    candidatePatientIds.add(pp.getId());
                    if (pp.getFullName() != null && !pp.getFullName().trim().isEmpty()) {
                        finalPatientName = pp.getFullName();
                    }
                }
            }
        } else {
            // Caller is Patient
            candidatePatientIds.add(currentUser.getId());
            standardPatientId = currentUser.getId();
            finalPatientName = currentFullName;

            Optional<PatientProfile> currentPatProf = patientProfileRepository.findByUser_Login(currentUser.getLogin());
            if (currentPatProf.isPresent()) {
                PatientProfile pp = currentPatProf.get();
                candidatePatientIds.add(pp.getId());
                standardPatientId = pp.getId();
                if (pp.getFullName() != null && !pp.getFullName().trim().isEmpty()) {
                    finalPatientName = pp.getFullName();
                }
            }

            if (reqDoctorId == null) {
                return ResponseEntity.badRequest().build();
            }
            candidateDoctorIds.add(reqDoctorId);
            standardDoctorId = reqDoctorId;
            finalDoctorName = reqDoctorName != null ? reqDoctorName : "Bác sĩ #" + reqDoctorId;

            // Resolve doctor profile if exists
            Optional<DoctorProfile> targetDocProf = doctorProfileRepository.findById(reqDoctorId);
            if (targetDocProf.isPresent()) {
                DoctorProfile dp = targetDocProf.get();
                if (dp.getUser() != null) {
                    candidateDoctorIds.add(dp.getUser().getId());
                }
                if (dp.getFullName() != null && !dp.getFullName().trim().isEmpty()) {
                    finalDoctorName = dp.getFullName();
                }
            } else {
                // If reqDoctorId was a user id, find profile
                Optional<DoctorProfile> dpOpt = doctorProfileRepository.findAll().stream()
                    .filter(d -> d.getUser() != null && d.getUser().getId().equals(reqDoctorId))
                    .findFirst();
                if (dpOpt.isPresent()) {
                    DoctorProfile dp = dpOpt.get();
                    candidateDoctorIds.add(dp.getId());
                    if (dp.getFullName() != null && !dp.getFullName().trim().isEmpty()) {
                        finalDoctorName = dp.getFullName();
                    }
                }
            }
        }

        // Look for existing conversation between candidate participants
        List<Conversation> existingList = conversationRepository.findExistingConversationsBetween(candidateDoctorIds, candidatePatientIds);
        Conversation conversation;

        if (!existingList.isEmpty()) {
            conversation = existingList.get(0);
            LOG.info("Found existing conversation #{} between doctor {} and patient {}",
                conversation.getId(), candidateDoctorIds, candidatePatientIds);

            // Normalize participants to standard IDs
            if (standardDoctorId != null && !standardDoctorId.equals(conversation.getDoctorId())) {
                conversation.setDoctorId(standardDoctorId);
            }
            if (standardPatientId != null && !standardPatientId.equals(conversation.getPatientId())) {
                conversation.setPatientId(standardPatientId);
            }
            if (finalDoctorName != null && (conversation.getDoctorName() == null || conversation.getDoctorName().startsWith("Bác sĩ #"))) {
                conversation.setDoctorName(finalDoctorName);
            }
            if (finalPatientName != null && (conversation.getPatientName() == null || conversation.getPatientName().startsWith("Bệnh nhân #"))) {
                conversation.setPatientName(finalPatientName);
            }
            conversation = conversationRepository.save(conversation);
        } else {
            Conversation c = new Conversation();
            c.setPatientId(standardPatientId);
            c.setDoctorId(standardDoctorId);
            c.setPatientName(finalPatientName);
            c.setDoctorName(finalDoctorName);
            c.setAppointmentId(appointmentId);
            c.setTreatmentPackageId(treatmentPackageId);
            c.setStatus("OPEN");
            c.setCreatedDate(Instant.now());
            c.setLastMessage("Bắt đầu cuộc trò chuyện tư vấn");
            c.setLastMessageTime(Instant.now());
            conversation = conversationRepository.save(c);
            LOG.info("Created new standardized conversation #{} for doctor {} and patient {}",
                conversation.getId(), standardDoctorId, standardPatientId);
        }

        if (initialMessage != null && !initialMessage.trim().isEmpty()) {
            ChatMessage msg = new ChatMessage();
            msg.setConversationId(conversation.getId());
            msg.setSenderId(currentUser.getId());
            msg.setSenderName(currentFullName);
            msg.setSenderRole(isDoc ? "ROLE_DOCTOR" : "ROLE_PATIENT");
            msg.setContent(initialMessage.trim());
            msg.setCreatedDate(Instant.now());
            msg.setIsRead(false);
            chatMessageRepository.save(msg);

            conversation.setLastMessage(msg.getContent());
            conversation.setLastMessageTime(msg.getCreatedDate());
            conversation = conversationRepository.save(conversation);
        }

        return ResponseEntity.ok(conversation);
    }

    /**
     * {@code PUT  /conversations/:id/read} : mark conversation messages as read with authorization check.
     */
    @PutMapping("/conversations/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
        LOG.debug("REST request to mark conversation {} as read", id);
        User currentUser = getCurrentAuthenticatedUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Optional<Conversation> convOpt = conversationRepository.findById(id);
        if (convOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Conversation conv = convOpt.get();
        if (!isParticipantOrAdmin(conv, currentUser)) {
            LOG.warn("🚨 [Security Alert] User '{}' (ID: {}) attempted to mark as read unauthorized conversation #{}",
                currentUser.getLogin(), currentUser.getId(), id);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        chatMessageRepository.markMessagesAsRead(id, currentUser.getId());
        return ResponseEntity.ok().build();
    }

    private User getCurrentAuthenticatedUser() {
        String currentLogin = SecurityUtils.getCurrentUserLogin().orElse(null);
        if (currentLogin == null) {
            return null;
        }
        return userRepository.findOneWithAuthoritiesByLogin(currentLogin).orElse(null);
    }

    private Set<Long> getParticipantIdsForUser(User user) {
        Set<Long> ids = new HashSet<>();
        if (user == null) {
            return ids;
        }
        ids.add(user.getId());
        if (user.getLogin() != null) {
            doctorProfileRepository.findByUser_Login(user.getLogin()).ifPresent(dp -> ids.add(dp.getId()));
            patientProfileRepository.findByUser_Login(user.getLogin()).ifPresent(pp -> ids.add(pp.getId()));
        }
        return ids;
    }

    private boolean isParticipantOrAdmin(Conversation conv, User user) {
        if (conv == null || user == null) {
            return false;
        }
        boolean isAdmin = user.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getName()));
        if (isAdmin) {
            return true;
        }
        Set<Long> myIds = getParticipantIdsForUser(user);
        return (conv.getPatientId() != null && myIds.contains(conv.getPatientId()))
            || (conv.getDoctorId() != null && myIds.contains(conv.getDoctorId()));
    }
}
