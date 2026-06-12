package it.eably.backend.repository;

import it.eably.backend.model.User;
import it.eably.backend.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for User entity data access.
 * 
 * Provides CRUD operations and custom query methods for User entities.
 * Spring Data JPA automatically implements this interface at runtime.
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * Finds a user by email address.
     * Used for authentication and email uniqueness validation.
     * 
     * @param email the email address to search for
     * @return Optional containing the user if found, empty otherwise
     */
    Optional<User> findByEmail(String email);
    
    /**
     * Finds a user by username.
     * Used for authentication and username uniqueness validation.
     * 
     * @param username the username to search for
     * @return Optional containing the user if found, empty otherwise
     */
    Optional<User> findByUsername(String username);
    
    /**
     * Checks if a user exists with the given email.
     * Useful for registration validation.
     * 
     * @param email the email address to check
     * @return true if a user with this email exists, false otherwise
     */
    boolean existsByEmail(String email);
    
    /**
     * Checks if a user exists with the given username.
     * Useful for registration validation.
     * 
     * @param username the username to check
     * @return true if a user with this username exists, false otherwise
     */
    boolean existsByUsername(String username);

    /**
     * Finds all users matching the given username or email.
     * Returns a list to safely handle edge cases where both fields match different users.
     *
     * @param username the username to check
     * @param email the email to check
     * @return list of users matching username or email (normally 0–2 entries)
     */
    List<User> findByUsernameOrEmail(String username, String email);
    
    /**
     * Finds all users with a specific role.
     * Useful for admin operations.
     * 
     * @param role the role to filter by
     * @return list of users with the specified role
     */
    List<User> findByRole(UserRole role);
    
    /**
     * Finds all active users.
     * Useful for admin operations and reporting.
     * 
     * @param isActive the active status to filter by
     * @return list of users with the specified active status
     */
    List<User> findByIsActive(Boolean isActive);
    
    /**
     * Finds all verified users.
     * Useful for admin operations and reporting.
     * 
     * @param isVerified the verified status to filter by
     * @return list of users with the specified verified status
     */
    List<User> findByIsVerified(Boolean isVerified);
    
    /**
     * Checks if a username exists for a different user (excluding the specified user ID).
     * Used for username uniqueness validation during profile updates.
     * 
     * @param username the username to check
     * @param id the user ID to exclude from the check
     * @return true if another user has this username, false otherwise
     */
    boolean existsByUsernameAndIdNot(String username, Long id);
    
    /**
     * Checks if an email exists for a different user (excluding the specified user ID).
     * Used for email uniqueness validation during profile updates.
     * 
     * @param email the email to check
     * @param id the user ID to exclude from the check
     * @return true if another user has this email, false otherwise
     */
    boolean existsByEmailAndIdNot(String email, Long id);
}
