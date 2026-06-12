package it.eably.backend.service.impl;

import it.eably.backend.exception.ValidationException;
import it.eably.backend.model.*;
import it.eably.backend.repository.AvailabilitySlotRepository;
import it.eably.backend.repository.BookingRepository;
import it.eably.backend.repository.ProfileRepository;
import it.eably.backend.repository.UserRepository;
import it.eably.backend.service.def.BookingService;
import it.eably.backend.service.def.PaymentService;
import it.eably.backend.service.observer.BookingObserver;
import it.eably.backend.service.observer.BookingSubject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Implementation of BookingService with thread-safe booking operations.
 *
 * 1. LOCK STRIPING PATTERN:
 *    - Uses ConcurrentHashMap to maintain one ReentrantLock per slot ID
 *    - Prevents contention between bookings for different slots
 *    - Only bookings for the SAME slot compete for the lock
 *
 * 2. DOUBLE-LAYER LOCKING:
 *    - Application-level lock (ReentrantLock) prevents race conditions in JVM
 *    - Database-level lock (PESSIMISTIC_WRITE) prevents race conditions across instances
 *    - Together they provide ABSOLUTE guarantee against double-booking
 *
 * 3. LOCK ACQUISITION PATTERN:
 *    - computeIfAbsent() ensures thread-safe lock creation
 *    - try-finally ensures lock is ALWAYS released, even on exceptions
 *    - Prevents deadlocks and resource leaks
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */
@Service
public class BookingServiceImpl implements BookingService, BookingSubject {

    private static final Logger log = LoggerFactory.getLogger(BookingServiceImpl.class);

    private final BookingRepository bookingRepository;
    private final AvailabilitySlotRepository availabilitySlotRepository;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final PaymentService paymentService;
    
    // Observers list for the Observer Pattern (Thread-safe collection)
    private final List<BookingObserver> observers = new CopyOnWriteArrayList<>();

    /**
     * Builds the booking service with required dependencies.
     *
     * @param bookingRepository repository for bookings
     * @param availabilitySlotRepository repository for availability slots
     * @param userRepository repository for users
     * @param profileRepository repository for profiles
     * @param paymentService payment service integration
     * @param springInjectedObservers observers injected by Spring, may be null
     */
    public BookingServiceImpl(BookingRepository bookingRepository,
                              AvailabilitySlotRepository availabilitySlotRepository,
                              UserRepository userRepository,
                              ProfileRepository profileRepository,
                              PaymentService paymentService,
                              List<BookingObserver> springInjectedObservers) {
        this.bookingRepository = bookingRepository;
        this.availabilitySlotRepository = availabilitySlotRepository;
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.paymentService = paymentService;
        if (springInjectedObservers != null) {
            this.observers.addAll(springInjectedObservers);
        }
    }
    
    // ========== OBSERVER PATTERN IMPLEMENTATION ==========
    /**
     * Notifies all registered observers about a booking status change.
     *
     * <p>Effect: triggers observer callbacks; any observer error is logged and
     * does not interrupt the notification chain.</p>
     *
     * @param booking booking that changed status
     */
    @Override
    public void notifyObservers(Booking booking) {
        log.debug("Notifying {} observers of booking {} status change to {}", 
                  observers.size(), booking.getId(), booking.getStatus());
        for (BookingObserver observer : observers) {
            try {
                observer.onBookingStatusChanged(booking);
            } catch (Exception e) {
                // Catch all exceptions so one failing observer doesn't break the notification chain
                log.error("Observer {} failed to process notification for booking {}: {}", 
                          observer.getObserverName(), booking.getId(), e.getMessage(), e);
            }
        }
    }

    /**
     * LOCK STRIPING: Concurrent map of locks, one per slot ID.
     *
     * Why ConcurrentHashMap?
     * - Thread-safe without synchronizing the entire map
     * - computeIfAbsent() is atomic, preventing duplicate lock creation
     * - Allows concurrent access to different slots
     *
     * Why ReentrantLock?
     * - More flexible than synchronized blocks
     * - Explicit lock/unlock for better control
     * - Can be used with try-finally for guaranteed release
     * - Supports fairness policy if needed
     *
     * This is the heart of the multithreading solution.
     */
    private final ConcurrentMap<String, ReentrantLock> slotLocks = new ConcurrentHashMap<>();


