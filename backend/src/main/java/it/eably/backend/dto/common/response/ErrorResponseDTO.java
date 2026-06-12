package it.eably.backend.dto.common.response;

import java.time.LocalDateTime;

/**
 * Standard error response DTO for all API errors.
 * Used by GlobalExceptionHandler to format error responses consistently.
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */
public record ErrorResponseDTO(
    Integer status,
    String message,
    LocalDateTime timestamp
) {}
