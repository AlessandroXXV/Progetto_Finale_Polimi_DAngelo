package it.eably.backend.exception;

/**
 * Exception thrown when entity validation fails.
 * 
 * This is a runtime exception that indicates business rule violations
 * during entity validation.
 * 
 * @author Alessandro D'Angelo
 * @version 1.0
 */
public class ValidationException extends RuntimeException {
    
    /**
     * Constructs a new ValidationException with the specified detail message.
     * 
     * @param message the detail message explaining the validation failure
     */
    public ValidationException(String message) {
        super(message);
    }
    
    /**
     * Constructs a new ValidationException with the specified detail message and cause.
     * 
     * @param message the detail message explaining the validation failure
     * @param cause the cause of the validation failure
     */
    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
