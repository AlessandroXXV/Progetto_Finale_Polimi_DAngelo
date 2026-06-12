package it.eably.backend.service;

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
import it.eably.backend.service.impl.ReviewServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ReviewService.
 * 
 * Tests cover:
 * - Review creation with comprehensive validation
 * - Review retrieval with ordering
 * - Detailed review queries
 * - Review existence checks
 * - Authorization and business rule validation
 * 
 * Target: 100% line and branch coverage.
 */
@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {
    
    @Mock
    private ReviewRepository reviewRepository;
    
    @Mock
    private BookingRepository bookingRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private ReviewMapper reviewMapper;

    @InjectMocks
    private ReviewServiceImpl reviewService;
    
    private User client;
    private User providerUser;
    private Profile provider;
    private Booking booking;
    private Review review;
    
    @BeforeEach
    void setUp() {
        // Create client
        client = new User();
        client.setId(1L);
        client.setUsername("client");
        client.setEmail("client@test.com");
        client.setPasswordHash("$2a$12$hash");
        client.setRole(UserRole.CLIENT);
        client.setIsActive(true);
        client.setIsVerified(true);
        
        // Create provider user
        providerUser = new User();
        providerUser.setId(2L);
        providerUser.setUsername("provider");
        providerUser.setEmail("provider@test.com");
        providerUser.setPasswordHash("$2a$12$hash");
        providerUser.setRole(UserRole.STUDENT);
        providerUser.setIsActive(true);
        providerUser.setIsVerified(true);
        
        // Create provider profile
        provider = new Profile();
        provider.setId(1L);
        provider.setUser(providerUser);
        provider.setTitle("Math Tutor");
        provider.setHourlyRate(new BigDecimal("50.00"));
        provider.setDeliveryMode(DeliveryMode.ONLINE);
        provider.setIsActive(true);
        
        // Create slot
        AvailabilitySlot slot = new AvailabilitySlot();
        slot.setId(1L);
        slot.setStudent(providerUser);
        slot.setDayOfWeek(DayOfWeek.MONDAY);
        slot.setStartTime(LocalTime.of(10, 0));
        slot.setEndTime(LocalTime.of(11, 0));
        slot.setStatus(SlotStatus.BOOKED);
        
        // Create booking
        booking = new Booking();
        booking.setId(1L);
        booking.setClient(client);
        booking.setProvider(providerUser);
        booking.setProfile(provider);
        booking.setAvailabilitySlot(slot);
        booking.setStatus(BookingStatus.COMPLETED);
        booking.setTotalAmount(new BigDecimal("50.00"));
        booking.setBookedAt(LocalDateTime.now());
        
        // Create review
        review = new Review();
        review.setId(1L);
        review.setBooking(booking);
        review.setReviewer(client);
        review.setReviewee(provider);
        review.setRating(5);
        review.setComment("Excellent service!");
        review.setReviewedAt(LocalDateTime.now());
    }
    
    // ========== CREATE REVIEW TESTS ==========
    
    @Test
    void testCreateReview_Success() {
        ReviewRequestDTO requestDTO = new ReviewRequestDTO(1L, 5, "Excellent!");
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(client));
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(reviewRepository.existsByBookingId(1L)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenReturn(review);
        when(reviewMapper.toResponseDTO(any(Review.class)))
            .thenReturn(new ReviewResponseDTO(1L, 1L, 1L, 1L, 5, "Excellent service!", review.getReviewedAt()));
        
        ReviewResponseDTO result = reviewService.createReview(1L, requestDTO);
        
        assertNotNull(result);
        assertEquals(1L, result.id());
        assertEquals(5, result.rating());
        assertEquals("Excellent service!", result.comment());
        
        verify(reviewRepository).save(any(Review.class));
    }
    
    @Test
    void testCreateReview_ClientNotFound_ThrowsException() {
        ReviewRequestDTO requestDTO = new ReviewRequestDTO(1L, 5, "Great!");
        
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        
        assertThrows(ResourceNotFoundException.class, () -> {
            reviewService.createReview(1L, requestDTO);
        });
        
        verify(reviewRepository, never()).save(any());
    }
    
    @Test
    void testCreateReview_BookingNotFound_ThrowsException() {
        ReviewRequestDTO requestDTO = new ReviewRequestDTO(1L, 5, "Great!");
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(client));
        when(bookingRepository.findById(1L)).thenReturn(Optional.empty());
        
        assertThrows(ResourceNotFoundException.class, () -> {
            reviewService.createReview(1L, requestDTO);
        });
        
        verify(reviewRepository, never()).save(any());
    }
    
    @Test
    void testCreateReview_ClientNotVerified_ThrowsAuthorizationException() {
        ReviewRequestDTO requestDTO = new ReviewRequestDTO(1L, 5, "Great!");
        
        client.setIsVerified(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(client));
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        
        assertThrows(AuthorizationException.class, () -> {
            reviewService.createReview(1L, requestDTO);
        });
        
        verify(reviewRepository, never()).save(any());
    }
    
    @Test
    void testCreateReview_NotBookingOwner_ThrowsAuthorizationException() {
        ReviewRequestDTO requestDTO = new ReviewRequestDTO(1L, 5, "Great!");
        
        when(userRepository.findById(999L)).thenReturn(Optional.of(client));
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        
        assertThrows(AuthorizationException.class, () -> {
            reviewService.createReview(999L, requestDTO);
        });
        
        verify(reviewRepository, never()).save(any());
    }
    
    @Test
    void testCreateReview_BookingNotCompleted_ThrowsConflictException() {
        ReviewRequestDTO requestDTO = new ReviewRequestDTO(1L, 5, "Great!");
        
        booking.setStatus(BookingStatus.CONFIRMED);
        when(userRepository.findById(1L)).thenReturn(Optional.of(client));
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        
        assertThrows(ConflictException.class, () -> {
            reviewService.createReview(1L, requestDTO);
        });
        
        verify(reviewRepository, never()).save(any());
    }
    
    @Test
    void testCreateReview_DuplicateReview_ThrowsConflictException() {
        ReviewRequestDTO requestDTO = new ReviewRequestDTO(1L, 5, "Great!");
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(client));
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(reviewRepository.existsByBookingId(1L)).thenReturn(true);
        
        assertThrows(ConflictException.class, () -> {
            reviewService.createReview(1L, requestDTO);
        });
        
        verify(reviewRepository, never()).save(any());
    }
    
    @Test
    void testCreateReview_RatingTooLow_ThrowsValidationException() {
        ReviewRequestDTO requestDTO = new ReviewRequestDTO(1L, 0, "Bad");
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(client));
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(reviewRepository.existsByBookingId(1L)).thenReturn(false);
        
        assertThrows(ValidationException.class, () -> {
            reviewService.createReview(1L, requestDTO);
        });
        
        verify(reviewRepository, never()).save(any());
    }
    
    @Test
    void testCreateReview_RatingTooHigh_ThrowsValidationException() {
        ReviewRequestDTO requestDTO = new ReviewRequestDTO(1L, 6, "Great");
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(client));
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(reviewRepository.existsByBookingId(1L)).thenReturn(false);
        
        assertThrows(ValidationException.class, () -> {
            reviewService.createReview(1L, requestDTO);
        });
        
        verify(reviewRepository, never()).save(any());
    }
    
    @Test
    void testCreateReview_NullComment_Success() {
        ReviewRequestDTO requestDTO = new ReviewRequestDTO(1L, 5, null);

        Review reviewWithoutComment = new Review();
        reviewWithoutComment.setId(1L);
        reviewWithoutComment.setBooking(booking);
        reviewWithoutComment.setReviewer(client);
        reviewWithoutComment.setReviewee(provider);
        reviewWithoutComment.setRating(5);
        reviewWithoutComment.setComment("");
        reviewWithoutComment.setReviewedAt(LocalDateTime.now());

        when(userRepository.findById(1L)).thenReturn(Optional.of(client));
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(reviewRepository.existsByBookingId(1L)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenReturn(reviewWithoutComment);
        when(reviewMapper.toResponseDTO(any(Review.class)))
            .thenReturn(new ReviewResponseDTO(1L, 1L, 1L, 1L, 5, "", reviewWithoutComment.getReviewedAt()));

        ReviewResponseDTO result = reviewService.createReview(1L, requestDTO);

        assertNotNull(result);
        assertEquals("", result.comment());

        verify(reviewRepository).save(argThat(r -> "".equals(r.getComment())));
    }

    @Test
    void testCreateReview_BlankComment_Success() {
        ReviewRequestDTO requestDTO = new ReviewRequestDTO(1L, 5, "   ");

        when(userRepository.findById(1L)).thenReturn(Optional.of(client));
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(reviewRepository.existsByBookingId(1L)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenReturn(review);
        when(reviewMapper.toResponseDTO(any(Review.class)))
            .thenReturn(new ReviewResponseDTO(1L, 1L, 1L, 1L, 5, "", review.getReviewedAt()));

        ReviewResponseDTO result = reviewService.createReview(1L, requestDTO);

        assertNotNull(result);
        verify(reviewRepository).save(argThat(r -> "".equals(r.getComment())));
    }

    @Test
    void testCreateReview_CommentTooShort_ThrowsValidationException() {
        ReviewRequestDTO requestDTO = new ReviewRequestDTO(1L, 5, "Too short");

        when(userRepository.findById(1L)).thenReturn(Optional.of(client));
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(reviewRepository.existsByBookingId(1L)).thenReturn(false);

        ValidationException ex = assertThrows(ValidationException.class, () -> {
            reviewService.createReview(1L, requestDTO);
        });

        assertEquals("Comment must be at least 10 characters when provided", ex.getMessage());
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    void testCreateReview_CommentAtMinLength_Success() {
        ReviewRequestDTO requestDTO = new ReviewRequestDTO(1L, 5, "1234567890");

        when(userRepository.findById(1L)).thenReturn(Optional.of(client));
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(reviewRepository.existsByBookingId(1L)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenReturn(review);
        when(reviewMapper.toResponseDTO(any(Review.class)))
            .thenReturn(new ReviewResponseDTO(1L, 1L, 1L, 1L, 5, "1234567890", review.getReviewedAt()));

        ReviewResponseDTO result = reviewService.createReview(1L, requestDTO);

        assertNotNull(result);
        verify(reviewRepository).save(argThat(r -> "1234567890".equals(r.getComment())));
    }
    
    // ========== GET REVIEWS BY REVIEWER TESTS ==========
    
    @Test
    void testGetReviewsByReviewer_Success() {
        when(reviewRepository.findByReviewerIdOrderByReviewedAtDesc(1L)).thenReturn(List.of(review));
        when(reviewMapper.toResponseDTOList(anyList()))
            .thenReturn(List.of(new ReviewResponseDTO(1L, 1L, 1L, 1L, 5, "Excellent service!", review.getReviewedAt())));
        
        List<ReviewResponseDTO> result = reviewService.getReviewsByReviewer(1L);
        
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).id());
        assertEquals(5, result.get(0).rating());
    }
    
    @Test
    void testGetReviewsByReviewer_EmptyList() {
        when(reviewRepository.findByReviewerIdOrderByReviewedAtDesc(1L)).thenReturn(List.of());
        when(reviewMapper.toResponseDTOList(anyList())).thenReturn(List.of());
        
        List<ReviewResponseDTO> result = reviewService.getReviewsByReviewer(1L);
        
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
    
    @Test
    void testGetReviewsByReviewer_MultipleReviews() {
        Review review2 = new Review();
        review2.setId(2L);
        review2.setBooking(booking);
        review2.setReviewer(client);
        review2.setReviewee(provider);
        review2.setRating(4);
        review2.setComment("Good");
        review2.setReviewedAt(LocalDateTime.now());
        
        when(reviewRepository.findByReviewerIdOrderByReviewedAtDesc(1L)).thenReturn(List.of(review, review2));
        when(reviewMapper.toResponseDTOList(anyList()))
            .thenReturn(List.of(
                new ReviewResponseDTO(1L, 1L, 1L, 1L, 5, "Excellent service!", review.getReviewedAt()),
                new ReviewResponseDTO(2L, 1L, 1L, 1L, 4, "Good", review2.getReviewedAt())
            ));
        
        List<ReviewResponseDTO> result = reviewService.getReviewsByReviewer(1L);
        
        assertNotNull(result);
        assertEquals(2, result.size());
    }
    
    // ========== GET REVIEWS WITH DETAILS BY REVIEWER TESTS ==========
    
    @Test
    void testGetReviewsWithDetailsByReviewer_Success() {
        when(reviewRepository.findDetailedReviewsByReviewerId(1L)).thenReturn(List.of(review));
        when(reviewMapper.toDetailResponseDTOList(anyList()))
            .thenReturn(List.of(new ReviewDetailResponseDTO(1L, 5, "Excellent service!", review.getReviewedAt(), "provider", "Math Tutor")));
        
        List<ReviewDetailResponseDTO> result = reviewService.getReviewsWithDetailsByReviewer(1L);
        
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).id());
        assertEquals("provider", result.get(0).studentUsername());
        assertEquals("Math Tutor", result.get(0).serviceTitle());
    }
    
    @Test
    void testGetReviewsWithDetailsByReviewer_EmptyList() {
        when(reviewRepository.findDetailedReviewsByReviewerId(1L)).thenReturn(List.of());
        when(reviewMapper.toDetailResponseDTOList(anyList())).thenReturn(List.of());
        
        List<ReviewDetailResponseDTO> result = reviewService.getReviewsWithDetailsByReviewer(1L);
        
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
    
    // ========== GET REVIEWS BY REVIEWEE TESTS ==========
    
    @Test
    void testGetReviewsByReviewee_Success() {
        when(profileRepository.findAllByUserId(2L)).thenReturn(List.of(provider));
        when(reviewRepository.findByRevieweeIdInOrderByReviewedAtDesc(List.of(1L))).thenReturn(List.of(review));
        when(reviewMapper.toResponseDTOList(anyList()))
            .thenReturn(List.of(new ReviewResponseDTO(1L, 1L, 1L, 1L, 5, "Excellent service!", review.getReviewedAt())));

        List<ReviewResponseDTO> result = reviewService.getReviewsByReviewee(2L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).id());
    }
    
    @Test
    void testGetReviewsByReviewee_NoProfile_ReturnsEmptyList() {
        when(profileRepository.findAllByUserId(2L)).thenReturn(List.of());

        List<ReviewResponseDTO> result = reviewService.getReviewsByReviewee(2L);
        
        assertNotNull(result);
        assertTrue(result.isEmpty());
        
        verify(reviewRepository, never()).findByRevieweeIdOrderByReviewedAtDesc(anyLong());
    }
    
    @Test
    void testGetReviewsByReviewee_EmptyList() {
        when(profileRepository.findAllByUserId(2L)).thenReturn(List.of(provider));
        when(reviewRepository.findByRevieweeIdInOrderByReviewedAtDesc(List.of(1L))).thenReturn(List.of());
        when(reviewMapper.toResponseDTOList(anyList())).thenReturn(List.of());

        List<ReviewResponseDTO> result = reviewService.getReviewsByReviewee(2L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
    
    // ========== GET REVIEWS BY PROFILE TESTS ==========

    @Test
    void testGetReviewsByProfile_Success() {
        ReviewResponseDTO reviewDTO = new ReviewResponseDTO(1L, 1L, 1L, 1L, 5, "Excellent service!", review.getReviewedAt());
        when(reviewRepository.findByRevieweeIdOrderByReviewedAtDesc(1L)).thenReturn(List.of(review));
        when(reviewMapper.toResponseDTOList(anyList())).thenReturn(List.of(reviewDTO));
        when(reviewRepository.calculateAverageRatingForProfile(1L)).thenReturn(5.0);

        ProfileReviewsResponseDTO result = reviewService.getReviewsByProfile(1L);

        assertNotNull(result);
        assertEquals(1L, result.profileId());
        assertEquals(5.0, result.averageRating());
        assertEquals(1, result.totalCount());
        assertEquals(1, result.reviews().size());
    }

    @Test
    void testGetReviewsByProfile_EmptyList() {
        when(reviewRepository.findByRevieweeIdOrderByReviewedAtDesc(1L)).thenReturn(List.of());
        when(reviewMapper.toResponseDTOList(anyList())).thenReturn(List.of());
        when(reviewRepository.calculateAverageRatingForProfile(1L)).thenReturn(null);

        ProfileReviewsResponseDTO result = reviewService.getReviewsByProfile(1L);

        assertNotNull(result);
        assertEquals(1L, result.profileId());
        assertNull(result.averageRating());
        assertEquals(0, result.totalCount());
        assertTrue(result.reviews().isEmpty());
    }

    // ========== EXISTS REVIEW FOR BOOKING TESTS ==========
    
    @Test
    void testExistsReviewForBooking_True() {
        when(reviewRepository.existsByBookingId(1L)).thenReturn(true);
        
        boolean result = reviewService.existsReviewForBooking(1L);
        
        assertTrue(result);
    }
    
    @Test
    void testExistsReviewForBooking_False() {
        when(reviewRepository.existsByBookingId(1L)).thenReturn(false);
        
        boolean result = reviewService.existsReviewForBooking(1L);
        
        assertFalse(result);
    }
}
