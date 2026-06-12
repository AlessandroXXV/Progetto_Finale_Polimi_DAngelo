package it.eably.backend.controller;

import it.eably.backend.dto.payment.request.PaymentConnectRequestDTO;
import it.eably.backend.dto.payment.request.PaymentPayoutRequestDTO;
import it.eably.backend.model.User;
import it.eably.backend.model.UserRole;
import it.eably.backend.service.def.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.List;

/**
 * REST controller for managing payments via Stripe.
 * <p>
 * Handles both standard payments (PaymentIntent) and the Stripe Connect flow
 * for payouts to students.
 * </p>
 *
 * Endpoints:
 * <ul>
 * <li>POST /api/v1/payments/intent                  – Creates a PaymentIntent for a booking</li>
 * <li>POST /api/v1/payments/connect/account-link    – Generates the link to connect the Stripe account (STUDENT)</li>
 * <li>POST /api/v1/payments/connect/complete        – Completes the Stripe account connection (STUDENT)</li>
 * <li>GET  /api/v1/payments/connect/balance         – Retrieves the Stripe Connect account balance (STUDENT)</li>
 * <li>POST /api/v1/payments/connect/payout          – Requests a payout to the bank account (STUDENT)</li>
 * <li>GET  /api/v1/payments/connect/payouts         – List of processed payouts (STUDENT)</li>
 * </ul>
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */
@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Builds the controller by injecting the payment service.
     *
     * @param paymentService the service for Stripe payment operations
     */
    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * Creates a Stripe PaymentIntent for a booking.
     *
     * @param payload map containing {@code bookingId}
     * @param user    the authenticated user (client)
     * @return map with the {@code clientSecret} to be used on the frontend to complete the payment
     */
    @PostMapping("/intent")
    public ResponseEntity<Map<String, String>> createIntent(@RequestBody Map<String, Long> payload,
                                                            @AuthenticationPrincipal User user) {
        Long bookingId = payload.get("bookingId");
        String secret = paymentService.createPaymentIntent(bookingId, user.getId());
        return ResponseEntity.ok(Map.of("clientSecret", secret));
    }

    /**
     * Generates a link to connect the student's Stripe account via Stripe Connect.
     * <p>
     * Accessible only to users with the {@code STUDENT} role.
     * </p>
     *
     * @param req  DTO containing {@code refreshUrl} and {@code returnUrl}
     * @param user the authenticated user (student)
     * @return map with the Stripe connection link URL
     */
    @PostMapping("/connect/account-link")
    public ResponseEntity<Map<String, String>> getAccountLink(@Valid @RequestBody PaymentConnectRequestDTO req,
                                                              @AuthenticationPrincipal User user) {
        requireStudent(user);
        String url = paymentService.getAccountLink(user.getId(), req.refreshUrl(), req.returnUrl());
        return ResponseEntity.ok(Map.of("url", url));
    }

    /**
     * Completes the Stripe account connection for the student.
     * <p>
     * To be invoked after the user has completed the OAuth flow on Stripe.
     * Accessible only to users with the {@code STUDENT} role.
     * </p>
     *
     * @param user the authenticated user (student)
     * @return map with the {@code stripeConnected} flag
     */
    @PostMapping("/connect/complete")
    public ResponseEntity<Map<String, Boolean>> completeConnectAccount(@AuthenticationPrincipal User user) {
        requireStudent(user);
        boolean stripeConnected = paymentService.completeConnectAccount(user.getId());
        return ResponseEntity.ok(Map.of("stripeConnected", stripeConnected));
    }

    /**
     * Retrieves the available balance on the student's Stripe Connect account.
     * <p>
     * Accessible only to users with the {@code STUDENT} role.
     * </p>
     *
     * @param user the authenticated user (student)
     * @return map with the balance details (available, pending, currency)
     */
    @GetMapping("/connect/balance")
    public ResponseEntity<Map<String, Object>> getBalance(@AuthenticationPrincipal User user) {
        requireStudent(user);
        return ResponseEntity.ok(paymentService.getConnectBalance(user.getId()));
    }

    /**
     * Requests a payout to the student's bank account.
     * <p>
     * Accessible only to users with the {@code STUDENT} role.
     * </p>
     *
     * @param payload DTO containing {@code amount} and {@code currency} (default {@code eur})
     * @param user    the authenticated user (student)
     * @return 200 OK if the payout was successfully requested
     */
    @PostMapping("/connect/payout")
    public ResponseEntity<Void> requestPayout(@Valid @RequestBody PaymentPayoutRequestDTO payload,
                                              @AuthenticationPrincipal User user) {
        requireStudent(user);
        String currency = payload.currency() == null ? "eur" : payload.currency().toLowerCase();
        paymentService.requestPayout(user.getId(), payload.amount(), currency);
        return ResponseEntity.ok().build();
    }

    /**
     * Retrieves the list of payouts processed by the student.
     * <p>
     * Accessible only to users with the {@code STUDENT} role.
     * </p>
     *
     * @param limit maximum number of payouts to return (default 10)
     * @param user  the authenticated user (student)
     * @return list of Stripe payouts
     */
    @GetMapping("/connect/payouts")
    public ResponseEntity<List<Object>> getPayouts(@RequestParam(defaultValue = "10") int limit,
                                                   @AuthenticationPrincipal User user) {
        requireStudent(user);
        return ResponseEntity.ok(paymentService.getConnectPayouts(user.getId(), limit));
    }

    /**
     * Verifies that the user has the {@code STUDENT} role.
     * Throws a {@link org.springframework.web.server.ResponseStatusException} with status 403 otherwise.
     *
     * @param user the authenticated user to verify
     */
    private void requireStudent(User user) {
        if (!UserRole.STUDENT.equals(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only students can use Stripe Connect");
        }
    }

}