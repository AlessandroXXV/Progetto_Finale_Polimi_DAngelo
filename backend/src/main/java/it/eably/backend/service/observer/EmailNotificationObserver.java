package it.eably.backend.service.observer;

import it.eably.backend.model.Booking;
import it.eably.backend.model.BookingCancellationReason;
import it.eably.backend.service.def.EmailQueueService;
import it.eably.backend.service.impl.email.EmailTemplateBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import jakarta.annotation.PreDestroy;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Concrete observer that sends email notifications on booking status changes.
 * 
 * DESIGN PATTERN: OBSERVER (Concrete Observer)
 * 
 * RESPONSIBILITY:
 * - Reacts to booking status changes
 * - Sends appropriate email notifications to client and provider
 * - Logs notification attempts for audit trail
 * 
 * NOTIFICATION SCENARIOS:
 * - PAYMENT_PENDING → CONFIRMED: Send confirmation email with booking details
 * - CONFIRMED → COMPLETED: Send completion email with review request
 * - Any → CANCELLED: Send cancellation email with refund information
 * 
 * DECOUPLING BENEFITS:
 * - BookingService doesn't know about email sending
 * - Email logic can be changed without touching business logic
 * - Easy to add SMS, push notifications, etc. as additional observers
 * - Email failures don't affect booking operations
 *
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */
@Component
public class EmailNotificationObserver implements BookingObserver {
    
    private static final Logger log = LoggerFactory.getLogger(EmailNotificationObserver.class);


    private final JavaMailSender mailSender;
    private final ResourceLoader resourceLoader;
    private final EmailQueueService emailQueueService;
    private final ExecutorService emailExecutor = Executors.newFixedThreadPool(2);

    @Value("${spring.mail.username:noreply@eably.it}")
    private String fromEmail;

    @Value("${app.email.logo-path:classpath:/static/images/logo.png}")
    private String logoPath;

    @Autowired
    public EmailNotificationObserver(JavaMailSender mailSender,
                                      ResourceLoader resourceLoader,
                                      EmailQueueService emailQueueService) {
        this.mailSender = mailSender;
        this.resourceLoader = resourceLoader;
        this.emailQueueService = emailQueueService;
    }

    /**
     * Reacts to booking status changes by sending appropriate email notifications.
     * 
     * OBSERVER PATTERN IN ACTION:
     * - This method is called automatically by the subject (BookingService)
     * - Observer queries the booking for current state
     * - Observer performs its specific action (email notification)
     * - Exceptions are caught and logged (don't propagate to subject)
     * 
     * @param booking the booking that changed status
     */
    @Override
    public void onBookingStatusChanged(Booking booking) {
        try {
            // Handle null booking gracefully
            if (booking == null) {
                log.warn("Observer received null booking, skipping notification");
                return;
            }
            if (booking.getStatus() == null) {
                log.warn("Observer received booking with null status, skipping notification");
                return;
            }
            
            log.info("Observer triggered for booking {} with status {}", 
                     booking.getId(), booking.getStatus());
            
            // Determine notification type based on status
            switch (booking.getStatus()) {
                case CONFIRMED:
                    sendConfirmationEmail(booking);
                    break;
                    
                case COMPLETED:
                    sendCompletionEmail(booking);
                    break;
                    
                case CANCELLED:
                    if (booking.getCancellationReason() == BookingCancellationReason.PAYMENT_TIMEOUT) {
                        sendTimeoutCancellationEmail(booking);
                    } else {
                        sendCancellationEmail(booking);
                    }
                    break;
                    
                case PAYMENT_PENDING:
                    sendPaymentPendingEmail(booking);
                    break;
                    
                default:
                    log.debug("No email notification configured for status: {}", booking.getStatus());
            }
            
        } catch (Exception e) {
            // CRITICAL: Catch all exceptions to prevent observer from breaking subject
            // Email failures should not cause booking operations to fail
            log.error("Failed to send email notification for booking {}: {}", 
                      booking.getId(), e.getMessage(), e);
        }
    }
    
    /**
     * Sends confirmation email after successful payment.
     * Modern HTML emails with design system styling.
     *
     * @param booking the confirmed booking
     */
    private void sendConfirmationEmail(Booking booking) {
        log.info("Sending confirmation email for booking {}", booking.getId());

        if (booking.getClient() == null || booking.getProvider() == null) {
            log.warn("Booking {} missing client or provider, skipping confirmation email", booking.getId());
            return;
        }

        String clientEmail = booking.getClient().getEmail();
        String providerName = booking.getProvider().getUsername();
        String providerEmail = booking.getProvider().getEmail();
        String clientName = booking.getClient().getUsername();
        String confirmationCode = booking.getConfirmationCode();

        EmailTemplateBuilder builder = new EmailTemplateBuilder();

        sendOrQueue(clientEmail, "Prenotazione Confermata - Eably",
                builder.buildConfirmationEmail(clientName, booking.getId(), providerName,
                        confirmationCode, booking.getTotalAmount().toString()));

        sendOrQueue(providerEmail, "Nuova Prenotazione Confermata - Eably",
                builder.buildProviderConfirmationEmail(providerName, booking.getId(),
                        clientName, booking.getTotalAmount().toString()));
    }
    
