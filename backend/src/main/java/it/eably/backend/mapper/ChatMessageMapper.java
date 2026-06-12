package it.eably.backend.mapper;

import it.eably.backend.dto.chat.ChatMessageDTO;
import it.eably.backend.model.ChatMessage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * MapStruct mapper for {@link ChatMessage} entity to DTO conversion.
 *
 * <p>Handles the flattening of nested associations ({@code booking.id},
 * {@code sender.id}, {@code sender.username}) and the renaming of
 * {@code createdAt} to {@code timestamp} required by the API contract.</p>
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */
@Mapper(componentModel = "spring")
public interface ChatMessageMapper {

    /**
     * Maps a {@link ChatMessage} entity to a {@link ChatMessageDTO}.
     *
     * <p>The following field mappings are applied:</p>
     * <ul>
     *   <li>{@code booking.id}       → {@code bookingId}</li>
     *   <li>{@code sender.id}        → {@code senderId}</li>
     *   <li>{@code sender.username}  → {@code senderUsername}</li>
     *   <li>{@code createdAt}        → {@code timestamp}</li>
     * </ul>
     *
     * @param chatMessage the chat message entity
     * @return the chat message DTO
     */
    @Mapping(source = "booking.id", target = "bookingId")
    @Mapping(source = "sender.id", target = "senderId")
    @Mapping(source = "sender.username", target = "senderUsername")
    @Mapping(source = "createdAt", target = "timestamp")
    ChatMessageDTO toDTO(ChatMessage chatMessage);

    /**
     * Maps a list of {@link ChatMessage} entities to a list of {@link ChatMessageDTO}s.
     *
     * @param messages the list of chat message entities
     * @return the list of chat message DTOs
     */
    List<ChatMessageDTO> toDTOList(List<ChatMessage> messages);
}

