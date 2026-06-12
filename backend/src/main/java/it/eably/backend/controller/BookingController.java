package it.eably.backend.controller;

import it.eably.backend.dto.booking.request.BookingRequestDTO;
import it.eably.backend.dto.booking.response.BookingResponseDTO;
import it.eably.backend.dto.booking.response.BookingCalendarEntryDTO;
import it.eably.backend.mapper.BookingMapper;
import it.eably.backend.model.Booking;
import it.eably.backend.model.BookingStatus;
import it.eably.backend.model.User;
import it.eably.backend.service.def.BookingService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * REST controller for booking operations.
 *
 * Endpoints:
 * <ul>
 * <li>POST /api/v1/bookings - Create new booking</li>
 * <li>POST /api/v1/bookings/{id}/confirm - Confirm booking after payment</li>
 * <li>POST /api/v1/bookings/{id}/complete - Complete booking with confirmation code</li>
 * <li>POST /api/v1/bookings/{id}/cancel - Cancel booking</li>
 * <li>GET /api/v1/bookings/{id} - Get booking by ID</li>
 * <li>GET /api/v1/bookings/my - Get current user's bookings</li>
 * <li>GET /api/v1/bookings/status/{status} - Get bookings by status</li>
 * </ul>
 *
 * Security:
 * - All endpoints require authentication
 * - User ID extracted from JWT token via Spring Security
 * - Authorization checks performed in service layer
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */
@RestController
@RequestMapping("/api/v1/bookings")
public class BookingController {

    private static final Logger log = LoggerFactory.getLogger(BookingController.class);

    private final BookingService bookingService;
    private final BookingMapper bookingMapper;

    public BookingController(BookingService bookingService,
                             BookingMapper bookingMapper) {
        this.bookingService = bookingService;
        this.bookingMapper = bookingMapper;
    }

