package it.eably.backend.service.impl.email;

/**
 * Builds HTML email templates for booking lifecycle notifications.
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */
public class EmailTemplateBuilder {

    /** Content-ID used for inline logo in email templates. */
    public static final String LOGO_CID = "logo";

    // Design system colors from frontend
    /** Primary brand purple. */
    private static final String PURPLE = "#5B4EE8";
    /** Primary dark navy for text. */
    private static final String NAVY = "#0F0E2A";
    /** Warm background tone. */
    private static final String WARM = "#FBF8F3";
    /** Muted text color. */
    private static final String TEXT_MUTED = "#6E6B8A";

    /** Default constructor. */
    public EmailTemplateBuilder() {
    }

    /**
     * Builds the booking confirmation email for a client.
     *
     * @param clientName client display name
     * @param bookingId booking id
     * @param providerName provider display name
     * @param confirmationCode confirmation code to share with provider
     * @param totalAmount total amount formatted for display
     * @return HTML email body
     */
    public String buildConfirmationEmail(
            String clientName,
            long bookingId,
            String providerName,
            String confirmationCode,
            String totalAmount) {

        return buildEmailTemplate(
                "Prenotazione Confermata",
                String.format(
                        "<h2 style=\"color: %s; margin: 0 0 1.5rem 0; font-size: 28px;\">Prenotazione #%d Confermata!</h2>" +
                        "<p style=\"font-size: 16px; color: %s; margin: 0 0 1.5rem 0; line-height: 1.6;\">" +
                        "Ciao <strong>%s</strong>,</p>" +
                        "<div style=\"background: %s; border-left: 4px solid %s; padding: 1.5rem; margin: 1.5rem 0; border-radius: 8px;\">" +
                        "<p style=\"margin: 0 0 0.5rem 0; font-size: 14px; color: %s;\"><strong>Dettagli Prenotazione</strong></p>" +
                        "<p style=\"margin: 0 0 0.5rem 0; font-size: 14px; color: %s;\">Prenotazione: #%d</p>" +
                        "<p style=\"margin: 0 0 0.5rem 0; font-size: 14px; color: %s;\">Provider: <strong>%s</strong></p>" +
                        "<p style=\"margin: 0 0 0.5rem 0; font-size: 14px; color: %s;\">Importo Totale: <strong>€%s</strong></p>" +
                        "</div>" +
                        "<div style=\"background: %s; padding: 1.5rem; border-radius: 8px; text-align: center; margin: 1.5rem 0;\">" +
                        "<p style=\"margin: 0 0 0.5rem 0; font-size: 12px; color: %s; text-transform: uppercase; letter-spacing: 1px;\">Codice di Conferma</p>" +
                        "<p style=\"margin: 0; font-size: 32px; font-weight: bold; color: %s; font-family: 'Courier New', monospace; letter-spacing: 4px;\">%s</p>" +
                        "</div>" +
                        "<p style=\"font-size: 14px; color: %s; margin: 1.5rem 0; line-height: 1.6;\">" +
                        "Fornisci questo codice al provider al termine del servizio." +
                        "</p>",
                        PURPLE, bookingId, TEXT_MUTED, clientName, WARM, PURPLE, NAVY, NAVY, bookingId, NAVY, providerName, NAVY, totalAmount, WARM, TEXT_MUTED, PURPLE, confirmationCode, TEXT_MUTED
                )
        );
    }

    /**
     * Builds the session completion email for a client.
     *
     * @param clientName client display name
     * @param providerName provider display name
     * @return HTML email body
     */
    public String buildCompletionEmail(
            String clientName,
            String providerName) {

        return buildEmailTemplate(
                "Sessione Completata",
                String.format(
                        "<h2 style=\"color: %s; margin: 0 0 1.5rem 0; font-size: 28px;\">Sessione Completata! 🎉</h2>" +
                        "<p style=\"font-size: 16px; color: %s; margin: 0 0 1.5rem 0; line-height: 1.6;\">" +
                        "Ciao <strong>%s</strong>,</p>" +
                        "<p style=\"font-size: 16px; color: %s; margin: 0 0 1.5rem 0; line-height: 1.6;\">" +
                        "La tua sessione con <strong>%s</strong> è stata completata con successo!</p>" +
                        "<div style=\"background: %s; border-radius: 8px; padding: 1.5rem; margin: 1.5rem 0; text-align: center;\">" +
                        "<p style=\"margin: 0; font-size: 16px; color: %s; line-height: 1.6;\">" +
                        "Considera di lasciare una recensione per aiutare altri studenti a trovare i migliori provider." +
                        "</p>" +
                        "</div>" +
                        "<table role=\"presentation\" style=\"margin: 1.5rem 0; width: 100%%; border-collapse: collapse;\">" +
                        "<tr>" +
                        "<td><a href=\"https://eably.it/reviews\" style=\"display: inline-block; background: %s; color: white; padding: 12px 24px; border-radius: 24px; text-decoration: none; font-weight: 500; font-size: 14px;\">Lascia una Recensione</a></td>" +
                        "</tr>" +
                        "</table>",
                        PURPLE, TEXT_MUTED, clientName, TEXT_MUTED, providerName, "#f0f0f0", NAVY, PURPLE
                )
        );
    }

