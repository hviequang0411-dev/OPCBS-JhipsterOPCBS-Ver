package com.ocbs.service;

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
import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing Chat Messages and Conversation security logic.
 * Shared across both REST and WebSocket STOMP messaging flows to enforce unified authorization.
 */
@Service
@Transactional
public class ChatMessageService {

    private static final Logger LOG = LoggerFactory.getLogger(ChatMessageService.class);

    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final PatientProfileRepository patientProfileRepository;

    public ChatMessageService(
        ConversationRepository conversationRepository,
        ChatMessageRepository chatMessageRepository,
        UserRepository userRepository,
        DoctorProfileRepository doctorProfileRepository,
        PatientProfileRepository patientProfileRepository
    ) {
        this.conversationRepository = conversationRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.userRepository = userRepository;
        this.doctorProfileRepository = doctorProfileRepository;
        this.patientProfileRepository = patientProfileRepository;
    }

    /**
     * Resolves authenticated User entity by username/login.
     */
    @Transactional(readOnly = true)
    public Optional<User> getUserByLogin(String login) {
        if (login == null || login.isBlank()) {
            return Optional.empty();
        }
        return userRepository.findOneWithAuthoritiesByLogin(login);
    }

    /**
     * Resolves all candidate participant IDs for a given user (User ID, Doctor Profile ID, Patient Profile ID).
     */
    @Transactional(readOnly = true)
    public Set<Long> getParticipantIdsForUser(User user) {
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

    /**
     * Checks if a user is a participant of a conversation or has ROLE_ADMIN.
     */
    @Transactional(readOnly = true)
    public boolean isParticipantOrAdmin(Conversation conv, User user) {
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

    /**
     * Checks if a user identified by login is authorized to subscribe/send messages to a conversation.
     */
    @Transactional(readOnly = true)
    public boolean isUserParticipantOrAdmin(Long conversationId, String login) {
        if (conversationId == null || login == null || login.isBlank()) {
            return false;
        }
        User user = getUserByLogin(login).orElse(null);
        if (user == null) {
            return false;
        }
        Conversation conv = conversationRepository.findById(conversationId).orElse(null);
        if (conv == null) {
            return false;
        }
        return isParticipantOrAdmin(conv, user);
    }

    private static final int MAX_CONTENT_LENGTH = 4000;

    /**
     * Encapsulates save message outcome indicating whether a new record was inserted or an existing idempotent record returned.
     */
    public static class SaveMessageResult {
        private final ChatMessage message;
        private final boolean isNew;

        public SaveMessageResult(ChatMessage message, boolean isNew) {
            this.message = message;
            this.isNew = isNew;
        }

        public ChatMessage getMessage() {
            return message;
        }

        public boolean isNew() {
            return isNew;
        }
    }

    // In-memory LRU idempotency cache bound strictly to conversationId + ":" + senderId + ":" + clientMessageId
    private final java.util.Map<String, ChatMessage> processedMessagesCache =
        java.util.Collections.synchronizedMap(new java.util.LinkedHashMap<String, ChatMessage>(256, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(java.util.Map.Entry<String, ChatMessage> eldest) {
                return size() > 2000;
            }
        });

    private String buildIdempotencyKey(Long conversationId, Long senderId, String clientMessageId) {
        return conversationId + ":" + senderId + ":" + clientMessageId.trim();
    }

    /**
     * Checks if a message with the given conversationId, senderId and clientMessageId has already been persisted.
     */
    @Transactional(readOnly = true)
    public Optional<ChatMessage> getProcessedMessage(Long conversationId, Long senderId, String clientMessageId) {
        if (conversationId == null || senderId == null || clientMessageId == null || clientMessageId.isBlank()) {
            return Optional.empty();
        }
        String key = buildIdempotencyKey(conversationId, senderId, clientMessageId);
        ChatMessage cached = processedMessagesCache.get(key);
        if (cached != null) {
            return Optional.of(cached);
        }
        return chatMessageRepository.findByConversationIdAndSenderIdAndClientMessageId(conversationId, senderId, clientMessageId.trim());
    }

    /**
     * Saves a message strictly using the authenticated user's identity.
     * Enforces strict authorization BEFORE checking or returning any idempotency result.
     * Guaranteed atomic transaction: message persistence, unique constraint deduplication, and summary update.
     */
    public SaveMessageResult saveMessageWithResult(Long conversationId, String senderLogin, String content, String clientMessageId) {
        if (conversationId == null) {
            throw new IllegalArgumentException("Conversation ID cannot be null");
        }
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Message content cannot be blank");
        }
        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new IllegalArgumentException("Message content exceeds maximum allowed length of " + MAX_CONTENT_LENGTH + " characters");
        }
        if (senderLogin == null || senderLogin.isBlank()) {
            throw new AccessDeniedException("Sender authentication required");
        }

        // 1. Resolve sender user identity
        User currentUser = getUserByLogin(senderLogin)
            .orElseThrow(() -> new AccessDeniedException("User not found for login: " + senderLogin));

        // 2. Resolve target conversation
        Conversation conv = conversationRepository.findById(conversationId)
            .orElseThrow(() -> new IllegalArgumentException("Conversation not found with ID: " + conversationId));

        // 3. MANDATORY AUTHORIZATION: Strictly verify caller is a member/admin BEFORE checking or leaking any idempotency data
        if (!isParticipantOrAdmin(conv, currentUser)) {
            LOG.warn("🚨 [Security Alert] User '{}' (ID: {}) unauthorized for conversation #{}",
                currentUser.getLogin(), currentUser.getId(), conversationId);
            throw new AccessDeniedException("User is not authorized to send messages to conversation #" + conversationId);
        }

        // 4. Idempotency Check: strictly bound to (conversationId + senderId + clientMessageId)
        String idempotencyKey = null;
        if (clientMessageId != null && !clientMessageId.isBlank()) {
            idempotencyKey = buildIdempotencyKey(conversationId, currentUser.getId(), clientMessageId);

            // Check in-memory LRU cache
            ChatMessage cached = processedMessagesCache.get(idempotencyKey);
            if (cached != null && conversationId.equals(cached.getConversationId()) && currentUser.getId().equals(cached.getSenderId())) {
                LOG.info("⚡ [IDEMPOTENCY] Key '{}' found in RAM cache (Message #{}). Returning cached entity without re-broadcasting.",
                    idempotencyKey, cached.getId());
                return new SaveMessageResult(cached, false);
            }

            // Check persistent MySQL database
            Optional<ChatMessage> existingOpt = chatMessageRepository.findByConversationIdAndSenderIdAndClientMessageId(
                conversationId, currentUser.getId(), clientMessageId.trim()
            );
            if (existingOpt.isPresent()) {
                ChatMessage existing = existingOpt.get();
                processedMessagesCache.put(idempotencyKey, existing);
                LOG.info("⚡ [IDEMPOTENCY] Key '{}' found in MySQL (Message #{}). Returning existing entity without re-broadcasting.",
                    idempotencyKey, existing.getId());
                return new SaveMessageResult(existing, false);
            }
        }

        // 5. Build new ChatMessage entity
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setId(null);
        chatMessage.setConversationId(conversationId);
        chatMessage.setSenderId(currentUser.getId());
        chatMessage.setClientMessageId(clientMessageId != null && !clientMessageId.isBlank() ? clientMessageId.trim() : null);

        String fullName = ((currentUser.getFirstName() != null ? currentUser.getFirstName() : "") + " " +
                           (currentUser.getLastName() != null ? currentUser.getLastName() : "")).trim();
        chatMessage.setSenderName(!fullName.isEmpty() ? fullName : currentUser.getLogin());

        boolean isDoc = currentUser.getAuthorities().stream().anyMatch(a -> "ROLE_DOCTOR".equals(a.getName()));
        chatMessage.setSenderRole(isDoc ? "ROLE_DOCTOR" : "ROLE_PATIENT");
        chatMessage.setCreatedDate(Instant.now());
        chatMessage.setIsRead(false);
        chatMessage.setContent(content.trim());

        ChatMessage saved;
        try {
            saved = chatMessageRepository.saveAndFlush(chatMessage);
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            // Concurrent duplicate caught by database composite unique index (conversation_id, sender_id, client_message_id)
            if (clientMessageId != null && !clientMessageId.isBlank()) {
                Optional<ChatMessage> duplicate = chatMessageRepository.findByConversationIdAndSenderIdAndClientMessageId(
                    conversationId, currentUser.getId(), clientMessageId.trim()
                );
                if (duplicate.isPresent()) {
                    ChatMessage existing = duplicate.get();
                    if (idempotencyKey != null) {
                        processedMessagesCache.put(idempotencyKey, existing);
                    }
                    LOG.info("⚡ [IDEMPOTENCY] Concurrent duplicate race caught for key '{}' (Message #{}). Returning existing record.",
                        idempotencyKey, existing.getId());
                    return new SaveMessageResult(existing, false);
                }
            }
            throw ex;
        }

        // 6. Update conversation summary atomically
        conv.setLastMessage(saved.getContent());
        conv.setLastMessageTime(saved.getCreatedDate());
        conversationRepository.saveAndFlush(conv);

        // 7. Store in memory cache
        if (idempotencyKey != null) {
            processedMessagesCache.put(idempotencyKey, saved);
        }

        LOG.info("Persisted message #{} for conversation #{} by user '{}' into MySQL",
            saved.getId(), conversationId, senderLogin);

        return new SaveMessageResult(saved, true);
    }

    /**
     * Backward-compatible saveMessage returning ChatMessage entity directly.
     */
    public ChatMessage saveMessage(Long conversationId, String senderLogin, String content, String clientMessageId) {
        return saveMessageWithResult(conversationId, senderLogin, content, clientMessageId).getMessage();
    }

    /**
     * Backward-compatible saveMessage for REST and existing callers without clientMessageId.
     */
    public ChatMessage saveMessage(Long conversationId, String senderLogin, String content) {
        return saveMessage(conversationId, senderLogin, content, null);
    }
}
