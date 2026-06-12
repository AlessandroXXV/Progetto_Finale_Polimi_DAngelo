package it.eably.backend.service;

import it.eably.backend.exception.ValidationException;
import it.eably.backend.model.*;
import it.eably.backend.repository.AvailabilitySlotRepository;
import it.eably.backend.repository.BookingRepository;
import it.eably.backend.repository.ProfileRepository;
import it.eably.backend.repository.UserRepository;
import it.eably.backend.service.def.PaymentService;
import it.eably.backend.service.impl.BookingServiceImpl;
import it.eably.backend.service.observer.BookingObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BookingServiceImpl.
 *
 * Tests cover:
 * - Booking creation with thread-safety considerations
 * - Booking confirmation flow
 * - Booking completion with confirmation code validation
 * - Booking cancellation with status and authorization checks
 * - Booking retrieval by various criteria
 * - Ownership checks
 */
@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private AvailabilitySlotRepository availabilitySlotRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private PaymentService paymentService;

    @Mock
    private BookingObserver mockObserver;

    private BookingServiceImpl bookingService;

    private User client;
    private User provider;
    private Profile profile;
    private AvailabilitySlot slot;
    private Booking booking;
    private LocalDate validBookingDate;

    @BeforeEach
    void setUp() {
        bookingService = new BookingServiceImpl(
                bookingRepository, availabilitySlotRepository, userRepository,
                profileRepository, paymentService, List.of(mockObserver));

        client = new User();
        client.setId(1L);
        client.setUsername("client");
        client.setEmail("client@test.com");
        client.setPasswordHash("$2a$12$hash");
        client.setRole(UserRole.CLIENT);
        client.setIsActive(true);
        client.setIsVerified(true);

        provider = new User();
        provider.setId(2L);
        provider.setUsername("provider");
        provider.setEmail("provider@test.com");
        provider.setPasswordHash("$2a$12$hash");
        provider.setRole(UserRole.STUDENT);
        provider.setIsActive(true);
        provider.setIsVerified(true);

        profile = new Profile();
        profile.setId(1L);
        profile.setUser(provider);
        profile.setTitle("Math Tutor");
        profile.setHourlyRate(new BigDecimal("60.00"));
        profile.setDeliveryMode(DeliveryMode.ONLINE);
        profile.setIsActive(true);

        slot = new AvailabilitySlot();
        slot.setId(1L);
        slot.setStudent(provider);
        slot.setDayOfWeek(DayOfWeek.MONDAY);
        slot.setStartTime(LocalTime.of(10, 0));
        slot.setEndTime(LocalTime.of(11, 0));
        slot.setStatus(SlotStatus.AVAILABLE);

        validBookingDate = LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));

        booking = new Booking();
        booking.setId(1L);
        booking.setClient(client);
        booking.setProvider(provider);
        booking.setProfile(profile);
        booking.setAvailabilitySlot(slot);
        booking.setStatus(BookingStatus.PAYMENT_PENDING);
        booking.setTotalAmount(new BigDecimal("60.00"));
        booking.setBookedAt(LocalDateTime.now());
    }

    // ========== CREATE BOOKING TESTS ==========

    @Test
    void createBooking_Success() {
        mockCreateBookingPreconditions(validBookingDate);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking toSave = invocation.getArgument(0);
            toSave.setId(10L);
            return toSave;
        });

        Booking result = bookingService.createBooking(1L, 1L, 1L, null, validBookingDate);

        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertEquals(BookingStatus.PAYMENT_PENDING, result.getStatus());
        assertEquals(validBookingDate, result.getBookingDate());
        verify(bookingRepository).save(any(Booking.class));
        verify(availabilitySlotRepository, never()).save(any());
        verify(mockObserver, times(1)).onBookingStatusChanged(any(Booking.class));
    }

    @Test
    void createBooking_NullDate_ThrowsException() {
        ValidationException ex = assertThrows(ValidationException.class, () ->
                bookingService.createBooking(1L, 1L, 1L, null, null));

        assertEquals("Booking date is required", ex.getMessage());
        verifyNoInteractions(availabilitySlotRepository);
    }

    @Test
    void createBooking_PastDate_ThrowsException() {
        LocalDate pastDate = LocalDate.now().minusDays(1);

        ValidationException ex = assertThrows(ValidationException.class, () ->
                bookingService.createBooking(1L, 1L, 1L, null, pastDate));

        assertEquals("Booking date must be today or in the future", ex.getMessage());
        verifyNoInteractions(availabilitySlotRepository);
    }

    @Test
    void createBooking_SlotNotAvailable_ThrowsException() {
        slot.setStatus(SlotStatus.CANCELLED);
        when(availabilitySlotRepository.findByIdWithLock(1L)).thenReturn(Optional.of(slot));

        ValidationException ex = assertThrows(ValidationException.class, () ->
                bookingService.createBooking(1L, 1L, 1L, null, validBookingDate));

        assertTrue(ex.getMessage().contains("not available"));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createBooking_SlotNotFound_ThrowsException() {
        when(availabilitySlotRepository.findByIdWithLock(1L)).thenReturn(Optional.empty());

        assertThrows(ValidationException.class, () ->
            bookingService.createBooking(1L, 1L, 1L, null, validBookingDate));

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createBooking_ClientNotFound_ThrowsException() {
        when(availabilitySlotRepository.findByIdWithLock(1L)).thenReturn(Optional.of(slot));
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ValidationException.class, () ->
            bookingService.createBooking(1L, 1L, 1L, null, validBookingDate));

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createBooking_ClientRoleInvalid_ThrowsException() {
        client.setRole(UserRole.STUDENT);
        when(availabilitySlotRepository.findByIdWithLock(1L)).thenReturn(Optional.of(slot));
        when(userRepository.findById(1L)).thenReturn(Optional.of(client));

        ValidationException ex = assertThrows(ValidationException.class, () ->
            bookingService.createBooking(1L, 1L, 1L, null, validBookingDate));

        assertEquals("Only CLIENT users can create bookings", ex.getMessage());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createBooking_ClientNotVerified_ThrowsException() {
        // Setup scenario: Client is not verified
        client.setIsVerified(false);
        when(availabilitySlotRepository.findByIdWithLock(1L)).thenReturn(Optional.of(slot));
        when(userRepository.findById(1L)).thenReturn(Optional.of(client));

        // Exception and verification failure
        ValidationException ex = assertThrows(ValidationException.class, () ->
            bookingService.createBooking(1L, 1L, 1L, null, validBookingDate));

        // Assert that the exception message indicates the client is not verified
        assertEquals("Your account is not verified. Complete identity verification before creating bookings", ex.getMessage());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createBooking_ProfileNotFound_ThrowsException() {
        when(availabilitySlotRepository.findByIdWithLock(1L)).thenReturn(Optional.of(slot));
        when(userRepository.findById(1L)).thenReturn(Optional.of(client));
        when(profileRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ValidationException.class, () ->
            bookingService.createBooking(1L, 1L, 1L, null, validBookingDate));

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createBooking_ProfileNotBelongToStudent_ThrowsException() {
        User otherProvider = new User();
        otherProvider.setId(99L);
        profile.setUser(otherProvider); // Profile belongs to different student

        when(availabilitySlotRepository.findByIdWithLock(1L)).thenReturn(Optional.of(slot));
        when(userRepository.findById(1L)).thenReturn(Optional.of(client));
        when(profileRepository.findById(1L)).thenReturn(Optional.of(profile));

        assertThrows(ValidationException.class, () ->
            bookingService.createBooking(1L, 1L, 1L, null, validBookingDate));

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createBooking_BookingDateWeekdayMismatch_ThrowsException() {
        when(availabilitySlotRepository.findByIdWithLock(1L)).thenReturn(Optional.of(slot));
        when(userRepository.findById(1L)).thenReturn(Optional.of(client));
        when(profileRepository.findById(1L)).thenReturn(Optional.of(profile));

        ValidationException ex = assertThrows(ValidationException.class, () ->
            bookingService.createBooking(1L, 1L, 1L, null, validBookingDate.plusDays(1)));

        assertEquals("Booking date does not match slot weekday", ex.getMessage());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createBooking_ClientTimedOutOnSameOccurrence_ThrowsException() {
        LocalDate bookingDate = validBookingDate;

        when(availabilitySlotRepository.findByIdWithLock(1L)).thenReturn(Optional.of(slot));
        when(userRepository.findById(1L)).thenReturn(Optional.of(client));
        when(profileRepository.findById(1L)).thenReturn(Optional.of(profile));
        when(bookingRepository.existsByClientIdAndAvailabilitySlotIdAndBookingDateAndStatusAndCancellationReason(
                1L,
                1L,
                bookingDate,
                BookingStatus.CANCELLED,
                BookingCancellationReason.PAYMENT_TIMEOUT
        )).thenReturn(true);

        ValidationException ex = assertThrows(ValidationException.class, () ->
                bookingService.createBooking(1L, 1L, 1L, null, bookingDate));

        assertTrue(ex.getMessage().contains("bloccata in modo permanente"));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createBooking_AlreadyBookedForDate_ThrowsException() {
        mockCreateBookingPreconditions(validBookingDate);
        when(bookingRepository.existsActiveByAvailabilitySlotIdAndBookingDate(1L, validBookingDate))
                .thenReturn(true);

        ValidationException ex = assertThrows(ValidationException.class, () ->
                bookingService.createBooking(1L, 1L, 1L, null, validBookingDate));

        assertEquals("Availability slot is already booked for the selected date", ex.getMessage());
        verify(bookingRepository, never()).save(any());
    }

    // ========== CONFIRM BOOKING TESTS ==========

    @Test
    void confirmBooking_Success() {
        booking.setStatus(BookingStatus.PAYMENT_PENDING);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        Booking result = bookingService.confirmBooking(1L, "pi_testintent", 1L);

        assertNotNull(result);
        assertEquals(BookingStatus.CONFIRMED, result.getStatus());
        assertEquals("pi_testintent", result.getPaymentIntentId());
        assertNotNull(result.getConfirmationCode());
        assertEquals(6, result.getConfirmationCode().length());
        verify(paymentService).validatePaymentIntentForBooking(1L, "pi_testintent");
        verify(bookingRepository).save(any(Booking.class));
        verify(mockObserver, times(1)).onBookingStatusChanged(any(Booking.class));
    }

    @Test
    void confirmBooking_NotFound_ThrowsException() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ValidationException.class, () ->
            bookingService.confirmBooking(1L, "pi_test", 1L));
    }

    @Test
    void confirmBooking_WrongStatus_ThrowsException() {
        booking.setStatus(BookingStatus.CONFIRMED); // Already confirmed
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        assertThrows(ValidationException.class, () ->
            bookingService.confirmBooking(1L, "pi_test", 1L));

        verify(paymentService, never()).validatePaymentIntentForBooking(anyLong(), anyString());
        verify(bookingRepository, never()).save(any());
    }

    // ========== COMPLETE BOOKING TESTS ==========

    @Test
    void completeBooking_Success() {
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setConfirmationCode("123456");
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        Booking result = bookingService.completeBooking(1L, "123456", 2L);

        assertNotNull(result);
        assertEquals(BookingStatus.COMPLETED, result.getStatus());
        verify(bookingRepository).save(any(Booking.class));
        verify(paymentService, never()).capturePayment(anyLong());
        verify(mockObserver, times(1)).onBookingStatusChanged(any(Booking.class));
    }

    @Test
    void completeBooking_WithPaymentIntent_CapturesPayment() {
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setConfirmationCode("123456");
        booking.setPaymentIntentId("pi_123");
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        Booking result = bookingService.completeBooking(1L, "123456", 2L);

        assertEquals(BookingStatus.COMPLETED, result.getStatus());
        verify(paymentService).capturePayment(1L);
    }

    @Test
    void completeBooking_NotFound_ThrowsException() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ValidationException.class, () ->
            bookingService.completeBooking(1L, "123456", 2L));
    }

    @Test
    void completeBooking_WrongStatus_ThrowsException() {
        booking.setStatus(BookingStatus.PAYMENT_PENDING);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        assertThrows(ValidationException.class, () ->
            bookingService.completeBooking(1L, "123456", 2L));

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void completeBooking_WrongConfirmationCode_ThrowsException() {
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setConfirmationCode("123456");
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        assertThrows(ValidationException.class, () ->
            bookingService.completeBooking(1L, "999999", 2L)); // Wrong code

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void completeBooking_NullConfirmationCode_ThrowsException() {
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setConfirmationCode(null); // No code set yet
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        assertThrows(ValidationException.class, () ->
            bookingService.completeBooking(1L, "123456", 2L));

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void completeBooking_ReleasesSlot_AndAllowsRebooking() {
        // Complete a booking, then verify same slot/date can be booked again when repository reports no active bookings.
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setConfirmationCode("123456");

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        Booking completed = bookingService.completeBooking(1L, "123456", 2L);

        assertEquals(BookingStatus.COMPLETED, completed.getStatus());

        // Rebook the same slot after completion
        mockCreateBookingPreconditions(validBookingDate);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking toSave = invocation.getArgument(0);
            toSave.setId(20L);
            return toSave;
        });

        Booking rebooked = bookingService.createBooking(1L, 1L, 1L, "second session", validBookingDate);

        assertNotNull(rebooked);
        assertEquals(20L, rebooked.getId());
        assertEquals(BookingStatus.PAYMENT_PENDING, rebooked.getStatus());
        verify(availabilitySlotRepository, never()).save(any());
    }

    // ========== CANCEL BOOKING TESTS ==========

    @Test
    void cancelBooking_ByClient_Success() {
        booking.setStatus(BookingStatus.CONFIRMED);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        Booking result = bookingService.cancelBooking(1L, 1L); // client ID = 1

        assertNotNull(result);
        assertEquals(BookingStatus.CANCELLED, result.getStatus());
        assertEquals(BookingCancellationReason.USER_CANCELLED, result.getCancellationReason());
        verify(bookingRepository).save(any(Booking.class));
        verify(paymentService, never()).cancelPayment(anyLong());
    }

    @Test
    void cancelBooking_ByProvider_Success() {
        booking.setStatus(BookingStatus.CONFIRMED);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        Booking result = bookingService.cancelBooking(1L, 2L); // provider ID = 2

        assertNotNull(result);
        assertEquals(BookingStatus.CANCELLED, result.getStatus());
    }

    @Test
    void cancelBooking_WithPaymentIntent_CancelsPayment() {
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setPaymentIntentId("pi_test_123");
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        Booking result = bookingService.cancelBooking(1L, 1L);

        assertEquals(BookingStatus.CANCELLED, result.getStatus());
        verify(paymentService).cancelPayment(1L);
        verify(mockObserver, times(1)).onBookingStatusChanged(any(Booking.class));
    }

    @Test
    void cancelBooking_NotFound_ThrowsException() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ValidationException.class, () ->
            bookingService.cancelBooking(1L, 1L));
    }

    @Test
    void cancelBooking_NotAuthorized_ThrowsException() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        // User 999 is neither client (1) nor provider (2)
        assertThrows(ValidationException.class, () ->
            bookingService.cancelBooking(1L, 999L));

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void cancelBooking_FinalState_ThrowsException() {
        booking.setStatus(BookingStatus.COMPLETED); // Final state, cannot cancel
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        assertThrows(ValidationException.class, () ->
            bookingService.cancelBooking(1L, 1L));

        verify(bookingRepository, never()).save(any());
    }

    // ========== GET BOOKING TESTS ==========

    @Test
    void getBookingById_Success() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        Booking result = bookingService.getBookingById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void getBookingById_NotFound_ThrowsException() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ValidationException.class, () ->
            bookingService.getBookingById(1L));
    }

    @Test
    void getBookingsByClient_ReturnsBookings() {
        when(bookingRepository.findByClientId(1L)).thenReturn(List.of(booking));

        List<Booking> result = bookingService.getBookingsByClient(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
    }


    @Test
    void getBookingsByProviderUser_CallsFindByProviderId_WithUserId() {
        // After fix: getBookingsByProviderUser should call findByProviderId(userId) directly
        // NOT iterate through profiles
        when(bookingRepository.findByProviderId(2L)).thenReturn(List.of(booking));

        List<Booking> result = bookingService.getBookingsByProviderUser(2L);

        // Verify that findByProviderId was called with the userId (2L), not a profile ID
        verify(bookingRepository, times(1)).findByProviderId(2L);
        verify(profileRepository, never()).findAllByUserId(any());  // No longer uses profile lookup
        
        assertEquals(1, result.size());
        assertTrue(result.contains(booking));
    }

    @Test
    void getBookingsByStatus_ReturnsBookings() {
        when(bookingRepository.findByStatus(BookingStatus.PAYMENT_PENDING)).thenReturn(List.of(booking));

        List<Booking> result = bookingService.getBookingsByStatus(BookingStatus.PAYMENT_PENDING);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    // ========== IS BOOKING OWNER TESTS ==========

    @Test
    void isBookingOwner_AsClient_ReturnsTrue() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        boolean result = bookingService.isBookingOwner(1L, 1L); // client ID

        assertTrue(result);
    }

    @Test
    void isBookingOwner_AsProvider_ReturnsTrue() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        boolean result = bookingService.isBookingOwner(1L, 2L); // provider ID

        assertTrue(result);
    }

    @Test
    void isBookingOwner_OtherUser_ReturnsFalse() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        boolean result = bookingService.isBookingOwner(1L, 999L);

        assertFalse(result);
    }

    @Test
    void isBookingOwner_NotFound_ThrowsException() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ValidationException.class, () ->
            bookingService.isBookingOwner(1L, 1L));
    }

    // ========== CANCEL DUE TO TIMEOUT TESTS ==========

    @Test
    void cancelBookingDueToTimeout_Success() {
        booking.setStatus(BookingStatus.PAYMENT_PENDING);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        bookingService.cancelBookingDueToTimeout(1L);

        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
        assertEquals(BookingCancellationReason.PAYMENT_TIMEOUT, booking.getCancellationReason());
        verify(bookingRepository).save(booking);
        verify(mockObserver, times(1)).onBookingStatusChanged(booking);
    }

    @Test
    void cancelBookingDueToTimeout_AlreadyCancelled_DoesNothing() {
        booking.setStatus(BookingStatus.CANCELLED);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        bookingService.cancelBookingDueToTimeout(1L);

        verify(bookingRepository, never()).save(any());
        verify(mockObserver, never()).onBookingStatusChanged(any());
    }

    @Test
    void cancelBookingDueToTimeout_NotFound_ThrowsException() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ValidationException.class, () ->
                bookingService.cancelBookingDueToTimeout(1L));
    }

    // ========== TOTAL AMOUNT CALCULATION TESTS ==========

    @Test
    void createBooking_CalculatesTotalAmountCorrectly() {
        // hourlyRate=60, duration=60min → total=60.00
        mockCreateBookingPreconditions(validBookingDate);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            assertEquals(new BigDecimal("60.00"), b.getTotalAmount());
            return b;
        });

        bookingService.createBooking(1L, 1L, 1L, "test notes", validBookingDate);

        verify(bookingRepository).save(any(Booking.class));
    }


    private void mockCreateBookingPreconditions(LocalDate bookingDate) {
        when(availabilitySlotRepository.findByIdWithLock(1L)).thenReturn(Optional.of(slot));
        when(userRepository.findById(1L)).thenReturn(Optional.of(client));
        when(profileRepository.findById(1L)).thenReturn(Optional.of(profile));
        when(bookingRepository.existsByClientIdAndAvailabilitySlotIdAndBookingDateAndStatusAndCancellationReason(
                1L,
                1L,
                bookingDate,
                BookingStatus.CANCELLED,
                BookingCancellationReason.PAYMENT_TIMEOUT
        )).thenReturn(false);
        when(bookingRepository.existsActiveByAvailabilitySlotIdAndBookingDate(1L, bookingDate)).thenReturn(false);
    }
}
