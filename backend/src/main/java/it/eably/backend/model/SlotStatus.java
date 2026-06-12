package it.eably.backend.model;

/**
 * Enumeration representing availability slot status.
 * 
 * Status Values:
 * - AVAILABLE: Slot is open for booking
 * - BOOKED: Slot has been reserved by a client
 * - CANCELLED: Slot has been cancelled by provider
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */
public enum SlotStatus {
    
    /**
     * Slot is available for booking.
     */
    AVAILABLE("Available", "Slot is open for booking"),
    
    /**
     * Slot has been booked by a client.
     */
    BOOKED("Booked", "Slot has been reserved"),
    
    /**
     * Slot has been cancelled by the provider.
     */
    CANCELLED("Cancelled", "Slot has been cancelled");
    
    private final String displayName;
    private final String description;
    
    /**
     * Constructor for SlotStatus enum.
     * 
     * @param displayName human-readable name for the status
     * @param description detailed description of the status
     */
    SlotStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    /**
     * Gets the display name of the status.
     * 
     * @return human-readable status name
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Gets the description of the status.
     * 
     * @return detailed status description
     */
    public String getDescription() {
        return description;
    }
}
