package it.eably.backend.controller;

import it.eably.backend.dto.chat.ChatMessageDTO;
import it.eably.backend.exception.AuthorizationException;
import it.eably.backend.model.User;
import it.eably.backend.service.def.BookingService;
import it.eably.backend.service.def.ChatMessageService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for the chat message history related to a booking.
 * <p>
 * All endpoints require authentication and that the user is a participant in the booking.
 * </p>
 *
 * Endpoints:
 * <ul>
 * <li>GET /api/v1/bookings/{bookingId}/messages – Retrieves the message history of a booking</li>
 * </ul>
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */
@RestController
@RequestMapping("/api/v1/bookings")
public class ChatHistoryController {

    private final ChatMessageService chatMessageService;
    private final BookingService bookingService;

    /**
     * Builds the controller by injecting the necessary services.
     *
     * @param chatMessageService the service for chat message management
     * @param bookingService     the service for booking management
     */
    public ChatHistoryController(ChatMessageService chatMessageService,
                                 BookingService bookingService) {
        this.chatMessageService = chatMessageService;
        this.bookingService = bookingService;
    }

    /**
     * Retrieves the chat message history for a booking.
     * <p>
     * Supports classic pagination (page/size) and ID-based cursor ({@code beforeId}).
     * If {@code beforeId} is provided, it returns the messages preceding that ID
     * (useful for infinite scrolling upwards).
     * </p>
     *
     * @param bookingId the booking ID
     * @param beforeId  (optional) ID of the message before which to fetch messages
     * @param limit     maximum number of messages to return when using {@code beforeId} (default 50)
     * @param page      page number for classic pagination (default 0)
     * @param size      page size for classic pagination (default 50)
     * @param user      the authenticated user
     * @return list of {@link ChatMessageDTO}
     * @throws it.eably.backend.exception.AuthorizationException if the user is not a participant in the booking
     */
    @GetMapping("/{bookingId}/messages")
    public ResponseEntity<List<ChatMessageDTO>> getMessageHistory(
            @PathVariable Long bookingId,
            @RequestParam(required = false) Long beforeId,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @AuthenticationPrincipal User user) {
        if (!bookingService.isBookingOwner(bookingId, user.getId())) {
            throw new AuthorizationException("User is not authorized to read chat for this booking");
        }

        if (beforeId != null) {
            return ResponseEntity.ok(chatMessageService.getBookingMessagesBefore(bookingId, beforeId, limit));
        }

        return ResponseEntity.ok(chatMessageService.getBookingMessages(bookingId, page, size));
    }
}