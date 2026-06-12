//    public void setIsPremium(Boolean premium) {
//        isPremium = premium;
//        return profiles;
//    }
//
//    public void setProfiles(List<Profile> profiles) {
//        this.profiles = profiles;
//    }
//    @Column(name = "is_premium", nullable = false)
//    @ColumnDefault("false")
//    public List<Profile> getProfiles() {
//        return profiles;
//    }
//
//    public void setProfiles(List<Profile> profiles) {
//        this.profiles = profiles;
//    }
package it.eably.backend.model;

import it.eably.backend.exception.ValidationException;
import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * User entity representing system users (clients, students, admins).
 * <p>
 * This entity extends BaseEntity and demonstrates:
 * - Inheritance from abstract base class
 * - JPA entity mapping with PostgreSQL
 * - Relationship management (1:1 with Profile)
 * - Business validation logic
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */
@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_user_email", columnList = "email"),
        @Index(name = "idx_user_username", columnList = "username"),
        @Index(name = "idx_user_role", columnList = "role"),
        @Index(name = "idx_user_active", columnList = "is_active")
})
public class User extends BaseEntity implements UserDetails {

    /**
     * User's unique username for identification.
     * Must be between 3 and 20 characters.
     */
    @Column(name = "username", unique = true, nullable = false, length = 20)
    private String username;

    /**
     * User's email address - unique identifier for authentication.
     */
    @Column(name = "email", unique = true, nullable = false, length = 255)
    private String email;

    /**
     * Hashed password using BCrypt.
     * Never store plain text passwords.
     */
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    /**
     * User's role in the system.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private UserRole role;

    /**
     * Flag indicating if the user account is active.
     * Inactive accounts cannot log in.
     */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    /**
     * Flag indicating if the user's email has been verified.
     */
    @Column(name = "is_verified", nullable = false)
    private Boolean isVerified = false;

    /**
     * Flag indicating if the user completed Stripe Connect onboarding.
     */
    @Column(name = "stripe_connected")
    private Boolean stripeConnected = false;

    /**
     * Stripe Connect account ID linked to this user (e.g., acct_...).
     */
    @Column(name = "stripe_account_id", length = 64)
    private String stripeAccountId;

    /**
     * Profile image data stored as Base64 encoded string.
     * Format: "data:image/jpeg;base64,..." or just the Base64 string.
     * Maximum size: 5MB (before encoding).
     */
    @Column(name = "profile_image_data", columnDefinition = "TEXT")
    private String profileImageData;

    /**
     * User's full name (optional).
     */
    @Column(name = "full_name", length = 100)
    private String fullName;

    /**
     * User's gender (optional).
     */
    @Column(name = "gender", length = 20)
    private String gender;

    /**
     * One-to-many relationship with Profile entity.
     * A student can have up to 5 service profiles.
     */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<Profile> profiles = new ArrayList<>();

    /**
     * Flag indicating if the user has a premium subscription.
     */
    @Column(name = "is_premium", nullable = false)
    @ColumnDefault("false")
    private Boolean isPremium = false;

    // Constructors

    public User()
    {
    }

    public User(String username, String email, String passwordHash, UserRole role, Boolean isActive, Boolean isVerified)
    {
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.isActive = isActive;
        this.isVerified = isVerified;
    }

    public User(String username, String email, String passwordHash, UserRole role, Boolean isActive, Boolean isVerified,
                String profileImageData, String fullName, String gender)
    {
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.isActive = isActive;
        this.isVerified = isVerified;
        this.profileImageData = profileImageData;
        this.fullName = fullName;
        this.gender = gender;
    }

