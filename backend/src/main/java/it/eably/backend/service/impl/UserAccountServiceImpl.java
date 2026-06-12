package it.eably.backend.service.impl;

import it.eably.backend.dto.user.request.UserUpdateDTO;
import it.eably.backend.exception.ConflictException;
import it.eably.backend.exception.ResourceNotFoundException;
import it.eably.backend.exception.ValidationException;
import it.eably.backend.model.User;
import it.eably.backend.repository.UserRepository;
import it.eably.backend.service.def.UserAccountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.util.Set;

/**
 * Service implementation for user account management.
 *
 * <p>Handles:</p>
 * <ul>
 *   <li>Partial profile updates with uniqueness validation</li>
 *   <li>Profile image upload/download/deletion (Base64 encoding)</li>
 *   <li>Verification status checks</li>
 * </ul>
 *
 * <p>All write operations are transactional.</p>
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */
@Service
public class UserAccountServiceImpl implements UserAccountService {

    /** Logger for user account operations. */
    private static final Logger logger = LoggerFactory.getLogger(UserAccountServiceImpl.class);
    /** Allowed MIME types for profile images. */
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    /** Maximum profile image size in bytes. */
    private static final long MAX_IMAGE_SIZE = 3 * 1024 * 1024; // 3MB

    /** Repository for users. */
    private final UserRepository userRepository;
    /** Encoder for user passwords. */
    private final PasswordEncoder passwordEncoder;

    /**
     * Builds the user account service with required dependencies.
     *
     * @param userRepository repository for users
     * @param passwordEncoder encoder for passwords
     */
    public UserAccountServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Updates editable user profile fields with validation.
     *
     * @param userId user id
     * @param updateDTO update payload
     * @return updated user entity
     * @throws ConflictException when username or email is taken
     * @throws ValidationException when email format is invalid
     * @throws ResourceNotFoundException when user is not found
     */
    @Override
    @Transactional
    public User updateUserProfile(Long userId, UserUpdateDTO updateDTO) {
        logger.debug("Updating profile for user ID: {}", userId);

        // Retrieve user entity
        User user = getUserById(userId);

        // Only apply username change if provided and unique globally.
        if (updateDTO.username() != null && !updateDTO.username().trim().isEmpty()) {
            if (userRepository.existsByUsernameAndIdNot(updateDTO.username(), userId)) {
                throw new ConflictException("Username already taken");
            }
            user.setUsername(updateDTO.username());
            logger.debug("Updated username for user ID: {}", userId);
        }

        // Validate email format and uniqueness across the entire platform.
        if (updateDTO.email() != null && !updateDTO.email().trim().isEmpty()) {
            if (!updateDTO.email().contains("@")) {
                throw new ValidationException("Invalid email format");
            }
            if (userRepository.existsByEmailAndIdNot(updateDTO.email(), userId)) {
                throw new ConflictException("Email already taken");
            }
            user.setEmail(updateDTO.email());
            logger.debug("Updated email for user ID: {}", userId);
        }

        // Use BCrypt bean to securely hash new password strings.
        if (updateDTO.password() != null && !updateDTO.password().trim().isEmpty()) {
            String hashedPassword = passwordEncoder.encode(updateDTO.password());
            user.setPasswordHash(hashedPassword);
            logger.debug("Updated password for user ID: {}", userId);
        }

        // Validate and update other fields
        if (updateDTO.fullName() != null) {
            user.setFullName(updateDTO.fullName());
            logger.debug("Updated full name for user ID: {}", userId);
        }
        if (updateDTO.gender() != null) {
            user.setGender(updateDTO.gender());
            logger.debug("Updated gender for user ID: {}", userId);
        }

        // Save updated user
        User savedUser = userRepository.save(user);
        logger.info("Successfully updated profile for user ID: {}", userId);

        return savedUser;
    }

