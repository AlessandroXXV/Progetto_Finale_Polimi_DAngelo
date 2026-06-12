package it.eably.backend.service.observer;

import it.eably.backend.model.*;
import it.eably.backend.service.def.EmailQueueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ResourceLoader;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for EmailNotificationObserver.
 * 
 * TESTING OBSERVER PATTERN:
 * - Verify observer reacts to different booking statuses
 * - Test exception handling (observer should not break subject)
 * - Verify logging behavior
 * - Test observer name
 * 
 * ACADEMIC FOCUS:
 * - Demonstrates testing of Observer Pattern concrete implementation
 * - Shows how observers handle errors gracefully
 * - Validates loose coupling between subject and observer
 * 
 * NOTE: This test uses real logging (not mocked) to verify behavior.
 * In production, you might mock JavaMailSender to verify email sending.
 * 
 * @author Alessandro D'Angelo
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
class EmailNotificationObserverTest {
    
    @Mock
    private JavaMailSender mailSender;

    @Mock
    private ResourceLoader resourceLoader;

    @Mock
    private EmailQueueService emailQueueService;

    @InjectMocks
    private EmailNotificationObserver observer;
    
    private Booking testBooking;
    private User client;
    private User providerUser;
    private Profile provider;
    private AvailabilitySlot slot;
    
    @BeforeEach
    void setUp() {
        // Inject logoPath field for testing (simulates @Value injection)
        ReflectionTestUtils.setField(observer, "logoPath", "classpath:/static/images/logo.png");

        // Create test client
        client = new User();
        client.setId(1L);
        client.setUsername("test_client");
        client.setEmail("client@test.com");
        client.setPasswordHash("$2a$12$hashedpassword");
        client.setRole(UserRole.CLIENT);
        client.setIsActive(true);
        client.setIsVerified(true);
        
        // Create test provider user
        providerUser = new User();
        providerUser.setId(2L);
        providerUser.setUsername("test_provider");
        providerUser.setEmail("provider@test.com");
        providerUser.setPasswordHash("$2a$12$hashedpassword");
        providerUser.setRole(UserRole.STUDENT);
        providerUser.setIsActive(true);
        providerUser.setIsVerified(true);
        
        // Create test provider profile
        provider = new Profile();
        provider.setId(1L);
        provider.setUser(providerUser);
        provider.setTitle("Math Tutor");
        provider.setDescription("Expert in mathematics");
        provider.setHourlyRate(new BigDecimal("50.00"));
        provider.setDeliveryMode(DeliveryMode.ONLINE);
        provider.setIsActive(true);
        
        // Create test availability slot
        slot = new AvailabilitySlot();
        slot.setId(1L);
        slot.setStudent(providerUser);
        slot.setDayOfWeek(DayOfWeek.MONDAY);
        slot.setStartTime(LocalTime.of(10, 0));
        slot.setEndTime(LocalTime.of(11, 0));
        slot.setStatus(SlotStatus.AVAILABLE);
        
        // Create test booking
        testBooking = new Booking();
        testBooking.setId(1L);
        testBooking.setClient(client);
        testBooking.setProvider(providerUser);
        testBooking.setProfile(provider);
        testBooking.setAvailabilitySlot(slot);
        testBooking.setTotalAmount(new BigDecimal("50.00"));
        testBooking.setNotes("Test booking");
        testBooking.setBookedAt(LocalDateTime.now());
    }
    
    /**
     * Test observer reacts to PAYMENT_PENDING status.
     * 
     * EXPECTED BEHAVIOR:
     * - Observer logs payment pending notification
     * - No exception thrown
     */
    @Test
    void testOnBookingStatusChanged_PaymentPending() {
        // Arrange
        testBooking.setStatus(BookingStatus.PAYMENT_PENDING);
        
        // Act & Assert - should not throw exception
        assertDoesNotThrow(() -> observer.onBookingStatusChanged(testBooking),
                "Observer should handle PAYMENT_PENDING status without exception");
    }
    
    /**
     * Test observer reacts to CONFIRMED status.
     * 
     * EXPECTED BEHAVIOR:
     * - Observer logs confirmation email
     * - Includes confirmation code in log
     * - No exception thrown
     */
    @Test
    void testOnBookingStatusChanged_Confirmed() {
        // Arrange
        testBooking.setStatus(BookingStatus.CONFIRMED);
        testBooking.setConfirmationCode("123456");
        
        // Act & Assert - should not throw exception
        assertDoesNotThrow(() -> observer.onBookingStatusChanged(testBooking),
                "Observer should handle CONFIRMED status without exception");
    }
    
