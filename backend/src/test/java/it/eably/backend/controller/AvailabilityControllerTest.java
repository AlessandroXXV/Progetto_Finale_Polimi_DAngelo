package it.eably.backend.controller;

import it.eably.backend.dto.availabilitySlot.request.AvailabilitySlotRequestDTO;
import it.eably.backend.dto.availabilitySlot.response.AvailabilitySlotResponseDTO;
import it.eably.backend.dto.common.response.SlotCountResponseDTO;
import it.eably.backend.exception.ConflictException;
import it.eably.backend.exception.ResourceNotFoundException;
import it.eably.backend.model.User;
import it.eably.backend.service.def.AvailabilityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AvailabilityController.
 *
 * Tests all endpoints by directly calling controller methods.
 * Target: 100% controller coverage.
 */
@ExtendWith(MockitoExtension.class)
class AvailabilityControllerTest {

    @Mock
    private AvailabilityService availabilityService;

    @InjectMocks
    private AvailabilityController availabilityController;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("testuser");
    }

    @Test
    void testCreateSlot_Success() {
        AvailabilitySlotRequestDTO requestDTO = new AvailabilitySlotRequestDTO(0, "10:00", "11:00");
        AvailabilitySlotResponseDTO responseDTO = new AvailabilitySlotResponseDTO(
            1L, 0, "10:00", "11:00", "AVAILABLE", 1L, false
        );

        when(availabilityService.createSlot(anyString(), any(AvailabilitySlotRequestDTO.class)))
            .thenReturn(responseDTO);

        ResponseEntity<AvailabilitySlotResponseDTO> response =
            availabilityController.createSlot(requestDTO, mockUser);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().id());
        assertEquals(0, response.getBody().dayOfWeek());

        verify(availabilityService).createSlot(eq("testuser"), eq(requestDTO));
    }

    @Test
    void testCreateSlot_Overlap_ThrowsConflictException() {
        AvailabilitySlotRequestDTO requestDTO = new AvailabilitySlotRequestDTO(0, "10:00", "11:00");

        when(availabilityService.createSlot(anyString(), any(AvailabilitySlotRequestDTO.class)))
            .thenThrow(new ConflictException("Slot overlaps with existing availability slot"));

        assertThrows(ConflictException.class, () ->
            availabilityController.createSlot(requestDTO, mockUser));
    }

    @Test
    void testDeleteSlot_Success() {
        doNothing().when(availabilityService).deleteSlot(anyLong(), anyString());

        ResponseEntity<Void> response = availabilityController.deleteSlot(1L, mockUser);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(availabilityService).deleteSlot(eq(1L), eq("testuser"));
    }

    @Test
    void testDeleteSlot_NotFound_ThrowsResourceNotFoundException() {
        doThrow(new ResourceNotFoundException("Slot not found"))
            .when(availabilityService).deleteSlot(anyLong(), anyString());

        assertThrows(ResourceNotFoundException.class, () ->
            availabilityController.deleteSlot(999L, mockUser));
    }

    @Test
    void testDeleteSlot_WithActiveBooking_ThrowsConflictException() {
        doThrow(new ConflictException("Cannot delete slot with active booking"))
            .when(availabilityService).deleteSlot(anyLong(), anyString());

        assertThrows(ConflictException.class, () ->
            availabilityController.deleteSlot(1L, mockUser));
    }

    @Test
    void testGetStudentSlots_Success() {
        AvailabilitySlotResponseDTO slot1 = new AvailabilitySlotResponseDTO(
            1L, 0, "10:00", "11:00", "AVAILABLE", 1L, false
        );
        AvailabilitySlotResponseDTO slot2 = new AvailabilitySlotResponseDTO(
            2L, 1, "14:00", "15:00", "BOOKED", 1L, true
        );

        when(availabilityService.getStudentSlots(1L)).thenReturn(List.of(slot1, slot2));

        ResponseEntity<List<AvailabilitySlotResponseDTO>> response =
            availabilityController.getStudentSlots(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        assertEquals(1L, response.getBody().get(0).id());
        assertTrue(response.getBody().get(1).isBooked());

        verify(availabilityService).getStudentSlots(1L);
    }

    @Test
    void testGetStudentSlots_EmptyList() {
        when(availabilityService.getStudentSlots(1L)).thenReturn(List.of());

        ResponseEntity<List<AvailabilitySlotResponseDTO>> response =
            availabilityController.getStudentSlots(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    void testGetMySlotCount_Success() {
        SlotCountResponseDTO responseDTO = new SlotCountResponseDTO(5L);

        when(availabilityService.getMySlotCount(anyString())).thenReturn(responseDTO);

        ResponseEntity<SlotCountResponseDTO> response =
            availabilityController.getMySlotCount(mockUser);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(5L, response.getBody().count());
        assertTrue(response.getBody().hasAvailability());

        verify(availabilityService).getMySlotCount("testuser");
    }

    @Test
    void testGetMySlotCount_ZeroSlots() {
        SlotCountResponseDTO responseDTO = new SlotCountResponseDTO(0L);

        when(availabilityService.getMySlotCount(anyString())).thenReturn(responseDTO);

        ResponseEntity<SlotCountResponseDTO> response =
            availabilityController.getMySlotCount(mockUser);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(0L, response.getBody().count());
        assertFalse(response.getBody().hasAvailability());
    }
}