    /**
     * Creates a new booking with ABSOLUTE double-booking prevention.
     *
     * MULTITHREADING STRATEGY:
     *
     * STEP 1: LOCK ACQUISITION (Application Level)
     * - computeIfAbsent() atomically creates lock if not exists
     * - Only ONE thread per slot can proceed past lock.lock()
     * - Other threads for the SAME slot wait here
     * - Threads for DIFFERENT slots proceed concurrently (lock striping benefit)
     *
     * STEP 2: DATABASE LOCK (Persistence Level)
     * - findByIdWithLock() uses PESSIMISTIC_WRITE
     * - Prevents race conditions across multiple application instances
     * - Database holds the lock until transaction commits
     *
     * STEP 3: BUSINESS VALIDATION
     * - Check slot status is AVAILABLE
     * - If not, throw exception (lock released in finally)
     *
     * STEP 4: ATOMIC STATE CHANGE
     * - Create booking in PAYMENT_PENDING status
     * - Mark slot as BOOKED
     * - Both changes in same transaction (atomicity)
     *
     * STEP 5: LOCK RELEASE
     * - finally block GUARANTEES lock release
     * - Even if exception occurs, lock is freed
     * - Next waiting thread can proceed
     */
    @Override
    @Transactional
    /**
     * Creates a new booking with double-booking prevention.
     *
     * <p>Effect: acquires an application lock and a DB lock, validates business
     * rules, creates a booking in PAYMENT_PENDING, and notifies observers.</p>
     *
     * @param clientId client id
     * @param slotId availability slot id
     * @param profileId profile id requested
     * @param notes optional notes
     * @param bookingDate date for the booking
     * @return created booking in PAYMENT_PENDING
     * @throws ValidationException when validation fails or authorization is invalid
     */
    public Booking createBooking(Long clientId, Long slotId, Long profileId, String notes, LocalDate bookingDate) {
        log.info("Creating booking for client {} slot {} profile {}", clientId, slotId, profileId);

        if (bookingDate == null) {
            throw new ValidationException("Booking date is required");
        }
        if (bookingDate.isBefore(LocalDate.now())) {
            throw new ValidationException("Booking date must be today or in the future");
        }

        // Key creation for lock striping: combines slot ID and booking date
        // to allow concurrent bookings on the same slot for different dates
        String lockKey = slotId + ":" + bookingDate;

        // Atomically retrieve or create a lock for this specific slot-date pair.
        ReentrantLock lock = slotLocks.computeIfAbsent(lockKey, k -> new ReentrantLock());

        // Acquire the application-level lock.
        lock.lock();
        log.debug("Lock acquired for {}", lockKey);

        try {
            // STEP 2: LOAD ENTITIES WITH DATABASE-LEVEL LOCK
            // DB Lock: PESSIMISTIC_WRITE prevents concurrent transactions across JVM instances from modifying the slot.
            AvailabilitySlot slot = availabilitySlotRepository.findByIdWithLock(slotId)
                    .orElseThrow(() -> new ValidationException("Availability slot not found with ID: " + slotId));

            // Check if the slot is available
            if (slot.getStatus() != SlotStatus.AVAILABLE) {
                throw new ValidationException("Slot is not available: " + slot.getStatus());
            }

            // Client identity and verification gating.
            User client = userRepository.findById(clientId)
                    .orElseThrow(() -> new ValidationException("Client not found with ID: " + clientId));

            if (client.getRole() != UserRole.CLIENT) {
                throw new ValidationException("Only CLIENT users can create bookings");
            }

            if (!Boolean.TRUE.equals(client.getIsVerified())) {
                throw new ValidationException("Your account is not verified. Complete identity verification before creating bookings");
            }

            // The provider is the student who owns the availability slot.
            User provider = slot.getStudent();

            // Resolve the specific service profile requested.
            Profile profile = profileRepository.findById(profileId)
                    .orElseThrow(() -> new ValidationException("Profile not found with ID: " + profileId));

            // Cross-check: the profile must belong to the slot owner.
            if (!profile.getUser().getId().equals(provider.getId())) {
                throw new ValidationException("The selected profile does not belong to the student who owns this slot");
            }

            // STEP 3: BUSINESS VALIDATION
            // Ensure the date matches the recurring day of the week defined in the slot.
            if (slot.getDayOfWeek() != bookingDate.getDayOfWeek()) {
                throw new ValidationException("Booking date does not match slot weekday");
            }

            // Permanent block: if the client failed to pay for the exact same slot/date recently, prevent retries.
            boolean clientTimedOutOnSameOccurrence = bookingRepository
                    .existsByClientIdAndAvailabilitySlotIdAndBookingDateAndStatusAndCancellationReason(
                            clientId,
                            slotId,
                            bookingDate,
                            BookingStatus.CANCELLED,
                            BookingCancellationReason.PAYMENT_TIMEOUT
                    );

            if (clientTimedOutOnSameOccurrence) {
                throw new ValidationException("Prenotazione bloccata in modo permanente: pagamento non completato entro 10 minuti per questa data e fascia oraria");
            }

            // Uniqueness check: a slot cannot have two active bookings on the same date.
            boolean alreadyBookedForDate = bookingRepository.existsActiveByAvailabilitySlotIdAndBookingDate(slotId, bookingDate);
            if (alreadyBookedForDate) {
                throw new ValidationException("Availability slot is already booked for the selected date");
            }

            // STEP 4: CALCULATE TOTAL COST
            // Derive the price based on the profile hourly rate and slot duration.
            BigDecimal hourlyRate = profile.getHourlyRate();

            // Get the duration in minutes
            long durationMinutes = slot.getDurationMinutes();

            BigDecimal totalAmount = hourlyRate
                    // Convert the amount to cost per minute
                    .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP)

                    // Multiply by the duration in minutes
                    .multiply(BigDecimal.valueOf(durationMinutes))

                    // Round to the nearest cent
                    .setScale(2, RoundingMode.HALF_UP);

            log.debug("Calculated total amount: {} (hourlyRate: {}, duration: {} min)",
                      totalAmount, hourlyRate, durationMinutes);

            // STEP 5: INITIALIZE BOOKING ENTITY
            Booking booking = new Booking();
            booking.setClient(client);
            booking.setProvider(provider);
            booking.setProfile(profile);
            booking.setAvailabilitySlot(slot);
            booking.setStatus(BookingStatus.PAYMENT_PENDING);
            booking.setCancellationReason(null);
            booking.setTotalAmount(totalAmount);
            booking.setNotes(notes);
            booking.setBookedAt(LocalDateTime.now());
            booking.setBookingDate(bookingDate);

            // Save booking entity
            Booking savedBooking = bookingRepository.save(booking);

            log.info("Booking created successfully with ID: {}", savedBooking.getId());

            // Dispatch events to registered observers.
            notifyObservers(savedBooking);
            return savedBooking;

        } finally {
            // GUARANTEED RELEASE: lock must be unlocked even if a runtime exception occurs to avoid deadlocks.
            lock.unlock();

            // Release the lock only if it belongs to the current slot
            // slotLocks.remove(lockKey, lock);
            log.debug("Lock released for {}", lockKey);
        }
    }

    @Override
    @Transactional
    /**
     * Confirms a booking after validating the payment intent.
     *
     * <p>Effect: sets status to CONFIRMED, stores payment intent id, generates
     * a confirmation code, and notifies observers.</p>
     *
     * @param bookingId booking id
     * @param paymentIntentId payment intent id
     * @param requesterId id of the requesting user
     * @return updated booking in CONFIRMED status
     * @throws ValidationException when the requester is not authorized or the state is invalid
     */
    public Booking confirmBooking(Long bookingId, String paymentIntentId, Long requesterId) {
        log.info("Confirming booking {} with payment intent {}", bookingId, paymentIntentId);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ValidationException("Booking not found with ID: " + bookingId));

        // Authorization gate.
        if (!booking.getClient().getId().equals(requesterId)) {
            throw new ValidationException("User is not authorized to confirm this booking");
        }
        // Validate current status
        if (booking.getStatus() != BookingStatus.PAYMENT_PENDING) {
            throw new ValidationException("Booking cannot be confirmed. Current status: " + booking.getStatus());
        }

        // Bridge to payment provider to verify the intent is authorized and matches our records.
        paymentService.validatePaymentIntentForBooking(bookingId, paymentIntentId);

        // Generate 6-digit confirmation code
        String confirmationCode = generateConfirmationCode();

        // Update booking
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setPaymentIntentId(paymentIntentId);
        booking.setConfirmationCode(confirmationCode);

        Booking savedBooking = bookingRepository.save(booking);
        log.info("Booking {} confirmed with code {}", bookingId, confirmationCode);

        // Notify observers about the booking confirmation
        notifyObservers(savedBooking);

        return savedBooking;
    }

    @Override
    @Transactional
    /**
     * Completes a booking after confirming the provided code.
     *
     * <p>Effect: sets status to COMPLETED and captures payment if present.</p>
     *
     * @param bookingId booking id
     * @param confirmationCode confirmation code provided by the client
     * @param requesterId id of the provider user
     * @return updated booking in COMPLETED status
     * @throws ValidationException when the requester is not authorized, the state is invalid,
     *                             or the code does not match
     */
    public Booking completeBooking(Long bookingId, String confirmationCode, Long requesterId) {
        log.info("Completing booking {} with confirmation code", bookingId);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ValidationException("Booking not found with ID: " + bookingId));

        // Only the performing student can mark the session as complete.
        if (!booking.getProvider().getId().equals(requesterId)) {
            throw new ValidationException("User is not authorized to complete this booking");
        }

        // Validate current booking status
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new ValidationException("Booking cannot be completed. Current status: " + booking.getStatus());
        }

        // OTP verification using the confirmation code generated at payment time.
        if (booking.getConfirmationCode() == null ||
            !booking.getConfirmationCode().equals(confirmationCode)) {
            log.warn("Invalid confirmation code for booking {}", bookingId);
            throw new ValidationException("Invalid confirmation code");
        }

        // Transition to final state.
        booking.setStatus(BookingStatus.COMPLETED);

        // Bridge to Stripe to capture the previously authorized funds.
        if (booking.getPaymentIntentId() != null && !booking.getPaymentIntentId().isBlank()) {
            paymentService.capturePayment(bookingId);
        }

        Booking savedBooking = bookingRepository.save(booking);
        log.info("Booking {} completed successfully", bookingId);

        // Notify observers about the booking completion
        notifyObservers(savedBooking);

        return savedBooking;
    }

    @Override
    @Transactional
    /**
     * Cancels a booking if the requester is authorized and state allows it.
     *
     * <p>Effect: sets status to CANCELLED and cancels payment if present.</p>
     *
     * @param bookingId booking id
     * @param userId id of the requesting user
     * @return updated booking in CANCELLED status
     * @throws ValidationException when not authorized or booking cannot be cancelled
     */
    public Booking cancelBooking(Long bookingId, Long userId) {
        log.info("Cancelling booking {} by user {}", bookingId, userId);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ValidationException("Booking not found with ID: " + bookingId));

        // Check authorization
        if (!isBookingOwner(bookingId, userId)) {
            throw new ValidationException("User is not authorized to cancel this booking");
        }

        // Check if booking can be cancelled
        if (!booking.canBeCancelled()) {
            throw new ValidationException("Booking cannot be cancelled. Current status: " + booking.getStatus());
        }

        // Update booking status
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancellationReason(BookingCancellationReason.USER_CANCELLED);

        // Bridge to Stripe to release authorized funds if they haven't been captured yet.
        if (booking.getPaymentIntentId() != null && !booking.getPaymentIntentId().isBlank()) {
            paymentService.cancelPayment(bookingId);
        }

        // Save booking
        Booking savedBooking = bookingRepository.save(booking);

        log.info("Booking {} cancelled successfully", bookingId);

        // Notify observers about the booking cancellation
        notifyObservers(savedBooking);

        return savedBooking;
    }

    @Override
    @Transactional(readOnly = true)
    /**
     * Retrieves a booking by id.
     *
     * @param bookingId booking id
     * @return booking if found
     * @throws ValidationException when booking is not found
     */
    public Booking getBookingById(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ValidationException("Booking not found with ID: " + bookingId));
    }

    @Override
    @Transactional(readOnly = true)
    /**
     * Retrieves all bookings for a client.
     *
     * @param clientId client id
     * @return list of client bookings
     */
    public List<Booking> getBookingsByClient(Long clientId) {
        return bookingRepository.findByClientId(clientId);
    }


    @Override
    @Transactional(readOnly = true)
    /**
     * Retrieves all bookings for a provider user.
     *
     * @param providerUserId provider user id
     * @return list of provider bookings
     */
    public List<Booking> getBookingsByProviderUser(Long providerUserId) {
        log.debug("Fetching bookings for provider user {}", providerUserId);
        return bookingRepository.findByProviderId(providerUserId);
    }

    @Override
    @Transactional(readOnly = true)
    /**
     * Retrieves bookings by status.
     *
     * @param status booking status
     * @return list of bookings with the given status
     */
    public List<Booking> getBookingsByStatus(BookingStatus status) {
        return bookingRepository.findByStatus(status);
    }

    @Override
    @Transactional(readOnly = true)
    /**
     * Checks whether the user (by id) is owner of the booking.
     *
     * <p>Owner means client or provider.</p>
     *
     * @param bookingId booking id
     * @param userId user id
     * @return true if the user is owner
     * @throws ValidationException when booking is not found
     */
    public boolean isBookingOwner(Long bookingId, Long userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ValidationException("Booking not found with ID: " + bookingId));

        // User is owner if they are the client or the provider (student)
        boolean isClient = booking.getClient().getId().equals(userId);
        boolean isProvider = booking.getProvider().getId().equals(userId);

        return isClient || isProvider;
    }

    @Override
    @Transactional(readOnly = true)
    /**
     * Checks whether the user (by username) is owner of the booking.
     *
     * @param bookingId booking id
     * @param username username of the user
     * @return true if the user is owner
     * @throws ValidationException when user or booking is not found
     */
    public boolean isBookingOwner(Long bookingId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ValidationException("User not found with username: " + username));

        return isBookingOwner(bookingId, user.getId());
    }

    @Override
    @Transactional
    /**
     * Cancels a booking when payment times out.
     *
     * <p>Effect: sets status to CANCELLED with PAYMENT_TIMEOUT reason and
     * notifies observers.</p>
     *
     * @param bookingId booking id
     * @throws ValidationException when booking is not found
     */
    public void cancelBookingDueToTimeout(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ValidationException("Booking not found: " + bookingId));

        // Idempotency: only cancel if still awaiting payment.
        if (booking.getStatus() != BookingStatus.PAYMENT_PENDING) {
            return;
        }

        // Automated state transition for unpaid sessions.
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancellationReason(BookingCancellationReason.PAYMENT_TIMEOUT);
        bookingRepository.save(booking);

        // Notify to trigger deletion of the payment intent or cleanup tasks.
        notifyObservers(booking);
    }

    /**
     * Generates a random 6-digit confirmation code.
     *
     * @return 6-digit numeric string
     */
    private String generateConfirmationCode() {
        int code = 100000 + java.util.concurrent.ThreadLocalRandom.current().nextInt(900000);
        return String.valueOf(code);
    }
}
