package it.eably.backend.service;

import it.eably.backend.dto.availabilitySlot.request.AvailabilitySlotRequestDTO;
import it.eably.backend.dto.availabilitySlot.response.AvailabilitySlotResponseDTO;
import it.eably.backend.dto.common.response.SlotCountResponseDTO;
import it.eably.backend.exception.AuthorizationException;
import it.eably.backend.exception.ConflictException;
import it.eably.backend.exception.ResourceNotFoundException;
import it.eably.backend.exception.ValidationException;
import it.eably.backend.mapper.AvailabilitySlotMapper;
import it.eably.backend.model.*;
import it.eably.backend.repository.AvailabilitySlotRepository;
import it.eably.backend.repository.BookingRepository;
import it.eably.backend.repository.UserRepository;
import it.eably.backend.service.impl.AvailabilityServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AvailabilityService.
 *
 * Tests cover:
 * - Slot creation with overlap detection
 * - Slot deletion with active booking checks
 * - Slot retrieval with booking status
 * - Slot counting
 * - Authorization checks
 * - Validation logic
 *
 * Target: 100% line and branch coverage.
 */
@ExtendWith(MockitoExtension.class)
class AvailabilityServiceTest {

    @Mock
    private AvailabilitySlotRepository availabilitySlotRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AvailabilitySlotMapper availabilitySlotMapper;

    @InjectMocks
    private AvailabilityServiceImpl availabilityService;

    private User student;
    private AvailabilitySlot slot;
    private AvailabilitySlotResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        student = new User();
        student.setId(1L);
        student.setUsername("student");
        student.setEmail("student@test.com");
        student.setPasswordHash("$2a$12$hash");
        student.setRole(UserRole.STUDENT);
        student.setIsActive(true);
        student.setIsVerified(true);
        student.setStripeConnected(true);

        slot = new AvailabilitySlot();
        slot.setId(1L);
        slot.setStudent(student);
        slot.setDayOfWeek(DayOfWeek.MONDAY);
        slot.setStartTime(LocalTime.of(10, 0));
        slot.setEndTime(LocalTime.of(11, 0));
        slot.setStatus(SlotStatus.AVAILABLE);

