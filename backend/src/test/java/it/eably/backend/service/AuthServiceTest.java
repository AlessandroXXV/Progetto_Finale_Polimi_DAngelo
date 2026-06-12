package it.eably.backend.service;

import it.eably.backend.dto.auth.response.AuthResponseDTO;
import it.eably.backend.dto.auth.request.LoginRequestDTO;
import it.eably.backend.dto.auth.request.RegisterRequestDTO;
import it.eably.backend.service.impl.AuthServiceImpl;
import it.eably.backend.exception.ValidationException;
import it.eably.backend.model.User;
import it.eably.backend.model.UserRole;
import it.eably.backend.repository.UserRepository;
import it.eably.backend.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthServiceImpl authService;

    private RegisterRequestDTO registerRequest;
    private LoginRequestDTO loginRequest;
    private User user;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequestDTO(
                "testuser",
                "test@example.com",
                "password123",
                "Test",
                "User",
                null
        );

        loginRequest = new LoginRequestDTO("testuser", "password123");

        user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPasswordHash("$2a$12$hashedpassword");
        user.setRole(UserRole.CLIENT);
        user.setIsActive(true);
        user.setIsVerified(false);
        user.setStripeConnected(false);
    }

    // ========== REGISTRATION TESTS ==========

    @Test
    void register_Success() {
        when(userRepository.findByUsernameOrEmail(anyString(), anyString())).thenReturn(List.of());
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$12$hashedpassword");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(jwtTokenProvider.generateTokenFromUsername(anyString(), anyList())).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(anyString())).thenReturn("refresh-token");

        AuthResponseDTO response = authService.register(registerRequest);

        assertNotNull(response);
        assertEquals("access-token", response.token());
        assertEquals("refresh-token", response.refreshToken());
        assertEquals(1L, response.userId());
        assertEquals("testuser", response.username());
        assertEquals("test@example.com", response.email());
        assertEquals("CLIENT", response.role());
        assertFalse(response.isVerified());

        verify(userRepository).findByUsernameOrEmail("testuser", "test@example.com");
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
        verify(jwtTokenProvider).generateTokenFromUsername(eq("testuser"), anyList());
        verify(jwtTokenProvider).generateRefreshToken("testuser");
    }

    @Test
    void register_EmailAlreadyExists_ThrowsException() {
        when(userRepository.findByUsernameOrEmail(anyString(), anyString())).thenReturn(List.of(user));

        ValidationException exception = assertThrows(ValidationException.class, () ->
                authService.register(registerRequest));

        assertEquals("Email already exists: test@example.com", exception.getMessage());
        verify(userRepository).findByUsernameOrEmail("testuser", "test@example.com");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_UsernameAlreadyExists_ThrowsException() {
        User userWithDifferentEmail = new User();
        userWithDifferentEmail.setUsername("testuser");
        userWithDifferentEmail.setEmail("other@example.com");
        when(userRepository.findByUsernameOrEmail(anyString(), anyString()))
                .thenReturn(List.of(userWithDifferentEmail));

        ValidationException exception = assertThrows(ValidationException.class, () ->
                authService.register(registerRequest));

        assertEquals("Username already exists: testuser", exception.getMessage());
        verify(userRepository).findByUsernameOrEmail("testuser", "test@example.com");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_PasswordIsHashed() {
        when(userRepository.findByUsernameOrEmail(anyString(), anyString())).thenReturn(List.of());
        when(passwordEncoder.encode("password123")).thenReturn("$2a$12$hashedpassword");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(jwtTokenProvider.generateTokenFromUsername(anyString(), anyList())).thenReturn("token");
        when(jwtTokenProvider.generateRefreshToken(anyString())).thenReturn("refresh");

        authService.register(registerRequest);

        verify(passwordEncoder).encode("password123");
    }

    @Test
    void register_DefaultRoleIsClient() {
        when(userRepository.findByUsernameOrEmail(anyString(), anyString())).thenReturn(List.of());
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$12$hashedpassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            assertEquals(UserRole.CLIENT, savedUser.getRole());
            return savedUser;
        });
        when(jwtTokenProvider.generateTokenFromUsername(anyString(), anyList())).thenReturn("token");
        when(jwtTokenProvider.generateRefreshToken(anyString())).thenReturn("refresh");

        authService.register(registerRequest);

        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_UserIsActiveByDefault() {
        when(userRepository.findByUsernameOrEmail(anyString(), anyString())).thenReturn(List.of());
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$12$hashedpassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            assertTrue(savedUser.getIsActive());
            return savedUser;
        });
        when(jwtTokenProvider.generateTokenFromUsername(anyString(), anyList())).thenReturn("token");
        when(jwtTokenProvider.generateRefreshToken(anyString())).thenReturn("refresh");

        authService.register(registerRequest);

        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_UserIsNotVerifiedByDefault() {
        when(userRepository.findByUsernameOrEmail(anyString(), anyString())).thenReturn(List.of());
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$12$hashedpassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            assertFalse(savedUser.getIsVerified());
            return savedUser;
        });
        when(jwtTokenProvider.generateTokenFromUsername(anyString(), anyList())).thenReturn("token");
        when(jwtTokenProvider.generateRefreshToken(anyString())).thenReturn("refresh");

        authService.register(registerRequest);

        verify(userRepository).save(any(User.class));
    }

    // ========== LOGIN TESTS ==========

    @Test
    void login_WithUsername_Success() {
        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(jwtTokenProvider.generateToken(authentication)).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken("testuser")).thenReturn("refresh-token");

        AuthResponseDTO response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("access-token", response.token());
        assertEquals("refresh-token", response.refreshToken());
        assertEquals(1L, response.userId());
        assertEquals("testuser", response.username());
        assertEquals("test@example.com", response.email());
        assertEquals("CLIENT", response.role());
        assertFalse(response.isVerified());

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository).findByUsername("testuser");
        verify(jwtTokenProvider).generateToken(authentication);
        verify(jwtTokenProvider).generateRefreshToken("testuser");
    }

    @Test
    void login_WithEmail_Success() {
        LoginRequestDTO emailLoginRequest = new LoginRequestDTO("test@example.com", "password123");
        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userRepository.findByUsername("test@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(jwtTokenProvider.generateToken(authentication)).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken("testuser")).thenReturn("refresh-token");

        AuthResponseDTO response = authService.login(emailLoginRequest);

        assertNotNull(response);
        assertEquals("access-token", response.token());
        assertEquals("refresh-token", response.refreshToken());

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository).findByUsername("test@example.com");
        verify(userRepository).findByEmail("test@example.com");
    }

    @Test
    void login_UserNotFound_ThrowsException() {
        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        ValidationException exception = assertThrows(ValidationException.class, () ->
                authService.login(loginRequest));

        assertEquals("User not found", exception.getMessage());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void login_AuthenticationCreatesCorrectToken() {
        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenAnswer(invocation -> {
                    UsernamePasswordAuthenticationToken token = invocation.getArgument(0);
                    assertEquals("testuser", token.getPrincipal());
                    assertEquals("password123", token.getCredentials());
                    return authentication;
                });
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(jwtTokenProvider.generateToken(authentication)).thenReturn("token");
        when(jwtTokenProvider.generateRefreshToken("testuser")).thenReturn("refresh");

        authService.login(loginRequest);

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }
}
