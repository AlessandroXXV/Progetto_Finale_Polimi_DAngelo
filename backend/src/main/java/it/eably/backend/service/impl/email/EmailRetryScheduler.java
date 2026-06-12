package it.eably.backend.service.impl.email;

import it.eably.backend.service.def.EmailQueueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job that triggers retries for pending email queue items.
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */
@Component
public class EmailRetryScheduler
{

    /**
     * Logger for scheduler events.
     */
    private static final Logger log = LoggerFactory.getLogger(EmailRetryScheduler.class);

    /**
     * Email queue service used to perform retries.
     */
    private final EmailQueueService emailQueueService;

    /**
     * Builds the retry scheduler with required dependencies.
     *
     * @param emailQueueService email queue service
     */
    public EmailRetryScheduler(EmailQueueService emailQueueService)
    {
        this.emailQueueService = emailQueueService;
    }

    /**
     * Retries pending emails on a fixed delay schedule.
     *
     * <p>Effect: invokes {@link EmailQueueService#retryPending()}.</p>
     */
    @Scheduled(fixedDelayString = "${app.email.retry-interval-ms:300000}")
    public void retryFailedEmails() {
        log.debug("Email retry scheduler running");
        // Delegating to the queue service which handles transaction boundaries and status updates.
        emailQueueService.retryPending();
    }
}
