package it.eably.backend.service.def;

import it.eably.backend.dto.admin.request.AdminUserActionDTO;
import it.eably.backend.dto.admin.response.AdminUserDetailDTO;
import it.eably.backend.dto.admin.response.AdminUserListDTO;
import it.eably.backend.dto.admin.response.AdminStatsResponseDTO;

import java.util.List;

/**
 * Service interface for Admin management of users.
 * <p>
 * Provides business logic for:
 * - Retrieving platform-wide statistics
 * - Managing user verification and status
 * - Querying user lists and details
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */
public interface AdminManagementService
{
    /**
     * Retrieves overall platform statistics for the dashboard.
     *
     * @return current user and booking counts
     */
    AdminStatsResponseDTO getStats();

    /**
     * Retrieves a filtered list of users for administration.
     *
     * @param role     optional role filter
     * @param verified optional verification status filter
     * @param active   optional active status filter
     * @return list of enriched user summaries
     */
    List<AdminUserListDTO> getUsers(String role, Boolean verified, Boolean active);

    /**
     * Retrieves deep insights about a specific user.
     *
     * @param userId internal user identifier
     * @return full user profile with statistics and services
     */
    AdminUserDetailDTO getUserDetails(Long userId);

    /**
     * Updates the verification flag for an identity account.
     *
     * @param userId   internal user identifier
     * @param verified target verification status
     * @return summary of the update action
     */
    AdminUserActionDTO setUserVerified(Long userId, boolean verified);

    /**
     * Toggles the active state for a user account.
     *
     * @param userId internal user identifier
     * @param active target active status
     * @return summary of the update action
     */
    AdminUserActionDTO setUserActive(Long userId, boolean active);
}
