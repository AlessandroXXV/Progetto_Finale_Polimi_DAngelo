package it.eably.backend.dto.admin.response;

import java.math.BigDecimal;

/**
 * Summary of a service/profile associated with a user, shown inside the admin user-detail view.
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */
public record AdminUserServiceDTO(
    Long id,
    String title,
    String deliveryMode,
    Boolean isActive,
    BigDecimal hourlyRate
) {}

