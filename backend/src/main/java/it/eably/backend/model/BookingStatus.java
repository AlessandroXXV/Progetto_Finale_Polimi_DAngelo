package it.eably.backend.model;

/**
 * Enumeration representing booking status lifecycle.
 * 
 * Status Flow (active):
 * PAYMENT_PENDING → CONFIRMED → COMPLETED
 *           ↘ CANCELLED (can happen at any stage)
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */
public enum BookingStatus {
    
    /**
     * Payment is being processed.
     * Stripe payment intent created but not confirmed.
     */
    PAYMENT_PENDING("Payment Pending", "Payment is being processed"),
    
    /**
     * Booking is confirmed after successful payment.
     * Service provider has been notified.
     */
    CONFIRMED("Confirmed", "Booking confirmed, payment successful"),
    
    /**
     * Service has been completed.
     * Ready for review.
     */
    COMPLETED("Completed", "Service completed successfully"),
    
    /**
     * Booking has been cancelled.
     * Can be cancelled by client or provider.
     */
    CANCELLED("Cancelled", "Booking has been cancelled");
    
    private final String displayName;
    private final String description;
    
    /**
     * Constructor for BookingStatus enum.
     * 
     * @param displayName human-readable name for the status
     * @param description detailed description of the status
     */
    BookingStatus(String displayName, String description) {
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
    
    /**
     * Checks if the booking can be cancelled in this status.
     * 
     * @return true if cancellation is allowed
     */
    public boolean isCancellable() {
        return this == PAYMENT_PENDING || this == CONFIRMED;
    }
    
    /**
     * Checks if the booking is in a final state.
     * 
     * @return true if status is final (COMPLETED or CANCELLED)
     */
    public boolean isFinalState() {
        return this == COMPLETED || this == CANCELLED;
    }
}
