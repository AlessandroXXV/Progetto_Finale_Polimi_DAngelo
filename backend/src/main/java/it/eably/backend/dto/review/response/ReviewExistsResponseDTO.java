package it.eably.backend.dto.review.response;

/**
 * DTO for review existence check response.
 * 
 * Used for:
 * - GET /api/v1/reviews/booking/{booking_id}/exists endpoint
 * 
 * Returns whether a review exists for a specific booking.
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */
public record ReviewExistsResponseDTO(
    Boolean exists
) {}
