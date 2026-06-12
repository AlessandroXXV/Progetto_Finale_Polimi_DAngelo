package it.eably.backend.mapper;

import it.eably.backend.dto.review.response.ReviewDetailResponseDTO;
import it.eably.backend.dto.review.response.ReviewResponseDTO;
import it.eably.backend.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReviewMapperTest {

    private ReviewMapper mapper;
    private Review review;
    private Booking booking;
    private User reviewer;
    private Profile reviewee;
    private User revieweeUser;

    @BeforeEach
    void setUp() {
        mapper = new ReviewMapperImpl();

        reviewer = new User();
        reviewer.setId(1L);
        reviewer.setUsername("reviewer_user");

        revieweeUser = new User();
        revieweeUser.setId(2L);
        revieweeUser.setUsername("student_user");

        reviewee = new Profile();
        reviewee.setId(5L);
        reviewee.setUser(revieweeUser);
        reviewee.setTitle("Math Tutor");
        reviewee.setHourlyRate(new BigDecimal("50.00"));
        reviewee.setDeliveryMode(DeliveryMode.ONLINE);
        reviewee.setIsActive(true);

        booking = new Booking();
        booking.setId(100L);

        review = new Review();
        review.setId(10L);
        review.setBooking(booking);
        review.setReviewer(reviewer);
        review.setReviewee(reviewee);
        review.setRating(5);
        review.setComment("Excellent session!");
        review.setReviewedAt(LocalDateTime.of(2024, 3, 15, 12, 0));
    }

    // toResponseDTO tests

    @Test
    void toResponseDTO_NullReview_ReturnsNull() {
        assertNull(mapper.toResponseDTO(null));
    }

    @Test
    void toResponseDTO_ReviewWithNoComment_CommentIsNull() {
        review.setComment(null);

        ReviewResponseDTO dto = mapper.toResponseDTO(review);

        assertNull(dto.comment());
    }

    @Test
    void toResponseDTO_ReviewWithBlankComment_CommentIsNull() {
        review.setComment("   ");

        ReviewResponseDTO dto = mapper.toResponseDTO(review);

        // hasComment() returns false for blank string, so comment is not copied
        assertNull(dto.comment());
    }

    @Test
    void toResponseDTO_NullBooking_BookingIdIsNull() {
        review.setBooking(null);

        ReviewResponseDTO dto = mapper.toResponseDTO(review);

        assertNull(dto.bookingId());
    }

    @Test
    void toResponseDTO_NullReviewer_ReviewerIdIsNull() {
        review.setReviewer(null);

        ReviewResponseDTO dto = mapper.toResponseDTO(review);

        assertNull(dto.reviewerId());
    }

    @Test
    void toResponseDTO_NullReviewee_RevieweeIdIsNull() {
        review.setReviewee(null);

        ReviewResponseDTO dto = mapper.toResponseDTO(review);

        assertNull(dto.revieweeId());
    }

    // toDetailResponseDTO tests

    @Test
    void toDetailResponseDTO_NullReview_ReturnsNull() {
        assertNull(mapper.toDetailResponseDTO(null));
    }

    @Test
    void toDetailResponseDTO_NullReviewee_StudentUsernameAndTitleNull() {
        review.setReviewee(null);

        ReviewDetailResponseDTO dto = mapper.toDetailResponseDTO(review);

        assertNull(dto.studentUsername());
        assertNull(dto.serviceTitle());
    }

    @Test
    void toDetailResponseDTO_NullRevieweeUser_StudentUsernameNull() {
        reviewee.setUser(null);

        ReviewDetailResponseDTO dto = mapper.toDetailResponseDTO(review);

        assertNull(dto.studentUsername());
        assertEquals("Math Tutor", dto.serviceTitle());
    }

    @Test
    void toDetailResponseDTO_ReviewWithNoComment_CommentIsNull() {
        review.setComment(null);

        ReviewDetailResponseDTO dto = mapper.toDetailResponseDTO(review);

        assertNull(dto.comment());
    }

    // toResponseDTOList tests

    @Test
    void toResponseDTOList_NullList_ReturnsNull() {
        assertNull(mapper.toResponseDTOList(null));
    }

    @Test
    void toResponseDTOList_WithReviews_MapsList() {
        Review review2 = new Review();
        review2.setId(20L);
        review2.setRating(3);

        List<ReviewResponseDTO> result = mapper.toResponseDTOList(Arrays.asList(review, review2));

        assertEquals(2, result.size());
        assertEquals(10L, result.get(0).id());
        assertEquals(20L, result.get(1).id());
    }

    // toDetailResponseDTOList tests

    @Test
    void toDetailResponseDTOList_NullList_ReturnsNull() {
        assertNull(mapper.toDetailResponseDTOList(null));
    }

    @Test
    void toDetailResponseDTOList_WithReviews_MapsList() {
        Review review2 = new Review();
        review2.setId(20L);
        review2.setRating(4);

        List<ReviewDetailResponseDTO> result = mapper.toDetailResponseDTOList(Arrays.asList(review, review2));

        assertEquals(2, result.size());
        assertEquals(10L, result.get(0).id());
        assertEquals(20L, result.get(1).id());
    }
}
