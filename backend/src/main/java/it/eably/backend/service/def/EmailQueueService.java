package it.eably.backend.service.def;


/**
 * Service interface for email queue management.
 * <p>
 * Handles persistence and retrieval of emails linked to booking sessions.
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */
public interface EmailQueueService {

    /**
     * Creates and persists a new email record for eventual delivery.
     *
     * @param recipientEmail destination address
     * @param subject        message subject line
     * @param htmlContent    complete HTML message body
     * @param logoPath       optional path or CID for inline logo
     */
    void enqueue(String recipientEmail, String subject, String htmlContent, String logoPath);

    /**
     * Executes retry attempts for all emails in PENDING status.
     * <p>Typically invoked by a background scheduler.</p>
     */
    void retryPending();
}
