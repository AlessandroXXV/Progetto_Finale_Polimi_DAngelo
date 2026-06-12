package it.eably.backend.service.def;

import it.eably.backend.model.Booking;
import it.eably.backend.model.BookingStatus;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.util.List;

/**
 * Service interface for Booking business logic.
 * <p>
 * This interface defines the contract for booking operations including:
 * - Creating bookings with concurrency control
 * - Managing booking lifecycle (confirm, complete, cancel)
 * - Querying bookings by various criteria
 * <p>
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */

@Validated
public interface BookingService
{

    /**
     * Creates a new booking for a client.
     *
     * @param clientId    the client user ID
     * @param slotId      the availability slot ID
     * @param profileId   the profile/service ID chosen by the client
     * @param bookingDate the selected calendar date for the session
     * @param notes       optional notes from client
     * @return the created booking
     */
    Booking createBooking(Long clientId, Long slotId, Long profileId, String notes, LocalDate bookingDate);

    /**
     * Confirms a booking after successful payment.
     * <p>
     * Updates booking status from PAYMENT_PENDING to CONFIRMED.
     * Generates a 6-digit confirmation code.
     *
     * @param bookingId       the booking ID
     * @param paymentIntentId the Stripe payment intent ID
     * @param requesterId     the ID of the user making the request (client)
     * @return the confirmed booking
     * @throws it.eably.backend.exception.ValidationException if booking not found or invalid status
     */
    Booking confirmBooking(Long bookingId, String paymentIntentId, Long requesterId);

    /**
     * Completes a booking after service delivery.
     * <p>
     * Validates the 6-digit confirmation code provided by the client.
     * Updates booking status from CONFIRMED to COMPLETED.
     *
     * @param bookingId        the booking ID
     * @param confirmationCode the 6-digit confirmation code
     * @param providerId       the ID of the provider making the request
     * @return the completed booking
     * @throws it.eably.backend.exception.ValidationException if code is invalid or booking not found
     */
    Booking completeBooking(Long bookingId, String confirmationCode, Long providerId);

    /**
     * Cancels a booking.
     * <p>
     * Can be cancelled by client, provider, or admin.
     * Updates booking status to CANCELLED.
     * Marks the availability slot as AVAILABLE again.
     *
     * @param bookingId the booking ID
     * @param userId    the user requesting cancellation (for authorization check)
     * @return the cancelled booking
     * @throws it.eably.backend.exception.ValidationException if booking cannot be cancelled
     */
    Booking cancelBooking(Long bookingId, Long userId);

    /**
     * Retrieves a booking by ID.
     *
     * @param bookingId the booking ID
     * @return the booking
     * @throws it.eably.backend.exception.ValidationException if booking not found
     */
    Booking getBookingById(Long bookingId);

    /**
     * Retrieves all bookings for a client.
     *
     * @param clientId the client user ID
     * @return list of bookings
     */
    List<Booking> getBookingsByClient(Long clientId);

    /**
     * Retrieves all bookings for every profile owned by a provider user.
     *
     * @param providerUserId the provider user ID
     * @return list of bookings across all profiles owned by that user
     */
    List<Booking> getBookingsByProviderUser(Long providerUserId);

    /**
     * Retrieves bookings by status.
     *
     * @param status the booking status
     * @return list of bookings
     */
    List<Booking> getBookingsByStatus(BookingStatus status);

    /**
     * Checks if a user is the owner of a booking (client or provider).
     * <p>
     * Used for authorization checks.
     *
     * @param bookingId the booking ID
     * @param userId    the user ID
     * @return true if user is owner
     */
    boolean isBookingOwner(Long bookingId, Long userId);

    /**
     * Checks if a user is the owner of a booking (client or provider) by username.
     *
     * @param bookingId the booking ID
     * @param username  the username
     * @return true if user is owner
     */
    boolean isBookingOwner(Long bookingId, String username);

    /**
     * Cancels a booking due to payment timeout.
     * System-initiated operation — no user authorization required.
     * Notifies all registered observers.
     *
     * @param bookingId the booking ID to cancel
     */
    void cancelBookingDueToTimeout(Long bookingId);

}
