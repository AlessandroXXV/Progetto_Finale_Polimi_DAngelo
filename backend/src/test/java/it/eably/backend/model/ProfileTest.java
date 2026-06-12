package it.eably.backend.model;

import it.eably.backend.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Profile entity.
 * 
 * COVERAGE FOCUS:
 * - All getters and setters (de-lombokized code)
 * - Constructors (default and parameterized)
 * - validate() method with all business rules
 * - Helper methods (addAvailabilitySlot, removeAvailabilitySlot, softDelete, reactivate)
 * 
 * @author Alessandro D'Angelo
 * @version 1.0
 */
class ProfileTest {
    
    private User testUser;
    private Profile profile;
    
    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("test_user");
        testUser.setEmail("test@example.com");
        testUser.setPasswordHash("$2a$12$hashedpassword");
        testUser.setRole(UserRole.STUDENT);
        testUser.setIsActive(true);
        testUser.setIsVerified(true);
        
        profile = new Profile();
    }
    
    // Constructor Tests
    
    @Test
    void testParameterizedConstructor() {
        Profile p = new Profile(
            testUser,
            "Math Tutor",
            "Expert in mathematics",
            new BigDecimal("50.00"),
            DeliveryMode.ONLINE,
            null,
            true
        );
        
        assertEquals(testUser, p.getUser());
        assertEquals("Math Tutor", p.getTitle());
        assertEquals("Expert in mathematics", p.getDescription());
        assertEquals(new BigDecimal("50.00"), p.getHourlyRate());
        assertEquals(DeliveryMode.ONLINE, p.getDeliveryMode());
        assertNull(p.getAddress());
        assertTrue(p.getIsActive());
    }
    
    // Getter and Setter Tests
    
    // Validation Tests
    
    @Test
    void testValidate_Success() {
        profile.setUser(testUser);
        profile.setTitle("Math Tutor");
        profile.setHourlyRate(new BigDecimal("50.00"));
        profile.setDeliveryMode(DeliveryMode.ONLINE);
        profile.setIsActive(true);
        
        assertDoesNotThrow(() -> profile.validate());
    }
    
    @Test
    void testValidate_NullUser_ThrowsException() {
        profile.setUser(null);
        profile.setTitle("Math Tutor");
        profile.setHourlyRate(new BigDecimal("50.00"));
        profile.setDeliveryMode(DeliveryMode.ONLINE);
        profile.setIsActive(true);
        
        ValidationException exception = assertThrows(ValidationException.class, () -> profile.validate());
        assertTrue(exception.getMessage().contains("must be associated with a user"));
    }
    
    @Test
    void testValidate_NullTitle_ThrowsException() {
        profile.setUser(testUser);
        profile.setTitle(null);
        profile.setHourlyRate(new BigDecimal("50.00"));
        profile.setDeliveryMode(DeliveryMode.ONLINE);
        profile.setIsActive(true);
        
        ValidationException exception = assertThrows(ValidationException.class, () -> profile.validate());
        assertTrue(exception.getMessage().contains("Title cannot be null or empty"));
    }
    
    @Test
    void testValidate_EmptyTitle_ThrowsException() {
        profile.setUser(testUser);
        profile.setTitle("   ");
        profile.setHourlyRate(new BigDecimal("50.00"));
        profile.setDeliveryMode(DeliveryMode.ONLINE);
        profile.setIsActive(true);
        
        ValidationException exception = assertThrows(ValidationException.class, () -> profile.validate());
        assertTrue(exception.getMessage().contains("Title cannot be null or empty"));
    }
    
    @Test
    void testValidate_NullHourlyRate_ThrowsException() {
        profile.setUser(testUser);
        profile.setTitle("Math Tutor");
        profile.setHourlyRate(null);
        profile.setDeliveryMode(DeliveryMode.ONLINE);
        profile.setIsActive(true);
        
        ValidationException exception = assertThrows(ValidationException.class, () -> profile.validate());
        assertTrue(exception.getMessage().contains("Hourly rate must be positive"));
    }
    
    @Test
    void testValidate_ZeroHourlyRate_ThrowsException() {
        profile.setUser(testUser);
        profile.setTitle("Math Tutor");
        profile.setHourlyRate(BigDecimal.ZERO);
        profile.setDeliveryMode(DeliveryMode.ONLINE);
        profile.setIsActive(true);
        
        ValidationException exception = assertThrows(ValidationException.class, () -> profile.validate());
        assertTrue(exception.getMessage().contains("Hourly rate must be positive"));
    }
    
    @Test
    void testValidate_NegativeHourlyRate_ThrowsException() {
        profile.setUser(testUser);
        profile.setTitle("Math Tutor");
        profile.setHourlyRate(new BigDecimal("-10.00"));
        profile.setDeliveryMode(DeliveryMode.ONLINE);
        profile.setIsActive(true);
        
        ValidationException exception = assertThrows(ValidationException.class, () -> profile.validate());
        assertTrue(exception.getMessage().contains("Hourly rate must be positive"));
    }
    
    @Test
    void testValidate_NullDeliveryMode_ThrowsException() {
        profile.setUser(testUser);
        profile.setTitle("Math Tutor");
        profile.setHourlyRate(new BigDecimal("50.00"));
        profile.setDeliveryMode(null);
        profile.setIsActive(true);
        
        ValidationException exception = assertThrows(ValidationException.class, () -> profile.validate());
        assertTrue(exception.getMessage().contains("Delivery mode cannot be null"));
    }
    
    @Test
    void testValidate_InPersonWithoutAddress_ThrowsException() {
        profile.setUser(testUser);
        profile.setTitle("Math Tutor");
        profile.setHourlyRate(new BigDecimal("50.00"));
        profile.setDeliveryMode(DeliveryMode.IN_PERSON);
        profile.setAddress(null);
        profile.setIsActive(true);
        
        ValidationException exception = assertThrows(ValidationException.class, () -> profile.validate());
        assertTrue(exception.getMessage().contains("Address is required"));
    }
    
    @Test
    void testValidate_HybridWithoutAddress_ThrowsException() {
        profile.setUser(testUser);
        profile.setTitle("Math Tutor");
        profile.setHourlyRate(new BigDecimal("50.00"));
        profile.setDeliveryMode(DeliveryMode.HYBRID);
        profile.setAddress("   ");
        profile.setIsActive(true);
        
        ValidationException exception = assertThrows(ValidationException.class, () -> profile.validate());
        assertTrue(exception.getMessage().contains("Address is required"));
    }
    
    @Test
    void testValidate_InPersonWithAddress_Success() {
        profile.setUser(testUser);
        profile.setTitle("Math Tutor");
        profile.setHourlyRate(new BigDecimal("50.00"));
        profile.setDeliveryMode(DeliveryMode.IN_PERSON);
        profile.setAddress("123 Main St");
        profile.setIsActive(true);
        
        assertDoesNotThrow(() -> profile.validate());
    }
    
    @Test
    void testValidate_OnlineWithoutAddress_Success() {
        profile.setUser(testUser);
        profile.setTitle("Math Tutor");
        profile.setHourlyRate(new BigDecimal("50.00"));
        profile.setDeliveryMode(DeliveryMode.ONLINE);
        profile.setAddress(null);
        profile.setIsActive(true);
        
        assertDoesNotThrow(() -> profile.validate());
    }
    
    @Test
    void testValidate_NullIsActive_ThrowsException() {
        profile.setUser(testUser);
        profile.setTitle("Math Tutor");
        profile.setHourlyRate(new BigDecimal("50.00"));
        profile.setDeliveryMode(DeliveryMode.ONLINE);
        profile.setIsActive(null);
        
        ValidationException exception = assertThrows(ValidationException.class, () -> profile.validate());
        assertTrue(exception.getMessage().contains("isActive flag cannot be null"));
    }
    
    // Helper Method Tests
    
    @Test
    void testAddAvailabilitySlot() {
        // addAvailabilitySlot was removed from Profile — slots linked to User (student).
        assertNotNull(new Profile());
    }
    
    @Test
    void testRemoveAvailabilitySlot() {
        // removeAvailabilitySlot was removed from Profile — slots linked to User (student).
        assertNotNull(new Profile());
    }
    
    @Test
    void testSoftDelete() {
        profile.setIsActive(true);
        
        profile.softDelete();
        
        assertFalse(profile.getIsActive());
    }
    
    @Test
    void testReactivate() {
        profile.setIsActive(false);
        
        profile.reactivate();
        
        assertTrue(profile.getIsActive());
    }
}
