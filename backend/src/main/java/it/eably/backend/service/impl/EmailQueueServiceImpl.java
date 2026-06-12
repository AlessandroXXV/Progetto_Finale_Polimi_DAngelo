package it.eably.backend.service.impl;

import it.eably.backend.model.EmailQueue;
import it.eably.backend.model.EmailQueueStatus;
import it.eably.backend.repository.EmailQueueRepository;
import it.eably.backend.service.def.EmailQueueService;
import it.eably.backend.service.impl.email.EmailTemplateBuilder;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Email queue service implementation for enqueueing and retrying emails.
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */
@Service
public class EmailQueueServiceImpl implements EmailQueueService {

    /** Logger for email queue events. */
    private static final Logger log = LoggerFactory.getLogger(EmailQueueServiceImpl.class);

    /** Repository for queued emails. */
    private final EmailQueueRepository emailQueueRepository;
    /** Mail sender for SMTP delivery. */
    private final JavaMailSender mailSender;
    /** Resource loader for optional inline assets. */
    private final ResourceLoader resourceLoader;

    /** Default sender address for outgoing email. */
    @Value("${spring.mail.username:noreply@eably.it}")
    private String fromEmail;

    /**
     * Builds the email queue service with required dependencies.
     *
     * @param emailQueueRepository repository for queued emails
     * @param mailSender mail sender for SMTP delivery
     * @param resourceLoader resource loader for inline assets
     */
    public EmailQueueServiceImpl(EmailQueueRepository emailQueueRepository,
                                 JavaMailSender mailSender,
                                 ResourceLoader resourceLoader) {
        this.emailQueueRepository = emailQueueRepository;
        this.mailSender = mailSender;
        this.resourceLoader = resourceLoader;
    }

    /**
     * Enqueues an email for later delivery.
     *
     * <p>Effect: persists a new EmailQueue entry.</p>
     *
     * @param recipientEmail recipient email address
     * @param subject email subject
     * @param htmlContent email HTML body
     * @param logoPath optional logo resource path
     */
    @Override
    @Transactional
    public void enqueue(String recipientEmail, String subject, String htmlContent, String logoPath) {

        // Create a new queue entity.
        // The email is NOT sent immediately; it is only saved to the database.
        // This asynchronous approach prevents blocking the user while waiting for the SMTP server response.
        EmailQueue queued = new EmailQueue(recipientEmail, subject, htmlContent, logoPath);
        emailQueueRepository.save(queued);
        log.info("Email queued for {}: {}", recipientEmail, subject);
    }

    /**
     * Retries pending emails that are due for delivery.
     *
     * <p>Effect: loads pending emails and attempts to send them.</p>
     */
    @Override
    public void retryPending() {
        // Fetch emails from the DB that are in "PENDING" status or need to be retried
        // (e.g., after a temporary failure).
        List<EmailQueue> pending = emailQueueRepository.findPendingEmailsToRetry();

        // If the queue is empty, exit immediately to save processing resources.
        if (pending.isEmpty()) return;

        log.info("Retrying {} queued email(s)", pending.size());

        // Iterate through each queued email and attempt to send them individually.
        for (EmailQueue email : pending) {
            trySend(email);
        }
    }

    /**
     * Attempts to send a queued email and updates its status.
     *
     * @param email queued email entity
     */
    private void trySend(EmailQueue email) {
        // Immediately update the last attempt timestamp and increment the counter.
        // This is crucial to prevent infinite sending loops in case of persistent errors.
        email.setLastAttemptAt(LocalDateTime.now());
        email.setRetryCount(email.getRetryCount() + 1);

        try {
            // Create a MimeMessage, which is required for complex emails
            // (HTML, attachments, inline images) instead of simple plain text.
            MimeMessage message = mailSender.createMimeMessage();

            // The 'true' flag indicates a "multipart" message (allows inline attachments).
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(email.getRecipientEmail());
            helper.setSubject(email.getSubject());

            // The second parameter 'true' tells the helper that the text is HTML formatted.
            helper.setText(email.getHtmlContent(), true);

            // Attempt to attach the logo (if present) before sending.
            attachLogo(helper, email.getLogoPath());

            // Actual SMTP delivery.
            mailSender.send(message);

            // If sending succeeds, update the status and clear any previous error messages.
            email.setStatus(EmailQueueStatus.SENT);
            email.setErrorMessage(null);
            log.info("Queued email SENT to {}: {}", email.getRecipientEmail(), email.getSubject());

        } catch (Exception e) {
            // In case of a sending error (e.g., SMTP unreachable, bad credentials),
            // save the error message to the database to facilitate debugging.
            email.setErrorMessage(e.getMessage());

            // Check the retry limit: if we have reached or exceeded the maximum allowed attempts...
            if (email.getRetryCount() >= email.getMaxRetries()) {
                // ...mark the email as permanently FAILED. It will no longer be picked up by retryPending().
                email.setStatus(EmailQueueStatus.FAILED);
                log.error("Email FAILED permanently for {} after {} retries: {}",
                        email.getRecipientEmail(), email.getMaxRetries(), e.getMessage());
            } else {
                // ...otherwise, log a warning. The status remains unchanged (typically PENDING)
                // and it will be retried during the next retryPending() execution.
                log.warn("Email retry {}/{} failed for {}: {}",
                        email.getRetryCount(), email.getMaxRetries(), email.getRecipientEmail(), e.getMessage());
            }
        }

        // Finally, regardless of the outcome (success or failure), save the changes to the DB
        // (new status, updated counter, error messages if any).
        emailQueueRepository.save(email);
    }

    /**
     * Attaches the logo inline if a valid resource path is provided.
     *
     * @param helper MIME helper
     * @param logoPath resource path to the logo
     */
    private void attachLogo(MimeMessageHelper helper, String logoPath) {
        // If there is no logo path provided, exit without doing anything.
        if (logoPath == null || logoPath.isBlank()) return;

        try {
            // Load the resource from the classpath or filesystem.
            Resource logoResource = resourceLoader.getResource(logoPath);

            if (logoResource.exists()) {
                // Adds the image as "inline".
                // EmailTemplateBuilder.LOGO_CID is the Content-ID that must match
                // the HTML tag in the email body, e.g., <img src="cid:value_of_LOGO_CID">.
                // This ensures the image is displayed directly inside the email body, not as a standard attachment.
                helper.addInline(EmailTemplateBuilder.LOGO_CID, logoResource);
            }
        } catch (Exception e) {
            // An error loading the logo shouldn't block the entire email from being sent,
            // so we log a warning and let the execution proceed.
            log.warn("Failed to attach logo in retry: {}", e.getMessage());
        }
    }
}

