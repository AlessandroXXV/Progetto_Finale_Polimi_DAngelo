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
 * Unit tests for Booking entity.
 * 
 * COVERAGE FOCUS:
 * - All getters and setters (de-lombokized code)
 * - Constructors (default and parameterized)
 * - validate() method with all business rules
 * - Helper methods (canBeCancelled, isFinalState, updateStatus)
 * 
 * @author Alessandro D'Angelo
 * @version 1.0
 */
class BookingTest {
    
    private User client;
    private User providerUser;
    private Profile provider;
    private AvailabilitySlot slot;
    private Booking booking;
    
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
        
        // Create availability slot
        slot = new AvailabilitySlot();
        slot.setId(1L);
        slot.setStudent(providerUser);
        slot.setDayOfWeek(DayOfWeek.MONDAY);
        slot.setStartTime(LocalTime.of(10, 0));
        slot.setEndTime(LocalTime.of(11, 0));
        slot.setStatus(SlotStatus.AVAILABLE);
        
        booking = new Booking();
    }
    
    // Constructor Tests
    
    @Test
    void testParameterizedConstructor() {
        LocalDateTime now = LocalDateTime.now();
        Booking b = new Booking(
            client,
            providerUser,
            provider,
            slot,
            BookingStatus.CONFIRMED,
            new BigDecimal("50.00"),
            "Test notes",
            now,
            "123456",
            "pi_test123"
        );
        
        assertEquals(client, b.getClient());
        assertEquals(providerUser, b.getProvider());
        assertEquals(provider, b.getProfile());
        assertEquals(slot, b.getAvailabilitySlot());
        assertEquals(BookingStatus.CONFIRMED, b.getStatus());
        assertEquals(new BigDecimal("50.00"), b.getTotalAmount());
        assertEquals("Test notes", b.getNotes());
        assertEquals(now, b.getBookedAt());
        assertEquals("123456", b.getConfirmationCode());
        assertEquals("pi_test123", b.getPaymentIntentId());
    }
    
    // Getter and Setter Tests
    
    @Test
    void testGetSetReview() {
        Review review = new Review();
        booking.setReview(review);
        assertEquals(review, booking.getReview());
    }
    
    // Validation Tests
    
    @Test
    void testValidate_Success() {
        booking.setClient(client);
        booking.setProvider(providerUser);
        booking.setProfile(provider);
        booking.setAvailabilitySlot(slot);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setTotalAmount(new BigDecimal("50.00"));
        booking.setBookedAt(LocalDateTime.now());
        booking.setConfirmationCode("123456");
        
        assertDoesNotThrow(() -> booking.validate());
    }
    
    @Test
    void testValidate_NullClient_ThrowsException() {
        booking.setClient(null);
        booking.setProvider(providerUser);
        booking.setProfile(provider);
        booking.setAvailabilitySlot(slot);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setTotalAmount(new BigDecimal("50.00"));
        booking.setBookedAt(LocalDateTime.now());
        
        ValidationException exception = assertThrows(ValidationException.class, () -> booking.validate());
        assertTrue(exception.getMessage().contains("must have a client"));
    }
    
    @Test
    void testValidate_NullProvider_ThrowsException() {
        booking.setClient(client);
        booking.setProvider(null);
        booking.setAvailabilitySlot(slot);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setTotalAmount(new BigDecimal("50.00"));
        booking.setBookedAt(LocalDateTime.now());
        
        ValidationException exception = assertThrows(ValidationException.class, () -> booking.validate());
        assertTrue(exception.getMessage().contains("must have a provider"));
    }
    
    @Test
    void testValidate_NullAvailabilitySlot_ThrowsException() {
        booking.setClient(client);
        booking.setProvider(providerUser);
        booking.setProfile(provider);
        booking.setAvailabilitySlot(null);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setTotalAmount(new BigDecimal("50.00"));
        booking.setBookedAt(LocalDateTime.now());
        
        ValidationException exception = assertThrows(ValidationException.class, () -> booking.validate());
        assertTrue(exception.getMessage().contains("must have an availability slot"));
    }
    
    @Test
    void testValidate_NullStatus_ThrowsException() {
        booking.setClient(client);
        booking.setProvider(providerUser);
        booking.setProfile(provider);
        booking.setAvailabilitySlot(slot);
        booking.setStatus(null);
        booking.setTotalAmount(new BigDecimal("50.00"));
        booking.setBookedAt(LocalDateTime.now());
        
        ValidationException exception = assertThrows(ValidationException.class, () -> booking.validate());
        assertTrue(exception.getMessage().contains("status cannot be null"));
    }
    
    @Test
    void testValidate_NullTotalAmount_ThrowsException() {
        booking.setClient(client);
        booking.setProvider(providerUser);
        booking.setProfile(provider);
        booking.setAvailabilitySlot(slot);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setTotalAmount(null);
        booking.setBookedAt(LocalDateTime.now());
        
        ValidationException exception = assertThrows(ValidationException.class, () -> booking.validate());
        assertTrue(exception.getMessage().contains("Total amount must be positive"));
    }
    
    @Test
    void testValidate_ZeroTotalAmount_ThrowsException() {
        booking.setClient(client);
        booking.setProvider(providerUser);
        booking.setProfile(provider);
        booking.setAvailabilitySlot(slot);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setTotalAmount(BigDecimal.ZERO);
        booking.setBookedAt(LocalDateTime.now());
        
        ValidationException exception = assertThrows(ValidationException.class, () -> booking.validate());
        assertTrue(exception.getMessage().contains("Total amount must be positive"));
    }
    
    @Test
    void testValidate_NegativeTotalAmount_ThrowsException() {
        booking.setClient(client);
        booking.setProvider(providerUser);
        booking.setProfile(provider);
        booking.setAvailabilitySlot(slot);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setTotalAmount(new BigDecimal("-10.00"));
        booking.setBookedAt(LocalDateTime.now());
        
        ValidationException exception = assertThrows(ValidationException.class, () -> booking.validate());
        assertTrue(exception.getMessage().contains("Total amount must be positive"));
    }
    
    @Test
    void testValidate_NullBookedAt_ThrowsException() {
        booking.setClient(client);
        booking.setProvider(providerUser);
        booking.setProfile(provider);
        booking.setAvailabilitySlot(slot);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setTotalAmount(new BigDecimal("50.00"));
        booking.setBookedAt(null);
        
        ValidationException exception = assertThrows(ValidationException.class, () -> booking.validate());
        assertTrue(exception.getMessage().contains("Booked at timestamp cannot be null"));
    }
    
    @Test
    void testValidate_InvalidConfirmationCodeLength_ThrowsException() {
        booking.setClient(client);
        booking.setProvider(providerUser);
        booking.setProfile(provider);
        booking.setAvailabilitySlot(slot);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setTotalAmount(new BigDecimal("50.00"));
        booking.setBookedAt(LocalDateTime.now());
        booking.setConfirmationCode("12345"); // Only 5 characters
        
        ValidationException exception = assertThrows(ValidationException.class, () -> booking.validate());
        assertTrue(exception.getMessage().contains("must be exactly 6 characters"));
    }
    
    @Test
    void testValidate_ValidConfirmationCode_Success() {
        booking.setClient(client);
        booking.setProvider(providerUser);
        booking.setProfile(provider);
        booking.setAvailabilitySlot(slot);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setTotalAmount(new BigDecimal("50.00"));
        booking.setBookedAt(LocalDateTime.now());
        booking.setConfirmationCode("123456"); // Exactly 6 characters
        
        assertDoesNotThrow(() -> booking.validate());
    }
    
    @Test
    void testValidate_NullConfirmationCode_Success() {
        booking.setClient(client);
        booking.setProvider(providerUser);
        booking.setProfile(provider);
        booking.setAvailabilitySlot(slot);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setTotalAmount(new BigDecimal("50.00"));
        booking.setBookedAt(LocalDateTime.now());
        booking.setConfirmationCode(null);
        
        assertDoesNotThrow(() -> booking.validate());
    }
    
    // Helper Method Tests
    
    @Test
    void testCanBeCancelled_RequestedRemoved_DefaultsToPaymentPending_ReturnsTrue() {
        // REQUESTED was removed; ensure PAYMENT_PENDING remains cancellable
        booking.setStatus(BookingStatus.PAYMENT_PENDING);
        assertTrue(booking.canBeCancelled());
    }
    
    @Test
    void testCanBeCancelled_PaymentPending_ReturnsTrue() {
        booking.setStatus(BookingStatus.PAYMENT_PENDING);
        assertTrue(booking.canBeCancelled());
    }
    
    @Test
    void testCanBeCancelled_Confirmed_ReturnsTrue() {
        booking.setStatus(BookingStatus.CONFIRMED);
        assertTrue(booking.canBeCancelled());
    }
    
    @Test
    void testCanBeCancelled_Completed_ReturnsFalse() {
        booking.setStatus(BookingStatus.COMPLETED);
        assertFalse(booking.canBeCancelled());
    }
    
    @Test
    void testCanBeCancelled_Cancelled_ReturnsFalse() {
        booking.setStatus(BookingStatus.CANCELLED);
        assertFalse(booking.canBeCancelled());
    }
    
    @Test
    void testIsFinalState_Completed_ReturnsTrue() {
        booking.setStatus(BookingStatus.COMPLETED);
        assertTrue(booking.isFinalState());
    }
    
    @Test
    void testIsFinalState_Cancelled_ReturnsTrue() {
        booking.setStatus(BookingStatus.CANCELLED);
        assertTrue(booking.isFinalState());
    }
    
    @Test
    void testIsFinalState_Confirmed_ReturnsFalse() {
        booking.setStatus(BookingStatus.CONFIRMED);
        assertFalse(booking.isFinalState());
    }
    
    @Test
    void testUpdateStatus() {
        booking.setStatus(BookingStatus.PAYMENT_PENDING);

        booking.updateStatus(BookingStatus.CONFIRMED);
        
        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
    }
}
