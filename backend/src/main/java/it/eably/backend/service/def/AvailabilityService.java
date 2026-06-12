package it.eably.backend.service.def;

import it.eably.backend.dto.availabilitySlot.request.AvailabilitySlotRequestDTO;
import it.eably.backend.dto.availabilitySlot.response.AvailabilitySlotResponseDTO;
import it.eably.backend.dto.common.response.SlotCountResponseDTO;

import java.util.List;

/**
 * Service interface for availability slot management.
 *
 * Provides business logic for:
 * - Creating slots with overlap detection
 * - Deleting slots (only if no active bookings)
 * - Retrieving slots for a student
 * - Counting slots for the authenticated user
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */
public interface AvailabilityService {

    /**
     * Creates a new availability slot for a student.
     * Validates overlap and duration constraints.
     *
     * @param username   the username of the student
     * @param requestDTO the slot creation request
     * @return the created slot as DTO
     */
    AvailabilitySlotResponseDTO createSlot(String username, AvailabilitySlotRequestDTO requestDTO);

    /**
     * Deletes an availability slot.
     * Only allowed if slot has no active bookings.
     *
     * @param slotId   the slot ID
     * @param username the username of the student (for authorization)
     */
    void deleteSlot(Long slotId, String username);

    /**
     * Retrieves all slots for a student with booking status.
     *
     * @param studentId the student user ID
     * @return list of slots with isBooked flag
     */
    List<AvailabilitySlotResponseDTO> getStudentSlots(Long studentId);

    /**
     * Counts the slots for the authenticated student.
     *
     * @param username the username of the student
     * @return slot count with hasAvailability flag
     */
    SlotCountResponseDTO getMySlotCount(String username);
}
