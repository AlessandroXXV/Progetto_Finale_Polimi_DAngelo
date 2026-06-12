package it.eably.backend.controller;

import it.eably.backend.dto.chat.ChatMessageDTO;
import it.eably.backend.service.def.BookingService;
import it.eably.backend.service.def.ChatMessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * WebSocket controller for real-time chat functionality.
 * 
 * WEBSOCKET ARCHITECTURE:
 * - Uses STOMP protocol over WebSocket
 * - Messages sent to /app/chat/{bookingId}
 * - Messages broadcast to /topic/booking/{bookingId}
 * - All participants in a booking can see messages in real-time
 * 
 * MESSAGE FLOW:
 * 1. Client connects to WebSocket endpoint (/ws-eably)
 * 2. Client subscribes to /topic/booking/{bookingId}
 * 3. Client sends message to /app/chat/{bookingId}
 * 4. Server receives message in sendMessage()
 * 5. Server enriches message with sender info
 * 6. Server broadcasts to /topic/booking/{bookingId}
 * 7. All subscribers receive the message
 * 
 * SECURITY:
 * - Authentication required (Spring Security integration)
 * - Authorization check: User must be participant in booking
 * - Sender info extracted from JWT token
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */
@Controller
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final BookingService bookingService;
    private final ChatMessageService chatMessageService;

    public ChatController(SimpMessagingTemplate messagingTemplate,
                         BookingService bookingService,
                         ChatMessageService chatMessageService) {
        this.messagingTemplate = messagingTemplate;
        this.bookingService = bookingService;
        this.chatMessageService = chatMessageService;
    }

    /**
     * Handles incoming chat messages for a specific booking.
     * <p>
     * MESSAGE MAPPING:
     * - Client sends to: /app/chat/{bookingId}
     * - Server receives here
     * - Server broadcasts to: /topic/booking/{bookingId}
     * <p>
     * PROCESS:
     * 1. Receive message from client
     * 2. Extract sender info from authentication
     * 3. Validate user is participant in booking
     * 4. Enrich message with sender details and timestamp
     * 5. Broadcast to all subscribers of booking topic
     * <p>
     * SECURITY:
     * - Authentication automatically handled by Spring Security
     * - Authorization check: User must be client or provider of booking
     *
     * @param bookingId the booking ID (from URL path)
     * @param message   the chat message payload
     * @param principal authenticated user principal (set during STOMP CONNECT)
     */
    @MessageMapping("/chat/{bookingId}")
    public void sendMessage(
            @DestinationVariable Long bookingId,
            @Payload ChatMessageDTO message,
            Principal principal)
    {

        log.info("Received chat message for booking {} from user", bookingId);

        try {

            // Extract sender info from authentication
            if (principal == null) {
                log.warn("Unauthenticated WebSocket message rejected for booking {}", bookingId);
                return;
            }
            String username = principal.getName();

            if (!bookingService.isBookingOwner(bookingId, username)) {
                log.warn("User {} attempted to send message to booking {} without authorization",
                        username, bookingId);
                return;
            }

            // Enrich message with sender details and timestamp
            ChatMessageDTO persistedMessage = chatMessageService.processAndSaveMessage(
                    bookingId,
                    username,
                    message.content()
            );

            String destination = "/topic/booking/" + bookingId;
            messagingTemplate.convertAndSend(destination, persistedMessage);

            log.info("Broadcast message to {}", destination);

        } catch (Exception e) {
            log.error("Error processing chat message for booking {}: {}", bookingId, e.getMessage(), e);
            // Don't propagate exception to client - WebSocket error handling is complex
        }
    }
}