    /**
     * Uploads a profile image for a user.
     *
     * <p>Effect: stores image as a Base64 data URI.</p>
     *
     * @param userId user id
     * @param imageFile uploaded image file
     * @throws ValidationException when type/size is invalid or file cannot be read
     * @throws ResourceNotFoundException when user is not found
     */
    @Override
    @Transactional
    public void uploadProfileImage(Long userId, MultipartFile imageFile) {
        logger.debug("Uploading profile image for user ID: {}", userId);

        // Validate image type
        String contentType = imageFile.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType)) {
            throw new ValidationException("Invalid image format. Allowed formats: JPG, PNG, WEBP");
        }

        // Check file size before processing to avoid unnecessary memory usage
        if (imageFile.getSize() > MAX_IMAGE_SIZE) {
            throw new ValidationException("Image size exceeds maximum allowed size of 3MB");
        }

        // Retrieve user entity
        User user = getUserById(userId);

        // Convert image to Base64 and store it in the user entity
        try {
            // Encode binary image as Base64 for zero-latency inline data URI serving.
            byte[] imageBytes = imageFile.getBytes();
            String base64String = Base64.getEncoder().encodeToString(imageBytes);
            String dataUri = "data:" + contentType + ";base64," + base64String;
            user.setProfileImageData(dataUri);

            // Save the updated user entity
            userRepository.save(user);
            logger.info("Successfully uploaded profile image for user ID: {}", userId);
        } catch (IOException e) {
            logger.error("Failed to read image file for user ID: {}", userId, e);
            throw new ValidationException("Failed to read image file: " + e.getMessage());
        }
    }

    /**
     * Retrieves profile image data for a user.
     *
     * @param userId user id
     * @return profile image bytes and content type
     * @throws ResourceNotFoundException when no image is present
     * @throws ValidationException when stored data is invalid
     */
    @Override
    @Transactional(readOnly = true)
    public UserAccountService.ProfileImageData getProfileImageData(Long userId) {
        logger.debug("Retrieving profile image for user ID: {}", userId);

        // Retrieve user entity
        User user = getUserById(userId);

        // Resource verification.
        if (user.getProfileImageData() == null || user.getProfileImageData().trim().isEmpty()) {
            throw new ResourceNotFoundException("User has no profile image");
        }

        // Parse the stored Data URI to extract the raw Base64 payload and MIME type.
        String rawData = user.getProfileImageData();
        String contentType = "image/jpeg";
        String base64Data = rawData;

        if (rawData.startsWith("data:")) {
            // Extract MIME type between "data:" and ";"
            int semicolonIndex = rawData.indexOf(';');
            if (semicolonIndex != -1) {
                contentType = rawData.substring(5, semicolonIndex);
            }
            // Extract pure base64 payload after the ","
            int commaIndex = rawData.indexOf(',');
            if (commaIndex != -1) {
                base64Data = rawData.substring(commaIndex + 1);
            }
        }

        try {
            // Decode back to primary binary representation for HTTP response streaming.
            byte[] imageBytes = Base64.getDecoder().decode(base64Data);
            logger.debug("Successfully retrieved profile image for user ID: {}", userId);
            return new UserAccountService.ProfileImageData(imageBytes, contentType);
        } catch (IllegalArgumentException e) {
            logger.error("Failed to decode Base64 image for user ID: {}", userId, e);
            throw new ValidationException("Invalid image data format");
        }
    }

    /**
     * Deletes the profile image for a user.
     *
     * @param userId user id
     * @throws ResourceNotFoundException when no image is present
     */
    @Override
    @Transactional
    public void deleteProfileImage(Long userId) {
        logger.debug("Deleting profile image for user ID: {}", userId);

        // Retrieve user entity
        User user = getUserById(userId);

        // Ensure the user actually has an image before attempting deletion
        if (user.getProfileImageData() == null || user.getProfileImageData().trim().isEmpty()) {
            throw new ResourceNotFoundException("User has no profile image");
        }

        // Clear the stored image data and persist the change
        user.setProfileImageData(null);
        userRepository.save(user);
        logger.info("Successfully deleted profile image for user ID: {}", userId);
    }

    /**
     * Checks whether a user is verified.
     *
     * @param userId user id
     * @return true if user is verified
     */
    @Override
    @Transactional(readOnly = true)
    public boolean isUserVerified(Long userId) {
        logger.debug("Checking verification status for user ID: {}", userId);

        // Retrieve user entity
        User user = getUserById(userId);

        // Treat null as unverified to avoid NullPointerException
        boolean isVerified = user.getIsVerified() != null && user.getIsVerified();
        logger.debug("User ID: {} verification status: {}", userId, isVerified);

        return isVerified;
    }

    /**
     * Loads a user by id or fails.
     *
     * @param id user id
     * @return user entity
     * @throws ValidationException when id is invalid
     * @throws ResourceNotFoundException when user is not found
     */
    @Override
    @Transactional(readOnly = true)
    public User getUserById(long id) {
        if (id <= 0) {
            throw new ValidationException("Invalid user ID: " + id);
        }
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));
    }
}
