package it.eably.backend.dto.admin.response;

import it.eably.backend.model.BookingStatus;

/**
 * Booking counts for a user, broken down by role and status.
 * Used in the admin user-detail view to give moderators a quick overview
 * without issuing separate count queries per status.
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */
public record AdminUserBookingStatsDTO(
    long totalAsClient,
    long totalAsProvider,
    long totalDistinctBookings,
    long paymentPending,
    long confirmed,
    long completed,
    long cancelled
) {
    /** Returns the pre-computed count for the given status. Exhaustive — compiler enforces all cases. */
    public long countForStatus(BookingStatus status) {
        return switch (status) {
            case PAYMENT_PENDING -> paymentPending;
            case CONFIRMED -> confirmed;
            case COMPLETED -> completed;
            case CANCELLED -> cancelled;
        };
    }
}

