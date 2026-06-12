package it.eably.backend.dto.review.response;

import java.time.LocalDateTime;

/**
 * DTO for basic review responses.
 * 
 * Contains:
 * - Review ID
 * - Booking ID
 * - Reviewer ID (client)
 * - Reviewee ID (student profile)
 * - Rating (1-5)
 * - Comment (optional)
 * - Reviewed at timestamp
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */
public record ReviewResponseDTO(
    Long id,
    Long bookingId,
    Long reviewerId,
    Long revieweeId,
    Integer rating,
    String comment,
    LocalDateTime reviewedAt
) {}
