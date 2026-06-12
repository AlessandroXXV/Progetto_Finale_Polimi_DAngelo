package it.eably.backend.mapper;

import it.eably.backend.dto.availabilitySlot.response.AvailabilitySlotResponseDTO;
import it.eably.backend.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AvailabilitySlotMapperTest {

    private AvailabilitySlotMapper mapper;
    private User student;

    @BeforeEach
    void setUp() {
        mapper = new AvailabilitySlotMapperImpl();

        student = new User();
        student.setId(1L);
        student.setUsername("student1");
    }

    // toResponseDTO tests

    @Test
    void toResponseDTO_NullSlot_ReturnsNull() {
        assertNull(mapper.toResponseDTO(null, false));
    }

    @Test
    void toResponseDTO_ValidSlot_NotBooked() {
        AvailabilitySlot slot = new AvailabilitySlot();
        slot.setId(10L);
        slot.setStudent(student);
        slot.setDayOfWeek(DayOfWeek.MONDAY);
        slot.setStartTime(LocalTime.of(9, 0));
        slot.setEndTime(LocalTime.of(10, 0));
        slot.setStatus(SlotStatus.AVAILABLE);

        AvailabilitySlotResponseDTO dto = mapper.toResponseDTO(slot, false);

        assertNotNull(dto);
        assertEquals(10L, dto.id());
        assertEquals(0, dto.dayOfWeek()); // MONDAY = 0
        assertEquals("09:00", dto.startTime());
        assertEquals("10:00", dto.endTime());
        assertEquals("AVAILABLE", dto.status());
        assertEquals(1L, dto.studentId());
        assertFalse(dto.isBooked());
    }

    @Test
    void toResponseDTO_ValidSlot_IsBooked() {
        AvailabilitySlot slot = new AvailabilitySlot();
        slot.setId(11L);
        slot.setStudent(student);
        slot.setDayOfWeek(DayOfWeek.FRIDAY);
        slot.setStartTime(LocalTime.of(14, 0));
        slot.setEndTime(LocalTime.of(15, 0));
        slot.setStatus(SlotStatus.BOOKED);

        AvailabilitySlotResponseDTO dto = mapper.toResponseDTO(slot, true);

        assertNotNull(dto);
        assertEquals(4, dto.dayOfWeek()); // FRIDAY = 4
        assertTrue(dto.isBooked());
    }

    @Test
    void toResponseDTO_NullStudent_StudentIdIsNull() {
        AvailabilitySlot slot = new AvailabilitySlot();
        slot.setId(12L);
        slot.setStudent(null);
        slot.setDayOfWeek(DayOfWeek.WEDNESDAY);
        slot.setStartTime(LocalTime.of(10, 0));
        slot.setEndTime(LocalTime.of(11, 0));
        slot.setStatus(SlotStatus.AVAILABLE);

        AvailabilitySlotResponseDTO dto = mapper.toResponseDTO(slot, false);

        assertNull(dto.studentId());
    }

    @Test
    void toResponseDTO_NullStartAndEndTime_FieldsNull() {
        AvailabilitySlot slot = new AvailabilitySlot();
        slot.setId(13L);
        slot.setStudent(student);
        slot.setDayOfWeek(DayOfWeek.TUESDAY);
        slot.setStartTime(null);
        slot.setEndTime(null);
        slot.setStatus(null);

        AvailabilitySlotResponseDTO dto = mapper.toResponseDTO(slot, false);

        assertNull(dto.startTime());
        assertNull(dto.endTime());
        assertNull(dto.status());
    }

    // dayOfWeekToInt tests

    @Test
    void dayOfWeekToInt_Null_ReturnsNull() {
        assertNull(mapper.dayOfWeekToInt(null));
    }

    @Test
    void dayOfWeekToInt_AllDays() {
        assertEquals(0, mapper.dayOfWeekToInt(DayOfWeek.MONDAY));
        assertEquals(1, mapper.dayOfWeekToInt(DayOfWeek.TUESDAY));
        assertEquals(2, mapper.dayOfWeekToInt(DayOfWeek.WEDNESDAY));
        assertEquals(3, mapper.dayOfWeekToInt(DayOfWeek.THURSDAY));
        assertEquals(4, mapper.dayOfWeekToInt(DayOfWeek.FRIDAY));
        assertEquals(5, mapper.dayOfWeekToInt(DayOfWeek.SATURDAY));
        assertEquals(6, mapper.dayOfWeekToInt(DayOfWeek.SUNDAY));
    }

    // toResponseDTOList tests

    @Test
    void toResponseDTOList_NullList_ReturnsNull() {
        assertNull(mapper.toResponseDTOList(null));
    }

    @Test
    void toResponseDTOList_SlotWithNoBooking_NotBooked() {
        AvailabilitySlot slot = new AvailabilitySlot();
        slot.setId(20L);
        slot.setStudent(student);
        slot.setDayOfWeek(DayOfWeek.MONDAY);
        slot.setStartTime(LocalTime.of(9, 0));
        slot.setEndTime(LocalTime.of(10, 0));
        slot.setStatus(SlotStatus.AVAILABLE);
        // bookings list is empty by default — no setup needed

        List<AvailabilitySlotResponseDTO> result = mapper.toResponseDTOList(List.of(slot));

        assertEquals(1, result.size());
        assertFalse(result.get(0).isBooked());
    }

    @Test
    void toResponseDTOList_SlotWithConfirmedBooking_IsBooked() {
        AvailabilitySlot slot = new AvailabilitySlot();
        slot.setId(21L);
        slot.setStudent(student);
        slot.setDayOfWeek(DayOfWeek.TUESDAY);
        slot.setStartTime(LocalTime.of(10, 0));
        slot.setEndTime(LocalTime.of(11, 0));
        slot.setStatus(SlotStatus.BOOKED);

        Booking booking = new Booking();
        booking.setStatus(BookingStatus.CONFIRMED);
        slot.setBookings(new ArrayList<>(List.of(booking)));

        List<AvailabilitySlotResponseDTO> result = mapper.toResponseDTOList(List.of(slot));

        assertEquals(1, result.size());
        assertTrue(result.get(0).isBooked());
    }

    @Test
    void toResponseDTOList_SlotWithCancelledBooking_NotBooked() {
        AvailabilitySlot slot = new AvailabilitySlot();
        slot.setId(22L);
        slot.setStudent(student);
        slot.setDayOfWeek(DayOfWeek.WEDNESDAY);
        slot.setStartTime(LocalTime.of(10, 0));
        slot.setEndTime(LocalTime.of(11, 0));
        slot.setStatus(SlotStatus.CANCELLED);

        Booking booking = new Booking();
        booking.setStatus(BookingStatus.CANCELLED);
        slot.setBookings(new ArrayList<>(List.of(booking)));

        List<AvailabilitySlotResponseDTO> result = mapper.toResponseDTOList(List.of(slot));

        assertFalse(result.get(0).isBooked());
    }

    @Test
    void toResponseDTOList_SlotWithCompletedBooking_NotBooked() {
        AvailabilitySlot slot = new AvailabilitySlot();
        slot.setId(23L);
        slot.setStudent(student);
        slot.setDayOfWeek(DayOfWeek.THURSDAY);
        slot.setStartTime(LocalTime.of(10, 0));
        slot.setEndTime(LocalTime.of(11, 0));
        slot.setStatus(SlotStatus.AVAILABLE);

        Booking booking = new Booking();
        booking.setStatus(BookingStatus.COMPLETED);
        slot.setBookings(new ArrayList<>(List.of(booking)));

        List<AvailabilitySlotResponseDTO> result = mapper.toResponseDTOList(List.of(slot));

        assertFalse(result.get(0).isBooked());
    }

    @Test
    void toResponseDTOList_MixedSlots() {
        AvailabilitySlot available = new AvailabilitySlot();
        available.setId(30L);
        available.setStudent(student);
        available.setDayOfWeek(DayOfWeek.MONDAY);
        available.setStartTime(LocalTime.of(9, 0));
        available.setEndTime(LocalTime.of(10, 0));
        available.setStatus(SlotStatus.AVAILABLE);

        AvailabilitySlot booked = new AvailabilitySlot();
        booked.setId(31L);
        booked.setStudent(student);
        booked.setDayOfWeek(DayOfWeek.TUESDAY);
        booked.setStartTime(LocalTime.of(10, 0));
        booked.setEndTime(LocalTime.of(11, 0));
        booked.setStatus(SlotStatus.BOOKED);
        Booking booking = new Booking();
        booking.setStatus(BookingStatus.PAYMENT_PENDING);
        booked.setBookings(new ArrayList<>(List.of(booking)));

        List<AvailabilitySlotResponseDTO> result = mapper.toResponseDTOList(Arrays.asList(available, booked));

        assertEquals(2, result.size());
        assertFalse(result.get(0).isBooked());
        assertTrue(result.get(1).isBooked());
    }
}
