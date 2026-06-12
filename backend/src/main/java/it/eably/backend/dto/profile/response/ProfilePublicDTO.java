package it.eably.backend.dto.profile.response;

import it.eably.backend.model.DeliveryMode;

import java.math.BigDecimal;


/**
 * DTO for profile response data.
 *
 * This record encapsulates profile data to be sent to the public routes of client.
 * Excludes sensitive user information (id, email) and profile details (createdAt, updatedAt).
 *
 * @param id           profile ID
 * @param username     user's username
 * @param title        professional title
 * @param description  service description
 * @param hourlyRate   hourly rate
 * @param deliveryMode delivery mode
 * @param address      physical address
 * @param isActive     profile active status
 * @author Alessandro D'Angelo
 * @version 1.0
 */
public record ProfilePublicDTO(
        Long id,
        String username,
        String title,
        String description,
        BigDecimal hourlyRate,
        DeliveryMode deliveryMode,
        String address,
        Boolean isActive
)
{
}
