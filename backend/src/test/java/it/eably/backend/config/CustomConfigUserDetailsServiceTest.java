package it.eably.backend.config;

import it.eably.backend.model.User;
import it.eably.backend.model.UserRole;
import it.eably.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the {@link UserDetailsService} bean produced by {@link CustomConfig}.
 *
 * Exercises the real production bean (username lookup with email fallback) instead of
 * a re-implementation, so authentication logic is actually covered.
 *
 * COVERAGE FOCUS:
 * - Load user by username
 * - Load user by email (fallback)
 * - User not found scenarios
 * - Authority mapping
 * - Account status mapping
 */
@ExtendWith(MockitoExtension.class)
class CustomConfigUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    private UserDetailsService userDetailsService;

    private User testUser;

    @BeforeEach
    void setUp() {
        userDetailsService = new CustomConfig(userRepository).userDetailsService();
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPasswordHash("$2a$12$hashedpassword");
        testUser.setRole(UserRole.CLIENT);
        testUser.setIsActive(true);
        testUser.setIsVerified(true);
    }

    @Test
    void testLoadUserByUsername_ByUsername_Success() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername("testuser");

        // Assert
        assertNotNull(userDetails);
        assertEquals("testuser", userDetails.getUsername());
        assertEquals("$2a$12$hashedpassword", userDetails.getPassword());
        assertTrue(userDetails.isEnabled());
        assertTrue(userDetails.isAccountNonLocked());
        assertFalse(userDetails.getAuthorities().isEmpty());

        verify(userRepository).findByUsername("testuser");
    }

    @Test
    void testLoadUserByUsername_ByEmail_Success() {
        // Arrange
        when(userRepository.findByUsername("test@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername("test@example.com");

        // Assert
        assertNotNull(userDetails);
        assertEquals("testuser", userDetails.getUsername());
        assertEquals("$2a$12$hashedpassword", userDetails.getPassword());

        verify(userRepository).findByUsername("test@example.com");
        verify(userRepository).findByEmail("test@example.com");
    }

    @Test
    void testLoadUserByUsername_NotFound_ThrowsException() {
        // Arrange
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("nonexistent")).thenReturn(Optional.empty());

        // Act & Assert
        UsernameNotFoundException exception = assertThrows(
            UsernameNotFoundException.class,
            () -> userDetailsService.loadUserByUsername("nonexistent")
        );

        assertTrue(exception.getMessage().contains("User not found"));
        assertTrue(exception.getMessage().contains("nonexistent"));

        verify(userRepository).findByUsername("nonexistent");
        verify(userRepository).findByEmail("nonexistent");
    }

    @Test
    void testLoadUserByUsername_InactiveUser_AccountLocked() {
        // Arrange
        testUser.setIsActive(false);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername("testuser");

        // Assert
        assertNotNull(userDetails);
        assertFalse(userDetails.isAccountNonLocked()); // Inactive = locked
        assertFalse(userDetails.isEnabled()); // Inactive = disabled
    }

    @Test
    void testLoadUserByUsername_ActiveUser_AccountNotLocked() {
        // Arrange
        testUser.setIsActive(true);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername("testuser");

        // Assert
        assertNotNull(userDetails);
        assertTrue(userDetails.isAccountNonLocked());
        assertTrue(userDetails.isEnabled());
    }

    @Test
    void testLoadUserByUsername_ClientRole_HasAuthority() {
        // Arrange
        testUser.setRole(UserRole.CLIENT);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername("testuser");

        // Assert
        assertNotNull(userDetails);
        assertEquals(1, userDetails.getAuthorities().size());
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_CLIENT")));
    }

    @Test
    void testLoadUserByUsername_StudentRole_HasAuthority() {
        // Arrange
        testUser.setRole(UserRole.STUDENT);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername("testuser");

        // Assert
        assertNotNull(userDetails);
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_STUDENT")));
    }

    @Test
    void testLoadUserByUsername_AdminRole_HasAuthority() {
        // Arrange
        testUser.setRole(UserRole.ADMIN);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername("testuser");

        // Assert
        assertNotNull(userDetails);
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    void testLoadUserByUsername_AccountNonExpired_AlwaysTrue() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername("testuser");

        // Assert
        assertTrue(userDetails.isAccountNonExpired());
    }

    @Test
    void testLoadUserByUsername_CredentialsNonExpired_AlwaysTrue() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername("testuser");

        // Assert
        assertTrue(userDetails.isCredentialsNonExpired());
    }

    @Test
    void testLoadUserByUsername_PreferUsernameOverEmail() {
        // Arrange
        User userByUsername = new User();
        userByUsername.setId(1L);
        userByUsername.setUsername("testuser");
        userByUsername.setEmail("test@example.com");
        userByUsername.setPasswordHash("$2a$12$hash1");
        userByUsername.setRole(UserRole.CLIENT);
        userByUsername.setIsActive(true);
        userByUsername.setIsVerified(true);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(userByUsername));

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername("testuser");

        // Assert
        assertEquals("testuser", userDetails.getUsername());
        assertEquals("$2a$12$hash1", userDetails.getPassword());

        // Verify email search was never called (username found first)
        verify(userRepository).findByUsername("testuser");
        verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    void testLoadUserByUsername_FallbackToEmail() {
        // Arrange
        when(userRepository.findByUsername("test@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername("test@example.com");

        // Assert
        assertNotNull(userDetails);
        assertEquals("testuser", userDetails.getUsername());

        verify(userRepository).findByUsername("test@example.com");
        verify(userRepository).findByEmail("test@example.com");
    }
}
