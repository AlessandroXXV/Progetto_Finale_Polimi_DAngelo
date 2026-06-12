package it.eably.backend.dto.review.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for creating reviews.
 * 
 * Validation rules:
 * - bookingId: required
 * - rating: 1-5 (required)
 * - comment: optional (if present, minimum 10 characters)
 *
 * Business rules (validated in service):
 * - Booking must be COMPLETED
 * - Only one review per booking
 * - Only the client of the booking can review
 * - Only verified users can review
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */
public record ReviewRequestDTO(
    @NotNull(message = "Booking ID is required")
    Long bookingId,
    
    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be between 1 and 5")
    @Max(value = 5, message = "Rating must be between 1 and 5")
    Integer rating,
    
    String comment
) {}
