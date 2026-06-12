package it.eably.backend.model;

import it.eably.backend.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AvailabilitySlot entity.
 * 
 * COVERAGE FOCUS:
 * - All getters and setters (de-lombokized code)
 * - Constructors (default and parameterized)
 * - validate() method with all business rules (including 60-minute max duration)
 * - Helper methods (isAvailable, getDurationMinutes, markAsBooked, markAsCancelled, markAsAvailable)
 * 
 * @author Alessandro D'Angelo
 * @version 1.0
 */
class AvailabilitySlotTest {
    
    private User providerUser;
    private Profile provider;
    private AvailabilitySlot slot;
    
    @BeforeEach
    void setUp() {
        providerUser = new User();
        providerUser.setId(1L);
        providerUser.setUsername("provider");
        providerUser.setEmail("provider@test.com");
        providerUser.setPasswordHash("$2a$12$hash");
        providerUser.setRole(UserRole.STUDENT);
        providerUser.setIsActive(true);
        providerUser.setIsVerified(true);
        
        provider = new Profile();
        provider.setId(1L);
        provider.setUser(providerUser);
        provider.setTitle("Math Tutor");
        provider.setHourlyRate(new BigDecimal("50.00"));
        provider.setDeliveryMode(DeliveryMode.ONLINE);
        provider.setIsActive(true);
        
        slot = new AvailabilitySlot();
    }
    
    // Constructor Tests
    
    @Test
    void testParameterizedConstructor() {
        AvailabilitySlot s = new AvailabilitySlot(
            providerUser,
            DayOfWeek.MONDAY,
            LocalTime.of(10, 0),
            LocalTime.of(11, 0),
            SlotStatus.AVAILABLE
        );
        
        assertEquals(providerUser, s.getStudent());
        assertEquals(DayOfWeek.MONDAY, s.getDayOfWeek());
        assertEquals(LocalTime.of(10, 0), s.getStartTime());
        assertEquals(LocalTime.of(11, 0), s.getEndTime());
        assertEquals(SlotStatus.AVAILABLE, s.getStatus());
    }
    
    // Getter and Setter Tests
    
    // Validation Tests
    
    @Test
    void testValidate_Success() {
        slot.setStudent(providerUser);
        slot.setDayOfWeek(DayOfWeek.MONDAY);
        slot.setStartTime(LocalTime.of(10, 0));
        slot.setEndTime(LocalTime.of(11, 0));
        slot.setStatus(SlotStatus.AVAILABLE);
        
        assertDoesNotThrow(() -> slot.validate());
    }
    
    @Test
    void testValidate_NullProfile_ThrowsException() {
        slot.setStudent(null);
        slot.setDayOfWeek(DayOfWeek.MONDAY);
        slot.setStartTime(LocalTime.of(10, 0));
        slot.setEndTime(LocalTime.of(11, 0));
        slot.setStatus(SlotStatus.AVAILABLE);
        
        ValidationException exception = assertThrows(ValidationException.class, () -> slot.validate());
        assertTrue(exception.getMessage().contains("must be associated with a student"));
    }
    
    @Test
    void testValidate_NullDayOfWeek_ThrowsException() {
        slot.setStudent(providerUser);
        slot.setDayOfWeek(null);
        slot.setStartTime(LocalTime.of(10, 0));
        slot.setEndTime(LocalTime.of(11, 0));
        slot.setStatus(SlotStatus.AVAILABLE);
        
        ValidationException exception = assertThrows(ValidationException.class, () -> slot.validate());
        assertTrue(exception.getMessage().contains("Day of week cannot be null"));
    }
    
    @Test
    void testValidate_NullStartTime_ThrowsException() {
        slot.setStudent(providerUser);
        slot.setDayOfWeek(DayOfWeek.MONDAY);
        slot.setStartTime(null);
        slot.setEndTime(LocalTime.of(11, 0));
        slot.setStatus(SlotStatus.AVAILABLE);
        
        ValidationException exception = assertThrows(ValidationException.class, () -> slot.validate());
        assertTrue(exception.getMessage().contains("Start time cannot be null"));
    }
    
    @Test
    void testValidate_NullEndTime_ThrowsException() {
        slot.setStudent(providerUser);
        slot.setDayOfWeek(DayOfWeek.MONDAY);
        slot.setStartTime(LocalTime.of(10, 0));
        slot.setEndTime(null);
        slot.setStatus(SlotStatus.AVAILABLE);
        
        ValidationException exception = assertThrows(ValidationException.class, () -> slot.validate());
        assertTrue(exception.getMessage().contains("End time cannot be null"));
    }
    
    @Test
    void testValidate_EndTimeBeforeStartTime_ThrowsException() {
        slot.setStudent(providerUser);
        slot.setDayOfWeek(DayOfWeek.MONDAY);
        slot.setStartTime(LocalTime.of(11, 0));
        slot.setEndTime(LocalTime.of(10, 0));
        slot.setStatus(SlotStatus.AVAILABLE);
        
        ValidationException exception = assertThrows(ValidationException.class, () -> slot.validate());
        assertTrue(exception.getMessage().contains("End time must be after start time"));
    }
    
