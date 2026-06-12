package it.eably.backend.service.def;

import it.eably.backend.dto.chat.ChatMessageDTO;

import java.util.List;

/**
 * Service interface for real-time chat message management.
 * <p>
 * Handles persistence and retrieval of messages linked to booking sessions.
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */
public interface ChatMessageService {

    /**
     * Sanitizes, persists, and prepares a new chat message for broadcast.
     *
     * @param bookingId internal booking identifier
     * @param username  sender's username
     * @param content   raw message text
     * @return the saved message as a DTO
     */
    ChatMessageDTO processAndSaveMessage(Long bookingId, String username, String content);

    /**
     * Retrieves a paginated history of messages for a specific booking.
     *
     * @param bookingId internal booking identifier
     * @param page      zero-based page index
     * @param size      number of messages per page
     * @return chronological list of messages
     */
    List<ChatMessageDTO> getBookingMessages(Long bookingId, int page, int size);

    /**
     * Retrieves messages older than a specific message ID (cursor-based pagination).
     *
     * @param bookingId internal booking identifier
     * @param beforeId  anchor message ID
     * @param limit     maximum results to return
     * @return list of older messages in chronological order
     */
    List<ChatMessageDTO> getBookingMessagesBefore(Long bookingId, Long beforeId, int limit);
}
