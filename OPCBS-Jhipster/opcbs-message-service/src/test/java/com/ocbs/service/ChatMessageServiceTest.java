package com.ocbs.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.ocbs.domain.Authority;
import com.ocbs.domain.ChatMessage;
import com.ocbs.domain.Conversation;
import com.ocbs.domain.User;
import com.ocbs.repository.ChatMessageRepository;
import com.ocbs.repository.ConversationRepository;
import com.ocbs.repository.DoctorProfileRepository;
import com.ocbs.repository.PatientProfileRepository;
import com.ocbs.repository.UserRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class ChatMessageServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DoctorProfileRepository doctorProfileRepository;

    @Mock
    private PatientProfileRepository patientProfileRepository;

    @InjectMocks
    private ChatMessageService chatMessageService;

    private User patientUser;
    private User doctorUser;
    private User intruderUser;
    private User adminUser;
    private Conversation conversation;

    @BeforeEach
    void setUp() {
        patientUser = new User();
        patientUser.setId(10L);
        patientUser.setLogin("patient1");
        patientUser.setFirstName("Nguyễn");
        patientUser.setLastName("Văn A");
        Authority patientAuth = new Authority();
        patientAuth.setName("ROLE_PATIENT");
        patientUser.setAuthorities(Set.of(patientAuth));

        doctorUser = new User();
        doctorUser.setId(20L);
        doctorUser.setLogin("doctor1");
        doctorUser.setFirstName("Bác sĩ");
        doctorUser.setLastName("Trần B");
        Authority docAuth = new Authority();
        docAuth.setName("ROLE_DOCTOR");
        doctorUser.setAuthorities(Set.of(docAuth));

        intruderUser = new User();
        intruderUser.setId(99L);
        intruderUser.setLogin("intruder");
        Authority userAuth = new Authority();
        userAuth.setName("ROLE_USER");
        intruderUser.setAuthorities(Set.of(userAuth));

        adminUser = new User();
        adminUser.setId(1L);
        adminUser.setLogin("admin");
        Authority adminAuth = new Authority();
        adminAuth.setName("ROLE_ADMIN");
        adminUser.setAuthorities(Set.of(adminAuth));

        conversation = new Conversation();
        conversation.setId(100L);
        conversation.setPatientId(10L);
        conversation.setDoctorId(20L);
        conversation.setStatus("OPEN");
    }

    @Test
    @DisplayName("1 & 4. Unauthorized user is rejected BEFORE checking or returning any idempotency result")
    void saveMessage_shouldRejectUnauthorizedUserBeforeIdempotencyCheck() {
        when(userRepository.findOneWithAuthoritiesByLogin("intruder")).thenReturn(Optional.of(intruderUser));
        when(conversationRepository.findById(100L)).thenReturn(Optional.of(conversation));

        // Intruder tries to supply a clientMessageId that belongs to someone else
        String victimClientMessageId = "cm_victim_secret_123";

        assertThatThrownBy(() -> chatMessageService.saveMessageWithResult(100L, "intruder", "Hacker content", victimClientMessageId))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessageContaining("not authorized");

        // Verify database findByConversationIdAndSenderIdAndClientMessageId was NEVER even queried for intruder
        verify(chatMessageRepository, never()).findByConversationIdAndSenderIdAndClientMessageId(any(), any(), any());
        verify(chatMessageRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("2. Same sender + conversation + clientMessageId replay returns existing record (isNew = false)")
    void saveMessage_sameSenderAndConversationAndClientMessageId_shouldDeduplicate() {
        when(userRepository.findOneWithAuthoritiesByLogin("patient1")).thenReturn(Optional.of(patientUser));
        when(conversationRepository.findById(100L)).thenReturn(Optional.of(conversation));

        String clientMessageId = "cm_patient_msg_1";

        ChatMessage firstSaved = new ChatMessage();
        firstSaved.setId(5001L);
        firstSaved.setConversationId(100L);
        firstSaved.setSenderId(10L);
        firstSaved.setClientMessageId(clientMessageId);
        firstSaved.setContent("First attempt");
        firstSaved.setCreatedDate(Instant.now());

        when(chatMessageRepository.findByConversationIdAndSenderIdAndClientMessageId(100L, 10L, clientMessageId))
            .thenReturn(Optional.empty()); // Not found initially
        when(chatMessageRepository.saveAndFlush(any(ChatMessage.class))).thenReturn(firstSaved);

        // 1st call: new message
        ChatMessageService.SaveMessageResult result1 = chatMessageService.saveMessageWithResult(
            100L, "patient1", "First attempt", clientMessageId
        );
        assertThat(result1.isNew()).isTrue();
        assertThat(result1.getMessage().getId()).isEqualTo(5001L);

        // 2nd call (retry after timeout or duplicate request): returns cached/existing record
        ChatMessageService.SaveMessageResult result2 = chatMessageService.saveMessageWithResult(
            100L, "patient1", "First attempt", clientMessageId
        );
        assertThat(result2.isNew()).isFalse();
        assertThat(result2.getMessage().getId()).isEqualTo(5001L);
        assertThat(result2.getMessage().getClientMessageId()).isEqualTo(clientMessageId);

        // Verify saveAndFlush was only called once for the first insertion
        verify(chatMessageRepository, times(1)).saveAndFlush(any(ChatMessage.class));
    }

    @Test
    @DisplayName("3. Same clientMessageId but DIFFERENT sender or conversation does NOT return existing message")
    void saveMessage_sameClientMessageIdDifferentSender_shouldNotReturnOtherUserMessage() {
        when(userRepository.findOneWithAuthoritiesByLogin("patient1")).thenReturn(Optional.of(patientUser));
        when(userRepository.findOneWithAuthoritiesByLogin("doctor1")).thenReturn(Optional.of(doctorUser));
        when(conversationRepository.findById(100L)).thenReturn(Optional.of(conversation));

        String reusedClientMsgId = "cm_shared_identifier_456";

        // Patient sends message with ID
        ChatMessage patientMsg = new ChatMessage();
        patientMsg.setId(7001L);
        patientMsg.setConversationId(100L);
        patientMsg.setSenderId(10L);
        patientMsg.setClientMessageId(reusedClientMsgId);
        patientMsg.setContent("Patient content");

        when(chatMessageRepository.findByConversationIdAndSenderIdAndClientMessageId(100L, 10L, reusedClientMsgId))
            .thenReturn(Optional.empty());
        when(chatMessageRepository.saveAndFlush(any(ChatMessage.class))).thenAnswer(invocation -> {
            ChatMessage m = invocation.getArgument(0);
            m.setId(7001L);
            return m;
        });

        ChatMessageService.SaveMessageResult res1 = chatMessageService.saveMessageWithResult(100L, "patient1", "Patient content", reusedClientMsgId);
        assertThat(res1.isNew()).isTrue();
        assertThat(res1.getMessage().getId()).isEqualTo(7001L);

        // Doctor sends message with exact same clientMessageId in the same conversation
        when(chatMessageRepository.findByConversationIdAndSenderIdAndClientMessageId(100L, 20L, reusedClientMsgId))
            .thenReturn(Optional.empty());

        ChatMessageService.SaveMessageResult res2 = chatMessageService.saveMessageWithResult(100L, "doctor1", "Doctor reply", reusedClientMsgId);
        assertThat(res2.isNew()).isTrue();
        assertThat(res2.getMessage().getId()).isEqualTo(7001L); // New entity saved
        assertThat(res2.getMessage().getSenderId()).isEqualTo(20L); // Bound strictly to Doctor
        assertThat(res2.getMessage().getContent()).isEqualTo("Doctor reply");
    }

    @Test
    @DisplayName("2b. Handles concurrent duplicate race via database unique constraint exception safely")
    void saveMessage_concurrentRaceCondition_shouldCatchDataIntegrityViolationAndReturnExisting() {
        when(userRepository.findOneWithAuthoritiesByLogin("patient1")).thenReturn(Optional.of(patientUser));
        when(conversationRepository.findById(100L)).thenReturn(Optional.of(conversation));

        String clientMessageId = "cm_race_condition_789";

        // 1st lookup returns empty (both threads see no record yet)
        when(chatMessageRepository.findByConversationIdAndSenderIdAndClientMessageId(100L, 10L, clientMessageId))
            .thenReturn(Optional.empty());

        ChatMessage concurrentSaved = new ChatMessage();
        concurrentSaved.setId(8888L);
        concurrentSaved.setConversationId(100L);
        concurrentSaved.setSenderId(10L);
        concurrentSaved.setClientMessageId(clientMessageId);
        concurrentSaved.setContent("Race message");

        // saveAndFlush throws DataIntegrityViolationException because concurrent thread inserted first
        when(chatMessageRepository.saveAndFlush(any(ChatMessage.class)))
            .thenThrow(new DataIntegrityViolationException("Duplicate entry for key ux_chat_msg_idempotency"));

        // Fallback query returns the record committed by concurrent thread
        when(chatMessageRepository.findByConversationIdAndSenderIdAndClientMessageId(100L, 10L, clientMessageId))
            .thenReturn(Optional.empty()) // Initial check
            .thenReturn(Optional.of(concurrentSaved)); // Check after exception

        ChatMessageService.SaveMessageResult result = chatMessageService.saveMessageWithResult(
            100L, "patient1", "Race message", clientMessageId
        );

        assertThat(result.isNew()).isFalse();
        assertThat(result.getMessage().getId()).isEqualTo(8888L);
    }

    @Test
    @DisplayName("Should successfully persist message and update conversation summary when valid")
    void saveMessage_shouldPersistAndAtomicUpdateSummary() {
        when(userRepository.findOneWithAuthoritiesByLogin("patient1")).thenReturn(Optional.of(patientUser));
        when(conversationRepository.findById(100L)).thenReturn(Optional.of(conversation));
        when(chatMessageRepository.saveAndFlush(any(ChatMessage.class))).thenAnswer(invocation -> {
            ChatMessage m = invocation.getArgument(0);
            m.setId(1001L);
            return m;
        });

        ChatMessage saved = chatMessageService.saveMessage(100L, "patient1", "Xin chào bác sĩ");

        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isEqualTo(1001L);
        assertThat(saved.getContent()).isEqualTo("Xin chào bác sĩ");
        assertThat(saved.getSenderId()).isEqualTo(10L);
        assertThat(saved.getSenderName()).isEqualTo("Nguyễn Văn A");
        assertThat(saved.getSenderRole()).isEqualTo("ROLE_PATIENT");

        verify(chatMessageRepository, times(1)).saveAndFlush(any(ChatMessage.class));
        verify(conversationRepository, times(1)).saveAndFlush(conversation);
        assertThat(conversation.getLastMessage()).isEqualTo("Xin chào bác sĩ");
    }

    @Test
    @DisplayName("Should reject blank message content")
    void saveMessage_shouldRejectBlankContent() {
        assertThatThrownBy(() -> chatMessageService.saveMessage(100L, "patient1", "   "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("blank");

        verify(chatMessageRepository, never()).saveAndFlush(any(ChatMessage.class));
    }
}
