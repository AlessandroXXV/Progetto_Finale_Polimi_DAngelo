package it.eably.backend.dto.profile.request;

import it.eably.backend.model.DeliveryMode;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * DTO for profile creation/update request.
 * 
 * This record encapsulates the data required to create or update a profile.
 * All fields are validated using Jakarta Bean Validation.
 * 
 * @param title professional title/headline
 * @param description detailed description of services
 * @param hourlyRate hourly rate (must be positive)
 * @param deliveryMode service delivery mode (ONLINE, IN_PERSON, HYBRID)
 * @param address physical address (required for IN_PERSON and HYBRID)
 * 
 * @author Alessandro D'Angelo
 * @version 1.0
 */
public record ProfileRequestDTO(
        
        @NotBlank(message = "Title is required")
        String title,
        
        String description,
        
        @NotNull(message = "Hourly rate is required")
        @DecimalMin(value = "0.01", message = "Hourly rate must be positive")
        BigDecimal hourlyRate,
        
        @NotNull(message = "Delivery mode is required")
        DeliveryMode deliveryMode,
        
        String address
) {
}
