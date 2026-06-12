package it.eably.backend.model;

import it.eably.backend.exception.ValidationException;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Review entity representing feedback on completed bookings.
 * 
 * This entity:
 * - Extends BaseEntity for common fields
 * - Has M:1 relationship with Booking
 * - Has M:1 relationship with User (reviewer)
 * - Has M:1 relationship with Profile (reviewee)
 * - Contains rating and comment
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */
@Entity
@Table(name = "reviews", indexes = {
    @Index(name = "idx_review_booking_id", columnList = "booking_id"),
    @Index(name = "idx_review_reviewer_id", columnList = "reviewer_id"),
    @Index(name = "idx_review_reviewee_id", columnList = "reviewee_id"),
    @Index(name = "idx_review_rating", columnList = "rating")
})
public class Review extends BaseEntity {
    
    /**
     * One-to-one relationship with Booking entity.
     * Each booking can have at most one review.
     * Lazy loading to avoid N+1 query problem.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;
    
    /**
     * Many-to-one relationship with User entity (person writing the review).
     * Lazy loading for performance.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id", nullable = false)
    private User reviewer;
    
    /**
     * Many-to-one relationship with Profile entity (person being reviewed).
     * Lazy loading for performance.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewee_id", nullable = false)
    private Profile reviewee;
    
    /**
     * Rating value (1-5 stars).
     * Must be between 1 and 5 inclusive.
     */
    @Column(name = "rating", nullable = false)
    private Integer rating;
    
    /**
     * Optional comment text.
     */
    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;
    
    /**
     * Timestamp when the review was created.
     */
    @Column(name = "reviewed_at", nullable = false)
    private LocalDateTime reviewedAt;
    
    // Constructors
    
    public Review() {
    }
    
    public Review(Booking booking, User reviewer, Profile reviewee, Integer rating, 
                  String comment, LocalDateTime reviewedAt) {
        this.booking = booking;
        this.reviewer = reviewer;
        this.reviewee = reviewee;
        this.rating = rating;
        this.comment = comment;
        this.reviewedAt = reviewedAt;
    }
    
    // Getters and Setters
    
    public Booking getBooking() {
        return booking;
    }
    
    public void setBooking(Booking booking) {
        this.booking = booking;
    }
    
    public User getReviewer() {
        return reviewer;
    }
    
    public void setReviewer(User reviewer) {
        this.reviewer = reviewer;
    }
    
    public Profile getReviewee() {
        return reviewee;
    }
    
    public void setReviewee(Profile reviewee) {
        this.reviewee = reviewee;
    }
    
    public Integer getRating() {
        return rating;
    }
    
    public void setRating(Integer rating) {
        this.rating = rating;
    }
    
    public String getComment() {
        return comment;
    }
    
    public void setComment(String comment) {
        this.comment = comment;
    }
    
    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }
    
    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }
    
    /**
     * Validates the Review entity according to business rules.
     * 
     * Validation Rules:
     * - Booking must not be null
     * - Reviewer must not be null
     * - Reviewee must not be null
     * - Rating must be between 1 and 5 inclusive
     * - Reviewed at timestamp must not be null
     * 
     * @throws ValidationException if validation fails
     */
    @Override
    public void validate() {
        if (booking == null) {
            throw new ValidationException("Review must be associated with a booking");
        }
        
        if (reviewer == null) {
            throw new ValidationException("Review must have a reviewer");
        }
        
        if (reviewee == null) {
            throw new ValidationException("Review must have a reviewee");
        }
        
        if (rating == null || rating < 1 || rating > 5) {
            throw new ValidationException("Rating must be between 1 and 5");
        }
        
        if (reviewedAt == null) {
            throw new ValidationException("Reviewed at timestamp cannot be null");
        }
    }
    
    /**
     * Checks if the review has a comment.
     * 
     * @return true if comment is present
     */
    public boolean hasComment() {
        return comment != null && !comment.trim().isEmpty();
    }
}
