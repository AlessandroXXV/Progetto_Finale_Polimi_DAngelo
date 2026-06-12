package it.eably.backend.dto.availabilitySlot.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * DTO for creating availability slots.
 *
 * Slots are tied to the authenticated student (extracted from the JWT),
 * not to a single profile/service.
 *
 * Validation rules:
 * - dayOfWeek: 0-6 (0=Monday, 6=Sunday)
 * - startTime: HH:mm format (24-hour)
 * - endTime: HH:mm format (24-hour)
 * - endTime must be after startTime (validated in service)
 * - duration must not exceed 60 minutes (validated in service)
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */
public record AvailabilitySlotRequestDTO(
    @NotNull(message = "Day of week is required")
    @Min(value = 0, message = "Day of week must be between 0 and 6")
    @Max(value = 6, message = "Day of week must be between 0 and 6")
    Integer dayOfWeek,

    @NotNull(message = "Start time is required")
    @Pattern(regexp = "^([01]\\d|2[0-3]):([0-5]\\d)$",
             message = "Start time must be in HH:mm format")
    String startTime,

    @NotNull(message = "End time is required")
    @Pattern(regexp = "^([01]\\d|2[0-3]):([0-5]\\d)$",
             message = "End time must be in HH:mm format")
    String endTime
) {}
