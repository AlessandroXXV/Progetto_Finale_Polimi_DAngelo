package it.eably.backend.exception;

/**
 * Exception thrown when a user attempts to access a resource without sufficient permissions.
 * Results in HTTP 403 Forbidden response.
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */
public class AuthorizationException extends RuntimeException {
    
    public AuthorizationException(String message) {
        super(message);
    }
    
    public AuthorizationException(String message, Throwable cause) {
        super(message, cause);
    }
}
