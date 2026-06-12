package it.eably.backend.service.impl;

import it.eably.backend.dto.chat.ChatMessageDTO;
import it.eably.backend.exception.ValidationException;
import it.eably.backend.mapper.ChatMessageMapper;
import it.eably.backend.model.Booking;
import it.eably.backend.model.ChatMessage;
import it.eably.backend.model.User;
import it.eably.backend.repository.BookingRepository;
import it.eably.backend.repository.ChatMessageRepository;
import it.eably.backend.repository.UserRepository;
import it.eably.backend.service.def.ChatMessageService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Chat message service implementation for booking conversations.
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */
@Service
public class ChatMessageServiceImpl implements ChatMessageService {

    /** Repository for chat messages. */
    private final ChatMessageRepository chatMessageRepository;
    /** Repository for bookings. */
    private final BookingRepository bookingRepository;
    /** Repository for users. */
    private final UserRepository userRepository;
    /** Mapper for chat message DTOs. */
    private final ChatMessageMapper chatMessageMapper;

    /**
     * Builds the chat message service with required dependencies.
     *
     * @param chatMessageRepository repository for chat messages
     * @param bookingRepository repository for bookings
     * @param userRepository repository for users
     * @param chatMessageMapper mapper for chat message DTOs
     */
    public ChatMessageServiceImpl(ChatMessageRepository chatMessageRepository,
                                  BookingRepository bookingRepository,
                                  UserRepository userRepository,
                                  ChatMessageMapper chatMessageMapper) {
        this.chatMessageRepository = chatMessageRepository;
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.chatMessageMapper = chatMessageMapper;
    }

    /**
     * Validates, processes, and persists a chat message for a booking.
     *
     * @param bookingId booking id
     * @param username sender username
     * @param content message content
     * @return saved chat message DTO
     * @throws ValidationException when content is blank or booking/user is not found
     */
    @Override
    @Transactional
    public ChatMessageDTO processAndSaveMessage(Long bookingId, String username, String content) {
        // Prevent storing blank or whitespace-only messages to keep chat clean.
        String sanitizedContent = content == null ? "" : content.trim();
        if (sanitizedContent.isBlank()) {
            throw new ValidationException("Chat message content cannot be empty");
        }

        // Verify the booking context exists before attaching a message.
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ValidationException("Booking not found with ID: " + bookingId));

        // Locate the sender in our user registry.
        User sender = userRepository.findByUsername(username)
                .orElseThrow(() -> new ValidationException("User not found: " + username));

        // Persist the message within the transactional context.
        ChatMessage chatMessage = new ChatMessage(booking, sender, sanitizedContent);
        ChatMessage savedMessage = chatMessageRepository.save(chatMessage);
        return chatMessageMapper.toDTO(savedMessage);
    }

    /**
     * Returns a page of messages for a booking in ascending order.
     *
     * @param bookingId booking id
     * @param page page index (0-based)
     * @param size page size (1..200)
     * @return ordered list of chat message DTOs
     * @throws ValidationException when page or size is invalid
     */
    @Override
    @Transactional(readOnly = true)
    public List<ChatMessageDTO> getBookingMessages(Long bookingId, int page, int size) {
        // Guard against invalid pagination parameters.
        if (page < 0) {
            throw new ValidationException("Page index cannot be negative");
        }
        validateLimit(size, "Page size");

        // Fetch the newest records first to optimize.
        Page<ChatMessage> pageResult = chatMessageRepository.findByBookingIdOrderByCreatedAtDesc(
                bookingId,
                PageRequest.of(page, size)
        );

        // Reverse the DESC result set back to chronological ASC order before returning to the UI.
        List<ChatMessage> orderedMessages = new ArrayList<>(pageResult.getContent());
        Collections.reverse(orderedMessages);
        return chatMessageMapper.toDTOList(orderedMessages);
    }

    /**
     * Returns messages before a given message id for a booking.
     *
     * @param bookingId booking id
     * @param beforeId exclusive upper bound message id
     * @param limit max results (1..200)
     * @return ordered list of chat message DTOs
     * @throws ValidationException when beforeId or limit is invalid
     */
    @Override
    @Transactional(readOnly = true)
    public List<ChatMessageDTO> getBookingMessagesBefore(Long bookingId, Long beforeId, int limit) {
        // ID must be positive for valid cursor-based filtering.
        if (beforeId == null || beforeId <= 0) {
            throw new ValidationException("beforeId must be a positive number");
        }
        validateLimit(limit, "Limit");

        // Cursor-based lookup: fetch older messages relative to a specific message ID.
        Page<ChatMessage> pageResult = chatMessageRepository.findByBookingIdAndIdLessThanOrderByIdDesc(
                bookingId,
                beforeId,
                PageRequest.of(0, limit)
        );

        // Switch derived DESC order back to chronological ASC for the frontend view.
        List<ChatMessage> orderedMessages = new ArrayList<>(pageResult.getContent());
        Collections.reverse(orderedMessages);
        return chatMessageMapper.toDTOList(orderedMessages);
    }

    /**
     * Validates a numeric limit field.
     *
     * @param value numeric value
     * @param fieldName field label for error messages
     * @throws ValidationException when value is outside 1..200
     */
    private void validateLimit(int value, String fieldName) {
        if (value < 1 || value > 200) {
            throw new ValidationException(fieldName + " must be between 1 and 200");
        }
    }
}