    /**
     * Builds the booking cancellation email for a client.
     *
     * @param clientName client display name
     * @param providerName provider display name
     * @param bookingId booking id
     * @return HTML email body
     */
    public String buildCancellationEmail(
            String clientName,
            String providerName,
            long bookingId) {

        return buildEmailTemplate(
                "Prenotazione Annullata",
                String.format(
                        "<h2 style=\"color: %s; margin: 0 0 1.5rem 0; font-size: 28px;\">Prenotazione Annullata</h2>" +
                        "<p style=\"font-size: 16px; color: %s; margin: 0 0 1.5rem 0; line-height: 1.6;\">" +
                        "Ciao <strong>%s</strong>,</p>" +
                        "<p style=\"font-size: 16px; color: %s; margin: 0 0 1.5rem 0; line-height: 1.6;\">" +
                        "La prenotazione #%d con <strong>%s</strong> è stata annullata.</p>" +
                        "<div style=\"background: #fff9f5; border-left: 4px solid #F0603A; padding: 1.5rem; margin: 1.5rem 0; border-radius: 8px;\">" +
                        "<p style=\"margin: 0; font-size: 14px; color: %s; line-height: 1.6;\">" +
                        "Se il pagamento è stato già elaborato, il rimborso sarà emesso entro 5-7 giorni lavorativi." +
                        "</p>" +
                        "</div>",
                        PURPLE, TEXT_MUTED, clientName, TEXT_MUTED, bookingId, providerName, TEXT_MUTED
                )
        );
    }

    /**
     * Builds the payment pending email for a client.
     *
     * @param clientName client display name
     * @param providerName provider display name
     * @param totalAmount total amount formatted for display
     * @return HTML email body
     */
    public String buildPaymentPendingEmail(
            String clientName,
            String providerName,
            String totalAmount) {

        return buildEmailTemplate(
                "Pagamento Richiesto",
                String.format(
                        "<h2 style=\"color: %s; margin: 0 0 1.5rem 0; font-size: 28px;\">Completa il Pagamento</h2>" +
                        "<p style=\"font-size: 16px; color: %s; margin: 0 0 1.5rem 0; line-height: 1.6;\">" +
                        "Ciao <strong>%s</strong>,</p>" +
                        "<p style=\"font-size: 16px; color: %s; margin: 0 0 1.5rem 0; line-height: 1.6;\">" +
                        "Una richiesta di prenotazione è stata creata con <strong>%s</strong>.</p>" +
                        "<div style=\"background: %s; border-radius: 8px; padding: 2rem; margin: 1.5rem 0; text-align: center;\">" +
                        "<p style=\"margin: 0 0 1rem 0; font-size: 14px; color: %s; text-transform: uppercase; letter-spacing: 1px;\">Importo Totale</p>" +
                        "<p style=\"margin: 0 0 1.5rem 0; font-size: 40px; font-weight: bold; color: %s;\">€%s</p>" +
                        "<a href=\"https://eably.it/payments\" style=\"display: inline-block; background: %s; color: white; padding: 14px 32px; border-radius: 24px; text-decoration: none; font-weight: 500; font-size: 16px;\">Completa Pagamento</a>" +
                        "</div>" +
                        "<p style=\"font-size: 14px; color: %s; margin: 1.5rem 0; line-height: 1.6;\">" +
                        "Completa il pagamento per confermare la tua prenotazione." +
                        "</p>",
                        PURPLE, TEXT_MUTED, clientName, TEXT_MUTED, providerName, WARM, TEXT_MUTED, PURPLE, totalAmount, PURPLE, TEXT_MUTED
                )
        );
    }

