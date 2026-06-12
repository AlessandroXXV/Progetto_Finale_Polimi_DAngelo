package it.eably.backend.model;

/**
 * Enumeration representing the delivery status of an {@link EmailQueue} entry.
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */
public enum EmailQueueStatus {

    /** The email is waiting to be sent. */
    PENDING,

    /** The email was successfully delivered. */
    SENT,

    /** All delivery attempts failed; no further retries will be made. */
    FAILED
}
