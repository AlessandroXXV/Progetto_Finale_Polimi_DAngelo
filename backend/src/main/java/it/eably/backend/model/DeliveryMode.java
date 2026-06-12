package it.eably.backend.model;

/**
 * Enumeration representing service delivery modes.
 * 
 * Delivery Modes:
 * - ONLINE: Remote service delivery (no physical address required)
 * - IN_PERSON: Physical location service (address required)
 * - HYBRID: Both online and in-person options (address required)
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */
public enum DeliveryMode {
    
    /**
     * Service delivered entirely online.
     * No physical address required.
     */
    ONLINE("Online", "Service delivered remotely via internet"),
    
    /**
     * Service delivered in person at a physical location.
     * Physical address is required.
     */
    IN_PERSON("In Person", "Service delivered at a physical location"),
    
    /**
     * Service can be delivered both online and in person.
     * Physical address is required for in-person option.
     */
    HYBRID("Hybrid", "Service available both online and in person");
    
    private final String displayName;
    private final String description;
    
    /**
     * Constructor for DeliveryMode enum.
     * 
     * @param displayName human-readable name for the delivery mode
     * @param description detailed description of the delivery mode
     */
    DeliveryMode(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    /**
     * Gets the display name of the delivery mode.
     * 
     * @return human-readable delivery mode name
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Gets the description of the delivery mode.
     * 
     * @return detailed delivery mode description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Checks if this delivery mode requires a physical address.
     * 
     * Business Rule: IN_PERSON and HYBRID modes require an address.
     * 
     * @return true if address is required
     */
    public boolean requiresAddress() {
        return this == IN_PERSON || this == HYBRID;
    }
}
