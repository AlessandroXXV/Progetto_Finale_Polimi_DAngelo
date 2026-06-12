package it.eably.backend.controller;

import it.eably.backend.dto.profile.request.ProfileRequestDTO;
import it.eably.backend.dto.profile.response.ProfilePublicDTO;
import it.eably.backend.dto.profile.response.ProfileResponseDTO;
import it.eably.backend.mapper.ProfileMapper;
import it.eably.backend.model.Profile;
import it.eably.backend.model.User;
import it.eably.backend.service.def.ProfileService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * REST controller for profile (service) operations.
 *
 * Endpoints:
 * <ul>
 * <li>POST   /api/v1/profiles          – Creates a new service (authenticated, max 5 per student)</li>
 * <li>GET    /api/v1/profiles/me       – Returns all services of the authenticated user</li>
 * <li>GET    /api/v1/profiles          – Returns all active services of all students (public)</li>
 * <li>GET    /api/v1/profiles/{id}     – Returns a single service by ID (public)</li>
 * <li>PUT    /api/v1/profiles/{id}     – Updates a service (owner only)</li>
 * <li>DELETE /api/v1/profiles/{id}     – Soft-deletes a service (owner only)</li>
 * </ul>
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */
@RestController
@RequestMapping("/api/v1/profiles")
public class ProfileController {

    private static final Logger log = LoggerFactory.getLogger(ProfileController.class);

    private final ProfileService profileService;
    private final ProfileMapper profileMapper;

    public ProfileController(ProfileService profileService,
                             ProfileMapper profileMapper) {
        this.profileService = profileService;
        this.profileMapper = profileMapper;
    }

    /**
     * Creates a new service for the authenticated user.
     * Limit: maximum {@value ProfileService#MAX_PROFILES_PER_USER} services per student.
     *
     * @param request service data
     * @param user    authenticated user
     * @return created service (201 Created)
     */
    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ProfileResponseDTO> createProfile(
            @Valid @RequestBody ProfileRequestDTO request,
            @AuthenticationPrincipal User user) {

        log.info("POST /api/v1/profiles - User: {}", user.getUsername());
        Long userId = user.getId();
        Profile profile = profileService.createProfile(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(profileMapper.toResponseDTO(profile));
    }

    /**
     * Returns all services of the authenticated user.
     * Useful for the student who wants to view/manage their own services.
     *
     * @param user authenticated user
     * @return list of their own services (200 OK)
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ProfileResponseDTO>> getMyProfiles(
            @AuthenticationPrincipal User user) {
        log.info("GET /api/v1/profiles/me - User: {}", user.getUsername());
        Long userId = user.getId();
        List<Profile> profiles = profileService.getProfilesByUserId(userId);
        return ResponseEntity.ok(profileMapper.toResponseDTOList(profiles));
    }

    /**
     * Returns all active services of all students.
     * Public endpoint used by clients to search for available services.
     *
     * @return list of all active services (200 OK)
     */
    @GetMapping
    public ResponseEntity<List<ProfilePublicDTO>> getAllActiveProfiles(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) BigDecimal maxRate,
            @RequestParam(required = false) Integer dayOfWeek,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        log.info("GET /api/v1/profiles with filters");
        List<Profile> profiles = resolveActiveProfiles(category, maxRate, dayOfWeek, startTime, endTime);
        log.debug("Returning {} active profiles", profiles.size());
        return ResponseEntity.ok(profileMapper.toPublicDTOList(profiles));
    }

