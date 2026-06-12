package it.eably.backend.controller;

import it.eably.backend.dto.booking.request.BookingRequestDTO;
import it.eably.backend.dto.booking.response.BookingResponseDTO;
import it.eably.backend.mapper.BookingMapper;
import it.eably.backend.model.AvailabilitySlot;
import it.eably.backend.model.Booking;
import it.eably.backend.model.BookingStatus;
import it.eably.backend.model.User;
import it.eably.backend.service.def.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingControllerTest {

    @Mock
    private BookingService bookingService;

    @Mock
    private BookingMapper bookingMapper;

    @InjectMocks
    private BookingController bookingController;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("testuser");
    }

    // ── create ──────────────────────────────────────────────────────────────

    @Test
    void createBooking_Success() {
        Booking booking = new Booking();
        booking.setId(1L);
        BookingResponseDTO responseDTO = buildResponseDTO(BookingStatus.PAYMENT_PENDING, null, null);
        LocalDate bookingDate = LocalDate.now().plusDays(1);

        when(bookingService.createBooking(anyLong(), anyLong(), anyLong(), anyString(), any(LocalDate.class)))
                .thenReturn(booking);
        when(bookingMapper.toResponseDTO(any())).thenReturn(responseDTO);

        ResponseEntity<BookingResponseDTO> response =
                bookingController.createBooking(new BookingRequestDTO(1L, 1L, bookingDate, "notes"), mockUser);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(bookingService).createBooking(eq(1L), eq(1L), eq(1L), eq("notes"), eq(bookingDate));
    }

    // ── confirm ──────────────────────────────────────────────────────────────

    @Test
    void confirmBooking_Success() {
        Booking booking = new Booking();
        BookingResponseDTO responseDTO = buildResponseDTO(BookingStatus.CONFIRMED, "123456", "pi_123");

        when(bookingService.confirmBooking(anyLong(), anyString(), anyLong())).thenReturn(booking);
        when(bookingMapper.toResponseDTOFull(any())).thenReturn(responseDTO);

        ResponseEntity<BookingResponseDTO> response =
                bookingController.confirmBooking(1L, Map.of("paymentIntentId", "pi_123"), mockUser);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(bookingService).confirmBooking(1L, "pi_123", 1L);
    }

    @Test
    void confirmBooking_MissingPaymentIntent_ReturnsBadRequest() {
        ResponseEntity<BookingResponseDTO> response = bookingController.confirmBooking(1L, Map.of(), mockUser);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(bookingService, never()).confirmBooking(anyLong(), anyString(), anyLong());
    }

    // ── complete ─────────────────────────────────────────────────────────────

    @Test
    void completeBooking_Success() {
        Booking booking = new Booking();
        BookingResponseDTO responseDTO = buildResponseDTO(BookingStatus.COMPLETED, "123456", "pi_123");

        when(bookingService.completeBooking(anyLong(), anyString(), anyLong())).thenReturn(booking);
        when(bookingMapper.toResponseDTOFull(any())).thenReturn(responseDTO);

        ResponseEntity<BookingResponseDTO> response =
                bookingController.completeBooking(1L, Map.of("confirmationCode", "123456"), mockUser);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(bookingService).completeBooking(1L, "123456", 1L);
    }

    @Test
    void completeBooking_MissingCode_ReturnsBadRequest() {
        ResponseEntity<BookingResponseDTO> response = bookingController.completeBooking(1L, Map.of(), mockUser);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    // ── cancel ────────────────────────────────────────────────────────────────

    @Test
    void cancelBooking_Success() {
        Booking booking = new Booking();
        BookingResponseDTO responseDTO = buildResponseDTO(BookingStatus.CANCELLED, null, null);

        when(bookingService.cancelBooking(anyLong(), anyLong())).thenReturn(booking);
        when(bookingMapper.toResponseDTO(any())).thenReturn(responseDTO);

        ResponseEntity<BookingResponseDTO> response = bookingController.cancelBooking(1L, mockUser);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(bookingService).cancelBooking(1L, 1L);
    }

    // ── get by id ─────────────────────────────────────────────────────────────

    @Test
    void getBookingById_Success() {
        Booking booking = new Booking();
        BookingResponseDTO responseDTO = buildResponseDTO(BookingStatus.CONFIRMED, "123456", "pi_123");

        when(bookingService.getBookingById(1L)).thenReturn(booking);
        when(bookingMapper.toResponseDTO(any())).thenReturn(responseDTO);

        ResponseEntity<BookingResponseDTO> response = bookingController.getBookingById(1L, mockUser);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(bookingService).getBookingById(1L);
    }

    // ── my bookings ───────────────────────────────────────────────────────────

    @Test
    void getMyBookings_ClientOnly() {
        when(bookingService.getBookingsByClient(1L)).thenReturn(List.of(new Booking()));
        when(bookingService.getBookingsByProviderUser(1L)).thenReturn(List.of());
        when(bookingMapper.toResponseDTOList(anyList())).thenReturn(List.of());

        ResponseEntity<List<BookingResponseDTO>> response = bookingController.getMyBookings(mockUser);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(bookingService).getBookingsByClient(1L);
        verify(bookingService).getBookingsByProviderUser(1L);
    }

    @Test
    void getMyBookings_ClientAndProvider() {
        when(bookingService.getBookingsByClient(1L)).thenReturn(List.of(new Booking()));
        when(bookingService.getBookingsByProviderUser(1L)).thenReturn(List.of(new Booking()));
        when(bookingMapper.toResponseDTOList(anyList())).thenReturn(List.of());

        ResponseEntity<List<BookingResponseDTO>> response = bookingController.getMyBookings(mockUser);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // ── by status ─────────────────────────────────────────────────────────────

    @Test
    void getBookingsByStatus_Success() {
        when(bookingService.getBookingsByStatus(BookingStatus.CONFIRMED)).thenReturn(List.of());
        when(bookingMapper.toResponseDTOList(anyList())).thenReturn(List.of());

        ResponseEntity<List<BookingResponseDTO>> response =
                bookingController.getBookingsByStatus(BookingStatus.CONFIRMED);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(bookingService).getBookingsByStatus(BookingStatus.CONFIRMED);
    }

    // ── by provider ───────────────────────────────────────────────────────────

    @Test
    void getBookingsByProviderUser_ReturnsCalendarEntries() {
        AvailabilitySlot slot = new AvailabilitySlot();
        slot.setId(20L);

        Booking booking = new Booking();
        booking.setId(30L);
        booking.setAvailabilitySlot(slot);
        booking.setBookingDate(LocalDate.of(2026, 1, 15));
        booking.setStatus(BookingStatus.CONFIRMED);

        when(bookingService.getBookingsByProviderUser(5L)).thenReturn(List.of(booking));

        ResponseEntity<?> response = bookingController.getBookingsByProviderUser(5L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(bookingService).getBookingsByProviderUser(5L);
    }


    // ── helpers ───────────────────────────────────────────────────────────────

    private BookingResponseDTO buildResponseDTO(BookingStatus status, String code, String pi) {
        return new BookingResponseDTO(
                1L, 1L, "testuser", "test@test.com", 1L, "provider", 1L, "Math Tutor",
                1L, "MONDAY", "10:00:00", "11:00:00", status,
                BigDecimal.valueOf(50), "notes", LocalDateTime.now(), code, pi
        );
    }
}
