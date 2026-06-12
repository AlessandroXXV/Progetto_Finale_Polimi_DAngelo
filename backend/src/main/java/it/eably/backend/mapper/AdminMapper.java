package it.eably.backend.mapper;

import it.eably.backend.dto.admin.response.AdminUserListDTO;
import it.eably.backend.dto.admin.response.AdminUserServiceDTO;
import it.eably.backend.dto.admin.response.AdminUserStripeDTO;
import it.eably.backend.model.Profile;
import it.eably.backend.model.User;
import org.springframework.stereotype.Component;

/**
 * Spring component responsible for mapping {@link User} and {@link Profile} entities
 * to admin-facing DTOs.
 *
 * <p>Unlike the MapStruct-based mappers in this package, {@code AdminMapper} is a plain
 * Spring {@code @Component} because the mappings require custom business logic
 * (e.g. booking counts, Stripe account validation) that cannot be expressed
 * through MapStruct annotations alone.</p>
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */
@Component
public class AdminMapper {

    /**
     * Maps a {@link User} entity and its aggregated booking/profile counts
     * to an {@link AdminUserListDTO} suitable for the admin user-list endpoint.
     *
     * @param user                 the user entity
     * @param profileCount         number of service profiles owned by the user
     * @param clientBookingCount   number of bookings where the user is the client
     * @param providerBookingCount number of bookings where the user is the provider
     * @return the admin user list DTO
     */
    public AdminUserListDTO toListDTO(User user, long profileCount, long clientBookingCount, long providerBookingCount) {
        return new AdminUserListDTO(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getRole() != null ? user.getRole().name() : null,
                user.getIsActive(),
                user.getIsVerified(),
                user.getStripeConnected(),
                profileCount,
                clientBookingCount,
                providerBookingCount
        );
    }

    /**
     * Maps a {@link Profile} entity to an {@link AdminUserServiceDTO}
     * containing the essential service information shown in the admin panel.
     *
     * @param profile the profile entity
     * @return the admin service DTO
     */
    public AdminUserServiceDTO toServiceDTO(Profile profile) {
        return new AdminUserServiceDTO(
                profile.getId(),
                profile.getTitle(),
                profile.getDeliveryMode() != null ? profile.getDeliveryMode().name() : null,
                profile.getIsActive(),
                profile.getHourlyRate()
        );
    }

    /**
     * Maps a {@link User} entity to an {@link AdminUserStripeDTO} containing
     * Stripe Connect account information.
     *
     * <p>The {@code hasStripeAccount} flag is derived by checking whether
     * {@code stripeAccountId} is non-null and non-blank.</p>
     *
     * @param user the user entity
     * @return the admin Stripe DTO
     */
    public AdminUserStripeDTO toStripeDTO(User user) {
        return new AdminUserStripeDTO(
                user.getStripeConnected(),
                user.getStripeAccountId(),
                user.getStripeAccountId() != null && !user.getStripeAccountId().isBlank()
        );
    }
}