        responseDTO = new AvailabilitySlotResponseDTO(1L, 0, "10:00", "11:00", "AVAILABLE", 1L, false);
    }

    // ========== CREATE SLOT TESTS ==========

    @Test
    void testCreateSlot_Success() {
        AvailabilitySlotRequestDTO requestDTO = new AvailabilitySlotRequestDTO(0, "10:00", "11:00");

        when(userRepository.findByUsername("student")).thenReturn(Optional.of(student));
        when(availabilitySlotRepository.countOverlappingSlotsByStudent(
                anyLong(), any(DayOfWeek.class), any(LocalTime.class), any(LocalTime.class)))
            .thenReturn(0L);
        when(availabilitySlotRepository.save(any(AvailabilitySlot.class))).thenReturn(slot);
        when(availabilitySlotMapper.toResponseDTO(any(AvailabilitySlot.class), anyBoolean()))
            .thenReturn(responseDTO);

        AvailabilitySlotResponseDTO result = availabilityService.createSlot("student", requestDTO);

        assertNotNull(result);
        assertEquals(1L, result.id());
        assertEquals(0, result.dayOfWeek());
        assertEquals("10:00", result.startTime());
        assertEquals("11:00", result.endTime());
        assertFalse(result.isBooked());

        verify(availabilitySlotRepository).save(any(AvailabilitySlot.class));
    }

    @Test
    void testCreateSlot_UserNotFound_ThrowsException() {
        AvailabilitySlotRequestDTO requestDTO = new AvailabilitySlotRequestDTO(0, "10:00", "11:00");

        when(userRepository.findByUsername("student")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
            availabilityService.createSlot("student", requestDTO));

        verify(availabilitySlotRepository, never()).save(any());
    }

    @Test
    void testCreateSlot_StripeNotConnected_ThrowsValidationException() {
        AvailabilitySlotRequestDTO requestDTO = new AvailabilitySlotRequestDTO(0, "10:00", "11:00");

        student.setStripeConnected(false);
        when(userRepository.findByUsername("student")).thenReturn(Optional.of(student));

        assertThrows(ValidationException.class, () ->
            availabilityService.createSlot("student", requestDTO));

        verify(availabilitySlotRepository, never()).save(any());
    }

    @Test
    void testCreateSlot_EndTimeBeforeStartTime_ThrowsValidationException() {
        AvailabilitySlotRequestDTO requestDTO = new AvailabilitySlotRequestDTO(0, "11:00", "10:00");

        when(userRepository.findByUsername("student")).thenReturn(Optional.of(student));

        assertThrows(ValidationException.class, () ->
            availabilityService.createSlot("student", requestDTO));

        verify(availabilitySlotRepository, never()).save(any());
    }

    @Test
    void testCreateSlot_DurationExceeds60Minutes_ThrowsValidationException() {
        AvailabilitySlotRequestDTO requestDTO = new AvailabilitySlotRequestDTO(0, "10:00", "11:30");

        when(userRepository.findByUsername("student")).thenReturn(Optional.of(student));
        when(availabilitySlotRepository.countOverlappingSlotsByStudent(
                anyLong(), any(DayOfWeek.class), any(LocalTime.class), any(LocalTime.class)))
            .thenReturn(0L);

        assertThrows(ValidationException.class, () ->
            availabilityService.createSlot("student", requestDTO));

        verify(availabilitySlotRepository, never()).save(any());
    }

    @Test
    void testCreateSlot_OverlapDetected_ThrowsConflictException() {
        AvailabilitySlotRequestDTO requestDTO = new AvailabilitySlotRequestDTO(0, "10:00", "11:00");

        when(userRepository.findByUsername("student")).thenReturn(Optional.of(student));
        when(availabilitySlotRepository.countOverlappingSlotsByStudent(
                anyLong(), any(DayOfWeek.class), any(LocalTime.class), any(LocalTime.class)))
            .thenReturn(1L);

        assertThrows(ConflictException.class, () ->
            availabilityService.createSlot("student", requestDTO));

        verify(availabilitySlotRepository, never()).save(any());
    }

    @Test
    void testCreateSlot_InvalidDayOfWeek_ThrowsValidationException() {
        AvailabilitySlotRequestDTO requestDTO = new AvailabilitySlotRequestDTO(7, "10:00", "11:00");

        when(userRepository.findByUsername("student")).thenReturn(Optional.of(student));

        assertThrows(ValidationException.class, () ->
            availabilityService.createSlot("student", requestDTO));

        verify(availabilitySlotRepository, never()).save(any());
    }

    // ========== DELETE SLOT TESTS ==========

    @Test
    void testDeleteSlot_Success() {
        when(availabilitySlotRepository.findByIdWithLock(1L)).thenReturn(Optional.of(slot));
        when(userRepository.findByUsername("student")).thenReturn(Optional.of(student));
        when(bookingRepository.existsActiveByAvailabilitySlotId(1L)).thenReturn(false);

        availabilityService.deleteSlot(1L, "student");

        assertEquals(SlotStatus.CANCELLED, slot.getStatus());
        verify(availabilitySlotRepository).save(slot);
        verify(availabilitySlotRepository, never()).delete(any());
    }

    @Test
    void testDeleteSlot_SlotNotFound_ThrowsException() {
        when(availabilitySlotRepository.findByIdWithLock(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
            availabilityService.deleteSlot(1L, "student"));

        verify(availabilitySlotRepository, never()).save(any());
    }

    @Test
    void testDeleteSlot_NotOwner_ThrowsAuthorizationException() {
        when(availabilitySlotRepository.findByIdWithLock(1L)).thenReturn(Optional.of(slot));

        User otherStudent = new User();
        otherStudent.setId(999L);
        when(userRepository.findByUsername("other")).thenReturn(Optional.of(otherStudent));

        assertThrows(AuthorizationException.class, () ->
            availabilityService.deleteSlot(1L, "other"));

        verify(availabilitySlotRepository, never()).save(any());
    }

    @Test
    void testDeleteSlot_WithActiveBooking_ThrowsConflictException() {
        when(availabilitySlotRepository.findByIdWithLock(1L)).thenReturn(Optional.of(slot));
        when(userRepository.findByUsername("student")).thenReturn(Optional.of(student));
        when(bookingRepository.existsActiveByAvailabilitySlotId(1L)).thenReturn(true);

        assertThrows(ConflictException.class, () ->
            availabilityService.deleteSlot(1L, "student"));

        verify(availabilitySlotRepository, never()).save(any());
    }

    @Test
    void testDeleteSlot_WithNonActiveBooking_Success() {
        when(availabilitySlotRepository.findByIdWithLock(1L)).thenReturn(Optional.of(slot));
        when(userRepository.findByUsername("student")).thenReturn(Optional.of(student));
        when(bookingRepository.existsActiveByAvailabilitySlotId(1L)).thenReturn(false);

        availabilityService.deleteSlot(1L, "student");

        assertEquals(SlotStatus.CANCELLED, slot.getStatus());
        verify(availabilitySlotRepository).save(slot);
        verify(availabilitySlotRepository, never()).delete(any());
    }

    // ========== GET STUDENT SLOTS TESTS ==========

    @Test
    void testGetStudentSlots_Success() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(availabilitySlotRepository.findByStudentIdWithBooking(1L)).thenReturn(List.of(slot));
        when(availabilitySlotMapper.toResponseDTOList(anyList())).thenReturn(List.of(responseDTO));

        List<AvailabilitySlotResponseDTO> result = availabilityService.getStudentSlots(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).id());
        assertFalse(result.get(0).isBooked());
    }

    @Test
    void testGetStudentSlots_WithBookedSlot() {
        Booking booking = new Booking();
        booking.setId(1L);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setClient(new User());
        booking.setProvider(student);
        booking.setAvailabilitySlot(slot);
        booking.setTotalAmount(new BigDecimal("50.00"));
        booking.setBookedAt(LocalDateTime.now());

        slot.setBookings(new ArrayList<>(List.of(booking)));

        AvailabilitySlotResponseDTO bookedDTO =
            new AvailabilitySlotResponseDTO(1L, 0, "10:00", "11:00", "BOOKED", 1L, true);

        when(userRepository.existsById(1L)).thenReturn(true);
        when(availabilitySlotRepository.findByStudentIdWithBooking(1L)).thenReturn(List.of(slot));
        when(availabilitySlotMapper.toResponseDTOList(anyList())).thenReturn(List.of(bookedDTO));

        List<AvailabilitySlotResponseDTO> result = availabilityService.getStudentSlots(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).isBooked());
    }

    @Test
    void testGetStudentSlots_UserNotFound_ThrowsException() {
        when(userRepository.existsById(1L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () ->
            availabilityService.getStudentSlots(1L));
    }

    @Test
    void testGetStudentSlots_EmptyList() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(availabilitySlotRepository.findByStudentIdWithBooking(1L)).thenReturn(List.of());
        when(availabilitySlotMapper.toResponseDTOList(anyList())).thenReturn(List.of());

        List<AvailabilitySlotResponseDTO> result = availabilityService.getStudentSlots(1L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ========== GET MY SLOT COUNT TESTS ==========

    @Test
    void testGetMySlotCount_Success() {
        when(userRepository.findByUsername("student")).thenReturn(Optional.of(student));
        when(availabilitySlotRepository.countByStudentId(1L)).thenReturn(5L);

        SlotCountResponseDTO result = availabilityService.getMySlotCount("student");

        assertNotNull(result);
        assertEquals(5L, result.count());
        assertTrue(result.hasAvailability());
    }

    @Test
    void testGetMySlotCount_ZeroSlots() {
        when(userRepository.findByUsername("student")).thenReturn(Optional.of(student));
        when(availabilitySlotRepository.countByStudentId(1L)).thenReturn(0L);

        SlotCountResponseDTO result = availabilityService.getMySlotCount("student");

        assertNotNull(result);
        assertEquals(0L, result.count());
        assertFalse(result.hasAvailability());
    }

    @Test
    void testGetMySlotCount_UserNotFound_ThrowsException() {
        when(userRepository.findByUsername("student")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
            availabilityService.getMySlotCount("student"));
    }

}
