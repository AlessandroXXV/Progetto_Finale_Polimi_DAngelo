package it.eably.backend.dto.availabilitySlot.response;

/**
 * DTO for availability slot responses.
 *
 * Includes:
 * - Slot ID
 * - Day of week (0-6)
 * - Start and end times
 * - Status
 * - studentId: the ID of the student user who owns the slot
 * - isBooked flag (true if slot has active booking)
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */
public record AvailabilitySlotResponseDTO(
    Long id,
    Integer dayOfWeek,
    String startTime,
    String endTime,
    String status,
    Long studentId,
    Boolean isBooked
) {}
