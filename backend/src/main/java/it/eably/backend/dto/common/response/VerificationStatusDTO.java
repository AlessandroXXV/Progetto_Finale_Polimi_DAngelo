package it.eably.backend.dto.common.response;

/**
 * DTO for user verification status response.
 * 
 * Used for:
 * - GET /api/v1/verification/status endpoint
 * 
 * Returns whether the user has completed KYC verification.
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */
public record VerificationStatusDTO(
    Boolean verified
) {}
