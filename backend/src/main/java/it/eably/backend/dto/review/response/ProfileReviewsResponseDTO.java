package it.eably.backend.dto.review.response;

import java.util.List;

/**
 * Aggregated review data for a tutor profile.
 * Bundles pre-computed {@code averageRating} and {@code totalCount} with the review list
 * so the profile page can render the summary card and review list in one fetch.
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */
public record ProfileReviewsResponseDTO(
    Long profileId,
    Double averageRating,
    long totalCount,
    List<ReviewResponseDTO> reviews
) {}
