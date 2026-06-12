package it.eably.backend.dto.profile.response;

import it.eably.backend.model.DeliveryMode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for profile response data.
 * 
 * This record encapsulates profile data to be sent to the client.
 * Includes user information and profile details.
 * 
 * @param id profile ID
 * @param userId associated user ID
 * @param username user's username
 * @param email user's email
 * @param title professional title
 * @param description service description
 * @param hourlyRate hourly rate
 * @param deliveryMode delivery mode
 * @param address physical address
 * @param isActive profile active status
 * @param createdAt profile creation timestamp
 * @param updatedAt profile last update timestamp
 * 
 * @author Alessandro D'Angelo
 * @version 1.0
 */
public record ProfileResponseDTO(
        Long id,
        Long userId,
        String username,
        String email,
        String title,
        String description,
        BigDecimal hourlyRate,
        DeliveryMode deliveryMode,
        String address,
        Boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
