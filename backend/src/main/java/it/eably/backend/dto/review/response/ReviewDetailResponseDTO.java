package it.eably.backend.dto.review.response;

import java.time.LocalDateTime;

/**
 * DTO for detailed review responses with student and service information.
 * 
 * Contains:
 * - Review ID
 * - Rating (1-5)
 * - Comment (optional)
 * - Reviewed at timestamp
 * - Student username (reviewee)
 * - Service title (from profile)
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */
public record ReviewDetailResponseDTO(
    Long id,
    Integer rating,
    String comment,
    LocalDateTime reviewedAt,
    String studentUsername,
    String serviceTitle
) {}