    /**
     * Builds the booking confirmation email for a provider.
     *
     * @param providerName provider display name
     * @param bookingId booking id
     * @param clientName client display name
     * @param totalAmount total amount formatted for display
     * @return HTML email body
     */
    public String buildProviderConfirmationEmail(
            String providerName,
            long bookingId,
            String clientName,
            String totalAmount) {

        return buildEmailTemplate(
                "Nuova Prenotazione Confermata",
                String.format(
                        "<h2 style=\"color: %s; margin: 0 0 1.5rem 0; font-size: 28px;\">Nuova Prenotazione! 🎯</h2>" +
                        "<p style=\"font-size: 16px; color: %s; margin: 0 0 1.5rem 0; line-height: 1.6;\">" +
                        "Ciao <strong>%s</strong>,</p>" +
                        "<p style=\"font-size: 16px; color: %s; margin: 0 0 1.5rem 0; line-height: 1.6;\">" +
                        "Hai una nuova prenotazione confermata!</p>" +
                        "<div style=\"background: %s; border-left: 4px solid %s; padding: 1.5rem; margin: 1.5rem 0; border-radius: 8px;\">" +
                        "<p style=\"margin: 0 0 0.5rem 0; font-size: 14px; color: %s;\"><strong>Dettagli Prenotazione</strong></p>" +
                        "<p style=\"margin: 0 0 0.5rem 0; font-size: 14px; color: %s;\">Prenotazione: #%d</p>" +
                        "<p style=\"margin: 0 0 0.5rem 0; font-size: 14px; color: %s;\">Cliente: <strong>%s</strong></p>" +
                        "<p style=\"margin: 0 0 0.5rem 0; font-size: 14px; color: %s;\">Importo: <strong>€%s</strong></p>" +
                        "</div>" +
                        "<a href=\"https://eably.it/bookings/%d\" style=\"display: inline-block; background: %s; color: white; padding: 12px 24px; border-radius: 24px; text-decoration: none; font-weight: 500; font-size: 14px;\">Visualizza Prenotazione</a>",
                        PURPLE, TEXT_MUTED, providerName, TEXT_MUTED, WARM, PURPLE, NAVY, NAVY, bookingId, NAVY, clientName, NAVY, totalAmount, bookingId, PURPLE
                )
        );
    }

    /**
     * Builds the session completion email for a provider.
     *
     * @param providerName provider display name
     * @param clientName client display name
     * @return HTML email body
     */
    public String buildProviderCompletionEmail(
            String providerName,
            String clientName) {

        return buildEmailTemplate(
                "Sessione Completata",
                String.format(
                        "<h2 style=\"color: %s; margin: 0 0 1.5rem 0; font-size: 28px;\">Sessione Completata! ✅</h2>" +
                        "<p style=\"font-size: 16px; color: %s; margin: 0 0 1.5rem 0; line-height: 1.6;\">" +
                        "Ciao <strong>%s</strong>,</p>" +
                        "<p style=\"font-size: 16px; color: %s; margin: 0 0 1.5rem 0; line-height: 1.6;\">" +
                        "La tua sessione con <strong>%s</strong> è stata marcata come completata. Ottimo lavoro!</p>" +
                        "<div style=\"background: %s; border-radius: 8px; padding: 1.5rem; margin: 1.5rem 0; text-align: center;\">" +
                        "<p style=\"margin: 0; font-size: 16px; color: %s; line-height: 1.6;\">" +
                        "Continua a fornire servizi di qualità per ottenere più prenotazioni." +
                        "</p>" +
                        "</div>",
                        PURPLE, TEXT_MUTED, providerName, TEXT_MUTED, clientName, "#f0f0f0", NAVY
                )
        );
    }

    /**
     * Builds the booking cancellation email for a provider.
     *
     * @param providerName provider display name
     * @param bookingId booking id
     * @param clientName client display name
     * @return HTML email body
     */
    public String buildProviderCancellationEmail(
            String providerName,
            long bookingId,
            String clientName) {

        return buildEmailTemplate(
                "Prenotazione Annullata",
                String.format(
                        "<h2 style=\"color: %s; margin: 0 0 1.5rem 0; font-size: 28px;\">Prenotazione Annullata</h2>" +
                        "<p style=\"font-size: 16px; color: %s; margin: 0 0 1.5rem 0; line-height: 1.6;\">" +
                        "Ciao <strong>%s</strong>,</p>" +
                        "<p style=\"font-size: 16px; color: %s; margin: 0 0 1.5rem 0; line-height: 1.6;\">" +
                        "La prenotazione #%d con <strong>%s</strong> è stata annullata.</p>",
                        PURPLE, TEXT_MUTED, providerName, TEXT_MUTED, bookingId, clientName
                )
        );
    }

