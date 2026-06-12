package it.eably.backend.model;

/**
 * Enumeration of reasons why a booking was cancelled.
 *
 * <p>Populated on a {@link Booking} only when its status transitions to
 * {@link BookingStatus#CANCELLED}.</p>
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */
public enum BookingCancellationReason {

    /** The booking was explicitly cancelled by the client or provider. */
    USER_CANCELLED,

    /** The booking was automatically cancelled because payment was not completed in time. */
    PAYMENT_TIMEOUT
}

