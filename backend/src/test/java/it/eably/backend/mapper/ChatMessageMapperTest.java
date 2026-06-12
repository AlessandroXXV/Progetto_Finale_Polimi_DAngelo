package it.eably.backend.mapper;

import it.eably.backend.dto.chat.ChatMessageDTO;
import it.eably.backend.model.Booking;
import it.eably.backend.model.ChatMessage;
import it.eably.backend.model.User;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChatMessageMapperTest {

    private final ChatMessageMapper mapper = Mappers.getMapper(ChatMessageMapper.class);

    @Test
    void toDTO_MapsNestedFields() {
        Booking booking = new Booking();
        booking.setId(99L);

        User sender = new User();
        sender.setId(11L);
        sender.setUsername("sender11");

        ChatMessage message = new ChatMessage();
        message.setId(5L);
        message.setBooking(booking);
        message.setSender(sender);
        message.setContent("hello world");
        message.setCreatedAt(LocalDateTime.of(2026, 4, 27, 12, 30));

        ChatMessageDTO dto = mapper.toDTO(message);

        assertEquals(5L, dto.id());
        assertEquals(99L, dto.bookingId());
        assertEquals(11L, dto.senderId());
        assertEquals("sender11", dto.senderUsername());
        assertEquals("hello world", dto.content());
        assertEquals(LocalDateTime.of(2026, 4, 27, 12, 30), dto.timestamp());
    }

    @Test
    void toDTOList_MapsAllMessages() {
        Booking booking = new Booking();
        booking.setId(1L);

        User sender = new User();
        sender.setId(2L);
        sender.setUsername("user2");

        ChatMessage m1 = new ChatMessage();
        m1.setId(1L);
        m1.setBooking(booking);
        m1.setSender(sender);
        m1.setContent("first");
        m1.setCreatedAt(LocalDateTime.of(2026, 4, 27, 10, 0));

        ChatMessage m2 = new ChatMessage();
        m2.setId(2L);
        m2.setBooking(booking);
        m2.setSender(sender);
        m2.setContent("second");
        m2.setCreatedAt(LocalDateTime.of(2026, 4, 27, 11, 0));

        List<ChatMessageDTO> result = mapper.toDTOList(List.of(m1, m2));

        assertEquals(2, result.size());
        assertEquals("first", result.getFirst().content());
        assertEquals("second", result.get(1).content());
    }
}

