package it.eably.backend.controller;

import it.eably.backend.dto.review.response.ProfileReviewsResponseDTO;
import it.eably.backend.dto.review.response.ReviewDetailResponseDTO;
import it.eably.backend.dto.review.response.ReviewExistsResponseDTO;
import it.eably.backend.dto.review.request.ReviewRequestDTO;
import it.eably.backend.dto.review.response.ReviewResponseDTO;
import it.eably.backend.exception.AuthorizationException;
import it.eably.backend.exception.ConflictException;
import it.eably.backend.model.User;
import it.eably.backend.service.def.ReviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ReviewController.
 */
@ExtendWith(MockitoExtension.class)
class ReviewControllerTest {

    @Mock
    private ReviewService reviewService;

    @InjectMocks
    private ReviewController reviewController;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("testuser");
    }

    @Test
    void testCreateReview_Success() {
        ReviewRequestDTO requestDTO = new ReviewRequestDTO(1L, 5, "Excellent!");
        ReviewResponseDTO responseDTO = new ReviewResponseDTO(
                1L, 1L, 1L, 2L, 5, "Excellent!", LocalDateTime.now()
        );

        when(reviewService.createReview(eq(1L), any(ReviewRequestDTO.class))).thenReturn(responseDTO);

        ResponseEntity<ReviewResponseDTO> response = reviewController.createReview(requestDTO, mockUser);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().id());
        assertEquals(5, response.getBody().rating());
        assertEquals("Excellent!", response.getBody().comment());

        verify(reviewService).createReview(eq(1L), eq(requestDTO));
    }

    @Test
    void testCreateReview_NotVerified_ThrowsAuthorizationException() {
        ReviewRequestDTO requestDTO = new ReviewRequestDTO(1L, 5, "Great!");

        when(reviewService.createReview(eq(1L), any(ReviewRequestDTO.class)))
                .thenThrow(new AuthorizationException("Only verified users can create reviews"));

        assertThrows(AuthorizationException.class, () ->
                reviewController.createReview(requestDTO, mockUser));
    }

    @Test
    void testCreateReview_BookingNotCompleted_ThrowsConflictException() {
        ReviewRequestDTO requestDTO = new ReviewRequestDTO(1L, 5, "Great!");

        when(reviewService.createReview(eq(1L), any(ReviewRequestDTO.class)))
                .thenThrow(new ConflictException("Can only review completed bookings"));

        assertThrows(ConflictException.class, () ->
                reviewController.createReview(requestDTO, mockUser));
    }

    @Test
    void testCreateReview_DuplicateReview_ThrowsConflictException() {
        ReviewRequestDTO requestDTO = new ReviewRequestDTO(1L, 5, "Great!");

        when(reviewService.createReview(eq(1L), any(ReviewRequestDTO.class)))
                .thenThrow(new ConflictException("Review already exists for this booking"));

        assertThrows(ConflictException.class, () ->
                reviewController.createReview(requestDTO, mockUser));
    }

    @Test
    void testGetReviewsByClient_Success() {
        ReviewResponseDTO review1 = new ReviewResponseDTO(1L, 1L, 1L, 2L, 5, "Excellent!", LocalDateTime.now());
        ReviewResponseDTO review2 = new ReviewResponseDTO(2L, 2L, 1L, 3L, 4, "Good", LocalDateTime.now());

        when(reviewService.getReviewsByReviewer(1L)).thenReturn(List.of(review1, review2));

        ResponseEntity<List<ReviewResponseDTO>> response = reviewController.getReviewsByClient(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        assertEquals(5, response.getBody().get(0).rating());
        assertEquals(4, response.getBody().get(1).rating());

        verify(reviewService).getReviewsByReviewer(1L);
    }

    @Test
    void testGetReviewsByClient_EmptyList() {
        when(reviewService.getReviewsByReviewer(1L)).thenReturn(List.of());

        ResponseEntity<List<ReviewResponseDTO>> response = reviewController.getReviewsByClient(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    void testGetReviewsWithDetailsByClient_Success() {
        ReviewDetailResponseDTO detail1 = new ReviewDetailResponseDTO(
                1L, 5, "Excellent!", LocalDateTime.now(), "student1", "Math Tutor"
        );
        ReviewDetailResponseDTO detail2 = new ReviewDetailResponseDTO(
                2L, 4, "Good", LocalDateTime.now(), "student2", "Physics Tutor"
        );

        when(reviewService.getReviewsWithDetailsByReviewer(1L)).thenReturn(List.of(detail1, detail2));

        ResponseEntity<List<ReviewDetailResponseDTO>> response =
                reviewController.getReviewsWithDetailsByClient(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        assertEquals("student1", response.getBody().get(0).studentUsername());
        assertEquals("Math Tutor", response.getBody().get(0).serviceTitle());

        verify(reviewService).getReviewsWithDetailsByReviewer(1L);
    }

    @Test
    void testGetReviewsWithDetailsByClient_EmptyList() {
        when(reviewService.getReviewsWithDetailsByReviewer(1L)).thenReturn(List.of());

        ResponseEntity<List<ReviewDetailResponseDTO>> response =
                reviewController.getReviewsWithDetailsByClient(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    void testGetReviewsByStudent_Success() {
        ReviewResponseDTO review1 = new ReviewResponseDTO(1L, 1L, 2L, 1L, 5, "Excellent!", LocalDateTime.now());

        when(reviewService.getReviewsByReviewee(1L)).thenReturn(List.of(review1));

        ResponseEntity<List<ReviewResponseDTO>> response = reviewController.getReviewsByStudent(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals(5, response.getBody().get(0).rating());

        verify(reviewService).getReviewsByReviewee(1L);
    }

    @Test
    void testGetReviewsByStudent_EmptyList() {
        when(reviewService.getReviewsByReviewee(1L)).thenReturn(List.of());

        ResponseEntity<List<ReviewResponseDTO>> response = reviewController.getReviewsByStudent(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    void testGetReviewsByProfile_Success() {
        ReviewResponseDTO reviewDTO = new ReviewResponseDTO(1L, 1L, 2L, 1L, 5, "Excellent!", LocalDateTime.now());
        ProfileReviewsResponseDTO responseDTO = new ProfileReviewsResponseDTO(1L, 4.8, 1, List.of(reviewDTO));

        when(reviewService.getReviewsByProfile(1L)).thenReturn(responseDTO);

        ResponseEntity<ProfileReviewsResponseDTO> response = reviewController.getReviewsByProfile(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().profileId());
        assertEquals(4.8, response.getBody().averageRating());
        assertEquals(1, response.getBody().totalCount());
        verify(reviewService).getReviewsByProfile(1L);
    }

    @Test
    void testGetReviewsByProfile_EmptyList() {
        ProfileReviewsResponseDTO responseDTO = new ProfileReviewsResponseDTO(1L, null, 0, List.of());

        when(reviewService.getReviewsByProfile(1L)).thenReturn(responseDTO);

        ResponseEntity<ProfileReviewsResponseDTO> response = reviewController.getReviewsByProfile(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNull(response.getBody().averageRating());
        assertTrue(response.getBody().reviews().isEmpty());
    }

    @Test
    void testCheckReviewExists_True() {
        when(reviewService.existsReviewForBooking(1L)).thenReturn(true);

        ResponseEntity<ReviewExistsResponseDTO> response = reviewController.checkReviewExists(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().exists());

        verify(reviewService).existsReviewForBooking(1L);
    }

    @Test
    void testCheckReviewExists_False() {
        when(reviewService.existsReviewForBooking(1L)).thenReturn(false);

        ResponseEntity<ReviewExistsResponseDTO> response = reviewController.checkReviewExists(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().exists());

        verify(reviewService).existsReviewForBooking(1L);
    }
}
