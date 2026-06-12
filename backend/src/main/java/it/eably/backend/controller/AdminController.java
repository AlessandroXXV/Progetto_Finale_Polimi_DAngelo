package it.eably.backend.controller;

import it.eably.backend.dto.admin.response.AdminStatsResponseDTO;
import it.eably.backend.dto.admin.request.AdminUserActionDTO;
import it.eably.backend.dto.admin.response.AdminUserDetailDTO;
import it.eably.backend.dto.admin.response.AdminUserListDTO;
import it.eably.backend.service.def.AdminManagementService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for platform administration operations.
 * <p>
 * All endpoints require the {@code ADMIN} role.
 * </p>
 *
 * <ul>
 * <li> GET  /api/v1/admin/stats                    – General platform statistics</li>
 * <li> GET  /api/v1/admin/users                    – User list with optional filters</li>
 * <li> GET  /api/v1/admin/users/{userId}           – Details of a single user</li>
 * <li> PUT  /api/v1/admin/users/{userId}/verify    – Verifies a user</li>
 * <li> PUT  /api/v1/admin/users/{userId}/unverify  – Removes verification from a user</li>
 * <li> PUT  /api/v1/admin/users/{userId}/suspend   – Suspends a user</li>
 * <li> PUT  /api/v1/admin/users/{userId}/activate  – Reactivates a suspended user</li>
 * </ul>
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminManagementService adminManagementService;

    /**
     * Builds the controller by injecting the administrative management service.
     *
     * @param adminManagementService the service for admin operations
     */
    public AdminController(AdminManagementService adminManagementService) {
        this.adminManagementService = adminManagementService;
    }

    /**
     * Returns general platform statistics.
     *
     * @return {@link AdminStatsResponseDTO} with aggregated platform data
     */
    @GetMapping("/stats")
    public ResponseEntity<AdminStatsResponseDTO> getStats() {
        return ResponseEntity.ok(adminManagementService.getStats());
    }

    /**
     * Returns the list of users with optional filters.
     *
     * @param role     filters by user role (e.g., {@code CLIENT}, {@code STUDENT})
     * @param verified filters by verification status
     * @param active   filters by activation status
     * @return list of {@link AdminUserListDTO}
     */
    @GetMapping("/users")
    public ResponseEntity<List<AdminUserListDTO>> getUsers(@RequestParam(required = false) String role, @RequestParam(required = false) Boolean verified, @RequestParam(required = false) Boolean active) {
        return ResponseEntity.ok(adminManagementService.getUsers(role, verified, active));
    }

    /**
     * Returns the complete details of a single user.
     *
     * @param userId the user ID
     * @return {@link AdminUserDetailDTO} with the user's data
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<AdminUserDetailDTO> getUserDetails(@PathVariable Long userId) {
        return ResponseEntity.ok(adminManagementService.getUserDetails(userId));
    }

    /**
     * Sets the user's verification status to {@code true}.
     *
     * @param userId the ID of the user to verify
     * @return {@link AdminUserActionDTO} with the outcome of the operation
     */
    @PutMapping("/users/{userId}/verify")
    public ResponseEntity<AdminUserActionDTO> verifyUser(@PathVariable Long userId) {
        return ResponseEntity.ok(adminManagementService.setUserVerified(userId, true));
    }

    /**
     * Removes the user's verification by setting the status to {@code false}.
     *
     * @param userId the ID of the user to unverify
     * @return {@link AdminUserActionDTO} with the outcome of the operation
     */
    @PutMapping("/users/{userId}/unverify")
    public ResponseEntity<AdminUserActionDTO> unverifyUser(@PathVariable Long userId) {
        return ResponseEntity.ok(adminManagementService.setUserVerified(userId, false));
    }

    /**
     * Suspends a user by setting the {@code active} flag to {@code false}.
     *
     * @param userId the ID of the user to suspend
     * @return {@link AdminUserActionDTO} with the outcome of the operation
     */
    @PutMapping("/users/{userId}/suspend")
    public ResponseEntity<AdminUserActionDTO> suspendUser(@PathVariable Long userId) {
        return ResponseEntity.ok(adminManagementService.setUserActive(userId, false));
    }

    /**
     * Reactivates a suspended user by setting the {@code active} flag to {@code true}.
     *
     * @param userId the ID of the user to reactivate
     * @return {@link AdminUserActionDTO} with the outcome of the operation
     */
    @PutMapping("/users/{userId}/activate")
    public ResponseEntity<AdminUserActionDTO> activateUser(@PathVariable Long userId) {
        return ResponseEntity.ok(adminManagementService.setUserActive(userId, true));
    }
}