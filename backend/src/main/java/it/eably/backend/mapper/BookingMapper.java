package it.eably.backend.mapper;

import it.eably.backend.dto.booking.response.BookingResponseDTO;
import it.eably.backend.model.Booking;
import it.eably.backend.model.BookingStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.stream.Collectors;

/**
 * MapStruct mapper for Booking entity to DTO conversion.
 * 
 * Provides two mapping paths:
 * - toResponseDTOFull: for confirm/complete operations that expose confirmation codes
 * - toResponseDTO: for read operations that hide confirmation codes until COMPLETED
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */
@Mapper(componentModel = "spring")
public interface BookingMapper {
    
    /**
     * Full DTO mapping that exposes confirmation code.
     * Used for confirm/complete operations.
     */
    @Mapping(source = "client.id", target = "clientId")
    @Mapping(source = "client.username", target = "clientUsername")
    @Mapping(source = "client.email", target = "clientEmail")
    @Mapping(source = "provider.id", target = "providerId")
    @Mapping(source = "provider.username", target = "providerUsername")
    @Mapping(source = "profile.id", target = "profileId")
    @Mapping(source = "profile.title", target = "profileTitle")
    @Mapping(source = "availabilitySlot.id", target = "slotId")
    @Mapping(source = "availabilitySlot.dayOfWeek", target = "slotDayOfWeek")
    @Mapping(source = "availabilitySlot.startTime", target = "slotStartTime")
    @Mapping(source = "availabilitySlot.endTime", target = "slotEndTime")
    BookingResponseDTO toResponseDTOFull(Booking booking);

    /**
     * Secured DTO mapping that hides confirmation code until COMPLETED.
     * Used for all read operations.
     */
    default BookingResponseDTO toResponseDTO(Booking booking) {
        BookingResponseDTO dto = toResponseDTOFull(booking);

        // Hide confirmation code if not COMPLETED
        if (booking.getStatus() != BookingStatus.COMPLETED && dto.confirmationCode() != null) {
            return new BookingResponseDTO(
                dto.id(),
                dto.clientId(),
                dto.clientUsername(),
                dto.clientEmail(),
                dto.providerId(),
                dto.providerUsername(),
                dto.profileId(),
                dto.profileTitle(),
                dto.slotId(),
                dto.slotDayOfWeek(),
                dto.slotStartTime(),
                dto.slotEndTime(),
                dto.status(),
                dto.totalAmount(),
                dto.notes(),
                dto.bookedAt(),
                dto.bookingDate(),
                null, // Confirmation code hidden
                dto.paymentIntentId(),
                dto.cancellationReason()
            );
        }
        return dto;
    }

    /**
     * Maps list with secured version (hides confirmation codes).
     */
    default List<BookingResponseDTO> toResponseDTOList(List<Booking> bookings) {
        return bookings.stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }
}