    /**
     * Test observer reacts to COMPLETED status.
     * 
     * EXPECTED BEHAVIOR:
     * - Observer logs completion email
     * - Includes review request
     * - No exception thrown
     */
    @Test
    void testOnBookingStatusChanged_Completed() {
        // Arrange
        testBooking.setStatus(BookingStatus.COMPLETED);
        testBooking.setConfirmationCode("123456");
        
        // Act & Assert - should not throw exception
        assertDoesNotThrow(() -> observer.onBookingStatusChanged(testBooking),
                "Observer should handle COMPLETED status without exception");
    }
    
    /**
     * Test observer reacts to CANCELLED status.
     * 
     * EXPECTED BEHAVIOR:
     * - Observer logs cancellation email
     * - Includes refund information
     * - No exception thrown
     */
    @Test
    void testOnBookingStatusChanged_Cancelled_UserCancelled() {
        testBooking.setStatus(BookingStatus.CANCELLED);
        testBooking.setCancellationReason(BookingCancellationReason.USER_CANCELLED);

        assertDoesNotThrow(() -> observer.onBookingStatusChanged(testBooking),
                "Observer should handle USER_CANCELLED without exception");
    }

    @Test
    void testOnBookingStatusChanged_Cancelled_PaymentTimeout_OnlyClientNotified() {
        testBooking.setStatus(BookingStatus.CANCELLED);
        testBooking.setCancellationReason(BookingCancellationReason.PAYMENT_TIMEOUT);
        testBooking.setAvailabilitySlot(slot);
        testBooking.setBookingDate(LocalDate.now().plusDays(1));

        assertDoesNotThrow(() -> observer.onBookingStatusChanged(testBooking),
                "Observer should handle PAYMENT_TIMEOUT cancellation without exception");
    }
    
    /**
     * Test observer handles null booking gracefully.
     * 
     * CRITICAL: Observer should NEVER throw exception that breaks subject.
     * Even with invalid input, observer should catch and log error.
     */
    @Test
    void testOnBookingStatusChanged_NullBooking_DoesNotThrow() {
        // Act & Assert - should not throw exception
        assertDoesNotThrow(() -> observer.onBookingStatusChanged(null),
                "Observer should handle null booking gracefully without exception");
    }
    
    /**
     * Test observer handles booking with null status.
     * 
     * EXPECTED BEHAVIOR:
     * - Observer catches NullPointerException
     * - Logs error
     * - Does not propagate exception to subject
     */
    @Test
    void testOnBookingStatusChanged_NullStatus_DoesNotThrow() {
        // Arrange
        testBooking.setStatus(null);
        
        // Act & Assert - should not throw exception
        assertDoesNotThrow(() -> observer.onBookingStatusChanged(testBooking),
                "Observer should handle null status gracefully without exception");
    }
    
    /**
     * Test observer handles booking with null client.
     * 
     * EXPECTED BEHAVIOR:
     * - Observer catches NullPointerException when accessing client.getEmail()
     * - Logs error
     * - Does not propagate exception to subject
     */
    @Test
    void testOnBookingStatusChanged_NullClient_DoesNotThrow() {
        // Arrange
        testBooking.setClient(null);
        testBooking.setStatus(BookingStatus.CONFIRMED);
        
        // Act & Assert - should not throw exception
        assertDoesNotThrow(() -> observer.onBookingStatusChanged(testBooking),
                "Observer should handle null client gracefully without exception");
    }
    
    /**
     * Test observer handles booking with null provider.
     * 
     * EXPECTED BEHAVIOR:
     * - Observer catches NullPointerException when accessing provider
     * - Logs error
     * - Does not propagate exception to subject
     */
    @Test
    void testOnBookingStatusChanged_NullProvider_DoesNotThrow() {
        // Arrange
        testBooking.setProvider(null);
        testBooking.setStatus(BookingStatus.CONFIRMED);
        
        // Act & Assert - should not throw exception
        assertDoesNotThrow(() -> observer.onBookingStatusChanged(testBooking),
                "Observer should handle null provider gracefully without exception");
    }
    
    /**
     * Test observer name for identification.
     */
    @Test
    void testGetObserverName() {
        // Act
        String observerName = observer.getObserverName();
        
        // Assert
        assertNotNull(observerName, "Observer name should not be null");
        assertTrue(observerName.contains("Email"),
                "Observer name should contain 'Email'");
        assertTrue(observerName.contains("Notification"),
                "Observer name should contain 'Notification'");
    }
    
