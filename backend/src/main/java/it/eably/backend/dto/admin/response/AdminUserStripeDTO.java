package it.eably.backend.dto.admin.response;

/**
 * Stripe account status snapshot for an admin user-detail view.
 * {@code stripeConnected} reflects onboarding completion; {@code hasStripeAccount}
 * indicates whether a Stripe account record exists even if onboarding is incomplete.
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */
public record AdminUserStripeDTO(
    Boolean stripeConnected,
    String stripeAccountId,
    Boolean hasStripeAccount
) {}

