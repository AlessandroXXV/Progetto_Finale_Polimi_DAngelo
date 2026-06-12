package it.eably.backend.repository;

import it.eably.backend.model.Profile;
import it.eably.backend.model.SlotStatus;
import it.eably.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

/**
 * Repository interface for Profile entity.
 * 
 * Provides CRUD operations and custom queries for Profile management.
 * A student can have up to 5 service profiles.
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */
@Repository
public interface ProfileRepository extends JpaRepository<Profile, Long> {

    /**
     * Finds all profiles belonging to a user.
     *
     * @param userId the user ID
     * @return list of profiles for that user
     */
    List<Profile> findAllByUserId(Long userId);

    /**
     * Finds all profiles belonging to a user (by User entity).
     *
     * @param user the user entity
     * @return list of profiles for that user
     */
    List<Profile> findAllByUser(User user);

    /**
     * Finds all active profiles.
     * 
     * @return list of active profiles
     */
    List<Profile> findByIsActiveTrue();

    /**
     * Finds active profiles by delivery mode.
     *
     * @param deliveryMode the delivery mode
     * @return list of active profiles with the specified delivery mode
     */
    List<Profile> findByDeliveryMode(it.eably.backend.model.DeliveryMode deliveryMode);

    /**
     * Counts profiles owned by a user.
     *
     * @param userId the user ID
     * @return number of profiles for that user
     */
    long countByUserId(Long userId);

    /**
     * Finds active profiles with hourly rate within a range.
     * 
     * @param minRate minimum hourly rate
     * @param maxRate maximum hourly rate
     * @return list of profiles within the rate range
     */
    @Query("SELECT p FROM Profile p WHERE p.isActive = true AND p.hourlyRate BETWEEN :minRate AND :maxRate")
    List<Profile> findActiveProfilesByHourlyRateRange(@Param("minRate") java.math.BigDecimal minRate,
                                                       @Param("maxRate") java.math.BigDecimal maxRate);

    /**
     * Searches active profiles using optional filters.
     * Boolean flags (e.g. {@code useCategory}) control which filters are applied,
     * allowing callers to pass {@code null} for unused parameters without breaking the query.
     *
     * @param useCategory            whether to filter by category keyword
     * @param category               keyword to match against the profile description
     * @param useMaxRate             whether to filter by maximum hourly rate
     * @param maxRate                maximum hourly rate (inclusive)
     * @param useAvailabilityFilters whether to filter by availability slot criteria
     * @param useDayOfWeek           whether to filter by day of week
     * @param dayOfWeek              the day of week to match
     * @param useStartTime           whether to filter by slot start time
     * @param startTime              the start time the slot must cover
     * @param useEndTime             whether to filter by slot end time
     * @param endTime                the end time the slot must cover
     * @param availableStatus        the slot status required (typically AVAILABLE)
     * @return list of active profiles matching all applied filters
     */
    @Query("""
            SELECT DISTINCT p
            FROM Profile p
            WHERE p.isActive = true
              AND (:useCategory = false OR LOWER(COALESCE(p.description, '')) LIKE LOWER(CONCAT('%', :category, '%')))
              AND (:useMaxRate = false OR p.hourlyRate <= :maxRate)
              AND (
                    :useAvailabilityFilters = false
                    OR EXISTS (
                        SELECT 1
                        FROM AvailabilitySlot s
                        WHERE s.student.id = p.user.id
                          AND s.status = :availableStatus
                          AND (:useDayOfWeek = false OR s.dayOfWeek = :dayOfWeek)
                          AND (:useStartTime = false OR s.startTime <= :startTime)
                          AND (:useEndTime = false OR s.endTime >= :endTime)
                    )
                  )
            """)
    List<Profile> searchActiveProfiles(
            @Param("useCategory") boolean useCategory,
            @Param("category") String category,
            @Param("useMaxRate") boolean useMaxRate,
            @Param("maxRate") BigDecimal maxRate,
            @Param("useAvailabilityFilters") boolean useAvailabilityFilters,
            @Param("useDayOfWeek") boolean useDayOfWeek,
            @Param("dayOfWeek") DayOfWeek dayOfWeek,
            @Param("useStartTime") boolean useStartTime,
            @Param("startTime") LocalTime startTime,
            @Param("useEndTime") boolean useEndTime,
            @Param("endTime") LocalTime endTime,
            @Param("availableStatus") SlotStatus availableStatus
    );

    /**
     * Checks if a profile exists for a given user ID.
     * 
     * @param userId the user ID
     * @return true if at least one profile exists
     */
    boolean existsByUserId(Long userId);

    /**
     * Counts active profiles owned by a user.
     * Used to enforce the maximum 5 active services rule per student.
     *
     * @param userId the user ID
     * @return number of active profiles for that user
     */
    long countByUserIdAndIsActiveTrue(Long userId);

    /**
     * Returns (userId, profileCount) pairs for all users in a single query.
     * Used by admin list to avoid N+1.
     */
    @Query("SELECT p.user.id, COUNT(p) FROM Profile p GROUP BY p.user.id")
    List<Object[]> countProfilesGroupedByUserId();
}