    // Getters and Setters

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities()
    {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public @Nullable String getPassword()
    {
        return passwordHash;
    }

    public String getUsername()
    {
        return username;
    }

    public void setUsername(String username)
    {
        this.username = username;
    }

    public String getEmail()
    {
        return email;
    }

    public void setEmail(String email)
    {
        this.email = email;
    }

    public String getPasswordHash()
    {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash)
    {
        this.passwordHash = passwordHash;
    }

    public UserRole getRole()
    {
        return role;
    }

    public void setRole(UserRole role)
    {
        this.role = role;
    }

    public Boolean getIsActive()
    {
        return isActive;
    }

    public void setIsActive(Boolean isActive)
    {
        this.isActive = isActive;
    }

    public Boolean getIsVerified()
    {
        return isVerified;
    }

    public void setIsVerified(Boolean isVerified)
    {
        this.isVerified = isVerified;
    }

    public Boolean getStripeConnected()
    {
        return stripeConnected;
    }

    public void setStripeConnected(Boolean stripeConnected)
    {
        this.stripeConnected = stripeConnected;
    }

    public String getStripeAccountId()
    {
        return stripeAccountId;
    }

    public void setStripeAccountId(String stripeAccountId)
    {
        this.stripeAccountId = stripeAccountId;
    }

    public String getProfileImageData()
    {
        return profileImageData;
    }

    public void setProfileImageData(String profileImageData)
    {
        this.profileImageData = profileImageData;
    }

    public String getFullName()
    {
        return fullName;
    }

    public void setFullName(String fullName)
    {
        this.fullName = fullName;
    }

    public String getGender()
    {
        return gender;
    }

    public void setGender(String gender)
    {
        this.gender = gender;
    }

    public List<Profile> getProfiles()
    {
        return profiles;
    }

    public void setProfiles(List<Profile> profiles)
    {
        this.profiles = profiles;
    }

    public Boolean getIsPremium()
    {
        return isPremium != null ? isPremium : false;
    }

    public void setIsPremium(Boolean premium)
    {
        isPremium = premium;
    }

    /**
     * Validates the User entity according to business rules.
     * <p>
     * Validation Rules:
     * - Username must not be null and must be between 3 and 20 characters
     * - Email must not be null and must contain '@'
     * - Password hash must not be null and must be at least 8 characters
     * - Role must not be null
     * - isActive and isVerified must not be null
     * <p>
     *
     * @throws ValidationException if validation fails
     */
    @Override
    public void validate()
    {
        if (username == null || username.trim().isEmpty()) {
            throw new ValidationException("Username cannot be null or empty");
        }

        if (username.length() < 3 || username.length() > 20) {
            throw new ValidationException("Username must be between 3 and 20 characters");
        }

        if (email == null || email.trim().isEmpty()) {
            throw new ValidationException("Email cannot be null or empty");
        }

        if (!email.contains("@")) {
            throw new ValidationException("Email must be a valid email address");
        }

        if (passwordHash == null || passwordHash.length() < 8) {
            throw new ValidationException("Password hash must be at least 8 characters");
        }

        if (role == null) {
            throw new ValidationException("User role cannot be null");
        }

        if (isActive == null) {
            throw new ValidationException("isActive flag cannot be null");
        }

        if (isVerified == null) {
            throw new ValidationException("isVerified flag cannot be null");
        }

        if (isPremium == null) {
            throw new ValidationException("isPremium flag cannot be null");
        }
    }

    /**
     * Checks if the user has a specific role.
     *
     * @param role the role to check
     * @return true if user has the specified role
     */
    public boolean hasRole(UserRole role)
    {
        return this.role == role;
    }

    /**
     * Checks if the user is an admin.
     *
     * @return true if user has ADMIN role
     */
    public boolean isAdmin()
    {
        return this.role == UserRole.ADMIN;
    }

    /**
     * Checks if the user is a student (service provider).
     *
     * @return true if user has STUDENT role
     */
    public boolean isStudent()
    {
        return this.role == UserRole.STUDENT;
    }

    /**
     * Checks if the user is a client.
     *
     * @return true if user has CLIENT role
     */
    public boolean isClient()
    {
        return this.role == UserRole.CLIENT;
    }

    @Override
    public boolean isEnabled()
    {
        return isActive != null && isActive;
    }

    @Override
    public boolean isAccountNonLocked()
    {
        return isActive != null && isActive;
    }
}
