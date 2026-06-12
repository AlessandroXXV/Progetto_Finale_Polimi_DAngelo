package it.eably.backend.service.observer;

import it.eably.backend.model.Booking;

/**
 * Observer interface for booking status change notifications.
 * 
 * DESIGN PATTERN: OBSERVER (GoF Behavioral Pattern)
 * 
 * PURPOSE:
 * - Defines a one-to-many dependency between objects
 * - When subject (Booking) changes state, all observers are notified automatically
 * - Observers can react to state changes without tight coupling
 * 
 * WHY OBSERVER PATTERN HERE?
 * - Booking status changes need to trigger multiple actions (email, SMS, logging, analytics)
 * - Business logic (BookingService) should not depend on notification mechanisms
 * - Easy to add new notification types without modifying existing code
 * - Promotes loose coupling and Single Responsibility Principle
 * 
 * PATTERN PARTICIPANTS:
 * - Observer (this interface): Defines update interface for objects that should be notified
 * - ConcreteObserver (EmailNotificationObserver, SmsNotificationObserver): Implements update logic
 * - Subject (BookingSubject): Maintains list of observers and notifies them of changes
 * - ConcreteSubject (BookingService): Implements subject interface and triggers notifications
 * 
 * BENEFITS:
 * 1. DECOUPLING: Business logic doesn't know about notification mechanisms
 * 2. EXTENSIBILITY: Add new observers without modifying subject
 * 3. DYNAMIC: Observers can be attached/detached at runtime
 * 4. BROADCAST: One state change notifies multiple observers
 * 
 * @author Alessandro D'Angelo
 * @version 1.0
 */
public interface BookingObserver {
    
    /**
     * Called when a booking's status changes.
     * 
     * OBSERVER NOTIFICATION:
     * - Subject calls this method on all registered observers
     * - Observer can query the booking for current state
     * - Observer performs its specific action (send email, log, etc.)
     * 
     * IMPLEMENTATION GUIDELINES:
     * - Keep this method fast (don't block)
     * - Handle exceptions internally (don't propagate to subject)
     * - Log errors but don't fail the booking operation
     * 
     * @param booking the booking that changed status
     */
    void onBookingStatusChanged(Booking booking);
    
    /**
     * Gets the name of this observer.
     * Used for logging and debugging.
     * 
     * @return observer name
     */
    String getObserverName();
}