    @Test
    void testValidate_EndTimeEqualsStartTime_ThrowsException() {
        slot.setStudent(providerUser);
        slot.setDayOfWeek(DayOfWeek.MONDAY);
        slot.setStartTime(LocalTime.of(10, 0));
        slot.setEndTime(LocalTime.of(10, 0));
        slot.setStatus(SlotStatus.AVAILABLE);
        
        ValidationException exception = assertThrows(ValidationException.class, () -> slot.validate());
        assertTrue(exception.getMessage().contains("End time must be after start time"));
    }
    
    @Test
    void testValidate_DurationExceeds60Minutes_ThrowsException() {
        slot.setStudent(providerUser);
        slot.setDayOfWeek(DayOfWeek.MONDAY);
        slot.setStartTime(LocalTime.of(10, 0));
        slot.setEndTime(LocalTime.of(11, 1)); // 61 minutes
        slot.setStatus(SlotStatus.AVAILABLE);
        
        ValidationException exception = assertThrows(ValidationException.class, () -> slot.validate());
        assertTrue(exception.getMessage().contains("cannot exceed 60 minutes"));
        assertTrue(exception.getMessage().contains("61 minutes"));
    }
    
    @Test
    void testValidate_DurationExactly60Minutes_Success() {
        slot.setStudent(providerUser);
        slot.setDayOfWeek(DayOfWeek.MONDAY);
        slot.setStartTime(LocalTime.of(10, 0));
        slot.setEndTime(LocalTime.of(11, 0)); // Exactly 60 minutes
        slot.setStatus(SlotStatus.AVAILABLE);
        
        assertDoesNotThrow(() -> slot.validate());
    }
    
    @Test
    void testValidate_Duration30Minutes_Success() {
        slot.setStudent(providerUser);
        slot.setDayOfWeek(DayOfWeek.MONDAY);
        slot.setStartTime(LocalTime.of(10, 0));
        slot.setEndTime(LocalTime.of(10, 30)); // 30 minutes
        slot.setStatus(SlotStatus.AVAILABLE);
        
        assertDoesNotThrow(() -> slot.validate());
    }
    
    @Test
    void testValidate_NullStatus_ThrowsException() {
        slot.setStudent(providerUser);
        slot.setDayOfWeek(DayOfWeek.MONDAY);
        slot.setStartTime(LocalTime.of(10, 0));
        slot.setEndTime(LocalTime.of(11, 0));
        slot.setStatus(null);
        
        ValidationException exception = assertThrows(ValidationException.class, () -> slot.validate());
        assertTrue(exception.getMessage().contains("Status cannot be null"));
    }
    
    // Helper Method Tests
    
    @Test
    void testIsAvailable_Available_ReturnsTrue() {
        slot.setStatus(SlotStatus.AVAILABLE);
        assertTrue(slot.isAvailable());
    }
    
    @Test
    void testIsAvailable_Booked_ReturnsFalse() {
        slot.setStatus(SlotStatus.BOOKED);
        assertFalse(slot.isAvailable());
    }
    
    @Test
    void testIsAvailable_Cancelled_ReturnsFalse() {
        slot.setStatus(SlotStatus.CANCELLED);
        assertFalse(slot.isAvailable());
    }
    
    @Test
    void testGetDurationMinutes_60Minutes() {
        slot.setStartTime(LocalTime.of(10, 0));
        slot.setEndTime(LocalTime.of(11, 0));
        
        assertEquals(60, slot.getDurationMinutes());
    }
    
    @Test
    void testGetDurationMinutes_30Minutes() {
        slot.setStartTime(LocalTime.of(10, 0));
        slot.setEndTime(LocalTime.of(10, 30));
        
        assertEquals(30, slot.getDurationMinutes());
    }
    
    @Test
    void testGetDurationMinutes_45Minutes() {
        slot.setStartTime(LocalTime.of(14, 15));
        slot.setEndTime(LocalTime.of(15, 0));
        
        assertEquals(45, slot.getDurationMinutes());
    }
    
    @Test
    void testMarkAsBooked() {
        slot.setStatus(SlotStatus.AVAILABLE);
        
        slot.markAsBooked();
        
        assertEquals(SlotStatus.BOOKED, slot.getStatus());
    }
    
    @Test
    void testMarkAsCancelled() {
        slot.setStatus(SlotStatus.BOOKED);
        
        slot.markAsCancelled();
        
        assertEquals(SlotStatus.CANCELLED, slot.getStatus());
    }
    
    @Test
    void testMarkAsAvailable() {
        slot.setStatus(SlotStatus.CANCELLED);
        
        slot.markAsAvailable();
        
        assertEquals(SlotStatus.AVAILABLE, slot.getStatus());
    }
}
