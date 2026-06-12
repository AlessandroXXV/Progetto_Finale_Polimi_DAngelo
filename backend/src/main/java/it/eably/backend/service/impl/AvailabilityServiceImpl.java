package it.eably.backend.service.impl;

import it.eably.backend.dto.availabilitySlot.request.AvailabilitySlotRequestDTO;
import it.eably.backend.dto.availabilitySlot.response.AvailabilitySlotResponseDTO;
import it.eably.backend.dto.common.response.SlotCountResponseDTO;
import it.eably.backend.exception.AuthorizationException;
import it.eably.backend.exception.ConflictException;
import it.eably.backend.exception.ResourceNotFoundException;
import it.eably.backend.exception.ValidationException;
import it.eably.backend.mapper.AvailabilitySlotMapper;
import it.eably.backend.model.AvailabilitySlot;
import it.eably.backend.model.SlotStatus;
import it.eably.backend.model.User;
import it.eably.backend.repository.AvailabilitySlotRepository;
import it.eably.backend.repository.BookingRepository;
import it.eably.backend.repository.UserRepository;
import it.eably.backend.service.def.AvailabilityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service implementation for availability slot management.
 *
 * <p>Handles:</p>
 * <ul>
 *   <li>Slot creation with overlap detection</li>
 *   <li>Slot deletion with active booking checks</li>
 *   <li>Slot retrieval with booking status</li>
 *   <li>Slot counting for dashboard banners</li>
 * </ul>
 *
 * <p>All write operations are transactional.</p>
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */
@Service
public class AvailabilityServiceImpl implements AvailabilityService {

    /** Logger for availability operations. */
    private static final Logger logger = LoggerFactory.getLogger(AvailabilityServiceImpl.class);

    /** Repository for availability slots. */
    private final AvailabilitySlotRepository availabilitySlotRepository;
    /** Repository for bookings. */
    private final BookingRepository bookingRepository;
    /** Repository for users. */
    private final UserRepository userRepository;
    /** Mapper for availability slot DTOs. */
    private final AvailabilitySlotMapper availabilitySlotMapper;

    /**
     * Builds the availability service with required dependencies.
     *
     * @param availabilitySlotRepository repository for availability slots
     * @param bookingRepository repository for bookings
     * @param userRepository repository for users
     * @param availabilitySlotMapper mapper for availability slots
     */
    public AvailabilityServiceImpl(AvailabilitySlotRepository availabilitySlotRepository,
                                   BookingRepository bookingRepository,
                                   UserRepository userRepository,
                                   AvailabilitySlotMapper availabilitySlotMapper) {
        this.availabilitySlotRepository = availabilitySlotRepository;
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.availabilitySlotMapper = availabilitySlotMapper;
    }

