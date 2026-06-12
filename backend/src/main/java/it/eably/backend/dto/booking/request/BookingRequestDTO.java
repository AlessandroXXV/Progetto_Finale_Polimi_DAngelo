package it.eably.backend.dto.booking.request;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * DTO for creating a new booking.
 * 
 * This record encapsulates the data required from the client to create a booking.
 * The client ID is extracted from the JWT token in the controller.
 * 
 * @param slotId the availability slot ID to book
 * @param profileId the ID of the profile/service to book
 * @param bookingDate calendar date for the requested booking session (ISO yyyy-MM-dd)
 * @param notes optional notes from the client
 * 
 * @author Alessandro D'Angelo
 * @version 1.0
 */
public record BookingRequestDTO(
        
        @NotNull(message = "Slot ID is required")
        @Min(value = 1, message = "Slot ID must be a positive number")
        Long slotId,
        
        @NotNull(message = "Profile ID is required")
        @Min(value = 1, message = "Slot ID must be a positive number")
        Long profileId,

        @NotNull(message = "Booking date is required")
        @FutureOrPresent(message = "Booking date must be in the future or today")
        LocalDate bookingDate,

        String notes
) {
}
