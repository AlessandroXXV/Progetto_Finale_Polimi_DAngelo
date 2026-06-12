package it.eably.backend.service.def;

import it.eably.backend.dto.review.response.ProfileReviewsResponseDTO;
import it.eably.backend.dto.review.response.ReviewDetailResponseDTO;
import it.eably.backend.dto.review.request.ReviewRequestDTO;
import it.eably.backend.dto.review.response.ReviewResponseDTO;

import java.util.List;

/**
 * Service interface for review management.
 * <p>
 * Provides business logic for:
 * - Creating reviews (with validation)
 * - Retrieving reviews written by a client
 * - Retrieving reviews received by a student
 * - Checking review existence for a booking
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */
public interface ReviewService
{

    /**
     * Creates a new review for a completed booking.
     * Validates booking status, duplicate reviews, and authorization.
     *
     * @param clientId   the client user ID
     * @param requestDTO the review creation request
     * @return the created review as DTO
     */
    ReviewResponseDTO createReview(Long clientId, ReviewRequestDTO requestDTO);

    /**
     * Retrieves all reviews written by a client.
     *
     * @param reviewerId the reviewer user ID
     * @return list of reviews ordered by most recent first
     */
    List<ReviewResponseDTO> getReviewsByReviewer(Long reviewerId);

    /**
     * Retrieves detailed reviews written by a client with student and service info.
     *
     * @param reviewerId the reviewer user ID
     * @return list of detailed reviews ordered by most recent first
     */
    List<ReviewDetailResponseDTO> getReviewsWithDetailsByReviewer(Long reviewerId);

    /**
     * Retrieves all reviews received by a student.
     *
     * @param studentId the student user ID
     * @return list of reviews ordered by most recent first
     */
    List<ReviewResponseDTO> getReviewsByReviewee(Long studentId);

    /**
     * Retrieves reviews for a specific profile (service), with average rating.
     *
     * @param profileId the profile ID
     * @return reviews and average rating for that specific service
     */
    ProfileReviewsResponseDTO getReviewsByProfile(Long profileId);

    /**
     * Checks if a review exists for a booking.
     *
     * @param bookingId the booking ID
     * @return true if review exists, false otherwise
     */
    boolean existsReviewForBooking(Long bookingId);
}