    /**
     * Creates a new availability slot for the given student username.
     *
     * <p>Effect: validates ownership and time constraints, checks overlap,
     * persists the slot, and returns the mapped DTO.</p>
     *
     * @param username student username
     * @param requestDTO slot request data
     * @return created slot response DTO
     * @throws ResourceNotFoundException when the user is not found
     * @throws ValidationException when Stripe is not connected or time is invalid
     * @throws ConflictException when the slot overlaps an existing one
     */
    @Override
    @Transactional
    public AvailabilitySlotResponseDTO createSlot(String username, AvailabilitySlotRequestDTO requestDTO) {
        logger.debug("Creating availability slot for student username: {}", username);

        // Ensure the providing student exists in our system.
        User student = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));

        // Verify Stripe onboarding is complete before allowing slot creation
        if (!Boolean.TRUE.equals(student.getStripeConnected())) {
            throw new ValidationException("Complete Stripe onboarding before creating availability slots");
        }

        // Convert DTO integer representation to Java DayOfWeek and parse time strings.
        DayOfWeek dayOfWeek = convertIntToDayOfWeek(requestDTO.dayOfWeek());
        LocalTime startTime = LocalTime.parse(requestDTO.startTime(), DateTimeFormatter.ofPattern("HH:mm"));
        LocalTime endTime = LocalTime.parse(requestDTO.endTime(), DateTimeFormatter.ofPattern("HH:mm"));

        // Validate temporal constraints
        if (!endTime.isAfter(startTime)) {
            throw new ValidationException("End time must be after start time");
        }

        // Prevent schedule conflicts globally for this student (across all their services).
        long overlapCount = availabilitySlotRepository.countOverlappingSlotsByStudent(
            student.getId(), dayOfWeek, startTime, endTime);

        if (overlapCount > 0) {
            throw new ConflictException("Slot overlaps with existing availability slot");
        }

        // Initialize the recurring availability slot.
        AvailabilitySlot slot = new AvailabilitySlot();
        slot.setStudent(student);
        slot.setDayOfWeek(dayOfWeek);
        slot.setStartTime(startTime);
        slot.setEndTime(endTime);
        slot.setStatus(SlotStatus.AVAILABLE);

        slot.validate();
        AvailabilitySlot savedSlot = availabilitySlotRepository.save(slot);

        logger.info("Successfully created availability slot ID: {} for student username: {}",
            savedSlot.getId(), username);

        return availabilitySlotMapper.toResponseDTO(savedSlot, false);
    }

    /**
     * Deletes (logically) an availability slot owned by the given student.
     *
     * <p>Effect: applies a logical cancel to keep historical bookings.</p>
     *
     * @param slotId slot id
     * @param username student username
     * @throws ResourceNotFoundException when slot or user is not found
     * @throws AuthorizationException when the user does not own the slot
     * @throws ConflictException when active bookings exist for the slot
     */
    @Override
    @Transactional
    public void deleteSlot(Long slotId, String username) {
        logger.debug("Deleting availability slot ID: {} for student username: {}", slotId, username);

        // Use a pessimistic WRITE lock to prevent race conditions with concurrent booking attempts
        AvailabilitySlot slot = availabilitySlotRepository.findByIdWithLock(slotId)
            .orElseThrow(() -> new ResourceNotFoundException("Availability slot not found with ID: " + slotId));

        // Locate the requester.
        User student = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));

        // Security check: ensure the slot belongs to the authenticated student
        if (!slot.getStudent().getId().equals(student.getId())) {
            throw new AuthorizationException("You can only delete your own availability slots");
        }

        // Data integrity: do not allow deletion if the slot has future/active bookings
        if (bookingRepository.existsActiveByAvailabilitySlotId(slotId)) {
            throw new ConflictException(
                "Cannot delete slot with active booking");
        }

        // Logical delete: keep historical bookings linked to the slot by changing its status to CANCELLED
        slot.markAsCancelled();
        availabilitySlotRepository.save(slot);

        logger.info("Successfully deleted availability slot ID: {}", slotId);
    }

    /**
     * Retrieves availability slots for a student.
     *
     * @param studentId student id
     * @return list of slot response DTOs
     * @throws ResourceNotFoundException when the user is not found
     */
    @Override
    public List<AvailabilitySlotResponseDTO> getStudentSlots(Long studentId) {
        logger.debug("Retrieving availability slots for student ID: {}", studentId);

        // Validate existence of the student.
        if (!userRepository.existsById(studentId)) {
            throw new ResourceNotFoundException("User not found with ID: " + studentId);
        }

        // Retrieve slots using an optimized query that includes booking metadata for the UI renderer.
        List<AvailabilitySlot> slots = availabilitySlotRepository.findByStudentIdWithBooking(studentId);
        List<AvailabilitySlotResponseDTO> responseDTOs = availabilitySlotMapper.toResponseDTOList(slots);

        logger.debug("Found {} slots for student ID: {}", responseDTOs.size(), studentId);
        return responseDTOs;
    }

    /**
     * Returns the slot count for the given student username.
     *
     * @param username student username
     * @return slot count response DTO
     * @throws ResourceNotFoundException when the user is not found
     */
    @Override
    public SlotCountResponseDTO getMySlotCount(String username) {
        logger.debug("Counting availability slots for student username: {}", username);

        // Standard lookup for current user.
        User student = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));

        // Optimized count query used for dashboard performance metrics.
        long count = availabilitySlotRepository.countByStudentId(student.getId());

        logger.debug("Student username: {} has {} slots", username, count);
        return new SlotCountResponseDTO(count);
    }

    /**
     * Converts integer day of week to DayOfWeek enum.
     *
     * @param dayInt integer day of week (0=Monday, 1=Tuesday, ..., 6=Sunday)
     * @return corresponding DayOfWeek
     * @throws ValidationException when the input is outside 0..6
     */
    private DayOfWeek convertIntToDayOfWeek(Integer dayInt) {
        return switch (dayInt) {
            case 0 -> DayOfWeek.MONDAY;
            case 1 -> DayOfWeek.TUESDAY;
            case 2 -> DayOfWeek.WEDNESDAY;
            case 3 -> DayOfWeek.THURSDAY;
            case 4 -> DayOfWeek.FRIDAY;
            case 5 -> DayOfWeek.SATURDAY;
            case 6 -> DayOfWeek.SUNDAY;
            default -> throw new ValidationException("Invalid day of week: " + dayInt);
        };
    }

}
