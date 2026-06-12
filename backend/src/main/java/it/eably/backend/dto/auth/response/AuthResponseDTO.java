package it.eably.backend.dto.auth.response;

/**
 * DTO for authentication response.
 * 
 * This record encapsulates the JWT token and user information
 * returned after successful login or registration.
 * 
 * @param token JWT access token
 * @param refreshToken JWT refresh token (longer expiration)
 * @param userId user ID
 * @param username username
 * @param email email address
 * @param role user role (CLIENT, STUDENT, ADMIN)
 * @param stripeConnected whether Stripe Connect onboarding is completed
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */
public record AuthResponseDTO(
        String token,
        String refreshToken,
        Long userId,
        String username,
        String email,
        String role,
        Boolean stripeConnected,
        Boolean isVerified
) {}
