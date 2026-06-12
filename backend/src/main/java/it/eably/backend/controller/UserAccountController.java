package it.eably.backend.controller;

import it.eably.backend.dto.user.response.UserResponseDTO;
import it.eably.backend.dto.user.request.UserUpdateDTO;
import it.eably.backend.model.User;
import it.eably.backend.service.def.UserAccountService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST controller for user account management operations.
 *
 * Endpoints:
 * <ul>
 * <li>PATCH /api/v1/users/me - Update user profile</li>
 * <li>POST /api/v1/users/me/profile-image - Upload profile image</li>
 * <li>GET /api/v1/users/{user_id}/profile-image - Get profile image</li>
 * <li>DELETE /api/v1/users/me/profile-image - Delete profile image</li>
 * </ul>
 *
 * All endpoints require authentication.
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */
@RestController
@RequestMapping("/api/v1")
public class UserAccountController {
    
    private static final Logger logger = LoggerFactory.getLogger(UserAccountController.class);
    
    private final UserAccountService userAccountService;

    public UserAccountController(UserAccountService userAccountService) {
        this.userAccountService = userAccountService;
    }
    
    /**
     * Updates the authenticated user's profile.
     * Supports partial updates - only provided fields are updated.
     *
     * @param updateDTO the partial update data
     * @param user the authenticated user
     * @return ResponseEntity with updated user data
     */
    @PatchMapping("/users/me")
    public ResponseEntity<UserResponseDTO> updateProfile(
            @Valid @RequestBody UserUpdateDTO updateDTO,
            @AuthenticationPrincipal User user) {
        
        logger.info("PATCH /api/v1/users/me/ - User: {}", user.getUsername());
        
        // Extract user id from authentication
        long userId = user.getId();
        
        // Update profile
        User updatedUser = userAccountService.updateUserProfile(userId, updateDTO);
        
        // Map to response DTO
        UserResponseDTO responseDTO = mapToResponseDTO(updatedUser);
        
        logger.info("Successfully updated profile for user: {}", user.getUsername());
        return ResponseEntity.ok(responseDTO);
    }
    
    /**
     * Uploads a profile image for the authenticated user.
     * Image is stored as Base64 in the database.
     * 
     * @param image the image file (JPG, PNG, WEBP, max 5MB)
     * @param user the authenticated user
     * @return ResponseEntity with 200 OK
     */
    @PostMapping("/users/me/profile-image")
    public ResponseEntity<Void> uploadProfileImage(
            @RequestParam("image") MultipartFile image,
            @AuthenticationPrincipal User user) {
        
        logger.info("POST /api/v1/users/me/profile-image - User: {}", user.getUsername());

        // Extract user id from authentication
        long userId = user.getId();

        
        // Upload image
        userAccountService.uploadProfileImage(userId, image);
        
        logger.info("Successfully uploaded profile image for user: {}", user.getUsername());
        return ResponseEntity.ok().build();
    }
    
    /**
     * Retrieves the profile image for a user.
     * Returns the image bytes with appropriate Content-Type header.
     * 
     * @param userId the ID of the user
     * @return ResponseEntity with image bytes
     */
    @GetMapping("/users/{user_id}/profile-image")
    public ResponseEntity<byte[]> getProfileImage(@PathVariable("user_id") Long userId) {
        
        logger.info("GET /api/v1/users/{}/profile-image", userId);
        
        UserAccountService.ProfileImageData imageData = userAccountService.getProfileImageData(userId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(imageData.contentType()));
        headers.setCacheControl("no-cache, no-store, must-revalidate");
        headers.add("Pragma", "no-cache");
        headers.add("Expires", "0");

        logger.info("Successfully retrieved profile image for user ID: {}", userId);
        return new ResponseEntity<>(imageData.imageBytes(), headers, HttpStatus.OK);
    }
    
    /**
     * Deletes the profile image for the authenticated user.
     * 
     * @param user the authenticated user
     * @return ResponseEntity with 204 No Content
     */
    @DeleteMapping("/users/me/profile-image")
    public ResponseEntity<Void> deleteProfileImage(
            @AuthenticationPrincipal User user) {
        
        logger.info("DELETE /api/v1/users/me/profile-image - User: {}", user.getUsername());

        // Extract user id from authentication
        long userId = user.getId();
        
        // Delete image
        userAccountService.deleteProfileImage(userId);
        
        logger.info("Successfully deleted profile image for user: {}", user.getUsername());
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Maps User entity to UserResponseDTO.
     * Excludes sensitive information like passwordHash.
     * 
     * @param user the User entity
     * @return UserResponseDTO
     */
    private UserResponseDTO mapToResponseDTO(User user) {
        return new UserResponseDTO(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getRole().name(),
            user.getIsActive(),
            user.getIsVerified(),
            user.getStripeConnected(),
            user.getFullName(),
            user.getGender()
        );
    }
}
