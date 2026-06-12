package it.eably.backend.service.impl;

import it.eably.backend.dto.admin.request.AdminUserActionDTO;
import it.eably.backend.dto.admin.response.AdminUserBookingStatsDTO;
import it.eably.backend.dto.admin.response.AdminUserDetailDTO;
import it.eably.backend.dto.admin.response.AdminUserListDTO;
import it.eably.backend.dto.admin.response.AdminUserServiceDTO;
import it.eably.backend.dto.admin.response.AdminStatsResponseDTO;
import it.eably.backend.exception.ResourceNotFoundException;
import it.eably.backend.exception.ValidationException;
import it.eably.backend.mapper.AdminMapper;
import it.eably.backend.model.Booking;
import it.eably.backend.model.BookingStatus;
import it.eably.backend.model.Profile;
import it.eably.backend.model.User;
import it.eably.backend.model.UserRole;
import it.eably.backend.repository.BookingRepository;
import it.eably.backend.repository.ProfileRepository;
import it.eably.backend.repository.UserRepository;
import it.eably.backend.service.def.AdminManagementService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Admin management service implementation.
 *
 * Please see {@link AdminManagementService} for more details.
 *
 * <p>Handles admin queries for user lists, details, and status updates.</p>
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */
@Service
public class AdminManagementServiceImpl implements AdminManagementService
{

    /**
     * Repository for user data.
     */
    private final UserRepository userRepository;
    /**
     * Repository for booking data.
     */
    private final BookingRepository bookingRepository;
    /**
     * Repository for profile data.
     */
    private final ProfileRepository profileRepository;
    /**
     * Mapper for admin-facing DTOs.
     */
    private final AdminMapper adminMapper;

    /**
     * Builds the admin management service with required dependencies.
     *
     * @param userRepository    repository for users
     * @param bookingRepository repository for bookings
     * @param profileRepository repository for profiles
     * @param adminMapper       mapper for admin DTOs
     */
    public AdminManagementServiceImpl(UserRepository userRepository,
                                      BookingRepository bookingRepository,
                                      ProfileRepository profileRepository,
                                      AdminMapper adminMapper)
    {
        this.userRepository = userRepository;
        this.bookingRepository = bookingRepository;
        this.profileRepository = profileRepository;
        this.adminMapper = adminMapper;
    }

    /**
     * Returns high-level admin statistics.
     *
     * <p>Effect: reads aggregated counts of users and bookings.</p>
     *
     * @return stats summary
     */
    @Override
    @Transactional(readOnly = true)
    public AdminStatsResponseDTO getStats()
    {
        // Aggregate counts for the admin dashboard.
        long users = userRepository.count();
        long bookings = bookingRepository.count();
        return new AdminStatsResponseDTO(users, bookings);
    }

