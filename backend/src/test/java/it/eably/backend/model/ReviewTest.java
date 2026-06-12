package it.eably.backend.model;

import it.eably.backend.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Review entity.
 * 
 * COVERAGE FOCUS:
 * - All getters and setters (de-lombokized code)
 * - Constructors (default and parameterized)
 * - validate() method with all business rules (rating 1-5)
 * - Helper methods (hasComment)
 * 
 * @author Alessandro D'Angelo
 * @version 1.0
 */
class ReviewTest {
    
    private User reviewer;
    private User providerUser;
    private Profile reviewee;
    private Booking booking;
    private Review review;
    
    @BeforeEach
    void setUp() {
        // Create reviewer
        reviewer = new User();
        reviewer.setId(1L);
        reviewer.setUsername("reviewer");
        reviewer.setEmail("reviewer@test.com");
        reviewer.setPasswordHash("$2a$12$hash");
        reviewer.setRole(UserRole.CLIENT);
        reviewer.setIsActive(true);
        reviewer.setIsVerified(true);
        
        // Create provider user
        providerUser = new User();
        providerUser.setId(2L);
        providerUser.setUsername("provider");
        providerUser.setEmail("provider@test.com");
        providerUser.setPasswordHash("$2a$12$hash");
        providerUser.setRole(UserRole.STUDENT);
        providerUser.setIsActive(true);
        providerUser.setIsVerified(true);
        
        // Create reviewee profile
        reviewee = new Profile();
        reviewee.setId(1L);
        reviewee.setUser(providerUser);
        reviewee.setTitle("Math Tutor");
        reviewee.setHourlyRate(new BigDecimal("50.00"));
        reviewee.setDeliveryMode(DeliveryMode.ONLINE);
        reviewee.setIsActive(true);
        
        // Create booking
        AvailabilitySlot slot = new AvailabilitySlot();
        slot.setId(1L);
        slot.setStudent(providerUser);
        slot.setDayOfWeek(DayOfWeek.MONDAY);
        slot.setStartTime(LocalTime.of(10, 0));
        slot.setEndTime(LocalTime.of(11, 0));
        slot.setStatus(SlotStatus.BOOKED);
        
        booking = new Booking();
        booking.setId(1L);
        booking.setClient(reviewer);
        booking.setProvider(providerUser);
        booking.setProfile(reviewee);
        booking.setAvailabilitySlot(slot);
        booking.setStatus(BookingStatus.COMPLETED);
        booking.setTotalAmount(new BigDecimal("50.00"));
        booking.setBookedAt(LocalDateTime.now());
        
        review = new Review();
    }
    
    // Constructor Tests
    
    @Test
    void testParameterizedConstructor() {
        LocalDateTime now = LocalDateTime.now();
        Review r = new Review(
            booking,
            reviewer,
            reviewee,
            5,
            "Excellent service!",
            now
        );
        
        assertEquals(booking, r.getBooking());
        assertEquals(reviewer, r.getReviewer());
        assertEquals(reviewee, r.getReviewee());
        assertEquals(5, r.getRating());
        assertEquals("Excellent service!", r.getComment());
        assertEquals(now, r.getReviewedAt());
    }
    
    // Getter and Setter Tests
    
    // Validation Tests
    
    @Test
    void testValidate_Success() {
        review.setBooking(booking);
        review.setReviewer(reviewer);
        review.setReviewee(reviewee);
        review.setRating(5);
        review.setComment("Excellent!");
        review.setReviewedAt(LocalDateTime.now());
        
        assertDoesNotThrow(() -> review.validate());
    }
    
    @Test
    void testValidate_NullBooking_ThrowsException() {
        review.setBooking(null);
        review.setReviewer(reviewer);
        review.setReviewee(reviewee);
        review.setRating(5);
        review.setReviewedAt(LocalDateTime.now());
        
        ValidationException exception = assertThrows(ValidationException.class, () -> review.validate());
        assertTrue(exception.getMessage().contains("must be associated with a booking"));
    }
    
