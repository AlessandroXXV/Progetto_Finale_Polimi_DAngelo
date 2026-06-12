package it.eably.backend.service.def;

import it.eably.backend.dto.profile.request.ProfileRequestDTO;
import it.eably.backend.model.Profile;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service interface for profile management operations.
 * <p>
 * A student can own up to 5 service profiles (MAX_PROFILES_PER_USER).
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */
public interface ProfileService
{

    int MAX_PROFILES_PER_USER = 5;

    /**
     * Creates a new profile for a user (max {@value #MAX_PROFILES_PER_USER}).
     *
     * @param userId  the user ID
     * @param request profile request DTO
     * @return created profile entity
     * @throws it.eably.backend.exception.ValidationException if user not found or limit reached
     */
    Profile createProfile(Long userId, ProfileRequestDTO request);

    /**
     * Updates an existing profile.
     * Only the owner can update the profile.
     *
     * @param userId    the authenticated user ID (for ownership check)
     * @param profileId the profile ID to update
     * @param request   profile request DTO
     * @return updated profile entity
     * @throws it.eably.backend.exception.ValidationException if profile not found or user doesn't own it
     */
    Profile updateProfile(Long userId, Long profileId, ProfileRequestDTO request);

    /**
     * Retrieves a profile by its ID.
     *
     * @param profileId the profile ID
     * @return profile entity
     * @throws it.eably.backend.exception.ValidationException if profile not found
     */
    Profile getProfileById(Long profileId);

    /**
     * Retrieves all profiles belonging to a user.
     *
     * @param userId the user ID
     * @return list of profiles
     */
    List<Profile> getProfilesByUserId(Long userId);

    /**
     * Retrieves all active profiles (all students).
     *
     * @return list of active profiles
     */
    List<Profile> getAllActiveProfiles();

    /**
     * Retrieves active profiles with optional filters.
     *
     * @param category  text matched against profile description (case-insensitive)
     * @param maxRate   optional maximum hourly rate
     * @param dayOfWeek optional day filter (0=Monday..6=Sunday)
     * @param startTime optional start time filter (HH:mm)
     * @param endTime   optional end time filter (HH:mm)
     * @return list of active profiles matching filters
     */
    List<Profile> searchActiveProfiles(String category,
                                       BigDecimal maxRate,
                                       Integer dayOfWeek,
                                       String startTime,
                                       String endTime);

    /**
     * Soft deletes a profile by its ID.
     * Only the owner can delete their profile.
     *
     * @param userId    the authenticated user ID (for ownership check)
     * @param profileId the profile ID to delete
     * @throws it.eably.backend.exception.ValidationException if profile not found or user doesn't own it
     */
    void deleteProfile(Long userId, Long profileId);

    /**
     * Reactivates a soft-deleted profile.
     * Only the owner can reactivate their profile.
     *
     * @param userId    the authenticated user ID
     * @param profileId the profile ID to reactivate
     * @throws it.eably.backend.exception.ValidationException if profile not found or user doesn't own it
     */
    void reactivateProfile(Long userId, Long profileId);
}
