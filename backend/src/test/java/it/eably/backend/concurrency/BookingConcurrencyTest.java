package it.eably.backend.concurrency;

import it.eably.backend.exception.ValidationException;
import it.eably.backend.model.*;
import it.eably.backend.repository.AvailabilitySlotRepository;
import it.eably.backend.repository.BookingRepository;
import it.eably.backend.repository.ProfileRepository;
import it.eably.backend.repository.UserRepository;
import it.eably.backend.service.def.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for concurrent booking operations.
 * 
 * This test demonstrates that the Lock Striping
 * pattern with ReentrantLock prevents double-booking in a concurrent environment.
 * 
 * TEST SCENARIO:
 * - Create 1 availability slot
 * - Create 10 client users
 * - Launch 10 threads simultaneously trying to book the SAME slot
 * - Expected result: EXACTLY 1 booking succeeds, 9 fail with ValidationException
 * 
 * WHY THIS PROVES CORRECTNESS:
 * - Without locking: Multiple bookings would succeed (race condition)
 * - With incorrect locking: Deadlocks or inconsistent state
 * - With correct Lock Striping: Exactly 1 success, 9 failures
 * 
 * @author Alessandro D'Angelo
 * @version 1.0
 */
@SpringBootTest
@ActiveProfiles("test")
@EnabledIfSystemProperty(named = "runPostgresIT", matches = "true")
public class BookingConcurrencyTest {
    
    private static final Logger log = LoggerFactory.getLogger(BookingConcurrencyTest.class);
    
    @Autowired
    private BookingService bookingService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ProfileRepository profileRepository;
    
    @Autowired
    private AvailabilitySlotRepository availabilitySlotRepository;
    
    @Autowired
    private BookingRepository bookingRepository;
    
    private User provider;
    private Profile providerProfile;
    private AvailabilitySlot testSlot;
    private List<User> clients;
    
    /**
     * Sets up test data before each test.
     * 
     * Creates:
     * - 1 provider user with profile
     * - 1 availability slot (Monday 10:00-11:00)
     * - 10 client users
     */
    @BeforeEach
    @Transactional
    public void setUp() {
        // Clean up any existing data
        bookingRepository.deleteAll();
        availabilitySlotRepository.deleteAll();
        profileRepository.deleteAll();
        userRepository.deleteAll();
        
        // Create provider user
        provider = new User();
        provider.setUsername("provider_test");
        provider.setEmail("provider@test.com");
        provider.setPasswordHash("$2a$12$hashedpassword");
        provider.setRole(UserRole.STUDENT);
        provider.setIsActive(true);
        provider.setIsVerified(true);
        provider = userRepository.save(provider);
        
        // Create provider profile
        providerProfile = new Profile();
        providerProfile.setUser(provider);
        providerProfile.setTitle("Math Tutor");
        providerProfile.setDescription("Expert in mathematics");
        providerProfile.setHourlyRate(new BigDecimal("50.00"));
        providerProfile.setDeliveryMode(DeliveryMode.ONLINE);
        providerProfile.setIsActive(true);
        providerProfile = profileRepository.save(providerProfile);
        
        // Create availability slot (Monday 10:00-11:00)
        testSlot = new AvailabilitySlot();
        testSlot.setStudent(provider);
        testSlot.setDayOfWeek(DayOfWeek.MONDAY);
        testSlot.setStartTime(LocalTime.of(10, 0));
        testSlot.setEndTime(LocalTime.of(11, 0));
        testSlot.setStatus(SlotStatus.AVAILABLE);
        testSlot = availabilitySlotRepository.save(testSlot);
        
        // Create 10 client users
        clients = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            User client = new User();
            client.setUsername("client_" + i);
            client.setEmail("client" + i + "@test.com");
            client.setPasswordHash("$2a$12$hashedpassword");
            client.setRole(UserRole.CLIENT);
            client.setIsActive(true);
            client.setIsVerified(true);
            client = userRepository.save(client);
            clients.add(client);
        }
        
