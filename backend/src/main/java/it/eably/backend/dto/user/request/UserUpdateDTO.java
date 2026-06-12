package it.eably.backend.dto.user.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * DTO for partial user profile updates.
 * All fields are optional - only provided fields will be updated.
 * 
 * Validation rules:
 * - username: 3-20 characters if provided
 * - email: valid email format if provided
 * - password: minimum 8 characters if provided
 * - fullName: no restrictions
 * - gender: no restrictions
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */
public record UserUpdateDTO(
    @Size(min = 3, max = 20, message = "Username must be between 3 and 20 characters")
    String username,
    
    @Email(message = "Email must be valid")
    String email,
    
    @Size(min = 8, message = "Password must be at least 8 characters")
    String password,
    
    String fullName,
    
    String gender
) {}
