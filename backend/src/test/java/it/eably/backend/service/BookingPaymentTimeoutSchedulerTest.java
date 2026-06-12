package it.eably.backend.service;

import it.eably.backend.model.Booking;
import it.eably.backend.repository.BookingRepository;
import it.eably.backend.service.def.BookingService;
import it.eably.backend.service.impl.BookingPaymentTimeoutScheduler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingPaymentTimeoutSchedulerTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BookingService bookingService;

    @Test
    void cancelExpiredPendingPayments_NoExpiredBookings() {
        when(bookingRepository.findPaymentPendingBookingsExpiredBefore(any(LocalDateTime.class)))
                .thenReturn(List.of());

        BookingPaymentTimeoutScheduler scheduler = new BookingPaymentTimeoutScheduler(
                bookingRepository, bookingService, 10
        );

        scheduler.cancelExpiredPendingPayments();

        verify(bookingRepository).findPaymentPendingBookingsExpiredBefore(any(LocalDateTime.class));
        verify(bookingService, never()).cancelBookingDueToTimeout(anyLong());
    }

    @Test
    void cancelExpiredPendingPayments_WithExpiredBookings_CallsServiceForEach() {
        Booking booking1 = new Booking();
        booking1.setId(10L);

        Booking booking2 = new Booking();
        booking2.setId(20L);

        when(bookingRepository.findPaymentPendingBookingsExpiredBefore(any(LocalDateTime.class)))
                .thenReturn(List.of(booking1, booking2));

        BookingPaymentTimeoutScheduler scheduler = new BookingPaymentTimeoutScheduler(
                bookingRepository, bookingService, 10
        );

        scheduler.cancelExpiredPendingPayments();

        verify(bookingService).cancelBookingDueToTimeout(10L);
        verify(bookingService).cancelBookingDueToTimeout(20L);
        verify(bookingRepository, never()).saveAll(anyList());
    }

    @Test
    void cancelExpiredPendingPayments_ServiceThrowsOnOne_ContinuesWithOthers() {
        Booking booking1 = new Booking();
        booking1.setId(10L);

        Booking booking2 = new Booking();
        booking2.setId(20L);

        when(bookingRepository.findPaymentPendingBookingsExpiredBefore(any(LocalDateTime.class)))
                .thenReturn(List.of(booking1, booking2));
        doThrow(new RuntimeException("DB error")).when(bookingService).cancelBookingDueToTimeout(10L);

        BookingPaymentTimeoutScheduler scheduler = new BookingPaymentTimeoutScheduler(
                bookingRepository, bookingService, 10
        );

        assertDoesNotThrow(() -> scheduler.cancelExpiredPendingPayments());
        verify(bookingService).cancelBookingDueToTimeout(10L);
        verify(bookingService).cancelBookingDueToTimeout(20L);
    }
}
