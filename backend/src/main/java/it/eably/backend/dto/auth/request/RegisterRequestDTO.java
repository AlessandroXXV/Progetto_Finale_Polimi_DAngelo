package it.eably.backend.dto.auth.request;

import it.eably.backend.model.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for user registration request.
 * 
 * This record encapsulates the data required to register a new user.
 * All fields are validated using Jakarta Bean Validation.
 * 
 * @param username unique username (3-20 characters)
 * @param email unique email address
 * @param password plain text password (min 8 characters, will be hashed with BCrypt)
 * @param firstName user's first name
 * @param lastName user's last name
 * @param role user role: CLIENT (default) or STUDENT
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */
public record RegisterRequestDTO(
        
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 20, message = "Username must be between 3 and 20 characters")
        String username,
        
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,
        
        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password,
        
        @NotBlank(message = "First name is required")
        String firstName,
        
        @NotBlank(message = "Last name is required")
        String lastName,

        UserRole role
) {
}