    /**
     * Builds the timeout cancellation email for a client.
     *
     * @param clientName client display name
     * @param bookingId booking id
     * @param bookingDate booking date string
     * @param startTime session start time
     * @param endTime session end time
     * @return HTML email body
     */
    public String buildTimeoutCancellationEmail(
            String clientName,
            long bookingId,
            String bookingDate,
            String startTime,
            String endTime) {

        String content = "<h2 style=\"color: " + PURPLE + "; margin: 0 0 1.5rem 0; font-size: 28px;\">Prenotazione Annullata</h2>" +
                "<p style=\"font-size: 16px; color: " + TEXT_MUTED + "; margin: 0 0 1.5rem 0; line-height: 1.6;\">" +
                "Ciao <strong>" + clientName + "</strong>,</p>" +
                "<div style=\"background: #fff9f5; border-left: 4px solid #F0603A; padding: 1.5rem; margin: 1.5rem 0; border-radius: 8px;\">" +
                "<p style=\"margin: 0; font-size: 14px; color: " + TEXT_MUTED + "; line-height: 1.6;\">" +
                "La tua prenotazione <strong>#" + bookingId + "</strong> è stata annullata perché non hai completato il pagamento entro il tempo previsto (10 minuti).</p>" +
                "</div>" +
                "<div style=\"background: " + WARM + "; border-radius: 8px; padding: 1.5rem; margin: 1.5rem 0;\">" +
                "<p style=\"margin: 0 0 0.5rem 0; font-size: 12px; color: " + TEXT_MUTED + "; text-transform: uppercase; letter-spacing: 1px;\">Dettagli Sessione</p>" +
                "<p style=\"margin: 0 0 0.5rem 0; font-size: 14px; color: " + NAVY + ";\">Data: <strong>" + bookingDate + "</strong></p>" +
                "<p style=\"margin: 0; font-size: 14px; color: " + NAVY + ";\">Orario: <strong>" + startTime + " - " + endTime + "</strong></p>" +
                "</div>" +
                "<p style=\"font-size: 14px; color: " + TEXT_MUTED + "; margin: 1.5rem 0; line-height: 1.6;\">" +
                "Per questa specifica data e fascia oraria la prenotazione è ora bloccata in modo permanente. " +
                "Se ritieni ci sia un errore, <a href=\"https://eably.it/support\" style=\"color: " + PURPLE + "; text-decoration: none;\">contatta il supporto</a>." +
                "</p>";

        return buildEmailTemplate("Prenotazione Annullata - Timeout Pagamento", content);
    }

    /**
     * Wraps the provided HTML content in the shared email template.
     *
     * @param subject email subject
     * @param content HTML body content
     * @return full HTML email template
     */
    private String buildEmailTemplate(String subject, String content) {
        return new StringBuilder()
                .append("<!DOCTYPE html>\n")
                .append("<html style=\"font-family: 'DM Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;\">\n")
                .append("<head>\n")
                .append("<meta charset=\"UTF-8\">\n")
                .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n")
                .append("<title>").append(subject).append("</title>\n")
                .append("<style>\n")
                .append("body { margin: 0; padding: 0; background: #f5f5f5; }\n")
                .append(".email-wrapper { background: #f5f5f5; padding: 20px; }\n")
                .append(".email-container { max-width: 600px; margin: 0 auto; background: white; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 16px rgba(0,0,0,0.08); }\n")
                .append(".email-header { background: linear-gradient(135deg, ").append(PURPLE).append(" 0%, rgba(91,78,232,0.05) 100%); padding: 2rem; text-align: center; border-bottom: 1px solid #f0f0f0; }\n")
                .append(".email-logo { height: 40px; margin-bottom: 1rem; }\n")
                .append(".email-header-title { margin: 0; color: ").append(PURPLE).append("; font-size: 24px; font-weight: 700; }\n")
                .append(".email-body { padding: 2rem; }\n")
                .append(".email-footer { background: #f9f9f9; padding: 1.5rem 2rem; border-top: 1px solid #f0f0f0; font-size: 12px; color: ").append(TEXT_MUTED).append("; text-align: center; }\n")
                .append(".email-footer a { color: ").append(PURPLE).append("; text-decoration: none; }\n")
                .append("a { color: ").append(PURPLE).append("; }\n")
                .append("table { border-spacing: 0; border-collapse: collapse; }\n")
                .append("@media (max-width: 600px) { .email-container { width: 100%; } .email-body { padding: 1.5rem; } .email-header { padding: 1.5rem; } }\n")
                .append("</style>\n")
                .append("</head>\n")
                .append("<body>\n")
                .append("<div class=\"email-wrapper\">\n")
                .append("<div class=\"email-container\">\n")
                .append("<div class=\"email-header\">\n")
                .append("<img src=\"cid:").append(LOGO_CID).append("\" alt=\"Eably\" class=\"email-logo\">\n")
                .append("<h1 class=\"email-header-title\">Eably</h1>\n")
                .append("</div>\n")
                .append("<div class=\"email-body\">\n")
                .append(content).append("\n")
                .append("</div>\n")
                .append("<div class=\"email-footer\">\n")
                .append("<p style=\"margin: 0; padding: 0 0 0.5rem 0;\">© 2025 Eably. Tutti i diritti riservati.</p>\n")
                .append("<p style=\"margin: 0 0 0.5rem 0; padding: 0;\"><a href=\"https://eably.it\">Visita il nostro sito</a> | <a href=\"https://eably.it/support\">Contatta il supporto</a></p>\n")
                .append("</div>\n")
                .append("</div>\n")
                .append("</div>\n")
                .append("</body>\n")
                .append("</html>")
                .toString();
    }
}
