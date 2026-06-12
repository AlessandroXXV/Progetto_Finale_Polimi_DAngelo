package it.eably.backend.dto.admin.response;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Full user detail for the admin user-detail panel.
 * Aggregates identity fields, booking stats, Stripe status, and associated services
 * in a single response to avoid multiple round-trips from the admin frontend.
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */
public record AdminUserDetailDTO(
    Long id,
    String username,
    String email,
    String fullName,
    String gender,
    String role,
    Boolean isActive,
    Boolean isVerified,
    Boolean stripeConnected,
    String stripeAccountId,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    AdminUserBookingStatsDTO bookingStats,
    AdminUserStripeDTO stripeInfo,
    List<AdminUserServiceDTO> services
) {}

