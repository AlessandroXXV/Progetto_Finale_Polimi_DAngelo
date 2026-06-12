package it.eably.backend.dto.common.response;

/**
 * Response returned after a verification document upload.
 * Carries a human-readable status message indicating the outcome of the submission.
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */
public record VerificationDocumentsResponseDTO(
    String message
) {}

