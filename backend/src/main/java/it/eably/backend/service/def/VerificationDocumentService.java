package it.eably.backend.service.def;

import it.eably.backend.dto.common.response.VerificationDocumentsResponseDTO;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service interface for identity verification document submission.
 * <p>
 * Handles the collection and initial screening of KYC documents provided by users.
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */
public interface VerificationDocumentService {
    /**
     * Submits document images for background identity verification.
     *
     * @param userId        internal user account identifier
     * @param frontDocument image of the document front
     * @param backDocument  image of the document back
     * @return confirmation message summary
     */
    VerificationDocumentsResponseDTO submitDocuments(Long userId, MultipartFile frontDocument, MultipartFile backDocument);
}
