package it.eably.backend.dto.booking.response;

import it.eably.backend.model.BookingStatus;
import it.eably.backend.model.BookingCancellationReason;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for booking response data.
 * 
 * This record encapsulates booking data to be sent to the client.
 * Includes nested DTOs for related entities (client, provider, slot).
 * 
 * @param id booking ID
 * @param clientId client user ID
 * @param clientUsername client username
 * @param clientEmail client email
 * @param providerId provider profile ID
 * @param providerUsername provider username
 * @param profileId profile ID for the service booked
 * @param profileTitle professional title for the service booked
 * @param slotId availability slot ID
 * @param slotDayOfWeek day of week for the slot
 * @param slotStartTime start time of the slot
 * @param slotEndTime end time of the slot
 * @param status booking status
 * @param totalAmount total amount for the booking
 * @param notes optional notes from client
 * @param bookedAt timestamp when booking was created
 * @param bookingDate calendar date for the booked session
 * @param confirmationCode 6-digit confirmation code (null until confirmed)
 * @param paymentIntentId Stripe payment intent ID (null until payment)
 * @param cancellationReason reason for cancellation when status is CANCELLED
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */
public record BookingResponseDTO(
        Long id,
        Long clientId,
        String clientUsername,
        String clientEmail,
        Long providerId,
        String providerUsername,
        Long profileId,
        String profileTitle,
        Long slotId,
        String slotDayOfWeek,
        String slotStartTime,
        String slotEndTime,
        BookingStatus status,
        BigDecimal totalAmount,
        String notes,
        LocalDateTime bookedAt,
        LocalDate bookingDate,
        String confirmationCode,
        String paymentIntentId,
        BookingCancellationReason cancellationReason
) {
        public BookingResponseDTO(
                Long id,
                Long clientId,
                String clientUsername,
                String clientEmail,
                Long providerId,
                String providerUsername,
                Long profileId,
                String profileTitle,
                Long slotId,
                String slotDayOfWeek,
                String slotStartTime,
                String slotEndTime,
                BookingStatus status,
                BigDecimal totalAmount,
                String notes,
                LocalDateTime bookedAt,
                LocalDate bookingDate,
                String confirmationCode,
                String paymentIntentId
        ) {
                this(
                        id,
                        clientId,
                        clientUsername,
                        clientEmail,
                        providerId,
                        providerUsername,
                        profileId,
                        profileTitle,
                        slotId,
                        slotDayOfWeek,
                        slotStartTime,
                        slotEndTime,
                        status,
                        totalAmount,
                        notes,
                        bookedAt,
                        bookingDate,
                        confirmationCode,
                        paymentIntentId,
                        null
                );
        }

        public BookingResponseDTO(
                Long id,
                Long clientId,
                String clientUsername,
                String clientEmail,
                Long providerId,
                String providerUsername,
                Long profileId,
                String profileTitle,
                Long slotId,
                String slotDayOfWeek,
                String slotStartTime,
                String slotEndTime,
                BookingStatus status,
                BigDecimal totalAmount,
                String notes,
                LocalDateTime bookedAt,
                String confirmationCode,
                String paymentIntentId
        ) {
                this(
                        id,
                        clientId,
                        clientUsername,
                        clientEmail,
                        providerId,
                        providerUsername,
                        profileId,
                        profileTitle,
                        slotId,
                        slotDayOfWeek,
                        slotStartTime,
                        slotEndTime,
                        status,
                        totalAmount,
                        notes,
                        bookedAt,
                        null,
                        confirmationCode,
                        paymentIntentId,
                        null
                );
        }
}
