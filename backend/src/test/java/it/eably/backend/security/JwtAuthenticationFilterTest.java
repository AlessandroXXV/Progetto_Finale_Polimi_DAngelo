package it.eably.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import it.eably.backend.model.User;
import it.eably.backend.model.UserRole;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for JwtAuthenticationFilter.
 *
 * Tests cover:
 * - Valid Bearer token → authentication set in SecurityContext
 * - No Authorization header → filter chain continues without auth
 * - Invalid token (validateToken returns false) → continues without auth
 * - Exception during processing → caught, filter chain continues
 * - Token extraction from Authorization header
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private UserDetailsService userDetailsService;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain filterChain;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = new MockFilterChain();
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilter_ValidBearerToken_SetsAuthentication() throws ServletException, IOException {
        // Arrange
        String token = "valid.jwt.token";
        String username = "testuser";
        request.addHeader("Authorization", "Bearer " + token);

        User userDetails = new User();
        userDetails.setUsername(username);
        userDetails.setRole(UserRole.CLIENT);

        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(jwtTokenProvider.getUsernameFromToken(token)).thenReturn(username);
        when(userDetailsService.loadUserByUsername(username)).thenReturn(userDetails);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(username, SecurityContextHolder.getContext().getAuthentication().getName());
    }

    @Test
    void doFilter_NoAuthorizationHeader_ContinuesWithoutAuth() throws ServletException, IOException {
        // No Authorization header set

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Filter chain should continue
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(jwtTokenProvider, never()).validateToken(any());
    }

    @Test
    void doFilter_InvalidToken_ContinuesWithoutAuth() throws ServletException, IOException {
        String token = "invalid.jwt.token";
        request.addHeader("Authorization", "Bearer " + token);

        when(jwtTokenProvider.validateToken(token)).thenReturn(false);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(userDetailsService, never()).loadUserByUsername(any());
    }

    @Test
    void doFilter_ExceptionDuringProcessing_ContinuesFilterChain() throws ServletException, IOException {
        String token = "some.jwt.token";
        request.addHeader("Authorization", "Bearer " + token);

        when(jwtTokenProvider.validateToken(token)).thenThrow(new RuntimeException("JWT parse error"));

        // Should not throw, exception is caught internally
        assertDoesNotThrow(() ->
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain));
    }

    @Test
    void doFilter_BearerTokenWithWrongPrefix_ContinuesWithoutAuth() throws ServletException, IOException {
        // "Token " prefix instead of "Bearer "
        request.addHeader("Authorization", "Token some.jwt.token");

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(jwtTokenProvider, never()).validateToken(any());
    }

    @Test
    void doFilter_EmptyAuthorizationHeader_ContinuesWithoutAuth() throws ServletException, IOException {
        request.addHeader("Authorization", "");

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(jwtTokenProvider, never()).validateToken(any());
    }
}
