package it.eably.backend.dto.admin.response;

/**
 * Summary row for the admin user list view.
 * Contains just enough fields to render the table and filter/sort without loading
 * full user details for every row.
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */
public record AdminUserListDTO(
    Long id,
    String username,
    String email,
    String fullName,
    String role,
    Boolean isActive,
    Boolean isVerified,
    Boolean stripeConnected,
    long profileCount,
    long bookingAsClientCount,
    long bookingAsProviderCount
) {}