    /**
     * Returns all active services for authenticated users.
     * Unlike the public endpoint, it includes complete data (e.g., userId)
     * necessary to load availability and reviews.
     *
     * @param category   filters by service category
     * @param maxRate    filters by maximum hourly rate
     * @param dayOfWeek  filters by day of the week (0=Sunday, 6=Saturday)
     * @param startTime  filters by availability start time (HH:mm format)
     * @param endTime    filters by availability end time (HH:mm format)
     * @return list of {@link ProfileResponseDTO} with complete data (200 OK)
     */
    @GetMapping("/authenticated")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ProfileResponseDTO>> getAllActiveProfilesAuthenticated(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) BigDecimal maxRate,
            @RequestParam(required = false) Integer dayOfWeek,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        log.info("GET /api/v1/profiles/authenticated with filters");
        List<Profile> profiles = resolveActiveProfiles(category, maxRate, dayOfWeek, startTime, endTime);
        log.debug("Returning {} active profiles (authenticated)", profiles.size());
        return ResponseEntity.ok(profileMapper.toResponseDTOList(profiles));
    }

    /**
     * Returns a single service by ID.
     * Public endpoint — returns reduced data without userId.
     *
     * @param id service ID
     * @return the found service (200 OK)
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProfilePublicDTO> getProfileById(@PathVariable Long id) {
        log.info("GET /api/v1/profiles/{}", id);
        Profile profile = profileService.getProfileById(id);
        return ResponseEntity.ok(profileMapper.toPublicDTO(profile));
    }

    /**
     * Returns the full details of a service for authenticated users.
     * Includes userId to display avatars and load availability/reviews.
     * Requires authentication.
     *
     * @param id service ID
     * @return the found service with complete data (200 OK)
     */
    @GetMapping("/{id}/detail")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProfileResponseDTO> getProfileByIdAuthenticated(@PathVariable Long id) {
        log.info("GET /api/v1/profiles/{}/detail", id);
        Profile profile = profileService.getProfileById(id);
        return ResponseEntity.ok(profileMapper.toResponseDTO(profile));
    }

    /**
     * Returns all services of a specific student (public).
     * Useful for displaying the service catalog of a specific student.
     *
     * @param studentId student ID
     * @return list of the student's services (200 OK)
     */
    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<ProfilePublicDTO>> getProfilesByStudent(@PathVariable Long studentId) {
        log.info("GET /api/v1/profiles/student/{}", studentId);
        List<Profile> profiles = profileService.getProfilesByUserId(studentId);
        return ResponseEntity.ok(profileMapper.toPublicDTOList(profiles));
    }

    /**
     * Updates an existing service.
     * Only the owner can update it.
     *
     * @param id      ID of the service to update
     * @param request new service data
     * @param user    authenticated user
     * @return updated service (200 OK)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ProfileResponseDTO> updateProfile(
            @PathVariable Long id,
            @Valid @RequestBody ProfileRequestDTO request,
            @AuthenticationPrincipal User user) {

        log.info("PUT /api/v1/profiles/{} - User: {}", id, user.getUsername());
        Long userId = user.getId();
        Profile profile = profileService.updateProfile(userId, id, request);
        return ResponseEntity.ok(profileMapper.toResponseDTO(profile));
    }

    /**
     * Soft-deletes a service.
     * Only the owner can delete it.
     *
     * @param id   ID of the service to delete
     * @param user authenticated user
     * @return 204 No Content
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Void> deleteProfile(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {

        log.info("DELETE /api/v1/profiles/{} - User: {}", id, user.getUsername());
        Long userId = user.getId();
        profileService.deleteProfile(userId, id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Reactivates a previously deactivated service.
     * Only the owner can reactivate it.
     *
     * @param id   ID of the service to reactivate
     * @param user authenticated user
     * @return 204 No Content
     */
    @PatchMapping("/{id}/reactivate")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Void> reactivateProfile(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {

        log.info("PATCH /api/v1/profiles/{}/reactivate - User: {}", id, user.getUsername());
        Long userId = user.getId();
        profileService.reactivateProfile(userId, id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Resolves the list of active profiles applying filters if present.
     * If no filter is provided, it returns all active profiles.
     *
     * @param category   service category
     * @param maxRate    maximum hourly rate
     * @param dayOfWeek  day of the week
     * @param startTime  start time
     * @param endTime    end time
     * @return filtered or complete list of {@link Profile}
     */
    private List<Profile> resolveActiveProfiles(String category, BigDecimal maxRate,
                                                Integer dayOfWeek, String startTime, String endTime) {
        boolean hasFilters = category != null || maxRate != null
                || dayOfWeek != null || startTime != null || endTime != null;
        return hasFilters
                ? profileService.searchActiveProfiles(category, maxRate, dayOfWeek, startTime, endTime)
                : profileService.getAllActiveProfiles();
    }
}