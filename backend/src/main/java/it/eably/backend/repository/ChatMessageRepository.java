package it.eably.backend.repository;

import it.eably.backend.model.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for {@link it.eably.backend.model.ChatMessage} entity.
 *
 * Provides paginated queries for chat message retrieval within a booking conversation.
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */
@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /**
     * Finds a page of messages for a booking, ordered by creation time descending.
     * Used to load the most recent messages in a chat thread.
     *
     * @param bookingId the booking ID whose messages to retrieve
     * @param pageable  pagination parameters
     * @return page of messages ordered by createdAt descending
     */
    Page<ChatMessage> findByBookingIdOrderByCreatedAtDesc(Long bookingId, Pageable pageable);

    /**
     * Finds a page of messages for a booking with ID less than the given cursor, ordered by ID descending.
     * Used for cursor-based pagination to load older messages.
     *
     * @param bookingId the booking ID whose messages to retrieve
     * @param beforeId  the exclusive upper bound on message ID (cursor)
     * @param pageable  pagination parameters
     * @return page of messages before the cursor, ordered by ID descending
     */
    Page<ChatMessage> findByBookingIdAndIdLessThanOrderByIdDesc(Long bookingId, Long beforeId, Pageable pageable);
}
