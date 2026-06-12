package it.eably.backend.exception;

/**
 * Exception thrown when a requested resource is not found in the database.
 * Results in HTTP 404 Not Found response.
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */
public class ResourceNotFoundException extends RuntimeException {
    
    public ResourceNotFoundException(String message) {
        super(message);
    }
    
    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
