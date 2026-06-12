package it.eably.backend.service.def;

import it.eably.backend.dto.auth.response.AuthResponseDTO;
import it.eably.backend.dto.auth.request.LoginRequestDTO;
import it.eably.backend.dto.auth.request.RegisterRequestDTO;

/**
 * Service interface for authentication operations.
 * <p>
 * Provides business logic for:
 * - User registration with BCrypt password hashing
 * - User authentication with JWT token generation
 * - Email and username uniqueness validation
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */
public interface AuthService
{

    /**
     * Registers a new user.
     *
     * @param request registration request DTO
     * @return authentication response with JWT tokens
     * @throws it.eably.backend.exception.ValidationException if email or username already exists
     */
    AuthResponseDTO register(RegisterRequestDTO request);

    /**
     * Authenticates a user and generates JWT tokens.
     *
     * @param request login request DTO
     * @return authentication response with JWT tokens
     * @throws it.eably.backend.exception.ValidationException if authentication fails
     */
    AuthResponseDTO login(LoginRequestDTO request);
}
