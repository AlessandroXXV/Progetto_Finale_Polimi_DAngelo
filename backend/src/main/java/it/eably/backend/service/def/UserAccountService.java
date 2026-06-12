package it.eably.backend.service.def;

import it.eably.backend.dto.user.request.UserUpdateDTO;
import it.eably.backend.model.User;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service interface for user account management operations.
 * <p>
 * Provides business logic for:
 * - Partial profile updates
 * - Profile image upload/download/deletion (Base64 storage)
 * - Verification status checks
 * - Stripe Connect onboarding state updates
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */
public interface UserAccountService
{

    /**
     * Updates user profile with partial data.
     * Only non-null fields in the DTO will be updated.
     *
     * @param userId    the ID of the user to update
     * @param updateDTO the partial update data
     * @return the updated User entity
     */
    User updateUserProfile(Long userId, UserUpdateDTO updateDTO);

    /**
     * Uploads a profile image for the user.
     * Image is encoded as Base64 and stored in the database.
     *
     * @param userId    the ID of the user
     * @param imageFile the image file (JPG, PNG, WEBP, max 5MB)
     */
    void uploadProfileImage(Long userId, MultipartFile imageFile);

    /**
     * Holds the raw bytes and MIME type of a profile image.
     *
     * @param imageBytes  the raw image bytes
     * @param contentType the MIME type of the image
     */
    record ProfileImageData(byte[] imageBytes, String contentType)
    {
    }

    /**
     * Retrieves the profile image bytes and content type in a single DB lookup.
     *
     * @param userId the ID of the user
     * @return ProfileImageData containing raw bytes and MIME type
     */
    ProfileImageData getProfileImageData(Long userId);

    /**
     * Deletes the profile image for a user.
     * Sets profileImageData to null.
     *
     * @param userId the ID of the user
     */
    void deleteProfileImage(Long userId);

    /**
     * Checks if a user is verified (KYC completed).
     *
     * @param userId the ID of the user
     * @return true if user is verified, false otherwise
     */
    boolean isUserVerified(Long userId);

    /**
     * Retrieves a user by id.
     *
     * @param userId the id to look up
     * @return the User entity
     */
    User getUserById(long userId);
}
