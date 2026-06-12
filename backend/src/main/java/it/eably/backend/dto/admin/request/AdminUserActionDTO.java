package it.eably.backend.dto.admin.request;

/**
 * Moderation action applied to a user by an admin.
 * Either or both flags may be set in a single request to support combined
 * "verify and activate" operations without two round-trips.
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */
public record AdminUserActionDTO(
    Long id,
    Boolean isActive,
    Boolean isVerified
) {}

