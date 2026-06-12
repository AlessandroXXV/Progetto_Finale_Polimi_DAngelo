package it.eably.backend.exception;

/**
 * Exception thrown when a resource conflict occurs (e.g., duplicate username or email).
 * Results in HTTP 409 Conflict response.
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */
public class ConflictException extends RuntimeException {
    
    public ConflictException(String message) {
        super(message);
    }
}
