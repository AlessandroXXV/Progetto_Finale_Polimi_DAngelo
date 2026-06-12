package it.eably.backend.dto.user.response;

/**
 * DTO for user profile responses.
 * Excludes sensitive information like passwordHash.
 * 
 * Used for:
 * - GET /api/v1/users/me/ responses
 * - PATCH /api/v1/users/me/ responses
 * - Any endpoint returning user data
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */
public record UserResponseDTO(
    Long id,
    String username,
    String email,
    String role,
    Boolean isActive,
    Boolean isVerified,
    Boolean stripeConnected,
    String fullName,
    String gender
) {
    public UserResponseDTO(Long id,
                          String username,
                          String email,
                          String role,
                          Boolean isActive,
                          Boolean isVerified,
                          String fullName,
                          String gender) {
        this(id, username, email, role, isActive, isVerified, false, fullName, gender);
    }
}
