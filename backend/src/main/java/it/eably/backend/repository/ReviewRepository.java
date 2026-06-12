package it.eably.backend.repository;

import it.eably.backend.model.Review;
import it.eably.backend.model.Booking;
import it.eably.backend.model.Profile;
import it.eably.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Review entity.
 * 
 * Provides CRUD operations and custom queries for review management.
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */
@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    
    /**
     * Finds review by booking ID.
     * 
     * @param bookingId the booking ID
     * @return Optional containing the review if found
     */
    Optional<Review> findByBookingId(Long bookingId);
    
    /**
     * Finds review by booking.
     * 
     * @param booking the booking entity
     * @return Optional containing the review if found
     */
    Optional<Review> findByBooking(Booking booking);
    
    /**
     * Finds all reviews for a reviewee (profile being reviewed).
     * 
     * @param revieweeId the reviewee profile ID
     * @return list of reviews
     */
    List<Review> findByRevieweeId(Long revieweeId);
    
    /**
     * Finds all reviews written by a reviewer.
     * 
     * @param reviewerId the reviewer user ID
     * @return list of reviews
     */
    List<Review> findByReviewerId(Long reviewerId);
    
    /**
     * Finds reviews by reviewee profile.
     * 
     * @param reviewee the reviewee profile
     * @return list of reviews
     */
    List<Review> findByReviewee(Profile reviewee);
    
    /**
     * Finds reviews by reviewer user.
     * 
     * @param reviewer the reviewer user
     * @return list of reviews
     */
    List<Review> findByReviewer(User reviewer);
    
    /**
     * Calculates average rating for a profile.
     * 
     * @param revieweeId the reviewee profile ID
     * @return average rating or null if no reviews
     */
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.reviewee.id = :revieweeId")
    Double calculateAverageRatingForProfile(@Param("revieweeId") Long revieweeId);
    
    /**
     * Counts reviews for a profile.
     * 
     * @param revieweeId the reviewee profile ID
     * @return number of reviews
     */
    long countByRevieweeId(Long revieweeId);
    
    /**
     * Finds reviews by rating for a profile.
     * 
     * @param revieweeId the reviewee profile ID
     * @param rating the rating value
     * @return list of reviews with the specified rating
     */
    List<Review> findByRevieweeIdAndRating(Long revieweeId, Integer rating);
    
    /**
     * Checks if a review exists for a booking.
     * 
     * @param bookingId the booking ID
     * @return true if review exists
     */
    boolean existsByBookingId(Long bookingId);
    
    /**
     * Finds all reviews written by a reviewer, ordered by most recent first.
     * 
     * @param reviewerId the reviewer user ID
     * @return list of reviews ordered by reviewedAt descending
     */
    List<Review> findByReviewerIdOrderByReviewedAtDesc(Long reviewerId);
    
    /**
     * Finds all reviews for a reviewee (student), ordered by most recent first.
     *
     * @param revieweeId the reviewee profile ID
     * @return list of reviews ordered by reviewedAt descending
     */
    List<Review> findByRevieweeIdOrderByReviewedAtDesc(Long revieweeId);

    /**
     * Finds all reviews for a set of reviewee profiles, ordered by most recent first.
     * Used to aggregate reviews across all active profiles of a student.
     *
     * @param revieweeIds list of profile IDs
     * @return list of reviews ordered by reviewedAt descending
     */
    List<Review> findByRevieweeIdInOrderByReviewedAtDesc(List<Long> revieweeIds);
    
    /**
     * Finds detailed reviews written by a reviewer with student and service information.
     * Uses JOIN to fetch related data in a single query.
     * 
     * @param reviewerId the reviewer user ID
     * @return list of reviews with student username and service name
     */
    @Query("SELECT r FROM Review r " +
           "JOIN FETCH r.booking b " +
           "JOIN FETCH r.reviewee p " +
           "JOIN FETCH p.user u " +
           "WHERE r.reviewer.id = :reviewerId " +
           "ORDER BY r.reviewedAt DESC")
    List<Review> findDetailedReviewsByReviewerId(@Param("reviewerId") Long reviewerId);
}
