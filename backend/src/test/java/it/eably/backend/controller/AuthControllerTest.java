package it.eably.backend.controller;

import it.eably.backend.dto.auth.response.AuthResponseDTO;
import it.eably.backend.dto.auth.request.LoginRequestDTO;
import it.eably.backend.dto.auth.request.RegisterRequestDTO;
import it.eably.backend.exception.ConflictException;
import it.eably.backend.exception.ValidationException;
import it.eably.backend.service.def.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthController.
 * 
 * Tests all endpoints by directly calling controller methods.
 * Target: 100% controller coverage.
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {
    
    @Mock
    private AuthService authService;
    
    @InjectMocks
    private AuthController authController;
    
    @Test
    void testRegister_Success() {
        RegisterRequestDTO requestDTO = new RegisterRequestDTO(
            "testuser", "test@test.com", "password123", "John", "Doe", null
        );
        AuthResponseDTO responseDTO = new AuthResponseDTO(
            "jwt-token", "refresh-token", 1L, "testuser", "test@test.com", "CLIENT", false, false
        );
        
        when(authService.register(any(RegisterRequestDTO.class))).thenReturn(responseDTO);
        
        ResponseEntity<AuthResponseDTO> response = authController.register(requestDTO);
        
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("testuser", response.getBody().username());
        assertEquals("jwt-token", response.getBody().token());
        
        verify(authService).register(requestDTO);
    }
    
    @Test
    void testRegister_UsernameTaken_ThrowsConflictException() {
        RegisterRequestDTO requestDTO = new RegisterRequestDTO(
            "existinguser", "test@test.com", "password123", "John", "Doe", null
        );
        
        when(authService.register(any(RegisterRequestDTO.class)))
            .thenThrow(new ConflictException("Username already exists"));
        
        assertThrows(ConflictException.class, () -> {
            authController.register(requestDTO);
        });
    }
    
    @Test
    void testRegister_EmailTaken_ThrowsConflictException() {
        RegisterRequestDTO requestDTO = new RegisterRequestDTO(
            "testuser", "existing@test.com", "password123", "John", "Doe", null
        );
        
        when(authService.register(any(RegisterRequestDTO.class)))
            .thenThrow(new ConflictException("Email already exists"));
        
        assertThrows(ConflictException.class, () -> {
            authController.register(requestDTO);
        });
    }
    
    @Test
    void testLogin_Success() {
        LoginRequestDTO requestDTO = new LoginRequestDTO("testuser", "password123");
        AuthResponseDTO responseDTO = new AuthResponseDTO(
            "jwt-token", "refresh-token", 1L, "testuser", "test@test.com", "CLIENT", false, false
        );
        
        when(authService.login(any(LoginRequestDTO.class))).thenReturn(responseDTO);
        
        ResponseEntity<AuthResponseDTO> response = authController.login(requestDTO);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("testuser", response.getBody().username());
        assertEquals("jwt-token", response.getBody().token());
        
        verify(authService).login(requestDTO);
    }
    
    @Test
    void testLogin_InvalidCredentials_ThrowsValidationException() {
        LoginRequestDTO requestDTO = new LoginRequestDTO("testuser", "wrongpassword");
        
        when(authService.login(any(LoginRequestDTO.class)))
            .thenThrow(new ValidationException("Invalid credentials"));
        
        assertThrows(ValidationException.class, () -> {
            authController.login(requestDTO);
        });
    }
    
    @Test
    void testLogin_UserNotFound_ThrowsValidationException() {
        LoginRequestDTO requestDTO = new LoginRequestDTO("nonexistent", "password123");
        
        when(authService.login(any(LoginRequestDTO.class)))
            .thenThrow(new ValidationException("User not found"));
        
        assertThrows(ValidationException.class, () -> {
            authController.login(requestDTO);
        });
    }
}
