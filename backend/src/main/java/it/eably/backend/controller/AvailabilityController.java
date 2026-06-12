package it.eably.backend.controller;

import it.eably.backend.dto.availabilitySlot.request.AvailabilitySlotRequestDTO;
import it.eably.backend.dto.availabilitySlot.response.AvailabilitySlotResponseDTO;
import it.eably.backend.dto.common.response.SlotCountResponseDTO;
import it.eably.backend.model.User;
import it.eably.backend.service.def.AvailabilityService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for availability slot management.
 *
 * <p>
 * Endpoints:
 * <ul>
 * <li>POST /api/v1/availability/ - Create slot (STUDENT only)</li>
 * <li>DELETE /api/v1/availability/{slot_id} - Delete slot (STUDENT only)</li>
 * <li>GET /api/v1/availability/student/{student_id} - Get student slots</li>
 * <li>GET /api/v1/availability/me/count - Get my slot count (STUDENT only)</li>
 * </ul>
 * <p>
 * All endpoints require authentication.
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */
@RestController
@RequestMapping("/api/v1/availability")
public class AvailabilityController
{

    private static final Logger logger = LoggerFactory.getLogger(AvailabilityController.class);

    private final AvailabilityService availabilityService;

    public AvailabilityController(AvailabilityService availabilityService)
    {
        this.availabilityService = availabilityService;
    }

    /**
     * Creates a new availability slot for the authenticated student.
     *
     * @param requestDTO the slot creation request
     * @param user       the authenticated user
     * @return ResponseEntity with created slot data (201 Created)
     */
    @PostMapping("/")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<AvailabilitySlotResponseDTO> createSlot(
            @Valid @RequestBody AvailabilitySlotRequestDTO requestDTO,
            @AuthenticationPrincipal User user)
    {

        logger.info("POST /api/v1/availability/ - User: {}", user.getUsername());

        String username = user.getUsername();

        // Create slot
        AvailabilitySlotResponseDTO responseDTO = availabilityService.createSlot(username, requestDTO);

        logger.info("Successfully created availability slot for user: {}", username);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
    }

    /**
     * Deletes an availability slot.
     * Only the owner can delete their slots.
     *
     * @param slotId the slot ID
     * @param user   the authenticated user
     * @return ResponseEntity with 204 No Content
     */
    @DeleteMapping("/{slot_id}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Void> deleteSlot(
            @PathVariable("slot_id") Long slotId,
            @AuthenticationPrincipal User user)
    {

        logger.info("DELETE /api/v1/availability/{} - User: {}", slotId, user.getUsername());

        String username = user.getUsername();

        // Delete slot
        availabilityService.deleteSlot(slotId, username);

        logger.info("Successfully deleted availability slot ID: {} for user: {}", slotId, username);
        return ResponseEntity.noContent().build();
    }

    /**
     * Retrieves all availability slots for a student.
     * Returns slots with isBooked flag indicating active bookings.
     *
     * @param studentId the student user ID
     * @return ResponseEntity with list of slots
     */
    @GetMapping("/student/{student_id}")
    public ResponseEntity<List<AvailabilitySlotResponseDTO>> getStudentSlots(
            @PathVariable("student_id") Long studentId)
    {

        logger.info("GET /api/v1/availability/student/{}", studentId);

        // Get slots
        List<AvailabilitySlotResponseDTO> slots = availabilityService.getStudentSlots(studentId);

        logger.info("Retrieved {} slots for student ID: {}", slots.size(), studentId);
        return ResponseEntity.ok(slots);
    }

    /**
     * Gets the slot count for the authenticated student.
     * Used for displaying "Set your availability" banner.
     *
     * @param user the authenticated user
     * @return ResponseEntity with slot count
     */
    @GetMapping("/me/count")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<SlotCountResponseDTO> getMySlotCount(
            @AuthenticationPrincipal User user)
    {

        logger.info("GET /api/v1/availability/me/count - User: {}", user.getUsername());

        String username = user.getUsername();

        // Get count
        SlotCountResponseDTO countDTO = availabilityService.getMySlotCount(username);

        logger.info("Student {} has {} slots", username, countDTO.count());
        return ResponseEntity.ok(countDTO);
    }
}