    private void sendCompletionEmail(Booking booking) {
        log.info("Sending completion email for booking {}", booking.getId());

        if (booking.getClient() == null || booking.getProvider() == null) {
            log.warn("Booking {} missing client or provider, skipping completion email", booking.getId());
            return;
        }

        String clientEmail = booking.getClient().getEmail();
        String providerEmail = booking.getProvider().getEmail();
        String providerName = booking.getProvider().getUsername();
        String clientName = booking.getClient().getUsername();

        EmailTemplateBuilder builder = new EmailTemplateBuilder();

        sendOrQueue(clientEmail, "Sessione Completata - Eably",
                builder.buildCompletionEmail(clientName, providerName));

        sendOrQueue(providerEmail, "Sessione Completata - Eably",
                builder.buildProviderCompletionEmail(providerName, clientName));
    }
    
    private void sendCancellationEmail(Booking booking) {
        log.info("Sending cancellation email for booking {}", booking.getId());

        if (booking.getClient() == null || booking.getProvider() == null) {
            log.warn("Booking {} missing client or provider, skipping cancellation email", booking.getId());
            return;
        }

        String clientEmail = booking.getClient().getEmail();
        String providerEmail = booking.getProvider().getEmail();
        String providerName = booking.getProvider().getUsername();
        String clientName = booking.getClient().getUsername();

        EmailTemplateBuilder builder = new EmailTemplateBuilder();

        sendOrQueue(clientEmail, "Prenotazione Annullata - Eably",
                builder.buildCancellationEmail(clientName, providerName, booking.getId()));

        sendOrQueue(providerEmail, "Prenotazione Annullata - Eably",
                builder.buildProviderCancellationEmail(providerName, booking.getId(), clientName));
    }
    
    private void sendTimeoutCancellationEmail(Booking booking) {
        log.info("Sending payment timeout cancellation email for booking {}", booking.getId());

        if (booking.getClient() == null || booking.getAvailabilitySlot() == null) {
            log.warn("Booking {} missing client or availability slot, skipping timeout cancellation email", booking.getId());
            return;
        }

        String clientEmail = booking.getClient().getEmail();
        String clientName = booking.getClient().getUsername();
        String bookingDate = booking.getBookingDate().toString();
        String startTime = booking.getAvailabilitySlot().getStartTime().toString();
        String endTime = booking.getAvailabilitySlot().getEndTime().toString();

        String htmlContent = new EmailTemplateBuilder().buildTimeoutCancellationEmail(
                clientName, booking.getId(), bookingDate, startTime, endTime
        );

        sendOrQueue(clientEmail, "Prenotazione Annullata - Timeout Pagamento", htmlContent);
    }

    private void sendPaymentPendingEmail(Booking booking) {
        log.info("Sending payment pending email for booking {}", booking.getId());

        if (booking.getClient() == null || booking.getProvider() == null) {
            log.warn("Booking {} missing client or provider, skipping payment pending email", booking.getId());
            return;
        }

        String clientEmail = booking.getClient().getEmail();
        String clientName = booking.getClient().getUsername();
        String providerName = booking.getProvider().getUsername();

        EmailTemplateBuilder builder = new EmailTemplateBuilder();

        sendOrQueue(clientEmail, "Pagamento Richiesto - Eably",
                builder.buildPaymentPendingEmail(clientName, providerName, booking.getTotalAmount().toString()));
    }
    
    @Override
    public String getObserverName() {
        return "Email Notification Observer";
    }

    private void sendOrQueue(String to, String subject, String htmlContent) {
        emailExecutor.submit(() -> {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                helper.setFrom(fromEmail);
                helper.setTo(to);
                helper.setSubject(subject);
                helper.setText(htmlContent, true);
                attachLogo(helper);
                mailSender.send(message);
                log.info("Email SENT to {}: {}", to, subject);
            } catch (MessagingException e) {
                log.warn("Email send failed for {}, queuing for retry: {}", to, e.getMessage());
                emailQueueService.enqueue(to, subject, htmlContent, logoPath);
            }
        });
    }

    @PreDestroy
    public void shutdown() {
        emailExecutor.shutdown();
        try {
            if (!emailExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                emailExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            emailExecutor.shutdownNow();
        }
    }

    private void attachLogo(MimeMessageHelper helper) {
        try {
            Resource logoResource = resourceLoader.getResource(logoPath);
            if (logoResource.exists()) {
                helper.addInline(EmailTemplateBuilder.LOGO_CID, logoResource);
            } else {
                log.warn("Logo resource not found at {}", logoPath);
            }
        } catch (Exception e) {
            log.warn("Failed to attach logo: {}", e.getMessage());
        }
    }


}
