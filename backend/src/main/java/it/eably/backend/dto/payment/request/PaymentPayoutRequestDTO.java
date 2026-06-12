package it.eably.backend.dto.payment.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Request body for initiating a Stripe payout to a connected account.
 * {@code amount} is in the smallest currency unit (e.g. cents for EUR/USD) — never
 * a decimal value — to match the Stripe API convention.
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */
public record PaymentPayoutRequestDTO(
        @NotNull(message = "Amount is required")
        @Min(value = 1, message = "Payout amount must be greater than zero")
        Long amount,
        @Pattern(regexp = "^[a-zA-Z]{3}$", message = "Currency must be a 3-letter code")
        String currency
) {}
