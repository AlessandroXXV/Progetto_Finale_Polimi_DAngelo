package it.eably.backend.dto.booking.response;

import it.eably.backend.model.BookingStatus;

import java.time.LocalDate;

/**
 * Lightweight booking payload used by week-based calendars.
 *
 * Keeps only the data needed to mark occupied weekly slots.
 *
 * @param bookingId booking identifier
 * @param slotId availability slot identifier
 * @param bookingDate calendar date for the booking occurrence
 * @param status booking lifecycle status
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */
public record BookingCalendarEntryDTO(
        Long bookingId,
        Long slotId,
        LocalDate bookingDate,
        BookingStatus status
) {}

