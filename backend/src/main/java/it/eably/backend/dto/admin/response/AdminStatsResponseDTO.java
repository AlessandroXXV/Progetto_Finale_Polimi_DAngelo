package it.eably.backend.dto.admin.response;

/**
 * Platform-wide aggregate counters for the admin dashboard summary card.
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */
public record AdminStatsResponseDTO(
    long totalUsers,
    long totalBookings
) {}