    /**
     * Returns filtered and enriched user list for admin.
     *
     * <p>Effect: loads users, applies filters, and enriches with profile and booking counts.</p>
     *
     * @param role optional role filter ("all" or null to disable)
     * @param verified optional verification filter
     * @param active optional active filter
     * @return list of {@link AdminUserListDTO}
     * @throws ValidationException when role filter is invalid
     */
    @Override
    @Transactional(readOnly = true)
    public List<AdminUserListDTO> getUsers(String role, Boolean verified, Boolean active)
    {
        // Normalize role filter to enum (null means no filter).
        UserRole parsedRole = parseRole(role);

        // Preload counts to avoid per-user queries.
        Map<Long, Long> profileCounts = toCountMap(profileRepository.countProfilesGroupedByUserId());
        Map<Long, Long> clientBookingCounts = toCountMap(bookingRepository.countClientBookingsGroupedByUserId());
        Map<Long, Long> providerBookingCounts = toCountMap(bookingRepository.countProviderBookingsGroupedByUserId());

        return userRepository.findAll().stream()
                // Apply optional filters only when present.
                .filter(user -> parsedRole == null || user.getRole() == parsedRole)
                .filter(user -> verified == null || Boolean.TRUE.equals(user.getIsVerified()) == verified)
                .filter(user -> active == null || Boolean.TRUE.equals(user.getIsActive()) == active)
                .sorted(Comparator.comparing(User::getId, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .map(user -> adminMapper.toListDTO(
                        user,
                        profileCounts.getOrDefault(user.getId(), 0L),
                        clientBookingCounts.getOrDefault(user.getId(), 0L),
                        providerBookingCounts.getOrDefault(user.getId(), 0L)))
                .collect(Collectors.toList());
    }

    /**
     * Returns detailed user information for admin.
     *
     * <p>Effect: aggregates profiles, bookings, and booking stats for the user.</p>
     *
     * @param userId user id
     * @return {@link AdminUserDetailDTO} detailed user info
     * @throws ResourceNotFoundException when user is not found
     */
    @Override
    @Transactional(readOnly = true)
    public AdminUserDetailDTO getUserDetails(Long userId)
    {
        User user = findUser(userId);
        List<Profile> profiles = profileRepository.findAllByUserId(userId);
        List<Booking> clientBookings = bookingRepository.findByClientId(userId);
        List<Booking> providerBookings = bookingRepository.findByProviderId(userId);

        // Merge client/provider bookings without duplicates.
        Map<Long, Booking> uniqueBookings = Stream.concat(clientBookings.stream(), providerBookings.stream())
                .filter(b -> b.getId() != null)
                .collect(Collectors.toMap(Booking::getId, b -> b, (a, b) -> a, LinkedHashMap::new));

        // Aggregate counts by status for admin stats.
        Map<BookingStatus, Long> statusCounts = uniqueBookings.values().stream()
                .collect(Collectors.groupingBy(Booking::getStatus, Collectors.counting()));

        AdminUserBookingStatsDTO bookingStats = new AdminUserBookingStatsDTO(
                clientBookings.size(),
                providerBookings.size(),
                uniqueBookings.size(),
                statusCounts.getOrDefault(BookingStatus.PAYMENT_PENDING, 0L),
                statusCounts.getOrDefault(BookingStatus.CONFIRMED, 0L),
                statusCounts.getOrDefault(BookingStatus.COMPLETED, 0L),
                statusCounts.getOrDefault(BookingStatus.CANCELLED, 0L)
        );

        // Map profiles to admin-facing DTOs.
        List<AdminUserServiceDTO> services = profiles.stream()
                .sorted(Comparator.comparing(Profile::getId, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .map(adminMapper::toServiceDTO)
                .collect(Collectors.toList());

        return new AdminUserDetailDTO(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getGender(),
                user.getRole() != null ? user.getRole().name() : null,
                user.getIsActive(),
                user.getIsVerified(),
                user.getStripeConnected(),
                user.getStripeAccountId(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                bookingStats,
                adminMapper.toStripeDTO(user),
                services
        );
    }

    /**
     * Sets the verified flag for a user.
     *
     * <p>Effect: updates user verification status in persistence.</p>
     *
     * @param userId user id
     * @param verified new verification flag
     * @return {@link AdminUserActionDTO} updated user status summary
     * @throws ResourceNotFoundException when user is not found
     */
    @Override
    @Transactional
    public AdminUserActionDTO setUserVerified(Long userId, boolean verified)
    {
        User user = findUser(userId);
        // Update verification status only.
        user.setIsVerified(verified);
        User saved = userRepository.save(user);
        return new AdminUserActionDTO(saved.getId(), saved.getIsActive(), saved.getIsVerified());
    }

    /**
     * Sets the active flag for a user.
     *
     * <p>Effect: updates user active status in persistence.</p>
     *
     * @param userId user id
     * @param active new active flag
     * @return updated user status summary
     * @throws ResourceNotFoundException when user is not found
     */
    @Override
    @Transactional
    public AdminUserActionDTO setUserActive(Long userId, boolean active)
    {
        User user = findUser(userId);
        // Update activation status only.
        user.setIsActive(active);
        User saved = userRepository.save(user);
        return new AdminUserActionDTO(saved.getId(), saved.getIsActive(), saved.getIsVerified());
    }

    /**
     * Loads a user by id or fails.
     *
     * @param userId user id
     * @return user entity
     * @throws ResourceNotFoundException when user is not found
     */
    private User findUser(Long userId)
    {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
    }

    /**
     * Parses a role filter value.
     *
     * @param role raw role filter (null, blank, or "all" means no filter)
     * @return parsed role or null
     * @throws ValidationException when role is invalid
     */
    private UserRole parseRole(String role)
    {
        if (role == null || role.isBlank() || "all".equalsIgnoreCase(role)) {
            return null;
        }
        try {
            return UserRole.valueOf(role.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            // Expose invalid filter values to the caller.
            throw new ValidationException("Invalid role filter: " + role);
        }
    }

    /**
     * Converts grouped count rows into a map.
     *
     * @param rows rows with [id, count]
     * @return map of id to count
     */
    private Map<Long, Long> toCountMap(List<Object[]> rows)
    {
        // Map rows of [id, count] to a lookup map.
        return rows.stream().collect(Collectors.toMap(
                row -> (Long) row[0],
                row -> (Long) row[1]
        ));
    }
}
