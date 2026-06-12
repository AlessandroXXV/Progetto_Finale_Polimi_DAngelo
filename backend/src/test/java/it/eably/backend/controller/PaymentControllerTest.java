package it.eably.backend.controller;

import it.eably.backend.dto.payment.request.PaymentConnectRequestDTO;
import it.eably.backend.dto.payment.request.PaymentPayoutRequestDTO;
import it.eably.backend.model.User;
import it.eably.backend.model.UserRole;
import it.eably.backend.service.def.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private PaymentController paymentController;

    private User student;
    private User client;

    @BeforeEach
    void setUp() {
        student = new User();
        student.setId(2L);
        student.setUsername("student");
        student.setRole(UserRole.STUDENT);

        client = new User();
        client.setId(3L);
        client.setUsername("client");
        client.setRole(UserRole.CLIENT);
    }

    @Test
    void createIntent_Success() {
        when(paymentService.createPaymentIntent(99L, 3L)).thenReturn("pi_secret_123");

        ResponseEntity<Map<String, String>> response = paymentController.createIntent(
                Map.of("bookingId", 99L),
                client
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("pi_secret_123", response.getBody().get("clientSecret"));
    }




    @Test
    void getAccountLink_Student_Success() {
        when(paymentService.getAccountLink(2L, "https://refresh", "https://return")).thenReturn("https://stripe/link");

        ResponseEntity<Map<String, String>> response = paymentController.getAccountLink(
                new PaymentConnectRequestDTO("https://refresh", "https://return"),
                student
        );

        assertEquals("https://stripe/link", response.getBody().get("url"));
    }

    @Test
    void completeConnectAccount_Student_Success() {
        when(paymentService.completeConnectAccount(2L)).thenReturn(true);

        ResponseEntity<Map<String, Boolean>> response = paymentController.completeConnectAccount(student);

        assertEquals(true, response.getBody().get("stripeConnected"));
    }

    @Test
    void getBalance_Student_Success() {
        when(paymentService.getConnectBalance(2L))
                .thenReturn(Map.of("available", List.of(Map.of("amount", 1000L))));

        ResponseEntity<Map<String, Object>> response = paymentController.getBalance(student);

        assertEquals(1, ((List<?>) response.getBody().get("available")).size());
    }

    @Test
    void getPayouts_Student_Success() {
        when(paymentService.getConnectPayouts(2L, 5)).thenReturn(List.of(Map.of("id", "po_1")));

        ResponseEntity<List<Object>> response = paymentController.getPayouts(5, student);

        assertEquals(1, response.getBody().size());
    }

    @Test
    void requestPayout_UsesDefaultCurrencyAndAmountParsing() {
        ResponseEntity<Void> response = paymentController.requestPayout(
                new PaymentPayoutRequestDTO(1234L, null),
                student
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(paymentService).requestPayout(2L, 1234L, "eur");
    }
}
