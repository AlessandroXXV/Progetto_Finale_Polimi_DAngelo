package it.eably.backend.service.impl;

import com.stripe.exception.StripeException;
import com.stripe.model.Account;
import com.stripe.model.AccountLink;
import com.stripe.model.Balance;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Payout;
import com.stripe.net.RequestOptions;
import com.stripe.param.AccountCreateParams;
import com.stripe.param.AccountLinkCreateParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.PayoutCreateParams;
import com.stripe.param.PayoutListParams;
import it.eably.backend.exception.ResourceNotFoundException;
import it.eably.backend.exception.ValidationException;
import it.eably.backend.model.Booking;
import it.eably.backend.model.BookingStatus;
import it.eably.backend.model.User;
import it.eably.backend.repository.BookingRepository;
import it.eably.backend.repository.UserRepository;
import it.eably.backend.service.def.PaymentService;
import it.eably.backend.service.strategy.CommissionCalculator;
import it.eably.backend.service.strategy.CommissionStrategy;
import it.eably.backend.service.strategy.PremiumCommissionStrategy;
import it.eably.backend.service.strategy.StandardCommissionStrategy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Payment service implementation for Stripe payments and Connect operations.
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */
@Service
public class PaymentServiceImpl implements PaymentService {

    /** Default currency for Stripe operations. */
    private static final String DEFAULT_CURRENCY = "eur";
    /** Fixed PSP surcharge in cents added to client amount. */
    private static final long PSP_SURCHARGE_CENTS = 50L;

    /** Repository for users. */
    private final UserRepository userRepository;
    /** Repository for bookings. */
    private final BookingRepository bookingRepository;
    /** Commission calculator for platform fee. */
    private final CommissionCalculator commissionCalculator;
    /** Strategy for standard commission. */
    private final StandardCommissionStrategy standardCommissionStrategy;
    /** Strategy for premium commission. */
    private final PremiumCommissionStrategy premiumCommissionStrategy;
    /** Stripe secret key for API access. */
    private final String stripeSecretKey;

    /**
     * Builds the payment service with required dependencies.
     *
     * @param userRepository repository for users
     * @param bookingRepository repository for bookings
     * @param commissionCalculator calculator for commission fees
     * @param standardCommissionStrategy standard commission strategy
     * @param premiumCommissionStrategy premium commission strategy
     * @param stripeSecretKey Stripe secret key
     */
    public PaymentServiceImpl(UserRepository userRepository,
                              BookingRepository bookingRepository,
                              CommissionCalculator commissionCalculator,
                              StandardCommissionStrategy standardCommissionStrategy,
                              PremiumCommissionStrategy premiumCommissionStrategy,
                              @Value("${stripe.secret-key:}") String stripeSecretKey
    ) {
        this.userRepository = userRepository;
        this.bookingRepository = bookingRepository;
        this.commissionCalculator = commissionCalculator;
        this.standardCommissionStrategy = standardCommissionStrategy;
        this.premiumCommissionStrategy = premiumCommissionStrategy;
        this.stripeSecretKey = stripeSecretKey;
    }

    /**
     * Creates a Stripe payment intent for a booking.
     *
     * <p>Effect: validates booking ownership and status, computes fees, and creates
     * a manual-capture payment intent in Stripe.</p>
     *
     * @param bookingId booking id
     * @param requesterId requester user id
     * @return client secret for the payment intent
     * @throws ValidationException when validation fails or Stripe returns an error
     */
    @Override
    public String createPaymentIntent(Long bookingId, Long requesterId) {
        ensureStripeConfigured();
        Booking booking = findBooking(bookingId);

        // Security check: ensure only the client who created the booking can pay for it.
        if (!booking.getClient().getId().equals(requesterId)) {
            throw new ValidationException("Only the booking client can create a payment intent");
        }
        // Prevent paying for already confirmed or cancelled bookings.
        if (booking.getStatus() != BookingStatus.PAYMENT_PENDING) {
            throw new ValidationException("Booking is not awaiting payment");
        }

        User provider = booking.getProvider();

        // the provider must have a connected Stripe account to receive funds.
        String destinationAccountId = requireConnectedAccount(provider, true);

        // Convert the BigDecimal amount to Stripe's integer cent representation.
        long merchantAmountInCents = amountToCents(booking.getTotalAmount());

        // Calculate the platform fee based on the provider's commission strategy (Student vs Premium).
        long platformFeeInCents = computeApplicationFeeInCents(provider, booking.getTotalAmount());

        // Add a fixed surcharge to cover payment service provider costs.
        long clientAmountInCents = merchantAmountInCents + PSP_SURCHARGE_CENTS;

        try {
            // Configure the intent for manual capture (authorize now, capture on session completion).
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(clientAmountInCents)
                    .setCurrency(DEFAULT_CURRENCY)
                    .setCaptureMethod(PaymentIntentCreateParams.CaptureMethod.MANUAL)
                    .addPaymentMethodType("card")
                    .setApplicationFeeAmount(platformFeeInCents)
                    .setTransferData(PaymentIntentCreateParams.TransferData.builder()
                            .setDestination(destinationAccountId)
                            .build())
                    .putMetadata("bookingId", bookingId.toString())
                    .putMetadata("providerId", provider.getId().toString())
                    .build();

            // Use an idempotency key to prevent duplicate charges on network retries.
            PaymentIntent paymentIntent = PaymentIntent.create(
                    params,
                    requestOptionsWithIdempotencyKey("create-intent-booking-" + bookingId)
            );
            return paymentIntent.getClientSecret();
        } catch (StripeException e) {
            throw new ValidationException("Stripe error while creating payment intent: " + e.getMessage());
        }
    }