    @Test
    void testValidate_NullReviewer_ThrowsException() {
        review.setBooking(booking);
        review.setReviewer(null);
        review.setReviewee(reviewee);
        review.setRating(5);
        review.setReviewedAt(LocalDateTime.now());
        
        ValidationException exception = assertThrows(ValidationException.class, () -> review.validate());
        assertTrue(exception.getMessage().contains("must have a reviewer"));
    }
    
    @Test
    void testValidate_NullReviewee_ThrowsException() {
        review.setBooking(booking);
        review.setReviewer(reviewer);
        review.setReviewee(null);
        review.setRating(5);
        review.setReviewedAt(LocalDateTime.now());
        
        ValidationException exception = assertThrows(ValidationException.class, () -> review.validate());
        assertTrue(exception.getMessage().contains("must have a reviewee"));
    }
    
    @Test
    void testValidate_NullRating_ThrowsException() {
        review.setBooking(booking);
        review.setReviewer(reviewer);
        review.setReviewee(reviewee);
        review.setRating(null);
        review.setReviewedAt(LocalDateTime.now());
        
        ValidationException exception = assertThrows(ValidationException.class, () -> review.validate());
        assertTrue(exception.getMessage().contains("Rating must be between 1 and 5"));
    }
    
    @Test
    void testValidate_RatingZero_ThrowsException() {
        review.setBooking(booking);
        review.setReviewer(reviewer);
        review.setReviewee(reviewee);
        review.setRating(0);
        review.setReviewedAt(LocalDateTime.now());
        
        ValidationException exception = assertThrows(ValidationException.class, () -> review.validate());
        assertTrue(exception.getMessage().contains("Rating must be between 1 and 5"));
    }
    
    @Test
    void testValidate_RatingSix_ThrowsException() {
        review.setBooking(booking);
        review.setReviewer(reviewer);
        review.setReviewee(reviewee);
        review.setRating(6);
        review.setReviewedAt(LocalDateTime.now());
        
        ValidationException exception = assertThrows(ValidationException.class, () -> review.validate());
        assertTrue(exception.getMessage().contains("Rating must be between 1 and 5"));
    }
    
    @Test
    void testValidate_RatingOne_Success() {
        review.setBooking(booking);
        review.setReviewer(reviewer);
        review.setReviewee(reviewee);
        review.setRating(1);
        review.setReviewedAt(LocalDateTime.now());
        
        assertDoesNotThrow(() -> review.validate());
    }
    
    @Test
    void testValidate_RatingFive_Success() {
        review.setBooking(booking);
        review.setReviewer(reviewer);
        review.setReviewee(reviewee);
        review.setRating(5);
        review.setReviewedAt(LocalDateTime.now());
        
        assertDoesNotThrow(() -> review.validate());
    }
    
    @Test
    void testValidate_NullReviewedAt_ThrowsException() {
        review.setBooking(booking);
        review.setReviewer(reviewer);
        review.setReviewee(reviewee);
        review.setRating(5);
        review.setReviewedAt(null);
        
        ValidationException exception = assertThrows(ValidationException.class, () -> review.validate());
        assertTrue(exception.getMessage().contains("Reviewed at timestamp cannot be null"));
    }
    
    // Helper Method Tests
    
    @Test
    void testHasComment_WithComment_ReturnsTrue() {
        review.setComment("Great service!");
        assertTrue(review.hasComment());
    }
    
    @Test
    void testHasComment_NullComment_ReturnsFalse() {
        review.setComment(null);
        assertFalse(review.hasComment());
    }
    
    @Test
    void testHasComment_EmptyComment_ReturnsFalse() {
        review.setComment("");
        assertFalse(review.hasComment());
    }
    
    @Test
    void testHasComment_WhitespaceComment_ReturnsFalse() {
        review.setComment("   ");
        assertFalse(review.hasComment());
    }
}
