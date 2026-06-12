package it.eably.backend.service.def;

import java.util.Map;
import java.util.List;

/**
 * Service interface for comprehensive payment orchestration using Stripe.
 * <p>
 * Handles standard booking payments, Stripe Connect account management,
 * and payout workflows for service providers.
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */
public interface PaymentService {
    /**
     * Initializes a Stripe Payment Intent for a specific booking.
     *
     * @param bookingId   internal booking identifier
     * @param requesterId internal user account identifier (the client)
     * @return Stripe client secret for frontend checkout
     */
    String createPaymentIntent(Long bookingId, Long requesterId);

    /**
     * Verifies that a Payment Intent status is 'requires_capture' and matches the booking.
     *
     * @param bookingId       internal booking identifier
     * @param paymentIntentId external Stripe identifier
     */
    void validatePaymentIntentForBooking(Long bookingId, String paymentIntentId);

    /**
     * Executes funds capture for a previously authorized payment.
     *
     * @param bookingId internal booking identifier
     */
    void capturePayment(Long bookingId);

    /**
     * Cancels an authorized Payment Intent to release user funds.
     *
     * @param bookingId internal booking identifier
     */
    void cancelPayment(Long bookingId);

    /**
     * Creates a new Connect Express account for a student.
     *
     * @param userId internal user account identifier
     * @return external Stripe account identifier
     */
    String createConnectAccount(Long userId);

    /**
     * Generates a single-use onboarding URL for Connect setup.
     *
     * @param userId     internal user account identifier
     * @param refreshUrl callback for session expiration
     * @param returnUrl  callback for completion
     * @return target onboarding link
     */
    String getAccountLink(Long userId, String refreshUrl, String returnUrl);

    /**
     * Performs a final check on Connect onboarding status and updates user profile.
     *
     * @param userId internal user account identifier
     * @return true if charges and payouts are enabled
     */
    boolean completeConnectAccount(Long userId);

    /**
     * Retrieves current balance summary from a connected Stripe account.
     *
     * @param userId internal user account identifier
     * @return map of available and pending balance components
     */
    Map<String, Object> getConnectBalance(Long userId);

    /**
     * Initiates a manual payout from the platform to a provider's Connect account.
     *
     * @param userId        internal user account identifier
     * @param amountInCents units in cents
     * @param currency      ISO 4217 code (e.g., "eur")
     */
    void requestPayout(Long userId, long amountInCents, String currency);

    /**
     * Retrieves recent payout history for a connected account.
     *
     * @param userId internal user account identifier
     * @param limit  maximum results to return (1..100)
     * @return list of simplified payout records
     */
    List<Object> getConnectPayouts(Long userId, int limit);
}
