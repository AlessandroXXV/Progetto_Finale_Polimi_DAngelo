package it.eably.backend.dto.payment.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for creating a Stripe Connect onboarding link.
 * {@code refreshUrl} is where Stripe redirects when the link expires;
 * {@code returnUrl} is where Stripe redirects after the user completes onboarding.
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */
public record PaymentConnectRequestDTO(
    @NotBlank(message = "Refresh URL is required") String refreshUrl,
    @NotBlank(message = "Return URL is required") String returnUrl
) {}
