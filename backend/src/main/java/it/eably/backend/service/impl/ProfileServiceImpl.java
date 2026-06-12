package it.eably.backend.service.impl;

import it.eably.backend.dto.profile.request.ProfileRequestDTO;
import it.eably.backend.exception.ResourceNotFoundException;
import it.eably.backend.exception.ValidationException;
import it.eably.backend.model.Profile;
import it.eably.backend.model.SlotStatus;
import it.eably.backend.model.User;
import it.eably.backend.model.UserRole;
import it.eably.backend.repository.ProfileRepository;
import it.eably.backend.repository.UserRepository;
import it.eably.backend.service.def.ProfileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Service implementation for profile management operations.
 *
 * <p>A student can own up to {@value ProfileService#MAX_PROFILES_PER_USER} service profiles.</p>
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */
@Service
public class ProfileServiceImpl implements ProfileService {

    /** Logger for profile operations. */
    private static final Logger log = LoggerFactory.getLogger(ProfileServiceImpl.class);

    /** Repository for profiles. */
    private final ProfileRepository profileRepository;
    /** Repository for users. */
    private final UserRepository userRepository;

    /**
     * Builds the profile service with required dependencies.
     *
     * @param profileRepository repository for profiles
     * @param userRepository repository for users
     */
    public ProfileServiceImpl(ProfileRepository profileRepository, UserRepository userRepository) {
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
    }

    /**
     * Creates a new profile for a student user.
     *
     * <p>Effect: validates ownership and limits, then persists the profile.</p>
     *
     * @param userId user id
     * @param request profile request data
     * @return created profile
     * @throws ResourceNotFoundException when the user is not found
     * @throws ValidationException when user is not a student, Stripe is not connected,
     *                             title already exists, or max profiles reached
     */
    @Override
    @Transactional
    public Profile createProfile(Long userId, ProfileRequestDTO request) {
        log.info("Creating profile for user ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        // Only STUDENT users are allowed to advertise services.
        if (!user.getRole().equals(UserRole.STUDENT)) {
            throw new ValidationException("Only students can create services");
        }

        // Hard requirement: must have Stripe ready to receive payments before creating service profiles.
        if (!Boolean.TRUE.equals(user.getStripeConnected())) {
            throw new ValidationException("Complete Stripe onboarding before creating services");
        }

        // Prevent duplicate service titles for the same user to avoid UI confusion.
        boolean exists = profileRepository.findAllByUserId(userId).stream()
                .anyMatch(p -> p.getTitle().equalsIgnoreCase(request.title()));
        if (exists) {
            throw new ValidationException("You already have a service with the title: " + request.title());
        }

        // Enforce the business limit on the number of active profiles.
        long activeProfilesCount = profileRepository.countByUserIdAndIsActiveTrue(userId);
        if (activeProfilesCount >= MAX_PROFILES_PER_USER) {
            log.warn("Service creation failed: user {} already has {} active services", userId, activeProfilesCount);
            throw new ValidationException(
                    "Maximum number of profiles reached (" + MAX_PROFILES_PER_USER +
                    "). Please delete an existing service before creating a new one.");
        }

        // Initialize and persist the profile entity.
        Profile profile = new Profile();
        profile.setUser(user);
        profile.setTitle(request.title());
        profile.setDescription(request.description());
        profile.setHourlyRate(request.hourlyRate());
        profile.setDeliveryMode(request.deliveryMode());
        profile.setAddress(request.address());
        profile.setIsActive(true);

        profile.validate();
        Profile savedProfile = profileRepository.save(profile);
        log.info("Profile created successfully with ID: {}", savedProfile.getId());
        return savedProfile;
    }

    /**
     * Updates a profile owned by the given user.
     *
     * @param userId user id
     * @param profileId profile id
     * @param request profile request data
     * @return updated profile
     * @throws ResourceNotFoundException when the profile is not found
     * @throws ValidationException when the user is not authorized
     */
    @Override
    @Transactional
    public Profile updateProfile(Long userId, Long profileId, ProfileRequestDTO request) {
        log.info("Updating profile ID: {} for user ID: {}", profileId, userId);

        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found with ID: " + profileId));

        // Ensure the user is authorized to update the profile.
        if (!profile.getUser().getId().equals(userId)) {
            throw new ValidationException("You are not authorized to update this profile");
        }

        // Update the profile fields.
        profile.setTitle(request.title());
        profile.setDescription(request.description());
        profile.setHourlyRate(request.hourlyRate());
        profile.setDeliveryMode(request.deliveryMode());
        profile.setAddress(request.address());

        // Update the profile in the database.
        profile.validate();
        Profile updatedProfile = profileRepository.save(profile);
        log.info("Profile updated successfully with ID: {}", updatedProfile.getId());
        return updatedProfile;
    }

    /**
     * Retrieves a profile by id.
     *
     * @param profileId profile id
     * @return profile entity
     * @throws ResourceNotFoundException when the profile is not found
     */
    @Override
    @Transactional(readOnly = true)
    public Profile getProfileById(Long profileId) {
        log.info("Fetching profile with ID: {}", profileId);
        return profileRepository.findById(profileId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found with ID: " + profileId));
    }

    /**
     * Retrieves all profiles for a user.
     *
     * @param userId user id
     * @return list of profiles
     */
    @Override
    @Transactional(readOnly = true)
    public List<Profile> getProfilesByUserId(Long userId) {
        log.info("Fetching profiles for user ID: {}", userId);
        return profileRepository.findAllByUserId(userId);
    }

    /**
     * Retrieves all active profiles.
     *
     * @return list of active profiles
     */
    @Override
    @Transactional(readOnly = true)
    public List<Profile> getAllActiveProfiles() {
        log.info("Fetching all active profiles");
        List<Profile> profiles = profileRepository.findByIsActiveTrue();
        log.debug("Found {} active profiles", profiles.size());
        return profiles;
    }

    /**
     * Searches active profiles with optional availability filters.
     *
     * @param category optional category filter
     * @param maxRate optional maximum hourly rate
     * @param dayOfWeek optional day of week (0..6)
     * @param startTime optional start time (HH:mm)
     * @param endTime optional end time (HH:mm)
     * @return list of matching profiles
     * @throws ValidationException when date/time filters are invalid
     */
    @Override
    @Transactional(readOnly = true)
    public List<Profile> searchActiveProfiles(String category,
                                              BigDecimal maxRate,
                                              Integer dayOfWeek,
                                              String startTime,
                                              String endTime) {
        // Parse time strings and weekday integers to domain types or nulls.
        DayOfWeek parsedDay = dayOfWeek == null ? null : convertIntToDayOfWeek(dayOfWeek);
        LocalTime parsedStart = parseTimeOrNull(startTime, "startTime");
        LocalTime parsedEnd = parseTimeOrNull(endTime, "endTime");

        // Logic verification of the search time range.
        if (parsedStart != null && parsedEnd != null && !parsedEnd.isAfter(parsedStart)) {
            throw new ValidationException("endTime must be after startTime");
        }

        // Normalize inputs for JPA/Hibernate mapping.
        String normalizedCategory = normalizeBlank(category);
        boolean useCategory = normalizedCategory != null;
        boolean useMaxRate = maxRate != null;
        boolean useDayOfWeek = parsedDay != null;
        boolean useStartTime = parsedStart != null;
        boolean useEndTime = parsedEnd != null;

        // Complex search logic: only trigger availability JOINs if specific time filters are requested.
        boolean useAvailabilityFilters = useDayOfWeek || useStartTime || useEndTime;

        log.info("Searching active profiles with filters: category={}, maxRate={}, dayOfWeek={}, startTime={}, endTime={}",
                normalizedCategory, maxRate, dayOfWeek, startTime, endTime);

        return profileRepository.searchActiveProfiles(
                useCategory,
                normalizedCategory,
                useMaxRate,
                maxRate,
                useAvailabilityFilters,
                useDayOfWeek,
                parsedDay,
                useStartTime,
                parsedStart,
                useEndTime,
                parsedEnd,
                SlotStatus.AVAILABLE
        );
    }

    /**
     * Soft-deletes a profile owned by the given user.
     *
     * @param userId user id
     * @param profileId profile id
     * @throws ResourceNotFoundException when the profile is not found
     * @throws ValidationException when the user is not authorized
     */
    @Override
    @Transactional
    public void deleteProfile(Long userId, Long profileId) {
        log.info("Soft deleting profile ID: {} for user ID: {}", profileId, userId);

        // Retrieve the profile
        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found with ID: " + profileId));

        // Ensure the user is authorized to delete the profile.
        if (!profile.getUser().getId().equals(userId)) {
            throw new ValidationException("You are not authorized to delete this profile");
        }

        // Soft-delete the profile.
        profile.softDelete();
        profileRepository.save(profile);
        log.info("Profile soft deleted successfully with ID: {}", profile.getId());
    }

    /**
     * Reactivates a previously soft-deleted profile for the given user.
     *
     * @param userId user id
     * @param profileId profile id
     * @throws ResourceNotFoundException when the profile is not found
     * @throws ValidationException when the user is not authorized
     */
    @Override
    @Transactional
    public void reactivateProfile(Long userId, Long profileId) {
        log.info("Reactivating profile ID: {} for user ID: {}", profileId, userId);

        // Retrieve the profile
        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found with ID: " + profileId));

        // Ensure the user is authorized to reactivate the profile.
        if (!profile.getUser().getId().equals(userId)) {
            throw new ValidationException("You are not authorized to reactivate this profile");
        }

        profile.reactivate();
        profileRepository.save(profile);
        log.info("Profile reactivated successfully with ID: {}", profile.getId());
    }

    /**
     * Normalizes blank strings to null.
     *
     * @param value input string
     * @return trimmed string or null
     */
    private String normalizeBlank(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    /**
     * Parses a time string or returns null when blank.
     *
     * @param value time string
     * @param fieldName field label for errors
     * @return parsed LocalTime or null
     * @throws ValidationException when format is invalid
     */
    private LocalTime parseTimeOrNull(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalTime.parse(value.trim());
        } catch (DateTimeParseException ex) {
            throw new ValidationException(fieldName + " must be in HH:mm format");
        }
    }

    /**
     * Converts an integer day of week to DayOfWeek enum.
     *
     * @param dayInt day of week (0=Monday..6=Sunday)
     * @return DayOfWeek enum value
     * @throws ValidationException when dayInt is outside 0..6
     */
    private DayOfWeek convertIntToDayOfWeek(Integer dayInt) {
        return switch (dayInt) {
            case 0 -> DayOfWeek.MONDAY;
            case 1 -> DayOfWeek.TUESDAY;
            case 2 -> DayOfWeek.WEDNESDAY;
            case 3 -> DayOfWeek.THURSDAY;
            case 4 -> DayOfWeek.FRIDAY;
            case 5 -> DayOfWeek.SATURDAY;
            case 6 -> DayOfWeek.SUNDAY;
            default -> throw new ValidationException("Invalid dayOfWeek: " + dayInt + " (expected 0..6)");
        };
    }
}
