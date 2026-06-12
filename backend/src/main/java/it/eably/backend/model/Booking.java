package it.eably.backend.model;

import it.eably.backend.exception.ValidationException;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity representing a service reservation between a client and a student provider.
 *
 * <p>Table columns: {@code id, client_id, provider_id, profile_id, availability_slot_id,
 * status, total_amount, notes, booked_at, confirmation_code, payment_intent_id}.</p>
 *
 * <ul>
 *   <li>{@code client_id}            – {@link User} with role {@code CLIENT} who made the booking</li>
 *   <li>{@code provider_id}          – {@link User} with role {@code STUDENT} who delivers the service</li>
 *   <li>{@code profile_id}           – specific {@link Profile} (service) chosen by the client</li>
 *   <li>{@code availability_slot_id} – the availability slot used for this booking</li>
 * </ul>
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */
@Entity
@Table(name = "bookings", indexes = {
    @Index(name = "idx_booking_client_id", columnList = "client_id"),
    @Index(name = "idx_booking_provider_id", columnList = "provider_id"),
    @Index(name = "idx_booking_profile_id", columnList = "profile_id"),
    @Index(name = "idx_booking_status", columnList = "status"),
    @Index(name = "idx_booking_booked_at", columnList = "booked_at"),
    @Index(name = "idx_booking_booking_date", columnList = "booking_date")
})
public class Booking extends BaseEntity {

    /** The client who made the booking. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private User client;

    /** The student provider who delivers the service. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false)
    private User provider;

    /** The specific service profile chosen by the client. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private Profile profile;

    /**
     * The availability slot used for this booking.
     *
     * <p>Many-to-one: a single recurring slot may appear in multiple bookings over time.</p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "availability_slot_id", nullable = false)
    private AvailabilitySlot availabilitySlot;

    /** Current lifecycle status of the booking. Defaults to {@link BookingStatus#PAYMENT_PENDING}. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    // Default to PAYMENT_PENDING to match active creation flow (createBooking sets PAYMENT_PENDING)
    private BookingStatus status = BookingStatus.PAYMENT_PENDING;

    /** Total amount charged for this booking (must be positive). */
    @Column(name = "total_amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    /** Optional notes provided by the client at booking time. */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    /** Timestamp when the booking was created. */
    @Column(name = "booked_at", nullable = false)
    private LocalDateTime bookedAt;

    /** The calendar date on which the session is scheduled. */
    @Column(name = "booking_date")
    private LocalDate bookingDate;

    /** 6-character alphanumeric confirmation code sent to the client. */
    @Column(name = "confirmation_code", length = 6)
    private String confirmationCode;

    /** Stripe PaymentIntent ID associated with this booking's payment. */
    @Column(name = "payment_intent_id", length = 255)
    private String paymentIntentId;

    /** Reason for cancellation, populated only when the booking is cancelled. */
    @Enumerated(EnumType.STRING)
    @Column(name = "cancellation_reason", length = 40)
    private BookingCancellationReason cancellationReason;

    /** The review left for this booking, if any. */
    @OneToOne(mappedBy = "booking", fetch = FetchType.LAZY)
    private Review review;

    // Constructors

    /** Default no-arg constructor required by JPA. */
    public Booking() {}

    /**
     * Full constructor for creating a new booking.
     *
     * @param client             the client making the booking
     * @param provider           the student provider delivering the service
     * @param profile            the service profile chosen by the client
     * @param availabilitySlot   the availability slot used
     * @param status             the initial booking status
     * @param totalAmount        the total amount charged
     * @param notes              optional notes from the client
     * @param bookedAt           timestamp when the booking was created
     * @param confirmationCode   6-character confirmation code
     * @param paymentIntentId    Stripe PaymentIntent ID
     */
    public Booking(User client, User provider, Profile profile, AvailabilitySlot availabilitySlot,
                   BookingStatus status, BigDecimal totalAmount, String notes,
                   LocalDateTime bookedAt, String confirmationCode, String paymentIntentId) {
        this.client = client;
        this.provider = provider;
        this.profile = profile;
        this.availabilitySlot = availabilitySlot;
        this.status = status;
        this.totalAmount = totalAmount;
        this.notes = notes;
        this.bookedAt = bookedAt;
        this.confirmationCode = confirmationCode;
        this.paymentIntentId = paymentIntentId;
    }

    // Getters and Setters

    public User getClient() { return client; }
    public void setClient(User client) { this.client = client; }

    public User getProvider() { return provider; }
    public void setProvider(User provider) { this.provider = provider; }

    public Profile getProfile() { return profile; }
    public void setProfile(Profile profile) { this.profile = profile; }

    public AvailabilitySlot getAvailabilitySlot() { return availabilitySlot; }
    public void setAvailabilitySlot(AvailabilitySlot availabilitySlot) { this.availabilitySlot = availabilitySlot; }

    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getBookedAt() { return bookedAt; }
    public void setBookedAt(LocalDateTime bookedAt) { this.bookedAt = bookedAt; }

    public LocalDate getBookingDate() { return bookingDate; }
    public void setBookingDate(LocalDate bookingDate) { this.bookingDate = bookingDate; }

    public String getConfirmationCode() { return confirmationCode; }
    public void setConfirmationCode(String confirmationCode) { this.confirmationCode = confirmationCode; }

    public String getPaymentIntentId() { return paymentIntentId; }
    public void setPaymentIntentId(String paymentIntentId) { this.paymentIntentId = paymentIntentId; }

    public BookingCancellationReason getCancellationReason() { return cancellationReason; }
    public void setCancellationReason(BookingCancellationReason cancellationReason) { this.cancellationReason = cancellationReason; }

    public Review getReview() { return review; }
    public void setReview(Review review) { this.review = review; }

    @Override
    public void validate() {
        if (client == null) throw new ValidationException("Booking must have a client");
        if (provider == null) throw new ValidationException("Booking must have a provider");
        if (profile == null) throw new ValidationException("Booking must have a profile");
        if (availabilitySlot == null) throw new ValidationException("Booking must have an availability slot");
        if (status == null) throw new ValidationException("Booking status cannot be null");
        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0)
            throw new ValidationException("Total amount must be positive");
        if (bookedAt == null) throw new ValidationException("Booked at timestamp cannot be null");
        if (confirmationCode != null && confirmationCode.length() != 6)
            throw new ValidationException("Confirmation code must be exactly 6 characters");
    }

    /**
     * Checks whether this booking can still be cancelled based on its current status.
     *
     * @return {@code true} if the current status is cancellable
     */
    public boolean canBeCancelled() { return status.isCancellable(); }

    /**
     * Checks whether this booking has reached a terminal state.
     *
     * @return {@code true} if the current status is a final state
     */
    public boolean isFinalState() { return status.isFinalState(); }

    /**
     * Updates the booking status to the given value.
     *
     * @param newStatus the new status to set
     */
    public void updateStatus(BookingStatus newStatus) { this.status = newStatus; }
}
