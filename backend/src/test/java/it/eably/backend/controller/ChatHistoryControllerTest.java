package it.eably.backend.controller;

import it.eably.backend.dto.chat.ChatMessageDTO;
import it.eably.backend.exception.AuthorizationException;
import it.eably.backend.model.User;
import it.eably.backend.service.def.BookingService;
import it.eably.backend.service.def.ChatMessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatHistoryControllerTest {

    @Mock
    private ChatMessageService chatMessageService;

    @Mock
    private BookingService bookingService;

    @InjectMocks
    private ChatHistoryController chatHistoryController;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(10L);
        user.setUsername("student");
    }

    @Test
    void getMessageHistory_WithBeforeId_UsesCursorQuery() {
        List<ChatMessageDTO> history = List.of(
                new ChatMessageDTO(1L, 99L, 10L, "student", "hello", LocalDateTime.now())
        );

        when(bookingService.isBookingOwner(99L, 10L)).thenReturn(true);
        when(chatMessageService.getBookingMessagesBefore(99L, 50L, 20)).thenReturn(history);

        ResponseEntity<List<ChatMessageDTO>> response = chatHistoryController.getMessageHistory(
                99L, 50L, 20, 0, 30, user
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals("hello", response.getBody().getFirst().content());
    }

    @Test
    void getMessageHistory_WithoutBeforeId_UsesPaginationQuery() {
        User anotherUser = new User();
        anotherUser.setId(11L);
        anotherUser.setUsername("client");

        when(bookingService.isBookingOwner(100L, 11L)).thenReturn(true);
        when(chatMessageService.getBookingMessages(100L, 2, 15)).thenReturn(List.of());

        ResponseEntity<List<ChatMessageDTO>> response = chatHistoryController.getMessageHistory(
                100L, null, 50, 2, 15, anotherUser
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(0, response.getBody().size());
    }

    @Test
    void getMessageHistory_NotParticipant_ThrowsAuthorizationException() {
        User otherUser = new User();
        otherUser.setId(12L);

        when(bookingService.isBookingOwner(200L, 12L)).thenReturn(false);

        assertThrows(AuthorizationException.class, () ->
                chatHistoryController.getMessageHistory(200L, null, 50, 0, 50, otherUser));
    }
}
