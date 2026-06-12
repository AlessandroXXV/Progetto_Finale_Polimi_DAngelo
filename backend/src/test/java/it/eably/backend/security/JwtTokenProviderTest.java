package it.eably.backend.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for JwtTokenProvider.
 * 
 * COVERAGE FOCUS:
 * - Token generation
 * - Token validation
 * - Username extraction
 * - Expiration handling
 * - Error cases (invalid, expired, malformed tokens)
 * 
 * @author Alessandro D'Angelo
 * @version 1.0
 */
@ActiveProfiles("test")
class JwtTokenProviderTest {
    
    private JwtTokenProvider jwtTokenProvider;
    
    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        
        // Set test values using reflection
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret", 
            "testSecretKeyForJwtTokenGenerationAndValidationMustBeLongEnough");
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpirationMs", 3600000L); // 1 hour
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtRefreshExpirationMs", 86400000L); // 24 hours
    }
    
    @Test
    void testGenerateToken_Success() {
        // Arrange
        UserDetails userDetails = User.builder()
                .username("testuser")
                .password("password")
                .authorities("ROLE_CLIENT")
                .build();
        
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        
        // Act
        String token = jwtTokenProvider.generateToken(authentication);
        
        // Assert
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.split("\\.").length == 3); // JWT has 3 parts
    }
    
    @Test
    void testGenerateTokenFromUsername_Success() {
        // Arrange
        String username = "testuser";
        List<String> roles = Arrays.asList("ROLE_CLIENT", "ROLE_USER");
        
        // Act
        String token = jwtTokenProvider.generateTokenFromUsername(username, roles);
        
        // Assert
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }
    
    @Test
    void testGenerateRefreshToken_Success() {
        // Arrange
        String username = "testuser";
        
        // Act
        String refreshToken = jwtTokenProvider.generateRefreshToken(username);
        
        // Assert
        assertNotNull(refreshToken);
        assertFalse(refreshToken.isEmpty());
    }
    
    @Test
    void testGetUsernameFromToken_Success() {
        // Arrange
        String username = "testuser";
        List<String> roles = Arrays.asList("ROLE_CLIENT");
        String token = jwtTokenProvider.generateTokenFromUsername(username, roles);
        
        // Act
        String extractedUsername = jwtTokenProvider.getUsernameFromToken(token);
        
        // Assert
        assertEquals(username, extractedUsername);
    }
    
    @Test
    void testValidateToken_ValidToken_ReturnsTrue() {
        // Arrange
        String username = "testuser";
        List<String> roles = Arrays.asList("ROLE_CLIENT");
        String token = jwtTokenProvider.generateTokenFromUsername(username, roles);
        
        // Act
        boolean isValid = jwtTokenProvider.validateToken(token);
        
        // Assert
        assertTrue(isValid);
    }
    
    @Test
    void testValidateToken_InvalidToken_ReturnsFalse() {
        // Arrange
        String invalidToken = "invalid.token.here";
        
        // Act
        boolean isValid = jwtTokenProvider.validateToken(invalidToken);
        
        // Assert
        assertFalse(isValid);
    }
    
    @Test
    void testValidateToken_MalformedToken_ReturnsFalse() {
        // Arrange
        String malformedToken = "malformed";
        
        // Act
        boolean isValid = jwtTokenProvider.validateToken(malformedToken);
        
        // Assert
        assertFalse(isValid);
    }
    
    @Test
    void testValidateToken_EmptyToken_ReturnsFalse() {
        // Arrange
        String emptyToken = "";
        
        // Act
        boolean isValid = jwtTokenProvider.validateToken(emptyToken);
        
        // Assert
        assertFalse(isValid);
    }
    
    @Test
    void testValidateToken_NullToken_ReturnsFalse() {
        // Act
        boolean isValid = jwtTokenProvider.validateToken(null);
        
        // Assert
        assertFalse(isValid);
    }
    
    @Test
    void testGetExpirationDateFromToken_Success() {
        // Arrange
        String username = "testuser";
        List<String> roles = Arrays.asList("ROLE_CLIENT");
        String token = jwtTokenProvider.generateTokenFromUsername(username, roles);
        
        // Act
        Date expirationDate = jwtTokenProvider.getExpirationDateFromToken(token);
        
        // Assert
        assertNotNull(expirationDate);
        assertTrue(expirationDate.after(new Date())); // Should be in the future
    }
    
    @Test
    void testIsTokenExpired_NotExpired_ReturnsFalse() {
        // Arrange
        String username = "testuser";
        List<String> roles = Arrays.asList("ROLE_CLIENT");
        String token = jwtTokenProvider.generateTokenFromUsername(username, roles);
        
        // Act
        boolean isExpired = jwtTokenProvider.isTokenExpired(token);
        
        // Assert
        assertFalse(isExpired);
    }
    
    @Test
    void testIsTokenExpired_ExpiredToken_ReturnsTrue() {
        // Arrange - Create provider with very short expiration
        JwtTokenProvider shortExpirationProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(shortExpirationProvider, "jwtSecret", 
            "testSecretKeyForJwtTokenGenerationAndValidationMustBeLongEnough");
        ReflectionTestUtils.setField(shortExpirationProvider, "jwtExpirationMs", -1000L); // Already expired
        
        String username = "testuser";
        List<String> roles = Arrays.asList("ROLE_CLIENT");
        String token = shortExpirationProvider.generateTokenFromUsername(username, roles);
        
        // Act
        boolean isExpired = shortExpirationProvider.isTokenExpired(token);
        
        // Assert
        assertTrue(isExpired);
    }
    
    @Test
    void testIsTokenExpired_InvalidToken_ReturnsTrue() {
        // Arrange
        String invalidToken = "invalid.token.here";
        
        // Act
        boolean isExpired = jwtTokenProvider.isTokenExpired(invalidToken);
        
        // Assert
        assertTrue(isExpired); // Invalid tokens are treated as expired
    }
    
    @Test
    void testGenerateToken_WithMultipleRoles() {
        // Arrange
        List<GrantedAuthority> authorities = Arrays.asList(
            new SimpleGrantedAuthority("ROLE_CLIENT"),
            new SimpleGrantedAuthority("ROLE_STUDENT")
        );
        
        UserDetails userDetails = User.builder()
                .username("testuser")
                .password("password")
                .authorities(authorities)
                .build();
        
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        
        // Act
        String token = jwtTokenProvider.generateToken(authentication);
        
        // Assert
        assertNotNull(token);
        assertTrue(jwtTokenProvider.validateToken(token));
        assertEquals("testuser", jwtTokenProvider.getUsernameFromToken(token));
    }
    
    @Test
    void testGenerateTokenFromUsername_EmptyRoles() {
        // Arrange
        String username = "testuser";
        List<String> roles = Arrays.asList();
        
        // Act
        String token = jwtTokenProvider.generateTokenFromUsername(username, roles);
        
        // Assert
        assertNotNull(token);
        assertTrue(jwtTokenProvider.validateToken(token));
    }
    
    @Test
    void testTokenRoundTrip_GenerateAndValidate() {
        // Arrange
        String username = "roundtripuser";
        List<String> roles = Arrays.asList("ROLE_CLIENT");
        
        // Act
        String token = jwtTokenProvider.generateTokenFromUsername(username, roles);
        boolean isValid = jwtTokenProvider.validateToken(token);
        String extractedUsername = jwtTokenProvider.getUsernameFromToken(token);
        Date expirationDate = jwtTokenProvider.getExpirationDateFromToken(token);
        boolean isExpired = jwtTokenProvider.isTokenExpired(token);
        
        // Assert
        assertTrue(isValid);
        assertEquals(username, extractedUsername);
        assertNotNull(expirationDate);
        assertFalse(isExpired);
    }
}
