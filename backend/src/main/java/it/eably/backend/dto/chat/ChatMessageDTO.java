package it.eably.backend.dto.chat;

import java.time.LocalDateTime;

/**
 * DTO for chat messages in real-time booking discussions.
 * 
 * This record encapsulates chat message data for WebSocket communication.
 * Messages are sent via STOMP protocol to booking-specific topics.
 * 
 * MESSAGE FLOW:
 * 1. Client sends message to /app/chat/{bookingId}
 * 2. Server receives message in ChatController
 * 3. Server broadcasts message to /topic/booking/{bookingId}
 * 4. All subscribers to that topic receive the message
 * 
 * @param bookingId the booking ID this message belongs to
 * @param senderId the user ID of the sender
 * @param senderUsername the username of the sender
 * @param content the message content
 * @param timestamp when the message was sent
 * 
 * @author Alessandro D'Angelo
 * @version 1.0
 */
public record ChatMessageDTO(
        Long id,
        Long bookingId,
        Long senderId,
        String senderUsername,
        String content,
        LocalDateTime timestamp
) {
}
