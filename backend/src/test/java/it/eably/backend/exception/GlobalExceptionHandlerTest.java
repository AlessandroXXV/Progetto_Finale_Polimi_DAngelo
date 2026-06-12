package it.eably.backend.exception;

import it.eably.backend.dto.common.response.ErrorResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for GlobalExceptionHandler.
 *
 * Tests cover all 8 exception handlers:
 * - ValidationException → 400
 * - ResourceNotFoundException → 404
 * - AuthorizationException → 403
 * - ConflictException → 409
 * - MethodArgumentNotValidException → 400
 * - MaxUploadSizeExceededException → 413
 * - HttpMessageNotReadableException → 400
 * - Exception (generic) → 500
 */
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleValidationException_Returns400() {
        ValidationException ex = new ValidationException("Invalid input");

        ResponseEntity<ErrorResponseDTO> response = handler.handleValidationException(ex);

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().status());
        assertEquals("Invalid input", response.getBody().message());
        assertNotNull(response.getBody().timestamp());
    }

    @Test
    void handleResourceNotFoundException_Returns404() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Entity not found");

        ResponseEntity<ErrorResponseDTO> response = handler.handleResourceNotFoundException(ex);

        assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().status());
        assertEquals("Entity not found", response.getBody().message());
        assertNotNull(response.getBody().timestamp());
    }

    @Test
    void handleAuthorizationException_Returns403() {
        AuthorizationException ex = new AuthorizationException("Access denied");

        ResponseEntity<ErrorResponseDTO> response = handler.handleAuthorizationException(ex);

        assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(403, response.getBody().status());
        assertEquals("Access denied", response.getBody().message());
        assertNotNull(response.getBody().timestamp());
    }

    @Test
    void handleConflictException_Returns409() {
        ConflictException ex = new ConflictException("Resource already exists");

        ResponseEntity<ErrorResponseDTO> response = handler.handleConflictException(ex);

        assertEquals(HttpStatus.CONFLICT.value(), response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(409, response.getBody().status());
        assertEquals("Resource already exists", response.getBody().message());
        assertNotNull(response.getBody().timestamp());
    }

    @Test
    void handleMethodArgumentNotValidException_Returns400WithFieldErrors() throws Exception {
        BeanPropertyBindingResult bindingResult =
            new BeanPropertyBindingResult(new Object(), "testObject");
        bindingResult.addError(new FieldError("testObject", "email", "must not be blank"));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<ErrorResponseDTO> response = handler.handleValidationErrors(ex);

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().status());
        assertTrue(response.getBody().message().contains("email"));
        assertTrue(response.getBody().message().contains("must not be blank"));
        assertNotNull(response.getBody().timestamp());
    }

    @Test
    void handleMaxUploadSizeExceededException_Returns413() {
        MaxUploadSizeExceededException ex = new MaxUploadSizeExceededException(5 * 1024 * 1024L);

        ResponseEntity<ErrorResponseDTO> response = handler.handleMaxSizeException(ex);

        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE.value(), response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(413, response.getBody().status());
        assertTrue(response.getBody().message().contains("5MB"));
        assertNotNull(response.getBody().timestamp());
    }

    @Test
    void handleHttpMessageNotReadableException_Returns400() {
        HttpMessageNotReadableException ex =
            new HttpMessageNotReadableException("Invalid JSON", new MockHttpInputMessage(new byte[0]));

        ResponseEntity<ErrorResponseDTO> response = handler.handleHttpMessageNotReadable(ex);

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().status());
        assertTrue(response.getBody().message().contains("missing or contains invalid JSON"));
        assertNotNull(response.getBody().timestamp());
    }

    @Test
    void handleGenericException_Returns500() {
        Exception ex = new RuntimeException("Unexpected server error");

        ResponseEntity<ErrorResponseDTO> response = handler.handleGenericException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(500, response.getBody().status());
        assertEquals("An unexpected error occurred", response.getBody().message());
        assertNotNull(response.getBody().timestamp());
    }
}