    /**
     * Validates a payment intent against the given booking id.
     *
     * @param bookingId booking id
     * @param paymentIntentId Stripe payment intent id
     * @throws ValidationException when intent is invalid or does not match booking
     */
    @Override
    public void validatePaymentIntentForBooking(Long bookingId, String paymentIntentId) {
        ensureStripeConfigured();
        if (paymentIntentId == null || paymentIntentId.isBlank()) {
            throw new ValidationException("Payment intent ID is required");
        }

        try {
            // Retrieve current intent state from Stripe API.
            PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId, requestOptions());
            String status = paymentIntent.getStatus();

            // Requires_capture status is used when the payment is authorized but not yet captured.
            // If the payment intent is not in a requires_capture status, it's not authorized for manual capture'
            if (!"requires_capture".equals(status)) {
                throw new ValidationException("Payment intent is not authorized for manual capture");
            }

            // Cross-verify with metadata to ensure the intent wasn't reused for a different booking.
            String metadataBookingId = paymentIntent.getMetadata() != null
                    ? paymentIntent.getMetadata().get("bookingId")
                    : null;

            // If the payment intent metadata does not contain a bookingId or
            // if it does not match the specified bookingId, throw a validation exception
            if (metadataBookingId == null || !metadataBookingId.equals(String.valueOf(bookingId))) {
                throw new ValidationException("Payment intent does not belong to the specified booking");
            }
        } catch (StripeException e) {
            throw new ValidationException("Stripe error while validating payment intent: " + e.getMessage());
        }
    }

    /**
     * Captures a manual Stripe payment intent for a booking.
     *
     * @param bookingId booking id
     * @throws ValidationException when booking has no intent or Stripe fails
     */
    @Override
    public void capturePayment(Long bookingId) {
        ensureStripeConfigured();
        Booking booking = findBooking(bookingId);

        String paymentIntentId = booking.getPaymentIntentId();
        if (paymentIntentId == null || paymentIntentId.isBlank()) {
            throw new ValidationException("Booking has no payment intent to capture");
        }

        try {
            PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId, requestOptions());
            // Only perform capture if the status allows it; this handles race conditions with concurrent attempts.
            if ("requires_capture".equals(paymentIntent.getStatus())) {
                paymentIntent.capture(requestOptions());
            }
        } catch (StripeException e) {
            throw new ValidationException("Stripe error while capturing payment: " + e.getMessage());
        }
    }

    /**
     * Cancels a payment intent for a booking when possible.
     *
     * @param bookingId booking id
     * @throws ValidationException when Stripe fails
     */
    @Override
    public void cancelPayment(Long bookingId) {
        ensureStripeConfigured();
        Booking booking = findBooking(bookingId);

        String paymentIntentId = booking.getPaymentIntentId();

        // Pattern Safe no-op
        // If booking has no payment intent ID, don't need to cancel it
        if (paymentIntentId == null || paymentIntentId.isBlank()) {
            return;
        }

        try {
            PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId, requestOptions());
            String status = paymentIntent.getStatus();

            // Only cancel if the payment intent is not already canceled or succeeded
            if (!"succeeded".equals(status) && !"canceled".equals(status)) {
                paymentIntent.cancel(requestOptions());
            }
        } catch (StripeException e) {
            throw new ValidationException("Stripe error while canceling payment: " + e.getMessage());
        }
    }

    /**
     * Creates a Stripe Connect account for a user if missing.
     *
     * @param userId user id
     * @return Stripe account id
     * @throws ValidationException when Stripe fails
     */
    @Override
    public String createConnectAccount(Long userId) {
        ensureStripeConfigured();
        User user = findUser(userId);

        // Idempotency: don't create multiple Stripe accounts for the same internal user.
        if (user.getStripeAccountId() != null && !user.getStripeAccountId().isBlank()) {
            return user.getStripeAccountId();
        }

        try {
            // Initialize a 'Custom/Express' account for the student to receive payments.
            AccountCreateParams params = AccountCreateParams.builder()
                    .setType(AccountCreateParams.Type.EXPRESS)
                    .setCountry("IT")
                    .setEmail(user.getEmail())
                    .build();

            Account account = Account.create(params, requestOptions());

            // Bind the Stripe reference to user record.
            user.setStripeAccountId(account.getId());
            // Even if the account now exists, it is still not totally configured
            user.setStripeConnected(false);

            // Save user with updated Stripe account ID
            userRepository.save(user);
            return account.getId();
        } catch (StripeException e) {
            throw new ValidationException("Stripe error while creating Connect account: " + e.getMessage());
        }
    }

    /**
     * Creates an account onboarding link for the user's Connect account.
     *
     * @param userId user id
     * @param refreshUrl refresh URL for onboarding
     * @param returnUrl return URL for onboarding
     * @return account link URL
     * @throws ValidationException when Stripe fails
     */
    @Override
    public String getAccountLink(Long userId, String refreshUrl, String returnUrl) {
        ensureStripeConfigured();

        // Get user and ensure Stripe account ID is present
        User user = findUser(userId);
        String accountId = user.getStripeAccountId();
        if (accountId == null || accountId.isBlank()) {

            // If not present, create a new account
            accountId = createConnectAccount(userId);
        }

        try {

            // Create a temporary account link for onboarding
            AccountLinkCreateParams params = AccountLinkCreateParams.builder()
                    .setAccount(accountId)
                    .setRefreshUrl(refreshUrl)
                    .setReturnUrl(returnUrl)
                    .setType(AccountLinkCreateParams.Type.ACCOUNT_ONBOARDING)
                    .build();

            AccountLink accountLink = AccountLink.create(params, requestOptions());
            return accountLink.getUrl();
        } catch (StripeException e) {
            throw new ValidationException("Stripe error while creating account link: " + e.getMessage());
        }
    }

    /**
     * Completes a Connect account by checking onboarding status.
     *
     * @param userId user id
     * @return true if account is fully connected
     * @throws ResourceNotFoundException when account is missing
     * @throws ValidationException when Stripe fails
     */
    @Override
    public boolean completeConnectAccount(Long userId) {
        ensureStripeConfigured();

        // Get user and ensure Stripe account ID is present
        User user = findUser(userId);
        String accountId = user.getStripeAccountId();
        if (accountId == null || accountId.isBlank()) {
            throw new ResourceNotFoundException("Stripe account not connected");
        }

        try {
            // Check the actual onboarding status
            Account account = Account.retrieve(accountId, requestOptions());

            // Check if the account is fully connected
            boolean connected = Boolean.TRUE.equals(account.getDetailsSubmitted())
                    && Boolean.TRUE.equals(account.getChargesEnabled())
                    && Boolean.TRUE.equals(account.getPayoutsEnabled());

            // Update user's Stripe flag
            user.setStripeConnected(connected);
            userRepository.save(user);
            return connected;
        } catch (StripeException e) {
            throw new ValidationException("Stripe error while checking onboarding status: " + e.getMessage());
        }
    }

    /**
     * Retrieves balance for a connected Stripe account.
     *
     * @param userId user id
     * @return map with available and pending balances
     * @throws ValidationException when Stripe fails or account is not connected
     */
    @Override
    public Map<String, Object> getConnectBalance(Long userId) {
        ensureStripeConfigured();

        // Retrieve user and ensure Stripe account is connected
        User user = findUser(userId);
        String accountId = requireConnectedAccount(user, false);

        try {
            // Retrieve balance for the connected account
            RequestOptions options = requestOptionsForAccount(accountId);
            Balance balance = Balance.retrieve(options);
            return Map.of(
                    "available", balance.getAvailable(),
                    "pending", balance.getPending()
            );
        } catch (StripeException e) {
            throw new ValidationException("Stripe error while fetching balance: " + e.getMessage());
        }
    }

    /**
     * Requests a payout on a connected Stripe account.
     *
     * @param userId user id
     * @param amountInCents amount to pay out in cents
     * @param currency payout currency (defaults to eur if null)
     * @throws ValidationException when amount is invalid or Stripe fails
     */
    @Override
    public void requestPayout(Long userId, long amountInCents, String currency) {
        ensureStripeConfigured();

        // Retrieve user and ensure Stripe account is connected and onboarded
        User user = findUser(userId);
        String accountId = requireConnectedAccount(user, true);

        if (amountInCents <= 0) {
            throw new ValidationException("Payout amount must be greater than zero");
        }

        try {

            // Creates a payout to the connected account
            PayoutCreateParams params = PayoutCreateParams.builder()
                    .setAmount(amountInCents)
                    .setCurrency(currency == null ? "eur" : currency.toLowerCase())
                    .build();

            // Creates the payout using Stripe API
            Payout.create(params, requestOptionsForAccount(accountId));
        } catch (StripeException e) {
            throw new ValidationException("Stripe error while requesting payout: " + e.getMessage());
        }
    }

    /**
     * Retrieves recent payouts for a connected account.
     *
     * @param userId user id
     * @param limit max number of payouts (1..100)
     * @return list of payout summaries
     * @throws ValidationException when Stripe fails
     */
    @Override
    public List<Object> getConnectPayouts(Long userId, int limit) {
        ensureStripeConfigured();

        // Retrieve user and ensure Stripe account is connected
        User user = findUser(userId);
        String accountId = requireConnectedAccount(user, false);

        try {
            // Creates parameters for the payouts list
            PayoutListParams params = PayoutListParams.builder()

                    // Maximum limit is 100, minimum is 1
                    .setLimit((long) Math.max(1, Math.min(limit, 100)))
                    .build();

            // Calls the Stripe API to retrieve the payouts
            List<Object> payouts = new ArrayList<>();
            for (Payout payout : Payout.list(params, requestOptionsForAccount(accountId)).getData()) {
                payouts.add(Map.of(
                        "id", payout.getId(),
                        "amount", payout.getAmount(),
                        "status", payout.getStatus(),
                        "arrival_date", payout.getArrivalDate()
                ));
            }
            return payouts;
        } catch (StripeException e) {
            throw new ValidationException("Stripe error while fetching payouts: " + e.getMessage());
        }
    }

    /**
     * Loads a user by id or fails.
     *
     * @param userId user id
     * @return user entity
     * @throws ResourceNotFoundException when user is not found
     */
    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
    }


    /**
     * Ensures the user has a Stripe account connected and is onboarded
     *
     * @param user user entity
     * @param requireStripeConnectedFlag whether to require stripeConnected=true
     * @return Stripe account id
     * @throws ResourceNotFoundException when account id is missing
     * @throws ValidationException when onboarding is incomplete
     */
    private String requireConnectedAccount(User user, boolean requireStripeConnectedFlag)
    {
        String accountId = user.getStripeAccountId();
        if (accountId == null || accountId.isBlank()) {
            throw new ResourceNotFoundException("Stripe account not connected");
        }
        if (requireStripeConnectedFlag && !Boolean.TRUE.equals(user.getStripeConnected())) {
            throw new ValidationException("Stripe onboarding is not complete yet");
        }
        return accountId;
    }

    /**
     * Loads a booking by id or fails.
     *
     * @param bookingId booking id
     * @return booking entity
     * @throws ResourceNotFoundException when booking is not found
     */
    private Booking findBooking(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with ID: " + bookingId));
    }

    /**
     * Converts a monetary amount in euros to cents.
     *
     * @param amount amount in euros
     * @return amount in cents
     */
    private long amountToCents(BigDecimal amount) {
        return amount.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
    }

    /**
     * Computes platform fee in cents based on provider tier.
     *
     * @param provider provider user
     * @param totalAmountEuros total booking amount in euros
     * @return platform fee in cents
     */
    private long computeApplicationFeeInCents(User provider, BigDecimal totalAmountEuros) {
        // Apply the Strategy Pattern to determine the platform cut based on provider's premium status.
        CommissionStrategy strategy = provider.getIsPremium() != null && provider.getIsPremium()
                ? premiumCommissionStrategy
                : standardCommissionStrategy;
        
        BigDecimal feeEuros = commissionCalculator.calculateWith(totalAmountEuros, strategy);
        return amountToCents(feeEuros);
    }

    /**
     * Ensures Stripe secret key is configured.
     *
     * @throws ValidationException when key is missing
     */
    private void ensureStripeConfigured() {
        if (stripeSecretKey == null || stripeSecretKey.isBlank()) {
            throw new ValidationException("Stripe secret key is not configured");
        }
    }


    // Operates into the platform account
    /**
     * Builds request options for platform account.
     *
     * @return request options
     */
    private RequestOptions requestOptions()
    {
        return RequestOptions.builder()
                .setApiKey(stripeSecretKey)
                .build();
    }


    // Operates into a connected account
    /**
     * Builds request options for a connected account.
     *
     * @param accountId Stripe connected account id
     * @return request options
     */
    private RequestOptions requestOptionsForAccount(String accountId)
    {
        return RequestOptions.builder()
                .setApiKey(stripeSecretKey)
                .setStripeAccount(accountId)
                .build();
    }

    /**
     * Builds request options with idempotency key for platform account.
     *
     * @param idempotencyKey idempotency key value
     * @return request options
     */
    private RequestOptions requestOptionsWithIdempotencyKey(String idempotencyKey) {
        return RequestOptions.builder()
                .setApiKey(stripeSecretKey)
                .setIdempotencyKey(idempotencyKey)
                .build();
    }
}
