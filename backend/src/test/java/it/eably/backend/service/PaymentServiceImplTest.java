package it.eably.backend.service;

import com.stripe.exception.StripeException;
import com.stripe.model.Account;
import com.stripe.model.AccountLink;
import com.stripe.model.Balance;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Payout;
import com.stripe.model.PayoutCollection;
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
import it.eably.backend.model.UserRole;
import it.eably.backend.repository.BookingRepository;
import it.eably.backend.repository.UserRepository;
import it.eably.backend.service.impl.PaymentServiceImpl;
import it.eably.backend.service.strategy.CommissionCalculator;
import it.eably.backend.service.strategy.CommissionStrategy;
import it.eably.backend.service.strategy.PremiumCommissionStrategy;
import it.eably.backend.service.strategy.StandardCommissionStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private BookingRepository bookingRepository;
    
    @Mock
    private CommissionCalculator commissionCalculator;
    
    @Mock
    private StandardCommissionStrategy standardCommissionStrategy;
    
    @Mock
    private PremiumCommissionStrategy premiumCommissionStrategy;

    private PaymentServiceImpl paymentService;

    private User client;
    private User provider;
    private Booking booking;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentServiceImpl(userRepository, bookingRepository, commissionCalculator, standardCommissionStrategy, premiumCommissionStrategy, "sk_test_123");

        client = new User();
        client.setId(1L);
        client.setRole(UserRole.CLIENT);

        provider = new User();
        provider.setId(2L);
        provider.setRole(UserRole.STUDENT);
        provider.setStripeAccountId("acct_123");
        provider.setStripeConnected(true);

        booking = new Booking();
        booking.setId(10L);
        booking.setClient(client);
        booking.setProvider(provider);
        booking.setStatus(BookingStatus.PAYMENT_PENDING);
        booking.setTotalAmount(new BigDecimal("50.00"));
    }


    @Test
    void createPaymentIntent_RequesterNotClient_ThrowsValidationException() {
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));

        ValidationException ex = assertThrows(ValidationException.class,
                () -> paymentService.createPaymentIntent(10L, 999L));

        assertEquals("Only the booking client can create a payment intent", ex.getMessage());
    }

    @Test
    void createPaymentIntent_BookingWrongStatus_ThrowsValidationException() {
        booking.setStatus(BookingStatus.CONFIRMED);
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));

        ValidationException ex = assertThrows(ValidationException.class,
                () -> paymentService.createPaymentIntent(10L, 1L));

        assertEquals("Booking is not awaiting payment", ex.getMessage());
    }

    @Test
    void createPaymentIntent_ProviderNotConnected_ThrowsValidationException() {
        provider.setStripeConnected(false);
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));

        ValidationException ex = assertThrows(ValidationException.class,
                () -> paymentService.createPaymentIntent(10L, 1L));

        assertEquals("Stripe onboarding is not complete yet", ex.getMessage());
    }

    @Test
    void createPaymentIntent_Success_ReturnsClientSecret() {
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));
        when(commissionCalculator.calculateWith(any(BigDecimal.class), any(CommissionStrategy.class))).thenReturn(new BigDecimal("5.50"));

        PaymentIntent stripeIntent = mock(PaymentIntent.class);
        when(stripeIntent.getClientSecret()).thenReturn("pi_secret_abc");

        try (MockedStatic<PaymentIntent> paymentIntentStatic = mockStatic(PaymentIntent.class)) {
            paymentIntentStatic.when(() -> PaymentIntent.create(
                    any(PaymentIntentCreateParams.class),
                    any(RequestOptions.class)
            )).thenReturn(stripeIntent);

            String secret = paymentService.createPaymentIntent(10L, 1L);

            assertEquals("pi_secret_abc", secret);
        }
    }

    @Test
    void capturePayment_NoIntentId_ThrowsValidationException() {
        booking.setPaymentIntentId(" ");
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));

        ValidationException ex = assertThrows(ValidationException.class,
                () -> paymentService.capturePayment(10L));

        assertEquals("Booking has no payment intent to capture", ex.getMessage());
    }

    @Test
    void capturePayment_RequiresCapture_CallsStripeCapture() throws Exception {
        booking.setPaymentIntentId("pi_123");
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));

        PaymentIntent stripeIntent = mock(PaymentIntent.class);
        when(stripeIntent.getStatus()).thenReturn("requires_capture");

        try (MockedStatic<PaymentIntent> paymentIntentStatic = mockStatic(PaymentIntent.class)) {
            paymentIntentStatic.when(() -> PaymentIntent.retrieve(eq("pi_123"), any())).thenReturn(stripeIntent);

            paymentService.capturePayment(10L);

            verify(stripeIntent).capture(any(RequestOptions.class));
        }
    }

    @Test
    void cancelPayment_SucceededIntent_DoesNotCancel() throws Exception {
        booking.setPaymentIntentId("pi_456");
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));

        PaymentIntent stripeIntent = mock(PaymentIntent.class);
        when(stripeIntent.getStatus()).thenReturn("succeeded");

        try (MockedStatic<PaymentIntent> paymentIntentStatic = mockStatic(PaymentIntent.class)) {
            paymentIntentStatic.when(() -> PaymentIntent.retrieve(eq("pi_456"), any())).thenReturn(stripeIntent);

            paymentService.cancelPayment(10L);

            verify(stripeIntent, never()).cancel(any(RequestOptions.class));
        }
    }

    @Test
    void createConnectAccount_AlreadyExists_ReturnsExistingAccountId() {
        provider.setId(2L);
        when(userRepository.findById(2L)).thenReturn(Optional.of(provider));

        String accountId = paymentService.createConnectAccount(2L);

        assertEquals("acct_123", accountId);
        verify(userRepository, never()).save(any());
    }

    @Test
    void createConnectAccount_NewAccount_SavesUser() {
        User student = new User();
        student.setId(20L);
        student.setEmail("student@test.com");
        student.setStripeAccountId(null);
        student.setStripeConnected(false);
        when(userRepository.findById(20L)).thenReturn(Optional.of(student));

        Account account = mock(Account.class);
        when(account.getId()).thenReturn("acct_new");

        try (MockedStatic<Account> accountStatic = mockStatic(Account.class)) {
            accountStatic.when(() -> Account.create(
                    any(AccountCreateParams.class),
                    any(RequestOptions.class)
            )).thenReturn(account);

            String accountId = paymentService.createConnectAccount(20L);

            assertEquals("acct_new", accountId);
            assertEquals("acct_new", student.getStripeAccountId());
            assertFalse(student.getStripeConnected());
            verify(userRepository).save(student);
        }
    }

    @Test
    void getAccountLink_Success() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(provider));

        AccountLink accountLink = mock(AccountLink.class);
        when(accountLink.getUrl()).thenReturn("https://stripe/link");

        try (MockedStatic<AccountLink> accountLinkStatic = mockStatic(AccountLink.class)) {
            accountLinkStatic.when(() -> AccountLink.create(
                    any(AccountLinkCreateParams.class),
                    any(RequestOptions.class)
            )).thenReturn(accountLink);

            String url = paymentService.getAccountLink(2L, "https://refresh", "https://return");

            assertEquals("https://stripe/link", url);
        }
    }

    @Test
    void completeConnectAccount_UpdatesStripeConnectedFlag() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(provider));

        Account account = mock(Account.class);
        when(account.getDetailsSubmitted()).thenReturn(true);
        when(account.getChargesEnabled()).thenReturn(true);
        when(account.getPayoutsEnabled()).thenReturn(true);

        try (MockedStatic<Account> accountStatic = mockStatic(Account.class)) {
            accountStatic.when(() -> Account.retrieve(eq("acct_123"), any())).thenReturn(account);

            boolean connected = paymentService.completeConnectAccount(2L);

            assertTrue(connected);
            assertTrue(provider.getStripeConnected());
            verify(userRepository).save(provider);
        }
    }

    @Test
    void getConnectBalance_NoAccount_ThrowsResourceNotFoundException() {
        provider.setStripeAccountId(" ");
        when(userRepository.findById(2L)).thenReturn(Optional.of(provider));

        assertThrows(ResourceNotFoundException.class, () -> paymentService.getConnectBalance(2L));
    }

    @Test
    void getConnectBalance_Success() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(provider));

        Balance balance = mock(Balance.class);
        when(balance.getAvailable()).thenReturn(List.of());
        when(balance.getPending()).thenReturn(List.of());

        try (MockedStatic<Balance> balanceStatic = mockStatic(Balance.class)) {
            balanceStatic.when(() -> Balance.retrieve(any())).thenReturn(balance);

            Map<String, Object> result = paymentService.getConnectBalance(2L);

            assertTrue(result.containsKey("available"));
            assertTrue(result.containsKey("pending"));
        }
    }

    @Test
    void requestPayout_InvalidAmount_ThrowsValidationException() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(provider));

        ValidationException ex = assertThrows(ValidationException.class,
                () -> paymentService.requestPayout(2L, 0L, "eur"));

        assertEquals("Payout amount must be greater than zero", ex.getMessage());
    }

    @Test
    void requestPayout_Success() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(provider));

        try (MockedStatic<Payout> payoutStatic = mockStatic(Payout.class)) {
            payoutStatic.when(() -> Payout.create(
                    any(PayoutCreateParams.class),
                    any(RequestOptions.class)
            )).thenReturn(mock(Payout.class));

            paymentService.requestPayout(2L, 1250L, null);

            payoutStatic.verify(() -> Payout.create(
                    any(PayoutCreateParams.class),
                    any(RequestOptions.class)
            ));
        }
    }

    @Test
    void getConnectPayouts_Success_MapsPayload() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(provider));

        Payout payout = mock(Payout.class);
        when(payout.getId()).thenReturn("po_1");
        when(payout.getAmount()).thenReturn(1500L);
        when(payout.getStatus()).thenReturn("paid");
        when(payout.getArrivalDate()).thenReturn(1714000000L);

        PayoutCollection payoutCollection = mock(PayoutCollection.class);
        when(payoutCollection.getData()).thenReturn(List.of(payout));

        try (MockedStatic<Payout> payoutStatic = mockStatic(Payout.class)) {
            payoutStatic.when(() -> Payout.list(
                    any(PayoutListParams.class),
                    any(RequestOptions.class)
            )).thenReturn(payoutCollection);

            List<Object> result = paymentService.getConnectPayouts(2L, 200);

            assertEquals(1, result.size());
            @SuppressWarnings("unchecked")
            Map<String, Object> row = (Map<String, Object>) result.getFirst();
            assertEquals("po_1", row.get("id"));
            assertEquals(1500L, row.get("amount"));
            assertEquals("paid", row.get("status"));
        }
    }

    @Test
    void createPaymentIntent_StripeError_WrappedInValidationException() {
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));
        when(commissionCalculator.calculateWith(any(BigDecimal.class), any(CommissionStrategy.class))).thenReturn(new BigDecimal("5.50"));
        StripeException stripeException = mock(StripeException.class);
        when(stripeException.getMessage()).thenReturn("stripe-down");

        try (MockedStatic<PaymentIntent> paymentIntentStatic = mockStatic(PaymentIntent.class)) {
            paymentIntentStatic.when(() -> PaymentIntent.create(
                    any(PaymentIntentCreateParams.class),
                    any(RequestOptions.class)
            )).thenThrow(stripeException);

            ValidationException ex = assertThrows(ValidationException.class,
                    () -> paymentService.createPaymentIntent(10L, 1L));

            assertTrue(ex.getMessage().contains("Stripe error while creating payment intent"));
        }
    }

    @Test
    void validatePaymentIntentForBooking_MetadataMismatch_ThrowsValidationException() throws Exception {
        PaymentIntent stripeIntent = mock(PaymentIntent.class);
        when(stripeIntent.getStatus()).thenReturn("requires_capture");
        when(stripeIntent.getMetadata()).thenReturn(Map.of("bookingId", "999"));

        try (MockedStatic<PaymentIntent> paymentIntentStatic = mockStatic(PaymentIntent.class)) {
            paymentIntentStatic.when(() -> PaymentIntent.retrieve(eq("pi_123"), any()))
                    .thenReturn(stripeIntent);

            ValidationException ex = assertThrows(ValidationException.class,
                    () -> paymentService.validatePaymentIntentForBooking(10L, "pi_123"));

            assertEquals("Payment intent does not belong to the specified booking", ex.getMessage());
        }
    }

    @Test
    void ensureStripeConfigured_WhenMissingSecret_ThrowsValidationException() {
        PaymentServiceImpl unconfigured = new PaymentServiceImpl(userRepository, bookingRepository, commissionCalculator, standardCommissionStrategy, premiumCommissionStrategy, "");

        ValidationException ex = assertThrows(ValidationException.class,
                () -> unconfigured.capturePayment(10L));

        assertEquals("Stripe secret key is not configured", ex.getMessage());
    }
}







