package it.eably.backend.repository;

import it.eably.backend.model.AvailabilitySlot;
import it.eably.backend.model.SlotStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for {@link it.eably.backend.model.AvailabilitySlot} entity.
 *
 * Slots are linked to the student (student_id = users.id with role STUDENT).
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */
@Repository
public interface AvailabilitySlotRepository extends JpaRepository<AvailabilitySlot, Long> {

    /**
     * Finds a slot by ID with a pessimistic write lock.
     * Used in deleteSlot to prevent race conditions with concurrent booking creation.
     *
     * @param id the slot ID
     * @return Optional containing the slot if found
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM AvailabilitySlot s WHERE s.id = :id")
    Optional<AvailabilitySlot> findByIdWithLock(@Param("id") Long id);

    /**
     * Finds all slots for a student with their associated bookings (LEFT JOIN FETCH).
     * Excludes cancelled slots and orders results by day of week and start time.
     *
     * @param studentId the student user ID
     * @return list of non-cancelled slots with bookings eagerly loaded
     */
    @Query("SELECT DISTINCT s FROM AvailabilitySlot s " +
           "LEFT JOIN FETCH s.bookings b " +
           "WHERE s.student.id = :studentId " +
           "AND s.status <> it.eably.backend.model.SlotStatus.CANCELLED " +
           "ORDER BY s.dayOfWeek, s.startTime")
    List<AvailabilitySlot> findByStudentIdWithBooking(@Param("studentId") Long studentId);

    /**
     * Counts non-cancelled slots for a student.
     *
     * @param studentId the student user ID
     * @return number of non-cancelled slots
     */
    @Query("SELECT COUNT(s) FROM AvailabilitySlot s " +
           "WHERE s.student.id = :studentId " +
           "AND s.status <> it.eably.backend.model.SlotStatus.CANCELLED")
    long countByStudentId(@Param("studentId") Long studentId);

    /**
     * Counts all slots for a student excluding a specific status.
     * Typically used to count active (non-cancelled) slots.
     *
     * @param studentId the student user ID
     * @param status    the slot status to exclude
     * @return number of slots not matching the given status
     */
    long countAllByStudentIdAndStatusNot(Long studentId, SlotStatus status);
    

    /**
     * Counts overlapping slots for a student on a given day.
     * Used to prevent creation of slots that overlap in time.
     *
     * @param studentId the student user ID
     * @param dayOfWeek the day of the week to check
     * @param startTime the start time of the new slot
     * @param endTime   the end time of the new slot
     * @return number of existing slots that overlap with the given time range
     */
    @Query("SELECT COUNT(s) FROM AvailabilitySlot s " +
           "WHERE s.student.id = :studentId " +
           "AND s.dayOfWeek = :dayOfWeek " +
           "AND s.status <> it.eably.backend.model.SlotStatus.CANCELLED " +
           "AND (s.startTime < :endTime AND s.endTime > :startTime)")
    long countOverlappingSlotsByStudent(@Param("studentId") Long studentId,
                                        @Param("dayOfWeek") DayOfWeek dayOfWeek,
                                        @Param("startTime") LocalTime startTime,
                                        @Param("endTime") LocalTime endTime);
}
