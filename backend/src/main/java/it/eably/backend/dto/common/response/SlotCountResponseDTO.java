package it.eably.backend.dto.common.response;

/**
 * DTO for slot count response.
 * 
 * Used for:
 * - GET /api/v1/availability/me/count endpoint
 * - Displaying "Set your availability" banner in frontend
 * 
 * Fields:
 * - count: total number of slots
 * - hasAvailability: true if count > 0
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */
public record SlotCountResponseDTO(
    Long count,
    Boolean hasAvailability
) {
    /**
     * Constructor that automatically calculates hasAvailability.
     */
    public SlotCountResponseDTO(Long count) {
        this(count, count != null && count > 0);
    }
}
