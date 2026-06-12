package it.eably.backend.model;

/**
 * Enumeration representing user roles in the Eably system.
 * 
 * Role Hierarchy:
 * - CLIENT: Regular users who book services
 * - STUDENT: Service providers (tutors, consultants)
 * - ADMIN: System administrators with full access
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */
public enum UserRole {
    
    /**
     * Client role - users who book and pay for services.
     * Permissions: Create bookings, view own bookings, make payments, leave reviews.
     */
    CLIENT("Client", "Regular user who books services"),
    
    /**
     * Student role - service providers (tutors, consultants, professionals).
     * Permissions: Manage profile, set availability, accept bookings, receive payments.
     */
    STUDENT("Student", "Service provider (tutor, consultant)"),
    
    /**
     * Admin role - system administrators.
     * Permissions: Full system access, user management, booking management, system configuration.
     */
    ADMIN("Admin", "System administrator with full access");
    
    private final String displayName;
    private final String description;
    
    /**
     * Constructor for UserRole enum.
     * 
     * @param displayName human-readable name for the role
     * @param description detailed description of the role
     */
    UserRole(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    /**
     * Gets the display name of the role.
     * 
     * @return human-readable role name
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Gets the description of the role.
     * 
     * @return detailed role description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Gets the Spring Security authority string for this role.
     * 
     * @return authority string in format "ROLE_ROLENAME"
     */
    public String getAuthority() {
        return "ROLE_" + this.name();
    }
}
