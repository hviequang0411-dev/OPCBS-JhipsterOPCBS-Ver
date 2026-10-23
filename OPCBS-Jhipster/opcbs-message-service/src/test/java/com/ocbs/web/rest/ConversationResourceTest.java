package com.ocbs.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.ocbs.service.ChatMessageService;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class ConversationResourceTest {

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

    @Mock
    private ChatMessageService chatMessageService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private ConversationResource conversationResource;

    private User patientUser;
    private User intruderUser;
    private Conversation conversation;

    @BeforeEach
    void setUp() {
        patientUser = new User();
        patientUser.setId(10L);
        patientUser.setLogin("patient1");
        Authority auth = new Authority();
        auth.setName("ROLE_PATIENT");
        patientUser.setAuthorities(Set.of(auth));

        intruderUser = new User();
        intruderUser.setId(99L);
        intruderUser.setLogin("intruder");
        Authority intruderAuth = new Authority();
        intruderAuth.setName("ROLE_USER");
        intruderUser.setAuthorities(Set.of(intruderAuth));

        conversation = new Conversation();
        conversation.setId(100L);
        conversation.setPatientId(10L);
        conversation.setDoctorId(20L);
        conversation.setStatus("OPEN");

        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("patient1", "token")
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("2a & 5. REST fallback sendMessage with clientMessageId broadcasts on new message (201 Created)")
    void sendMessage_newRecord_shouldBroadcastAndReturnCreated() throws URISyntaxException {
        when(userRepository.findOneWithAuthoritiesByLogin("patient1")).thenReturn(Optional.of(patientUser));
        when(conversationRepository.findById(100L)).thenReturn(Optional.of(conversation));

        ChatMessage newMsg = new ChatMessage();
        newMsg.setId(301L);
        newMsg.setConversationId(100L);
        newMsg.setSenderId(10L);
        newMsg.setClientMessageId("cm_rest_1");
        newMsg.setContent("Fallback content");
        newMsg.setCreatedDate(Instant.now());

        ChatMessageService.SaveMessageResult result = new ChatMessageService.SaveMessageResult(newMsg, true);
        when(chatMessageService.saveMessageWithResult(100L, "patient1", "Fallback content", "cm_rest_1"))
            .thenReturn(result);

        ChatMessage req = new ChatMessage();
        req.setContent("Fallback content");
        req.setClientMessageId("cm_rest_1");

        ResponseEntity<ChatMessage> response = conversationResource.sendMessage(100L, req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(301L);

        // Verify broadcasted to topic so recipient in WebSocket room receives it immediately
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/conversations/100"), eq(newMsg));
    }

    @Test
    @DisplayName("2b & 5. REST fallback retry with same clientMessageId returns existing (200 OK) without duplicate broadcast")
    void sendMessage_idempotentDuplicate_shouldNotBroadcastAndReturnOk() throws URISyntaxException {
        when(userRepository.findOneWithAuthoritiesByLogin("patient1")).thenReturn(Optional.of(patientUser));
        when(conversationRepository.findById(100L)).thenReturn(Optional.of(conversation));

        ChatMessage existingMsg = new ChatMessage();
        existingMsg.setId(301L);
        existingMsg.setConversationId(100L);
        existingMsg.setSenderId(10L);
        existingMsg.setClientMessageId("cm_rest_1");
        existingMsg.setContent("Fallback content");
        existingMsg.setCreatedDate(Instant.now());

        // isNew = false indicates duplicate / replay
        ChatMessageService.SaveMessageResult result = new ChatMessageService.SaveMessageResult(existingMsg, false);
        when(chatMessageService.saveMessageWithResult(100L, "patient1", "Fallback content", "cm_rest_1"))
            .thenReturn(result);

        ChatMessage req = new ChatMessage();
        req.setContent("Fallback content");
        req.setClientMessageId("cm_rest_1");

        ResponseEntity<ChatMessage> response = conversationResource.sendMessage(100L, req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(301L);

        // Verify broadcast was SKIPPED to prevent room duplicate notification
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    @DisplayName("5. getMessages supports sinceId for incremental delta recovery after reconnect")
    void getMessages_withSinceId_returnsOnlyNewerMessages() {
        when(userRepository.findOneWithAuthoritiesByLogin("patient1")).thenReturn(Optional.of(patientUser));
        when(conversationRepository.findById(100L)).thenReturn(Optional.of(conversation));

        ChatMessage msg10 = new ChatMessage();
        msg10.setId(10L);
        msg10.setContent("Missed message 1");

        ChatMessage msg11 = new ChatMessage();
        msg11.setId(11L);
        msg11.setContent("Missed message 2");

        when(chatMessageRepository.findByConversationIdAndIdGreaterThanOrderByCreatedDateAsc(100L, 9L))
            .thenReturn(List.of(msg10, msg11));

        ResponseEntity<List<ChatMessage>> response = conversationResource.getMessages(100L, 9L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody().get(0).getId()).isEqualTo(10L);
        assertThat(response.getBody().get(1).getId()).isEqualTo(11L);

        verify(chatMessageRepository, times(1)).findByConversationIdAndIdGreaterThanOrderByCreatedDateAsc(100L, 9L);
        verify(chatMessageRepository, never()).findByConversationIdOrderByCreatedDateAsc(100L);
    }

    @Test
    @DisplayName("4. Non-participant user is forbidden (403) from sending messages")
    void sendMessage_unauthorizedUser_returnsForbidden() throws URISyntaxException {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("intruder", "token")
        );
        when(userRepository.findOneWithAuthoritiesByLogin("intruder")).thenReturn(Optional.of(intruderUser));
        when(conversationRepository.findById(100L)).thenReturn(Optional.of(conversation));

        ChatMessage req = new ChatMessage();
        req.setContent("Intruder text");

        ResponseEntity<ChatMessage> response = conversationResource.sendMessage(100L, req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(chatMessageService, never()).saveMessageWithResult(any(), any(), any(), any());
    }
}
