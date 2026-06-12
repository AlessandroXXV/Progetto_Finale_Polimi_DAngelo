package it.eably.backend.service.impl;

import it.eably.backend.dto.review.response.ProfileReviewsResponseDTO;
import it.eably.backend.dto.review.response.ReviewDetailResponseDTO;
import it.eably.backend.dto.review.request.ReviewRequestDTO;
import it.eably.backend.dto.review.response.ReviewResponseDTO;
import it.eably.backend.exception.AuthorizationException;
import it.eably.backend.exception.ConflictException;
import it.eably.backend.exception.ResourceNotFoundException;
import it.eably.backend.exception.ValidationException;
import it.eably.backend.mapper.ReviewMapper;
import it.eably.backend.model.*;
import it.eably.backend.repository.BookingRepository;
import it.eably.backend.repository.ProfileRepository;
import it.eably.backend.repository.ReviewRepository;
import it.eably.backend.repository.UserRepository;
import it.eably.backend.service.def.ReviewService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service implementation for review management.
 *
 * <p>Handles:</p>
 * <ul>
 *   <li>Review creation with comprehensive validation</li>
 *   <li>Review retrieval with ordering</li>
 *   <li>Detailed review queries with JOIN</li>
 *   <li>Review existence checks</li>
 * </ul>
 *
 * <p>All write operations are transactional.</p>
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */
@Service
public class ReviewServiceImpl implements ReviewService {

    /** Logger for review operations. */
    private static final Logger logger = LoggerFactory.getLogger(ReviewServiceImpl.class);
    /** Minimum length for optional review comments. */
    private static final int MIN_COMMENT_LENGTH = 10;

    /** Repository for reviews. */
    private final ReviewRepository reviewRepository;
    /** Repository for bookings. */
    private final BookingRepository bookingRepository;
    /** Repository for users. */
    private final UserRepository userRepository;
    /** Repository for profiles. */
    private final ProfileRepository profileRepository;
    /** Mapper for review DTOs. */
    private final ReviewMapper reviewMapper;

    /**
     * Builds the review service with required dependencies.
     *
     * @param reviewRepository repository for reviews
     * @param bookingRepository repository for bookings
     * @param userRepository repository for users
     * @param profileRepository repository for profiles
     * @param reviewMapper mapper for review DTOs
     */
    public ReviewServiceImpl(ReviewRepository reviewRepository,
                             BookingRepository bookingRepository,
                             UserRepository userRepository,
                             ProfileRepository profileRepository,
                             ReviewMapper reviewMapper) {
        this.reviewRepository = reviewRepository;
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.reviewMapper = reviewMapper;
    }

    /**
     * Creates a review for a completed booking.
     *
     * <p>Effect: validates ownership, status, duplicates and rating, then saves the review.</p>
     *
     * @param clientId reviewer user id
     * @param requestDTO review request data
     * @return created review response DTO
     * @throws ResourceNotFoundException when client or booking is not found
     * @throws AuthorizationException when client is not verified or not booking owner
     * @throws ConflictException when booking is not completed or review already exists
     * @throws ValidationException when rating/comment are invalid
     */
    @Override
    @Transactional
    public ReviewResponseDTO createReview(Long clientId, ReviewRequestDTO requestDTO) {
        logger.debug("Creating review for booking ID: {} by client ID: {}", requestDTO.bookingId(), clientId);

        // Load necessary entities for validation and association.
        User client = userRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with ID: " + clientId));

        Booking booking = bookingRepository.findById(requestDTO.bookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with ID: " + requestDTO.bookingId()));

        // Verify client is verified
        if (!client.getIsVerified()) {
            throw new AuthorizationException("Only verified users can create reviews");
        }

        // Check if client is owner of the booking
        if (!booking.getClient().getId().equals(clientId)) {
            throw new AuthorizationException("You can only review your own bookings");
        }

        // reviews are only permitted for sessions that reached a successful conclusion.
        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new ConflictException(
                    "Can only review completed bookings. Current status: " + booking.getStatus());
        }

        // Verify there is only one review per booking to prevent rating manipulation.
        if (reviewRepository.existsByBookingId(requestDTO.bookingId())) {
            throw new ConflictException("Review already exists for this booking");
        }

        // Basic range check for standard 1-5 star rating system.
        if (requestDTO.rating() < 1 || requestDTO.rating() > 5) {
            throw new ValidationException("Rating must be between 1 and 5");
        }

        // Normalize optional comment and validate min length only when present
        String normalizedComment = requestDTO.comment() == null ? "" : requestDTO.comment().trim();
        if (!normalizedComment.isEmpty() && normalizedComment.length() < MIN_COMMENT_LENGTH) {
            throw new ValidationException("Comment must be at least 10 characters when provided");
        }

        // Extract reviewee (profile/service from booking)
        Profile reviewee = booking.getProfile();

        // Create review entity
        Review review = new Review();
        review.setBooking(booking);
        review.setReviewer(client);
        review.setReviewee(reviewee);
        review.setRating(requestDTO.rating());
        review.setComment(normalizedComment);
        review.setReviewedAt(LocalDateTime.now());

        Review savedReview = reviewRepository.save(review);

        logger.info("Successfully created review ID: {} for booking ID: {} by client ID: {}",
                savedReview.getId(), requestDTO.bookingId(), clientId);

        return reviewMapper.toResponseDTO(savedReview);
    }

