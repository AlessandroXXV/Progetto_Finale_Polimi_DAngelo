package it.eably.backend.service.impl;

import it.eably.backend.dto.common.response.VerificationDocumentsResponseDTO;
import it.eably.backend.exception.ResourceNotFoundException;
import it.eably.backend.exception.ValidationException;
import it.eably.backend.model.User;
import it.eably.backend.repository.UserRepository;
import it.eably.backend.service.def.VerificationDocumentService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Service implementation for identity verification document submission.
 *
 * <p>Validates PDF attachments and sends them via email to the configured recipient.</p>
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */
@Service
public class VerificationDocumentServiceImpl implements VerificationDocumentService
{

	/**
	 * PDF MIME type for validation.
	 */
	private static final String PDF_CONTENT_TYPE = "application/pdf";
	/**
	 * Maximum file size allowed for each document.
	 */
	private static final long MAX_FILE_SIZE = 3 * 1024 * 1024; // 3MB

	/**
	 * Mail sender for document delivery.
	 */
	private final JavaMailSender mailSender;
	/**
	 * Repository for user lookup.
	 */
	private final UserRepository userRepository;
	/**
	 * Recipient email for verification documents.
	 */
	private final String recipientEmail;
	/**
	 * Sender email for outgoing messages.
	 */
	private final String fromEmail;

	/**
	 * Builds the verification document service with required dependencies.
	 *
	 * @param mailSender     mail sender
	 * @param userRepository repository for users
	 * @param recipientEmail destination email for documents
	 * @param fromEmail      sender email
	 */
	public VerificationDocumentServiceImpl(JavaMailSender mailSender,
	                                       UserRepository userRepository,
	                                       @Value("${eably.verification.documents.recipient-email:developer@example.com}") String recipientEmail,
	                                       @Value("${spring.mail.username:notifications@eably.local}") String fromEmail)
	{
		this.mailSender = mailSender;
		this.userRepository = userRepository;
		this.recipientEmail = recipientEmail;
		this.fromEmail = fromEmail;
	}

	/**
	 * Submits verification documents for a user.
	 *
	 * <p>Effect: validates PDFs and sends them via email to the configured recipient.</p>
	 *
	 * @param userId        user id
	 * @param frontDocument front-side PDF
	 * @param backDocument  back-side PDF
	 * @return response DTO with confirmation message
	 * @throws ResourceNotFoundException when the user is not found
	 * @throws ValidationException       when documents are invalid or send fails
	 */
	@Override
	public VerificationDocumentsResponseDTO submitDocuments(Long userId, MultipartFile frontDocument, MultipartFile backDocument)
	{
		// Retrieve requesting user to personalize the submission.
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

		// Enforce screening rules (PDF only, file size limits).
		validatePdf(frontDocument, "frontDocument");
		validatePdf(backDocument, "backDocument");

		try {
			// Build a secure SMTP message for high-privilege processing.
			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, true);
			helper.setTo(recipientEmail);
			if (!fromEmail.isBlank()) {
				helper.setFrom(fromEmail);
			}
			helper.setSubject("Documenti verifica identità - " + user.getUsername());
			helper.setText(buildEmailBody(user), false);

			// Attach binary payloads as named stream resources.
			helper.addAttachment(buildAttachmentName("front", frontDocument), new ByteArrayResource(frontDocument.getBytes()));
			helper.addAttachment(buildAttachmentName("back", backDocument), new ByteArrayResource(backDocument.getBytes()));

			// Execute synchronous mail dispatch.
			mailSender.send(message);
		} catch (MessagingException | IOException e) {
			throw new ValidationException("Unable to send verification documents: " + e.getMessage());
		}

		return new VerificationDocumentsResponseDTO(
				"Documenti inviati con successo. Riceverai conferma via email."
		);
	}

	/**
	 * Validates that a file is a non-empty PDF within size limits.
	 *
	 * @param file      multipart file
	 * @param fieldName field name for error messages
	 * @throws ValidationException when validation fails
	 */
	private void validatePdf(MultipartFile file, String fieldName)
	{
		if (file == null || file.isEmpty()) {
			throw new ValidationException(fieldName + " is required");
		}
		String contentType = file.getContentType();
		if (!PDF_CONTENT_TYPE.equalsIgnoreCase(contentType)) {
			throw new ValidationException(fieldName + " must be a PDF file");
		}
		if (file.getSize() > MAX_FILE_SIZE) {
			throw new ValidationException(fieldName + " exceeds maximum size of 3MB");
		}
	}

	/**
	 * Builds the email body with user details.
	 *
	 * @param user user entity
	 * @return email body text
	 */
	private String buildEmailBody(User user)
	{
		return "Nuova richiesta verifica identità ricevuta." +
				"\n\nUtente: " + user.getUsername() +
				"\nNome: " + (user.getFullName() != null ? user.getFullName() : "-") +
				"\nEmail: " + user.getEmail() +
				"\nID utente: " + user.getId();
	}

	/**
	 * Builds a safe attachment file name.
	 *
	 * @param prefix label prefix
	 * @param file   multipart file
	 * @return sanitized attachment name
	 */
	private String buildAttachmentName(String prefix, MultipartFile file)
	{
		String original = file.getOriginalFilename();
		String sanitized = original == null || original.isBlank()
				? prefix + ".pdf"
				: original.replaceAll("[^a-zA-Z0-9._-]", "_");
		return prefix + "_" + sanitized;
	}
}

