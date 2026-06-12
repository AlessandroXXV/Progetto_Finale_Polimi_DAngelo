package it.eably.backend.dto.auth.request;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO for user login request.
 * 
 * This record encapsulates the credentials required for authentication.
 * Supports login with either username or email.
 * 
 * @param usernameOrEmail username or email for login
 * @param password plain text password (will be validated against BCrypt hash)
 * 
 * @author Alessandro D'Angelo
 * @version 1.0
 */
public record LoginRequestDTO(
        
        @NotBlank(message = "Username or email is required")
        String usernameOrEmail,
        
        @NotBlank(message = "Password is required")
        String password
) {
}