        log.info("Test setup complete: 1 provider, 1 slot, 10 clients");
    }
    
    /**
     * Concurrent booking prevention.
     * 
     * This test simulates 10 threads trying to book the same slot simultaneously.
     * 
     * EXPECTED BEHAVIOR:
     * - Exactly 1 booking succeeds (slot is booked)
     * - Exactly 9 bookings fail (ValidationException: slot not available)
     * 
     * HOW IT WORKS:
     * 1. Create ExecutorService with 10 threads
     * 2. Submit 10 booking tasks simultaneously
     * 3. Use CountDownLatch to ensure all threads start at the same time
     * 4. Count successes and failures
     * 5. Assert: 1 success, 9 failures
     * 
     * WHY THIS PROVES LOCK STRIPING WORKS:
     * - Without locking: Multiple threads would succeed (race condition)
     * - With Lock Striping: Only 1 thread acquires the lock, books the slot
     * - Other 9 threads: Wait for lock, then see slot is BOOKED, throw exception
     * 
     * @throws InterruptedException if thread execution is interrupted
     */
    @Test
    public void testConcurrentBooking_OnlyOneSucceeds() throws InterruptedException {
        log.info("=== STARTING CONCURRENCY TEST ===");
        log.info("Slot ID: {}, Status: {}", testSlot.getId(), testSlot.getStatus());
        
        // Number of concurrent threads
        final int THREAD_COUNT = 10;
        final LocalDate bookingDate = LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));

        // ExecutorService to manage threads
        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);
        
        // CountDownLatch to synchronize thread start (all threads start at the same time)
        CountDownLatch startLatch = new CountDownLatch(1);
        
        // CountDownLatch to wait for all threads to complete
        CountDownLatch completionLatch = new CountDownLatch(THREAD_COUNT);
        
        // Atomic counters for thread-safe counting
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        
        // List to store results from each thread
        List<Future<BookingResult>> futures = new ArrayList<>();
        
        // Submit 10 booking tasks
        for (int i = 0; i < THREAD_COUNT; i++) {
            final int clientIndex = i;
            
            Future<BookingResult> future = executorService.submit(() -> {
                try {
                    // Wait for all threads to be ready
                    startLatch.await();
                    
                    log.info("Thread {} attempting to book slot {}", clientIndex, testSlot.getId());
                    
                    // Attempt to create booking
                    Booking booking = bookingService.createBooking(
                            clients.get(clientIndex).getId(),
                            testSlot.getId(),
                            providerProfile.getId(),
                            "Concurrent booking test",
                            bookingDate
                    );
                    
                    log.info("Thread {} SUCCESS: Booking created with ID {}", clientIndex, booking.getId());
                    successCount.incrementAndGet();
                    return new BookingResult(true, null, booking.getId());
                    
                } catch (ValidationException e) {
                    log.info("Thread {} FAILED: {}", clientIndex, e.getMessage());
                    failureCount.incrementAndGet();
                    return new BookingResult(false, e.getMessage(), null);
                    
                } catch (Exception e) {
                    log.error("Thread {} UNEXPECTED ERROR: {}", clientIndex, e.getMessage(), e);
                    return new BookingResult(false, "Unexpected error: " + e.getMessage(), null);
                    
                } finally {
                    completionLatch.countDown();
                }
            });
            
            futures.add(future);
        }
        
        // Release all threads at the same time (simulates concurrent access)
        log.info("Releasing all {} threads simultaneously...", THREAD_COUNT);
        startLatch.countDown();
        
        // Wait for all threads to complete (timeout: 30 seconds)
        boolean completed = completionLatch.await(30, TimeUnit.SECONDS);
        assertTrue(completed, "All threads should complete within 30 seconds");
        
        // Shutdown executor
        executorService.shutdown();
        executorService.awaitTermination(5, TimeUnit.SECONDS);
        
        // Log results
        log.info("=== CONCURRENCY TEST RESULTS ===");
        log.info("Total threads: {}", THREAD_COUNT);
        log.info("Successful bookings: {}", successCount.get());
        log.info("Failed bookings: {}", failureCount.get());
        
        // Print detailed results
        for (int i = 0; i < futures.size(); i++) {
            try {
                BookingResult result = futures.get(i).get();
                if (result.success) {
                    log.info("Client {}: SUCCESS (Booking ID: {})", i, result.bookingId);
                } else {
                    log.info("Client {}: FAILED ({})", i, result.errorMessage);
                }
            } catch (Exception e) {
                log.error("Error getting result for client {}: {}", i, e.getMessage());
            }
        }
        
        // Key assertions
        log.info("=== VERIFYING LOCK STRIPING CORRECTNESS ===");
        
        // Assertion 1: Exactly 1 booking should succeed
        assertEquals(1, successCount.get(), 
                "LOCK STRIPING VERIFICATION: Exactly 1 booking should succeed");
        
        // Assertion 2: Exactly 9 bookings should fail
        assertEquals(9, failureCount.get(), 
                "LOCK STRIPING VERIFICATION: Exactly 9 bookings should fail");
        
        // Assertion 3: Total should be 10
        assertEquals(THREAD_COUNT, successCount.get() + failureCount.get(),
                "Total results should equal thread count");
        
        // Assertion 4: Slot remains AVAILABLE because occupancy is tracked by bookingDate
        AvailabilitySlot updatedSlot = availabilitySlotRepository.findById(testSlot.getId())
                .orElseThrow(() -> new AssertionError("Slot not found"));
        assertEquals(SlotStatus.AVAILABLE, updatedSlot.getStatus(),
                "Slot status should remain AVAILABLE because booking lock is date-scoped");

        // Assertion 5: Verify exactly 1 booking exists in database
        long bookingCount = bookingRepository.count();
        assertEquals(1, bookingCount,
                "Exactly 1 booking should exist in database");
        
        log.info("=== TEST PASSED: LOCK STRIPING WORKS CORRECTLY ===");
        log.info("CONCLUSION: ReentrantLock with Lock Striping prevents double-booking");
        log.info("RESULT: This test demonstrates thread-safe booking");
    }
    
    /**
     * Helper class to store booking attempt results.
     */
    private static class BookingResult {
        final boolean success;
        final String errorMessage;
        final Long bookingId;
        
        BookingResult(boolean success, String errorMessage, Long bookingId) {
            this.success = success;
            this.errorMessage = errorMessage;
            this.bookingId = bookingId;
        }
    }
}
