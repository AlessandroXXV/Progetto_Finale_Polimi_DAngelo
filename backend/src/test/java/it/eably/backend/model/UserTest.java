package it.eably.backend.model;

import it.eably.backend.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for User entity.
 * 
 * Tests cover all getters, setters, constructors, validation logic,
 * and helper methods to achieve 100% coverage.
 * 
 * @author Alessandro D'Angelo
 * @version 1.0
 */
class UserTest {
    
    private User user;
    
    @BeforeEach
    void setUp() {
        user = new User();
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPasswordHash("$2a$12$hashedpassword");
        user.setRole(UserRole.CLIENT);
        user.setIsActive(true);
        user.setIsVerified(false);
    }
    
    // ========== CONSTRUCTOR TESTS ==========
    
    @Test
    void testParameterizedConstructor() {
        User newUser = new User("username", "email@test.com", "hash", UserRole.STUDENT, true, true);
        
        assertEquals("username", newUser.getUsername());
        assertEquals("email@test.com", newUser.getEmail());
        assertEquals("hash", newUser.getPasswordHash());
        assertEquals(UserRole.STUDENT, newUser.getRole());
        assertTrue(newUser.getIsActive());
        assertTrue(newUser.getIsVerified());
    }
    
    // ========== GETTER/SETTER TESTS ==========
    
    @Test
    void testGetSetProfiles() {
        Profile profile = new Profile();
        user.setProfiles(java.util.List.of(profile));
        assertEquals(1, user.getProfiles().size());
        assertEquals(profile, user.getProfiles().get(0));
    }
    
    @Test
    void testGetSetIsPremium() {
        user.setIsPremium(true);
        assertTrue(user.getIsPremium());
    }
    
    // ========== VALIDATION TESTS ==========
    
    @Test
    void testValidate_Success() {
        assertDoesNotThrow(() -> user.validate());
    }
    
    @Test
    void testValidate_WithNullUsername_ShouldThrowException() {
        user.setUsername(null);
        
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            user.validate();
        });
        
        assertEquals("Username cannot be null or empty", exception.getMessage());
    }
    
    @Test
    void testValidate_WithEmptyUsername_ShouldThrowException() {
        user.setUsername("   ");
        
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            user.validate();
        });
        
        assertEquals("Username cannot be null or empty", exception.getMessage());
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"ab", "a", ""})
    void testValidate_WithTooShortUsername_ShouldThrowException(String username) {
        user.setUsername(username);
        
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            user.validate();
        });
        
        assertTrue(exception.getMessage().contains("Username must be between 3 and 20 characters") ||
                   exception.getMessage().contains("Username cannot be null or empty"));
    }
    
    @Test
    void testValidate_WithTooLongUsername_ShouldThrowException() {
        user.setUsername("a".repeat(21));
        
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            user.validate();
        });
        
        assertEquals("Username must be between 3 and 20 characters", exception.getMessage());
    }
    
    @Test
    void testValidate_WithNullEmail_ShouldThrowException() {
        user.setEmail(null);
        
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            user.validate();
        });
        
        assertEquals("Email cannot be null or empty", exception.getMessage());
    }
    
    @Test
    void testValidate_WithEmptyEmail_ShouldThrowException() {
        user.setEmail("   ");
        
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            user.validate();
        });
        
        assertEquals("Email cannot be null or empty", exception.getMessage());
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"invalidemail", "test", "test.com"})
    void testValidate_WithInvalidEmailFormat_ShouldThrowException(String email) {
        user.setEmail(email);
        
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            user.validate();
        });
        
        assertEquals("Email must be a valid email address", exception.getMessage());
    }
    
    @Test
    void testValidate_WithNullPasswordHash_ShouldThrowException() {
        user.setPasswordHash(null);
        
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            user.validate();
        });
        
        assertEquals("Password hash must be at least 8 characters", exception.getMessage());
    }
    
    @Test
    void testValidate_WithShortPasswordHash_ShouldThrowException() {
        user.setPasswordHash("short");
        
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            user.validate();
        });
        
        assertEquals("Password hash must be at least 8 characters", exception.getMessage());
    }
    
    @Test
    void testValidate_WithNullRole_ShouldThrowException() {
        user.setRole(null);
        
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            user.validate();
        });
        
        assertEquals("User role cannot be null", exception.getMessage());
    }
    
    @Test
    void testValidate_WithNullIsActive_ShouldThrowException() {
        user.setIsActive(null);
        
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            user.validate();
        });
        
        assertEquals("isActive flag cannot be null", exception.getMessage());
    }
    
    @Test
    void testValidate_WithNullIsVerified_ShouldThrowException() {
        user.setIsVerified(null);
        
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            user.validate();
        });
        
        assertEquals("isVerified flag cannot be null", exception.getMessage());
    }
    
    // ========== HELPER METHOD TESTS ==========
    
    @Test
    void testHasRole_WithMatchingRole_ReturnsTrue() {
        assertTrue(user.hasRole(UserRole.CLIENT));
    }
    
    @Test
    void testHasRole_WithDifferentRole_ReturnsFalse() {
        assertFalse(user.hasRole(UserRole.ADMIN));
    }
    
    @Test
    void testIsAdmin_WithAdminRole_ReturnsTrue() {
        user.setRole(UserRole.ADMIN);
        assertTrue(user.isAdmin());
    }
    
    @Test
    void testIsAdmin_WithNonAdminRole_ReturnsFalse() {
        assertFalse(user.isAdmin());
    }
    
    @Test
    void testIsStudent_WithStudentRole_ReturnsTrue() {
        user.setRole(UserRole.STUDENT);
        assertTrue(user.isStudent());
    }
    
    @Test
    void testIsStudent_WithNonStudentRole_ReturnsFalse() {
        assertFalse(user.isStudent());
    }
    
    @Test
    void testIsClient_WithClientRole_ReturnsTrue() {
        assertTrue(user.isClient());
    }
    
    @Test
    void testIsClient_WithNonClientRole_ReturnsFalse() {
        user.setRole(UserRole.ADMIN);
        assertFalse(user.isClient());
    }
}
