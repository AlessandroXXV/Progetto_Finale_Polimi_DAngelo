package it.eably.backend.service;

import it.eably.backend.dto.chat.ChatMessageDTO;
import it.eably.backend.exception.ValidationException;
import it.eably.backend.mapper.ChatMessageMapper;
import it.eably.backend.model.Booking;
import it.eably.backend.model.ChatMessage;
import it.eably.backend.model.User;
import it.eably.backend.repository.BookingRepository;
import it.eably.backend.repository.ChatMessageRepository;
import it.eably.backend.repository.UserRepository;
import it.eably.backend.service.impl.ChatMessageServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatMessageServiceImplTest {

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ChatMessageMapper chatMessageMapper;

    @InjectMocks
    private ChatMessageServiceImpl chatMessageService;

    @Test
    void processAndSaveMessage_ResolvesUsernameAndPersists() {
        User sender = new User();
        sender.setId(2L);
        sender.setUsername("alice");

        Booking booking = new Booking();
        booking.setId(1L);

        ChatMessage entity = new ChatMessage(booking, sender, "hello");
        entity.setId(10L);
        entity.setCreatedAt(LocalDateTime.now());

        ChatMessageDTO dto = new ChatMessageDTO(10L, 1L, 2L, "alice", "hello", entity.getCreatedAt());

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(sender));
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(entity);
        when(chatMessageMapper.toDTO(entity)).thenReturn(dto);

        ChatMessageDTO result = chatMessageService.processAndSaveMessage(1L, "alice", "hello");

        assertEquals(10L, result.id());
        assertEquals("hello", result.content());
        verify(userRepository, never()).findById(anyLong());
    }

    @Test
    void processAndSaveMessage_UserNotFoundThrowsValidationException() {
        Booking booking = new Booking();
        booking.setId(1L);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());
        assertThrows(ValidationException.class, () -> chatMessageService.processAndSaveMessage(1L, "unknown", "hello"));
    }

    @Test
    void getBookingMessages_WithPagination_ReturnsChronologicalPage() {
        ChatMessage newer = new ChatMessage();
        ChatMessage older = new ChatMessage();

        // Repository returns DESC page content; service reverses to ASC for UI.
        ChatMessageDTO olderDto = new ChatMessageDTO(10L, 100L, 2L, "alice", "older", LocalDateTime.now());
        ChatMessageDTO newerDto = new ChatMessageDTO(11L, 100L, 3L, "bob", "newer", LocalDateTime.now());

        when(chatMessageRepository.findByBookingIdOrderByCreatedAtDesc(eq(100L), eq(PageRequest.of(1, 2))))
                .thenReturn(new PageImpl<>(List.of(newer, older)));
        when(chatMessageMapper.toDTOList(List.of(older, newer))).thenReturn(List.of(olderDto, newerDto));

        List<ChatMessageDTO> result = chatMessageService.getBookingMessages(100L, 1, 2);

        assertEquals(2, result.size());
        assertEquals("older", result.get(0).content());
        assertEquals("newer", result.get(1).content());
    }

    @Test
    void getBookingMessages_WithInvalidPagination_ThrowsValidationException() {
        assertThrows(ValidationException.class, () -> chatMessageService.getBookingMessages(100L, -1, 20));
        assertThrows(ValidationException.class, () -> chatMessageService.getBookingMessages(100L, 0, 0));
        assertThrows(ValidationException.class, () -> chatMessageService.getBookingMessages(100L, 0, 300));
    }

    @Test
    void getBookingMessagesBefore_WithCursor_ReturnsChronologicalPage() {
        ChatMessage newerOlder = new ChatMessage();
        ChatMessage olderOlder = new ChatMessage();

        ChatMessageDTO olderDto = new ChatMessageDTO(20L, 100L, 2L, "alice", "older", LocalDateTime.now());
        ChatMessageDTO newerDto = new ChatMessageDTO(21L, 100L, 3L, "bob", "newer", LocalDateTime.now());

        when(chatMessageRepository.findByBookingIdAndIdLessThanOrderByIdDesc(eq(100L), eq(50L), eq(PageRequest.of(0, 2))))
                .thenReturn(new PageImpl<>(List.of(newerOlder, olderOlder)));
        when(chatMessageMapper.toDTOList(List.of(olderOlder, newerOlder))).thenReturn(List.of(olderDto, newerDto));

        List<ChatMessageDTO> result = chatMessageService.getBookingMessagesBefore(100L, 50L, 2);

        assertEquals(2, result.size());
        assertEquals("older", result.get(0).content());
        assertEquals("newer", result.get(1).content());
    }

    @Test
    void getBookingMessagesBefore_WithInvalidCursor_ThrowsValidationException() {
        assertThrows(ValidationException.class, () -> chatMessageService.getBookingMessagesBefore(100L, null, 20));
        assertThrows(ValidationException.class, () -> chatMessageService.getBookingMessagesBefore(100L, 0L, 20));
        assertThrows(ValidationException.class, () -> chatMessageService.getBookingMessagesBefore(100L, 10L, 0));
    }
}
