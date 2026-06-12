package it.eably.backend.service.impl;

import it.eably.backend.model.Booking;
import it.eably.backend.repository.BookingRepository;
import it.eably.backend.service.def.BookingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled job that cancels bookings stuck in PAYMENT_PENDING beyond a timeout.
 *
 * <p>Enabled by configuration property and runs at a fixed delay.</p>
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */
@Service
@ConditionalOnProperty(value = "eably.booking.payment-timeout-scheduler-enabled", havingValue = "true", matchIfMissing = true)
public class BookingPaymentTimeoutScheduler {

    /** Logger for scheduler events. */
    private static final Logger log = LoggerFactory.getLogger(BookingPaymentTimeoutScheduler.class);

    /** Repository for booking reads. */
    private final BookingRepository bookingRepository;
    /** Booking service used to cancel timeouts. */
    private final BookingService bookingService;
    /** Timeout window in minutes for pending payments. */
    private final long paymentTimeoutMinutes;

    /**
     * Builds the scheduler with required dependencies and timeout configuration.
     *
     * @param bookingRepository repository for bookings
     * @param bookingService booking service to cancel timeouts
     * @param paymentTimeoutMinutes timeout in minutes before cancellation
     */
    public BookingPaymentTimeoutScheduler(
            BookingRepository bookingRepository,
            BookingService bookingService,
            @Value("${eably.booking.payment-timeout-minutes:10}") long paymentTimeoutMinutes
    ) {
        this.bookingRepository = bookingRepository;
        this.bookingService = bookingService;
        this.paymentTimeoutMinutes = paymentTimeoutMinutes;
    }

    /**
     * Cancels pending payments that exceeded the timeout threshold.
     *
     * <p>Effect: searches for expired PAYMENT_PENDING bookings and attempts to cancel them.</p>
     */
    @Scheduled(fixedDelayString = "${eably.booking.payment-timeout-check-interval-ms:30000}")
    public void cancelExpiredPendingPayments() {
        // Calculate the absolute cutoff time based on current business rules.
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(paymentTimeoutMinutes);

        // Batch query for all relevant bookings that haven't been completed or cancelled on time.
        List<Booking> expired = bookingRepository.findPaymentPendingBookingsExpiredBefore(cutoff);

        if (expired.isEmpty()) {
            return; // Quick exit to reduce processing overhead.
        }

        int cancelled = 0;
        for (Booking booking : expired) {
            try {
                // Execute individual cancellation which also triggers Observer notifications (e.g., UI updates via WebSocket).
                bookingService.cancelBookingDueToTimeout(booking.getId());
                cancelled++;
            } catch (Exception ex) {
                // Safe handling to ensure one failing record doesn't block the entire batch cleanup.
                log.error("Failed to cancel booking {} due to timeout", booking.getId(), ex);
            }
        }

        log.info("Cancelled {} payment-pending bookings due to timeout", cancelled);
    }
}


