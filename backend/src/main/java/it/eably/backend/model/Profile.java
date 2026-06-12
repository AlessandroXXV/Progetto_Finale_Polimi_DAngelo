package it.eably.backend.model;

import it.eably.backend.exception.ValidationException;
import jakarta.persistence.*;

import java.math.BigDecimal;

/**
 * Profile entity representing professional user profiles (Services).
 * * This entity:
 * - Extends BaseEntity for common fields
 * - Has M:1 relationship with User (A student can have up to 5 services)
 * - Contains professional service information
 *
 * @author Alessandro D'Angelo
 * @version 1.1
 */
@Entity
@Table(name = "profiles", indexes = {
        @Index(name = "idx_profile_user_id", columnList = "user_id"),
        @Index(name = "idx_profile_active", columnList = "is_active")
})
public class Profile extends BaseEntity {

    /**
     * Many-to-one relationship with User entity.
     * A student can have up to 5 profiles (services).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Title of the service offered (e.g. "Java tutoring"). */
    @Column(name = "title", nullable = false, length = 255)
    private String title;

    /** Optional detailed description of the service. */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** Hourly rate charged for this service (must be positive). */
    @Column(name = "hourly_rate", precision = 10, scale = 2, nullable = false)
    private BigDecimal hourlyRate;

    /** Delivery mode of the service (online, in-person, or hybrid). */
    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_mode", nullable = false, length = 20)
    private DeliveryMode deliveryMode;

    /** Physical address required when {@code deliveryMode} is {@link DeliveryMode#IN_PERSON} or {@link DeliveryMode#HYBRID}. */
    @Column(name = "address", length = 500)
    private String address;

    /** Whether this profile is currently active and visible to clients. Defaults to {@code true}. */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    /** Default no-arg constructor required by JPA. */
    public Profile() {}

    /**
     * Full constructor for creating a new profile.
     *
     * @param user          the student who owns this profile
     * @param title         the service title
     * @param description   optional service description
     * @param hourlyRate    the hourly rate charged
     * @param deliveryMode  the delivery mode
     * @param address       physical address (required for in-person/hybrid modes)
     * @param isActive      whether the profile is active
     */
    public Profile(User user, String title, String description, BigDecimal hourlyRate,
                   DeliveryMode deliveryMode, String address, Boolean isActive) {
        this.user = user;
        this.title = title;
        this.description = description;
        this.hourlyRate = hourlyRate;
        this.deliveryMode = deliveryMode;
        this.address = address;
        this.isActive = isActive;
    }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getHourlyRate() { return hourlyRate; }
    public void setHourlyRate(BigDecimal hourlyRate) { this.hourlyRate = hourlyRate; }

    public DeliveryMode getDeliveryMode() { return deliveryMode; }
    public void setDeliveryMode(DeliveryMode deliveryMode) { this.deliveryMode = deliveryMode; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }


    @Override
    public void validate() {
        if (user == null) {
            throw new ValidationException("Profile must be associated with a user");
        }
        if (title == null || title.trim().isEmpty()) {
            throw new ValidationException("Title cannot be null or empty");
        }
        if (hourlyRate == null || hourlyRate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Hourly rate must be positive");
        }
        if (deliveryMode == null) {
            throw new ValidationException("Delivery mode cannot be null");
        }
        if (deliveryMode.requiresAddress()) {
            if (address == null || address.trim().isEmpty()) {
                throw new ValidationException(
                        "Address is required for " + deliveryMode.getDisplayName() + " delivery mode");
            }
        }
        if (isActive == null) {
            throw new ValidationException("isActive flag cannot be null");
        }
    }


    /** Soft-deletes this profile by setting {@code isActive} to {@code false}. */
    public void softDelete() { this.isActive = false; }

    /** Reactivates this profile by setting {@code isActive} to {@code true}. */
    public void reactivate() { this.isActive = true; }
}