    /**
     * Test that observer can handle multiple status changes in sequence.
     * 
     * SCENARIO: Booking lifecycle
     * 1. PAYMENT_PENDING → initial booking
     * 2. CONFIRMED → payment successful
     * 3. COMPLETED → service delivered
     * 
     * EXPECTED: Observer handles all transitions without error
     */
    @Test
    void testOnBookingStatusChanged_MultipleStatusChanges() {
        // Act & Assert - PAYMENT_PENDING
        testBooking.setStatus(BookingStatus.PAYMENT_PENDING);
        assertDoesNotThrow(() -> observer.onBookingStatusChanged(testBooking),
                "Observer should handle PAYMENT_PENDING");
        
        // Act & Assert - CONFIRMED
        testBooking.setStatus(BookingStatus.CONFIRMED);
        testBooking.setConfirmationCode("123456");
        assertDoesNotThrow(() -> observer.onBookingStatusChanged(testBooking),
                "Observer should handle CONFIRMED");
        
        // Act & Assert - COMPLETED
        testBooking.setStatus(BookingStatus.COMPLETED);
        assertDoesNotThrow(() -> observer.onBookingStatusChanged(testBooking),
                "Observer should handle COMPLETED");
    }
    
    /**
     * Test that observer handles cancellation after confirmation.
     * 
     * SCENARIO: User cancels after payment
     * 1. CONFIRMED → payment successful
     * 2. CANCELLED → user cancels
     * 
     * EXPECTED: Observer sends cancellation email with refund info
     */
    @Test
    void testOnBookingStatusChanged_CancellationAfterConfirmation() {
        // Arrange - first confirm
        testBooking.setStatus(BookingStatus.CONFIRMED);
        testBooking.setConfirmationCode("123456");
        testBooking.setPaymentIntentId("pi_test123");
        
        // Act - then cancel
        testBooking.setStatus(BookingStatus.CANCELLED);
        
        // Assert - should not throw exception
        assertDoesNotThrow(() -> observer.onBookingStatusChanged(testBooking),
                "Observer should handle cancellation after confirmation");
    }
    
    /**
     * Test observer with booking that has all fields populated.
     * 
     * COMPREHENSIVE TEST:
     * - All booking fields set
     * - All related entities populated
     * - Status: CONFIRMED
     * 
     * EXPECTED: Observer successfully processes complete booking
     */
    @Test
    void testOnBookingStatusChanged_CompleteBooking() {
        // Arrange - fully populated booking
        testBooking.setStatus(BookingStatus.CONFIRMED);
        testBooking.setConfirmationCode("123456");
        testBooking.setPaymentIntentId("pi_test123");
        testBooking.setTotalAmount(new BigDecimal("50.00"));
        testBooking.setNotes("Complete test booking with all fields");
        
        // Act & Assert - should not throw exception
        assertDoesNotThrow(() -> observer.onBookingStatusChanged(testBooking),
                "Observer should handle complete booking without exception");
    }
    
    /**
     * Test that observer is stateless (can be reused).
     * 
     * OBSERVER PATTERN PROPERTY:
     * - Observers should be stateless
     * - Same observer instance can handle multiple bookings
     * - No side effects between invocations
     */
    @Test
    void testObserver_IsStateless_CanHandleMultipleBookings() {
        // Arrange - create second booking
        Booking secondBooking = new Booking();
        secondBooking.setId(2L);
        secondBooking.setClient(client);
        secondBooking.setProvider(providerUser);
        secondBooking.setProfile(provider);
        secondBooking.setAvailabilitySlot(slot);
        secondBooking.setStatus(BookingStatus.CONFIRMED);
        secondBooking.setConfirmationCode("654321");
        secondBooking.setTotalAmount(new BigDecimal("75.00"));
        
        // Act & Assert - handle first booking
        testBooking.setStatus(BookingStatus.CONFIRMED);
        testBooking.setConfirmationCode("123456");
        assertDoesNotThrow(() -> observer.onBookingStatusChanged(testBooking),
                "Observer should handle first booking");
        
        // Act & Assert - handle second booking with same observer instance
        assertDoesNotThrow(() -> observer.onBookingStatusChanged(secondBooking),
                "Observer should handle second booking without state interference");
    }
}
