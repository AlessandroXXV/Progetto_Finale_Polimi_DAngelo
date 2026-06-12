package it.eably.backend.repository;

import it.eably.backend.model.Booking;
import it.eably.backend.model.BookingCancellationReason;
import it.eably.backend.model.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Booking entity.
 * 
 * Provides CRUD operations and custom queries for booking management.
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */
@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    
    /**
     * Finds all bookings for a client.
     * 
     * @param clientId the client user ID
     * @return list of bookings
     */
    List<Booking> findByClientId(Long clientId);
    
    /**
     * Finds all bookings for a provider.
     * 
     * @param providerId the provider profile ID
     * @return list of bookings
     */
    List<Booking> findByProviderId(Long providerId);
    
    /**
     * Finds all bookings with a specific status.
     * 
     * @param status the booking status
     * @return list of bookings
     */
    List<Booking> findByStatus(BookingStatus status);
    
    /**
     * Finds booking by payment intent ID.
     * 
     * @param paymentIntentId the Stripe payment intent ID
     * @return Optional containing the booking if found
     */
    Optional<Booking> findByPaymentIntentId(String paymentIntentId);
    
    /**
     * Finds booking by availability slot ID.
     * 
     * @param availabilitySlotId the slot ID
     * @return Optional containing the booking if found
     */
    Optional<Booking> findByAvailabilitySlotId(Long availabilitySlotId);

    /**
     * Returns true if the slot has at least one active booking.
     */
    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM Booking b " +
           "WHERE b.availabilitySlot.id = :availabilitySlotId " +
           "AND b.status NOT IN (it.eably.backend.model.BookingStatus.CANCELLED, it.eably.backend.model.BookingStatus.COMPLETED)")
    boolean existsActiveByAvailabilitySlotId(@Param("availabilitySlotId") Long availabilitySlotId);

    /**
     * Returns true if the slot has at least one active booking on a specific date.
     * Used to prevent double-booking of the same slot on the same day.
     *
     * @param availabilitySlotId the availability slot ID
     * @param bookingDate        the date to check
     * @return true if an active booking exists for the slot on that date
     */
    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM Booking b " +
           "WHERE b.availabilitySlot.id = :availabilitySlotId " +
           "AND b.bookingDate = :bookingDate " +
           "AND b.status NOT IN (it.eably.backend.model.BookingStatus.CANCELLED, it.eably.backend.model.BookingStatus.COMPLETED)")
    boolean existsActiveByAvailabilitySlotIdAndBookingDate(@Param("availabilitySlotId") Long availabilitySlotId,
                                                            @Param("bookingDate") LocalDate bookingDate);
    /**
     * Finds all bookings in PAYMENT_PENDING status whose creation time is before the given cutoff.
     * Used by the scheduler to expire bookings that were never paid.
     *
     * @param cutoff the datetime threshold; bookings created before this are considered expired
     * @return list of expired payment-pending bookings
     */
    @Query("SELECT b FROM Booking b " +
           "WHERE b.status = it.eably.backend.model.BookingStatus.PAYMENT_PENDING " +
           "AND b.bookedAt <= :cutoff")
    List<Booking> findPaymentPendingBookingsExpiredBefore(@Param("cutoff") LocalDateTime cutoff);

    /**
     * Checks if a booking exists matching all the given criteria.
     * Used to detect duplicate or re-booking attempts after cancellation.
     *
     * @param clientId             the client user ID
     * @param availabilitySlotId   the availability slot ID
     * @param bookingDate          the booking date
     * @param status               the booking status
     * @param cancellationReason   the cancellation reason
     * @return true if a matching booking record exists
     */
    boolean existsByClientIdAndAvailabilitySlotIdAndBookingDateAndStatusAndCancellationReason(
            Long clientId,
            Long availabilitySlotId,
            LocalDate bookingDate,
            BookingStatus status,
            BookingCancellationReason cancellationReason
    );


    /**
     * Counts bookings for a client.
     * 
     * @param clientId the client user ID
     * @return number of bookings
     */
    long countByClientId(Long clientId);
    
    /**
     * Counts bookings for a provider.
     * 
     * @param providerId the provider profile ID
     * @return number of bookings
     */
    long countByProviderId(Long providerId);

    /**
     * Returns (clientId, bookingCount) pairs for all clients in a single query.
     * Used by admin list to avoid N+1.
     */
    @Query("SELECT b.client.id, COUNT(b) FROM Booking b GROUP BY b.client.id")
    List<Object[]> countClientBookingsGroupedByUserId();

    /**
     * Returns (providerId, bookingCount) pairs for all providers in a single query.
     * Used by admin list to avoid N+1.
     */
    @Query("SELECT b.provider.id, COUNT(b) FROM Booking b GROUP BY b.provider.id")
    List<Object[]> countProviderBookingsGroupedByUserId();
}
