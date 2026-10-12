package com.ocbs.web.rest;

import com.ocbs.domain.ChatMessage;
import com.ocbs.domain.Conversation;
import com.ocbs.domain.User;
import com.ocbs.repository.ChatMessageRepository;
import com.ocbs.repository.ConversationRepository;
import com.ocbs.repository.UserRepository;
import com.ocbs.security.SecurityUtils;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing Conversations and ChatMessages.
 */
@RestController
@RequestMapping("/api")
@Transactional
public class ConversationResource {

    private static final Logger LOG = LoggerFactory.getLogger(ConversationResource.class);

    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;

    public ConversationResource(
        ConversationRepository conversationRepository,
        ChatMessageRepository chatMessageRepository,
        UserRepository userRepository
    ) {
        this.conversationRepository = conversationRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.userRepository = userRepository;
    }

    /**
     * {@code GET  /conversations} : get all conversations for current logged-in user.
     */
    @GetMapping("/conversations")
    public List<Map<String, Object>> getMyConversations() {
        LOG.debug("REST request to get my conversations");
        String currentLogin = SecurityUtils.getCurrentUserLogin().orElse(null);
        if (currentLogin == null) {
            return Collections.emptyList();
        }

        User currentUser = userRepository.findOneByLogin(currentLogin).orElse(null);
        if (currentUser == null) {
            return Collections.emptyList();
        }

        Long userId = currentUser.getId();
        List<Conversation> conversations = conversationRepository.findAllByUserId(userId);
        
        // If empty by strict userId, also check if there are open conversations
        if (conversations.isEmpty()) {
            conversations = conversationRepository.findAll();
        }

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
     * {@code GET  /conversations/:id} : get conversation by id.
     */
    @GetMapping("/conversations/{id}")
    public ResponseEntity<Conversation> getConversation(@PathVariable Long id) {
        LOG.debug("REST request to get Conversation : {}", id);
        return conversationRepository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    /**
     * {@code GET  /conversations/:id/messages} : get all messages for a conversation.
     */
    @GetMapping("/conversations/{id}/messages")
    public ResponseEntity<List<ChatMessage>> getMessages(@PathVariable Long id) {
        LOG.debug("REST request to get messages for conversation : {}", id);
        if (!conversationRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        // Auto mark as read for current user
        String currentLogin = SecurityUtils.getCurrentUserLogin().orElse(null);
        if (currentLogin != null) {
            userRepository.findOneByLogin(currentLogin).ifPresent(u -> {
                chatMessageRepository.markMessagesAsRead(id, u.getId());
            });
        }

        List<ChatMessage> messages = chatMessageRepository.findByConversationIdOrderByCreatedDateAsc(id);
        return ResponseEntity.ok(messages);
    }

    /**
     * {@code POST  /conversations/:id/messages} : send a new message in a conversation.
     */
    @PostMapping("/conversations/{id}/messages")
    public ResponseEntity<ChatMessage> sendMessage(@PathVariable Long id, @RequestBody ChatMessage chatMessage) throws URISyntaxException {
        LOG.debug("REST request to send message in conversation : {}", id);
        Optional<Conversation> convOpt = conversationRepository.findById(id);
        if (convOpt.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        Conversation conv = convOpt.get();
        String currentLogin = SecurityUtils.getCurrentUserLogin().orElse("system");
        var user = userRepository.findOneByLogin(currentLogin).orElse(null);

        chatMessage.setConversationId(id);
        if (user != null) {
            chatMessage.setSenderId(user.getId());
            String fullName = ((user.getFirstName() != null ? user.getFirstName() : "") + " " + (user.getLastName() != null ? user.getLastName() : "")).trim();
            chatMessage.setSenderName(!fullName.isEmpty() ? fullName : user.getLogin());
            
            boolean isDoc = user.getAuthorities().stream().anyMatch(a -> a.getName().equals("ROLE_DOCTOR"));
            chatMessage.setSenderRole(isDoc ? "ROLE_DOCTOR" : "ROLE_PATIENT");
        }
        if (chatMessage.getCreatedDate() == null) {
            chatMessage.setCreatedDate(Instant.now());
        }
        chatMessage.setIsRead(false);

        ChatMessage saved = chatMessageRepository.save(chatMessage);

        // Update conversation summary
        conv.setLastMessage(saved.getContent());
        conv.setLastMessageTime(saved.getCreatedDate());
        conversationRepository.save(conv);

        return ResponseEntity.created(new URI("/api/conversations/" + id + "/messages/" + saved.getId())).body(saved);
    }

    /**
     * {@code POST  /conversations/start} : start or get existing conversation.
     */
    @PostMapping("/conversations/start")
    public ResponseEntity<Conversation> startConversation(@RequestBody Map<String, Object> request) throws URISyntaxException {
        LOG.debug("REST request to start or get conversation : {}", request);
        String currentLogin = SecurityUtils.getCurrentUserLogin().orElse("anonymous");
        User currentUser = userRepository.findOneByLogin(currentLogin).orElse(null);

        Long patientId = request.get("patientId") != null ? Long.valueOf(request.get("patientId").toString()) : null;
        Long doctorId = request.get("doctorId") != null ? Long.valueOf(request.get("doctorId").toString()) : null;
        String patientName = request.get("patientName") != null ? request.get("patientName").toString() : null;
        String doctorName = request.get("doctorName") != null ? request.get("doctorName").toString() : null;
        Long appointmentId = request.get("appointmentId") != null ? Long.valueOf(request.get("appointmentId").toString()) : null;
        Long treatmentPackageId = request.get("treatmentPackageId") != null ? Long.valueOf(request.get("treatmentPackageId").toString()) : null;
        String initialMessage = request.get("initialMessage") != null ? request.get("initialMessage").toString() : null;

        if (currentUser != null) {
            boolean isDoc = currentUser.getAuthorities().stream().anyMatch(a -> a.getName().equals("ROLE_DOCTOR"));
            String userFull = ((currentUser.getFirstName() != null ? currentUser.getFirstName() : "") + " " + (currentUser.getLastName() != null ? currentUser.getLastName() : "")).trim();
            if (userFull.isEmpty()) userFull = currentUser.getLogin();

            if (isDoc) {
                if (doctorId == null) doctorId = currentUser.getId();
                if (doctorName == null) doctorName = userFull;
                if (patientId == null) patientId = 1L; // default patient if not specified
            } else {
                if (patientId == null) patientId = currentUser.getId();
                if (patientName == null) patientName = userFull;
                if (doctorId == null) doctorId = 2L; // default doctor if not specified
            }
        }

        if (patientId == null) patientId = 1L;
        if (doctorId == null) doctorId = 2L;

        final Long fPatientId = patientId;
        final Long fDoctorId = doctorId;
        final String fPatientName = patientName != null ? patientName : "Bệnh nhân #" + patientId;
        final String fDoctorName = doctorName != null ? doctorName : "Bác sĩ #" + doctorId;

        Conversation conversation = conversationRepository.findByPatientIdAndDoctorId(patientId, doctorId)
            .orElseGet(() -> {
                Conversation c = new Conversation();
                c.setPatientId(fPatientId);
                c.setDoctorId(fDoctorId);
                c.setPatientName(fPatientName);
                c.setDoctorName(fDoctorName);
                c.setAppointmentId(appointmentId);
                c.setTreatmentPackageId(treatmentPackageId);
                c.setStatus("OPEN");
                c.setCreatedDate(Instant.now());
                c.setLastMessage("Bắt đầu cuộc trò chuyện tư vấn");
                c.setLastMessageTime(Instant.now());
                return conversationRepository.save(c);
            });

        if (initialMessage != null && !initialMessage.trim().isEmpty()) {
            ChatMessage msg = new ChatMessage();
            msg.setConversationId(conversation.getId());
            msg.setSenderId(currentUser != null ? currentUser.getId() : fPatientId);
            msg.setSenderName(currentUser != null ? currentUser.getLogin() : fPatientName);
            msg.setSenderRole(currentUser != null && currentUser.getAuthorities().stream().anyMatch(a -> a.getName().equals("ROLE_DOCTOR")) ? "ROLE_DOCTOR" : "ROLE_PATIENT");
            msg.setContent(initialMessage);
            msg.setCreatedDate(Instant.now());
            msg.setIsRead(false);
            chatMessageRepository.save(msg);

            conversation.setLastMessage(initialMessage);
            conversation.setLastMessageTime(msg.getCreatedDate());
            conversation = conversationRepository.save(conversation);
        }

        return ResponseEntity.ok(conversation);
    }

    /**
     * {@code PUT  /conversations/:id/read} : mark conversation messages as read.
     */
    @PutMapping("/conversations/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
        LOG.debug("REST request to mark conversation {} as read", id);
        String currentLogin = SecurityUtils.getCurrentUserLogin().orElse(null);
        if (currentLogin != null) {
            userRepository.findOneByLogin(currentLogin).ifPresent(u -> {
                chatMessageRepository.markMessagesAsRead(id, u.getId());
            });
        }
        return ResponseEntity.ok().build();
    }
}

