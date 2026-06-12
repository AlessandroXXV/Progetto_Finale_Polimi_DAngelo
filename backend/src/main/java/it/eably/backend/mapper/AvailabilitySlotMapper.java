package it.eably.backend.mapper;

import it.eably.backend.dto.availabilitySlot.response.AvailabilitySlotResponseDTO;
import it.eably.backend.model.AvailabilitySlot;
import it.eably.backend.model.BookingStatus;
import org.mapstruct.Mapper;

import java.time.DayOfWeek;
import java.util.List;
import java.util.stream.Collectors;

/**
 * MapStruct mapper for AvailabilitySlot entity to DTO conversion.
 *
 * The {@code isBooked} field cannot be derived from the entity alone — it depends
 * on whether the associated booking is in an active state.  For this reason the
 * main mapping method accepts an explicit {@code isBooked} flag and is
 * implemented as a {@code default} method rather than a generated one.
 *
 * The day-of-week conversion follows the project convention: MONDAY = 0 … SUNDAY = 6.
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */
@Mapper(componentModel = "spring")
public interface AvailabilitySlotMapper {

    /**
     * Maps an {@link AvailabilitySlot} to an {@link AvailabilitySlotResponseDTO}.
     *
     * @param slot     the availability slot entity
     * @param isBooked true if the slot has an active booking
     * @return the response DTO
     */
    default AvailabilitySlotResponseDTO toResponseDTO(AvailabilitySlot slot, boolean isBooked) {
        if (slot == null) return null;
        return new AvailabilitySlotResponseDTO(
                slot.getId(),
                dayOfWeekToInt(slot.getDayOfWeek()),
                slot.getStartTime() != null ? slot.getStartTime().toString() : null,
                slot.getEndTime()   != null ? slot.getEndTime().toString()   : null,
                slot.getStatus()    != null ? slot.getStatus().name()        : null,
                slot.getStudent()   != null ? slot.getStudent().getId()      : null,
                isBooked
        );
    }

    /**
     * Maps a list of slots.  The {@code isBooked} flag is computed per-slot
     * (a slot is booked when it has a non-null booking that is not CANCELLED or COMPLETED).
     *
     * @param slots the list of availability slot entities
     * @return the list of response DTOs
     */
    default List<AvailabilitySlotResponseDTO> toResponseDTOList(List<AvailabilitySlot> slots) {
        if (slots == null) return null;
        return slots.stream()
                .map(slot -> {
                    boolean isBooked = slot.getBookings() != null && slot.getBookings().stream().anyMatch(this::isActiveBooking);
                    return toResponseDTO(slot, isBooked);
                })
                .collect(Collectors.toList());
    }

    /**
     * Converts a {@link DayOfWeek} enum to the project integer convention (MONDAY = 0).
     */
    default Integer dayOfWeekToInt(DayOfWeek dayOfWeek) {
        if (dayOfWeek == null) return null;
        return switch (dayOfWeek) {
            case MONDAY    -> 0;
            case TUESDAY   -> 1;
            case WEDNESDAY -> 2;
            case THURSDAY  -> 3;
            case FRIDAY    -> 4;
            case SATURDAY  -> 5;
            case SUNDAY    -> 6;
        };
    }

    private boolean isActiveBooking(it.eably.backend.model.Booking booking) {
        BookingStatus status = booking.getStatus();
        return status != BookingStatus.CANCELLED && status != BookingStatus.COMPLETED;
    }
}
