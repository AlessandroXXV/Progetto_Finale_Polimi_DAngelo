package it.eably.backend.service.observer;

import it.eably.backend.model.Booking;

/**
 * Subject interface for managing booking observers.
 * 
 * DESIGN PATTERN: OBSERVER (Subject Interface)
 * 
 * RESPONSIBILITY:
 * - Maintains a list of observers
 * - Notifies all observers when state changes
 * 
 * WHY SEPARATE INTERFACE?
 * - Separates observer management from business logic
 * - Allows different subjects to share same observer management code
 * - Follows Interface Segregation Principle
 * 
 * PATTERN IN ACTION:
     * 1. Observers are injected via Spring DI in the constructor
     * 2. Subject performs business operation
     * 3. Subject calls notifyObservers() when state changes
     * 4. Each observer's onBookingStatusChanged() is called
 * 
 * @author Alessandro D'Angelo
 * @version 1.0
 */
public interface BookingSubject {
    
    /**
     * Notifies all attached observers of a booking status change.
     * 
     * NOTIFICATION STRATEGY:
     * - Iterates through all registered observers
     * - Calls onBookingStatusChanged() on each observer
     * - Handles exceptions from individual observers gracefully
     * - Ensures one failing observer doesn't prevent others from being notified
     * 
     * @param booking the booking that changed status
     */
    void notifyObservers(Booking booking);
}
