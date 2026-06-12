package it.eably.backend.model;

import it.eably.backend.exception.ValidationException;
import jakarta.persistence.*;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a recurring weekly availability time slot owned by a student.
 *
 * <p>Table columns: {@code id, student_id, day_of_week, start_time, end_time, status}.</p>
 *
 * <p>The slot belongs to a {@link User} with role {@code STUDENT} and is <em>not</em>
 * tied to a specific profile or service — the client selects the profile at booking time.</p>
 *
 * <p>Slot duration is capped at 60 minutes; validation is enforced by {@link #validate()}.</p>
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */
@Entity
@Table(name = "availability_slots", indexes = {
    @Index(name = "idx_slot_student_id", columnList = "student_id"),
    @Index(name = "idx_slot_status", columnList = "status"),
    @Index(name = "idx_slot_day_of_week", columnList = "day_of_week")
})
public class AvailabilitySlot extends BaseEntity {

    /** The student who owns this availability slot. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    /** Day of the week on which this slot recurs. */
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 10)
    private DayOfWeek dayOfWeek;

    /** Start time of the slot (inclusive). */
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    /** End time of the slot (exclusive). Must be after {@code startTime}. */
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    /** Current status of the slot. Defaults to {@link SlotStatus#AVAILABLE}. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SlotStatus status = SlotStatus.AVAILABLE;

    /** History of bookings associated with this recurring slot. */
    @OneToMany(mappedBy = "availabilitySlot", fetch = FetchType.LAZY)
    private List<Booking> bookings = new ArrayList<>();


    // Constructors

    /** Default no-arg constructor required by JPA. */
    public AvailabilitySlot() {}

    /**
     * Full constructor for creating a new availability slot.
     *
     * @param student    the student who owns the slot
     * @param dayOfWeek  the day of the week the slot recurs on
     * @param startTime  the start time of the slot
     * @param endTime    the end time of the slot
     * @param status     the initial status of the slot
     */
    public AvailabilitySlot(User student, DayOfWeek dayOfWeek, LocalTime startTime,
                            LocalTime endTime, SlotStatus status) {
        this.student = student;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
    }

    // Getters and Setters

    /** @return the student who owns this slot */
    public User getStudent() { return student; }
    public void setStudent(User student) { this.student = student; }

    /** @return the day of the week this slot recurs on */
    public DayOfWeek getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(DayOfWeek dayOfWeek) { this.dayOfWeek = dayOfWeek; }

    /** @return the start time of this slot */
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    /** @return the end time of this slot */
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

    /** @return the current status of this slot */
    public SlotStatus getStatus() { return status; }
    public void setStatus(SlotStatus status) { this.status = status; }

    /** @return the list of bookings associated with this slot */
    public List<Booking> getBookings() { return bookings; }
    public void setBookings(List<Booking> bookings) { this.bookings = bookings; }

    @Override
    public void validate() {
        if (student == null) throw new ValidationException("Availability slot must be associated with a student");
        if (dayOfWeek == null) throw new ValidationException("Day of week cannot be null");
        if (startTime == null) throw new ValidationException("Start time cannot be null");
        if (endTime == null) throw new ValidationException("End time cannot be null");
        if (!endTime.isAfter(startTime)) throw new ValidationException("End time must be after start time");

        long durationMinutes = Duration.between(startTime, endTime).toMinutes();
        if (durationMinutes > 60) {
            throw new ValidationException(
                String.format("Slot duration cannot exceed 60 minutes (current: %d minutes)", durationMinutes));
        }
        if (status == null) throw new ValidationException("Status cannot be null");
    }

    /**
     * Checks whether this slot is currently available for booking.
     *
     * @return {@code true} if status is {@link SlotStatus#AVAILABLE}
     */
    public boolean isAvailable() { return status == SlotStatus.AVAILABLE; }

    /**
     * Returns the duration of this slot in minutes.
     *
     * @return duration in minutes between {@code startTime} and {@code endTime}
     */
    public long getDurationMinutes() { return Duration.between(startTime, endTime).toMinutes(); }

    /** Marks this slot as booked. */
    public void markAsBooked() { this.status = SlotStatus.BOOKED; }

    /** Marks this slot as cancelled. */
    public void markAsCancelled() { this.status = SlotStatus.CANCELLED; }

    /** Marks this slot as available again. */
    public void markAsAvailable() { this.status = SlotStatus.AVAILABLE; }
}
