package it.eably.backend.controller;

import it.eably.backend.dto.common.response.VerificationDocumentsResponseDTO;
import it.eably.backend.dto.common.response.VerificationStatusDTO;
import it.eably.backend.model.User;
import it.eably.backend.service.def.UserAccountService;
import it.eably.backend.service.def.VerificationDocumentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST controller for managing user identity verification.
 * <p>
 * Allows users to check their verification status and submit identity
 * documents to request verification.
 * </p>
 *
 * Endpoints:
 * <ul>
 * <li>GET  /api/v1/verification/status    – Returns the verification status of the authenticated user</li>
 * <li>POST /api/v1/verification/documents – Submits identity documents for verification</li>
 * </ul>
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */
@RestController
@RequestMapping("/api/v1/verification")
public class VerificationController {

    private static final Logger logger = LoggerFactory.getLogger(VerificationController.class);

    private final UserAccountService userAccountService;
    private final VerificationDocumentService verificationDocumentService;

    /**
     * Builds the controller by injecting the necessary services.
     *
     * @param userAccountService          the service for user account management
     * @param verificationDocumentService the service for verification document management
     */
    public VerificationController(UserAccountService userAccountService,
                                  VerificationDocumentService verificationDocumentService) {
        this.userAccountService = userAccountService;
        this.verificationDocumentService = verificationDocumentService;
    }

    /**
     * Returns the verification status of the authenticated user.
     *
     * @param user the authenticated user
     * @return {@link VerificationStatusDTO} with the {@code verified} flag
     */
    @GetMapping("/status")
    public ResponseEntity<VerificationStatusDTO> getVerificationStatus(
            @AuthenticationPrincipal User user) {

        logger.info("GET /api/v1/verification/status - User: {}", user.getUsername());

        boolean isVerified = userAccountService.isUserVerified(user.getId());
        logger.info("Verification status for user {}: {}", user.getUsername(), isVerified);
        return ResponseEntity.ok(new VerificationStatusDTO(isVerified));
    }

    /**
     * Submits the user's identity documents to request verification.
     * <p>
     * Accepts the front and back of the document as multipart files.
     * The documents are saved and queued for review by administrators.
     * </p>
     *
     * @param frontDocument file containing the front of the identity document
     * @param backDocument  file containing the back of the identity document
     * @param user          the authenticated user
     * @return {@link VerificationDocumentsResponseDTO} with the submission outcome
     */
    @PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<VerificationDocumentsResponseDTO> submitVerificationDocuments(
            @RequestParam("frontDocument") MultipartFile frontDocument,
            @RequestParam("backDocument") MultipartFile backDocument,
            @AuthenticationPrincipal User user) {

        logger.info("POST /api/v1/verification/documents - User: {}", user.getUsername());

        VerificationDocumentsResponseDTO response = verificationDocumentService.submitDocuments(
                user.getId(), frontDocument, backDocument);

        return ResponseEntity.ok(response);
    }
}