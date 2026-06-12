package it.eably.backend.controller;

import it.eably.backend.dto.chat.ChatMessageDTO;
import it.eably.backend.service.def.BookingService;
import it.eably.backend.service.def.ChatMessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.security.Principal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private BookingService bookingService;

    @Mock
    private ChatMessageService chatMessageService;

    @InjectMocks
    private ChatController chatController;

    private Principal mockPrincipal;

    @BeforeEach
    void setUp() {
        mockPrincipal = () -> "testuser";
    }

    @Test
    void sendMessage_Success_BroadcastsToTopic() {
        when(bookingService.isBookingOwner(1L, "testuser")).thenReturn(true);
        when(chatMessageService.processAndSaveMessage(1L, "testuser", "Hello"))
                .thenReturn(new ChatMessageDTO(99L, 1L, 1L, "testuser", "Hello", LocalDateTime.now()));

        ChatMessageDTO message = new ChatMessageDTO(null, 1L, null, null, "Hello", null);
        chatController.sendMessage(1L, message, mockPrincipal);

        verify(messagingTemplate).convertAndSend(eq("/topic/booking/1"), any(ChatMessageDTO.class));
    }

    @Test
    void sendMessage_NullPrincipal_DoesNotProcess() {
        ChatMessageDTO message = new ChatMessageDTO(null, 1L, null, null, "Hello", null);
        chatController.sendMessage(1L, message, null);

        verify(bookingService, never()).isBookingOwner(anyLong(), anyString());
        verify(chatMessageService, never()).processAndSaveMessage(anyLong(), anyString(), anyString());
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(ChatMessageDTO.class));
    }

    @Test
    void sendMessage_NotParticipant_DoesNotSaveOrBroadcast() {
        when(bookingService.isBookingOwner(1L, "testuser")).thenReturn(false);

        ChatMessageDTO message = new ChatMessageDTO(null, 1L, null, null, "Hello", null);
        chatController.sendMessage(1L, message, mockPrincipal);

        verify(chatMessageService, never()).processAndSaveMessage(anyLong(), anyString(), anyString());
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(ChatMessageDTO.class));
    }

    @Test
    void sendMessage_UserNotFound_DoesNotBroadcast() {
        when(bookingService.isBookingOwner(1L, "testuser")).thenReturn(true);
        when(chatMessageService.processAndSaveMessage(anyLong(), anyString(), anyString()))
                .thenThrow(new RuntimeException("User not found"));

        ChatMessageDTO message = new ChatMessageDTO(null, 1L, null, null, "Hello", null);
        chatController.sendMessage(1L, message, mockPrincipal);

        verify(messagingTemplate, never()).convertAndSend(anyString(), any(ChatMessageDTO.class));
    }

    @Test
    void sendMessage_ServiceThrows_DoesNotPropagateException() {
        when(bookingService.isBookingOwner(1L, "testuser")).thenReturn(true);
        when(chatMessageService.processAndSaveMessage(anyLong(), anyString(), anyString()))
                .thenThrow(new RuntimeException("Error"));

        ChatMessageDTO message = new ChatMessageDTO(null, 1L, null, null, "Hello", null);

        assertDoesNotThrow(() -> chatController.sendMessage(1L, message, mockPrincipal));
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(ChatMessageDTO.class));
    }
}