    /**
     * Returns reviews written by a reviewer.
     *
     * @param reviewerId reviewer user id
     * @return list of review response DTOs
     */
    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponseDTO> getReviewsByReviewer(Long reviewerId) {
        logger.debug("Retrieving reviews written by reviewer ID: {}", reviewerId);

        List<Review> reviews = reviewRepository.findByReviewerIdOrderByReviewedAtDesc(reviewerId);
        List<ReviewResponseDTO> responseDTOs = reviewMapper.toResponseDTOList(reviews);

        logger.debug("Found {} reviews written by reviewer ID: {}", responseDTOs.size(), reviewerId);
        return responseDTOs;
    }

    /**
     * Returns detailed reviews written by a reviewer.
     *
     * @param reviewerId reviewer user id
     * @return list of review detail response DTOs
     */
    @Override
    @Transactional(readOnly = true)
    public List<ReviewDetailResponseDTO> getReviewsWithDetailsByReviewer(Long reviewerId) {
        logger.debug("Retrieving detailed reviews written by reviewer ID: {}", reviewerId);

        List<Review> reviews = reviewRepository.findDetailedReviewsByReviewerId(reviewerId);
        List<ReviewDetailResponseDTO> responseDTOs = reviewMapper.toDetailResponseDTOList(reviews);

        logger.debug("Found {} detailed reviews written by reviewer ID: {}", responseDTOs.size(), reviewerId);
        return responseDTOs;
    }

    /**
     * Returns reviews received by a student across active profiles.
     *
     * @param studentId student user id
     * @return list of review response DTOs
     */
    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponseDTO> getReviewsByReviewee(Long studentId) {
        logger.debug("Retrieving reviews received by student ID: {}", studentId);

        // Filter active profiles for the student
        List<Long> activeProfileIds = profileRepository.findAllByUserId(studentId).stream()
                .filter(p -> Boolean.TRUE.equals(p.getIsActive()))
                .map(Profile::getId)
                .toList();

        // Early return if no active profiles found
        if (activeProfileIds.isEmpty()) {
            logger.debug("Student ID: {} has no active profiles, returning empty review list", studentId);
            return List.of();
        }

        List<Review> reviews = reviewRepository.findByRevieweeIdInOrderByReviewedAtDesc(activeProfileIds);
        List<ReviewResponseDTO> responseDTOs = reviewMapper.toResponseDTOList(reviews);

        logger.debug("Found {} reviews received by student ID: {}", responseDTOs.size(), studentId);
        return responseDTOs;
    }

    /**
     * Returns reviews for a profile with average rating.
     *
     * @param profileId profile id
     * @return profile reviews response DTO
     */
    @Override
    @Transactional(readOnly = true)
    public ProfileReviewsResponseDTO getReviewsByProfile(Long profileId) {
        logger.debug("Retrieving reviews for profile ID: {}", profileId);

        List<Review> reviews = reviewRepository.findByRevieweeIdOrderByReviewedAtDesc(profileId);
        List<ReviewResponseDTO> responseDTOs = reviewMapper.toResponseDTOList(reviews);
        Double averageRating = reviewRepository.calculateAverageRatingForProfile(profileId);

        logger.debug("Found {} reviews for profile ID: {}, avg: {}", responseDTOs.size(), profileId, averageRating);
        return new ProfileReviewsResponseDTO(profileId, averageRating, responseDTOs.size(), responseDTOs);
    }

    /**
     * Checks whether a review exists for a booking.
     *
     * @param bookingId booking id
     * @return true when a review exists
     */
    @Override
    @Transactional(readOnly = true)
    public boolean existsReviewForBooking(Long bookingId) {
        logger.debug("Checking if review exists for booking ID: {}", bookingId);
        boolean exists = reviewRepository.existsByBookingId(bookingId);
        logger.debug("Review exists for booking ID {}: {}", bookingId, exists);
        return exists;
    }
}