    /**
     * Creates a new booking.
     *
     * Extracts client ID from authenticated user (JWT token).
     * Validates request data using Jakarta Bean Validation.
     * Uses secure mapping to hide confirmation code on newly created bookings.
     *
     * @param request the booking request DTO
     * @param user Spring Security authentication object
     * @return created booking response DTO with confirmation code hidden
     */
    @PostMapping
    public ResponseEntity<BookingResponseDTO> createBooking (
            @Valid @RequestBody BookingRequestDTO request,
            @AuthenticationPrincipal User user) {

        log.info("Creating booking for slot {}", request.slotId());

        // Extract user ID from JWT token
        Long clientId = user.getId();

        // Create booking via service
        Booking booking = bookingService.createBooking(
                clientId,
                request.slotId(),
                request.profileId(),
                request.notes(),
                request.bookingDate()
        );

        // Use secure mapping to hide confirmation codes until COMPLETED
        BookingResponseDTO response = bookingMapper.toResponseDTO(booking);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Confirms a booking after successful payment.
     *
     * Updates booking status from PAYMENT_PENDING to CONFIRMED.
     * Generates 6-digit confirmation code.
     * Confirmation code is visible in response.
     *
     * @param id booking ID
     * @param payload request body containing paymentIntentId
     * @return confirmed booking response DTO with confirmation code visible
     */
    @PostMapping("/{id}/confirm")
    public ResponseEntity<BookingResponseDTO> confirmBooking (
            @PathVariable Long id,
            @RequestBody Map<String, String> payload,
            @AuthenticationPrincipal User user) {

        log.info("Confirming booking {}", id);

        String paymentIntentId = payload.get("paymentIntentId");
        if (paymentIntentId == null || paymentIntentId.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        Booking booking = bookingService.confirmBooking(id, paymentIntentId, user.getId());
        // Use full mapping (not secure) because we just confirmed and need to show the code
        BookingResponseDTO response = bookingMapper.toResponseDTOFull(booking);

        return ResponseEntity.ok(response);
    }

    /**
     * Completes a booking after service delivery.
     *
     * Validates 6-digit confirmation code.
     * Updates booking status from CONFIRMED to COMPLETED.
     * Confirmation code remains visible in response.
     *
     * @param id booking ID
     * @param payload request body containing confirmationCode
     * @return completed booking response DTO with confirmation code visible
     */
    @PostMapping("/{id}/complete")
    public ResponseEntity<BookingResponseDTO> completeBooking(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload,
            @AuthenticationPrincipal User user) {

        log.info("Completing booking {}", id);

        String confirmationCode = payload.get("confirmationCode");
        if (confirmationCode == null || confirmationCode.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        Booking booking = bookingService.completeBooking(id, confirmationCode, user.getId());
        // Use full mapping for completed bookings to show confirmation code
        BookingResponseDTO response = bookingMapper.toResponseDTOFull(booking);

        return ResponseEntity.ok(response);
    }

    /**
     * Cancels a booking.
     *
     * Checks authorization (client or provider can cancel).
     * Marks availability slot as AVAILABLE again.
     * Uses secure mapping to hide confirmation code on cancelled bookings.
     *
     * @param id booking ID
     * @param userDetails Spring Security authentication User
     * @return cancelled booking response DTO with confirmation code hidden
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<BookingResponseDTO> cancelBooking(
            @PathVariable Long id,
            @AuthenticationPrincipal User userDetails) {

        log.info("Cancelling booking {}", id);

        Long userId = userDetails.getId();
        Booking booking = bookingService.cancelBooking(id, userId);
        // Use secure mapping for cancelled bookings too
        BookingResponseDTO response = bookingMapper.toResponseDTO(booking);

        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a booking by ID.
     *
     * Confirmation code is hidden unless booking status is COMPLETED.
     *
     * @param id booking ID
     * @return booking response DTO with confirmation code hidden as needed
     */
    @GetMapping("/{id}")
    public ResponseEntity<BookingResponseDTO> getBookingById(@PathVariable Long id, @AuthenticationPrincipal  User  user) {
        log.info("Fetching booking {}", id);

        Booking booking = bookingService.getBookingById(id);

        Long userId = user.getId();
        boolean isClientOwner = booking.getClient() != null && booking.getClient().getId().equals(userId);

        // Show confirmation code to the booking client as soon as booking is CONFIRMED.
        BookingResponseDTO response =
                isClientOwner && booking.getStatus() == BookingStatus.CONFIRMED
                        ? bookingMapper.toResponseDTOFull(booking)
                        : bookingMapper.toResponseDTO(booking);

        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves all bookings for the authenticated user.
     *
     * Returns bookings where user is either client or provider.
     * Combines both lists and removes duplicates.
     *
     * @param user Spring Security authentication object
     * @return list of booking response DTOs
     */
    @GetMapping("/my")
    public ResponseEntity<List<BookingResponseDTO>> getMyBookings(@AuthenticationPrincipal User user) {
        log.info("Fetching bookings for current user");

        Long userId = user.getId();

        // Get bookings as client
        List<Booking> clientBookings = bookingService.getBookingsByClient(userId);
        log.debug("Found {} bookings as client", clientBookings.size());

        // Get bookings as provider/student
        List<Booking> providerBookings = bookingService.getBookingsByProviderUser(userId);
        log.debug("Found {} bookings as provider", providerBookings.size());

        // Merge lists and remove duplicates by ID
        List<Booking> allBookings = Stream.concat(clientBookings.stream(), providerBookings.stream())
                .distinct()
                .collect(Collectors.toList());

        log.debug("Total bookings for user: {}", allBookings.size());

        // Use secure mapping to hide confirmation codes until COMPLETED
        List<BookingResponseDTO> response = bookingMapper.toResponseDTOList(allBookings);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves bookings by status.
     *
     * Admin endpoint for monitoring bookings.
     * Uses secure mapping to hide confirmation codes until COMPLETED.
     *
     * @param status booking status
     * @return list of booking response DTOs
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<BookingResponseDTO>> getBookingsByStatus(
            @PathVariable BookingStatus status) {

        log.info("Fetching bookings with status {}", status);

        List<Booking> bookings = bookingService.getBookingsByStatus(status);
        // Use secure mapping to hide confirmation codes
        List<BookingResponseDTO> response = bookingMapper.toResponseDTOList(bookings);

        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves the bookings of a provider (student) in calendar format.
     *
     * <p>
     * Returns a reduced list containing the booking ID, slot ID, date, and status,
     * useful for displaying the student's session calendar.
     * </p>
     *
     * @param providerUserId the ID of the student/provider user
     * @return list of {@link BookingCalendarEntryDTO}
     */
    @GetMapping("/provider/{providerUserId}")
    public ResponseEntity<List<BookingCalendarEntryDTO>> getBookingsByProviderUser(@PathVariable Long providerUserId) {
        List<BookingCalendarEntryDTO> response = bookingService.getBookingsByProviderUser(providerUserId).stream()
                .map(booking -> new BookingCalendarEntryDTO(
                        booking.getId(),
                        booking.getAvailabilitySlot().getId(),
                        booking.getBookingDate(),
                        booking.getStatus()
                ))
                .toList();
        return ResponseEntity.ok(response);
    }

}