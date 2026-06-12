package it.eably.backend.controller;

import it.eably.backend.dto.review.response.ProfileReviewsResponseDTO;
import it.eably.backend.dto.review.response.ReviewDetailResponseDTO;
import it.eably.backend.dto.review.response.ReviewExistsResponseDTO;
import it.eably.backend.dto.review.request.ReviewRequestDTO;
import it.eably.backend.dto.review.response.ReviewResponseDTO;
import it.eably.backend.model.User;
import it.eably.backend.service.def.ReviewService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for review management.
 * 
 * Endpoints:
 * <ul>
 * <li>POST /api/v1/reviews/ - Create review (CLIENT only, verified)</li>
 * <li>GET /api/v1/reviews/client/{user_id} - Get reviews written by client</li>
 * <li>GET /api/v1/reviews/client/{user_id}/details - Get detailed reviews written by client</li>
 * <li>GET /api/v1/reviews/student/{user_id} - Get reviews received by student</li>
 * <li>GET /api/v1/reviews/booking/{booking_id}/exists - Check if review exists</li>
 * </ul>
 * 
 * All endpoints require authentication.
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */
@RestController
@RequestMapping("/api/v1/reviews")
public class ReviewController {
    
    private static final Logger logger = LoggerFactory.getLogger(ReviewController.class);
    
    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }
    
    /**
     * Creates a new review for a completed booking.
     * Only verified clients can create reviews.
     * 
     * @param requestDTO the review creation request
     * @param user the authenticated user
     * @return ResponseEntity with created review data (201 Created)
     */
    @PostMapping
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<ReviewResponseDTO> createReview(
            @Valid @RequestBody ReviewRequestDTO requestDTO,
            @AuthenticationPrincipal User user) {
        
        logger.info("POST /api/v1/reviews/ - User: {}", user.getUsername());

        ReviewResponseDTO responseDTO = reviewService.createReview(user.getId(), requestDTO);

        logger.info("Successfully created review ID: {} for booking ID: {} by user: {}",
            responseDTO.id(), requestDTO.bookingId(), user.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
    }
    
    /**
     * Retrieves all reviews written by a client.
     * 
     * @param userId the client user ID
     * @return ResponseEntity with list of reviews
     */
    @GetMapping("/client/{user_id}")
    public ResponseEntity<List<ReviewResponseDTO>> getReviewsByClient(@PathVariable("user_id") Long userId) {
        
        logger.info("GET /api/v1/reviews/client/{}", userId);
        
        // Get reviews
        List<ReviewResponseDTO> reviews = reviewService.getReviewsByReviewer(userId);
        
        logger.info("Retrieved {} reviews written by client ID: {}", reviews.size(), userId);
        return ResponseEntity.ok(reviews);
    }
    
    /**
     * Retrieves detailed reviews written by a client with student and service info.
     * 
     * @param userId the client user ID
     * @return ResponseEntity with list of detailed reviews
     */
    @GetMapping("/client/{user_id}/details")
    public ResponseEntity<List<ReviewDetailResponseDTO>> getReviewsWithDetailsByClient(
            @PathVariable("user_id") Long userId) {
        
        logger.info("GET /api/v1/reviews/client/{}/details", userId);
        
        // Get detailed reviews
        List<ReviewDetailResponseDTO> reviews = reviewService.getReviewsWithDetailsByReviewer(userId);
        
        logger.info("Retrieved {} detailed reviews written by client ID: {}", reviews.size(), userId);
        return ResponseEntity.ok(reviews);
    }
    
    /**
     * Retrieves all reviews received by a student.
     * 
     * @param userId the student user ID
     * @return ResponseEntity with list of reviews
     */
    @GetMapping("/student/{user_id}")
    public ResponseEntity<List<ReviewResponseDTO>> getReviewsByStudent(@PathVariable("user_id") Long userId) {
        
        logger.info("GET /api/v1/reviews/student/{}", userId);
        
        // Get reviews
        List<ReviewResponseDTO> reviews = reviewService.getReviewsByReviewee(userId);
        
        logger.info("Retrieved {} reviews received by student ID: {}", reviews.size(), userId);
        return ResponseEntity.ok(reviews);
    }
    
    /**
     * Retrieves reviews and average rating for a specific profile (service).
     *
     * @param profileId the profile ID
     * @return ResponseEntity with reviews and average rating for that service
     */
    @GetMapping("/profile/{profile_id}")
    public ResponseEntity<ProfileReviewsResponseDTO> getReviewsByProfile(
            @PathVariable("profile_id") Long profileId) {

        logger.info("GET /api/v1/reviews/profile/{}", profileId);

        ProfileReviewsResponseDTO response = reviewService.getReviewsByProfile(profileId);

        logger.info("Retrieved {} reviews for profile ID: {}", response.totalCount(), profileId);
        return ResponseEntity.ok(response);
    }

    /**
     * Checks if a review exists for a booking.
     *
     * @param bookingId the booking ID
     * @return ResponseEntity with exists flag
     */
    @GetMapping("/booking/{booking_id}/exists")
    public ResponseEntity<ReviewExistsResponseDTO> checkReviewExists(
            @PathVariable("booking_id") Long bookingId) {
        
        logger.info("GET /api/v1/reviews/booking/{}/exists", bookingId);
        
        // Check existence
        boolean exists = reviewService.existsReviewForBooking(bookingId);
        ReviewExistsResponseDTO responseDTO = new ReviewExistsResponseDTO(exists);
        
        logger.info("Review exists for booking ID {}: {}", bookingId, exists);
        return ResponseEntity.ok(responseDTO);
    }
}
