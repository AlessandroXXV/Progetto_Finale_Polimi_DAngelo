package it.eably.backend.controller;

import it.eably.backend.dto.common.response.VerificationDocumentsResponseDTO;
import it.eably.backend.dto.common.response.VerificationStatusDTO;
import it.eably.backend.exception.ValidationException;
import it.eably.backend.model.User;
import it.eably.backend.service.def.UserAccountService;
import it.eably.backend.service.def.VerificationDocumentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VerificationControllerTest {

    @Mock
    private UserAccountService userAccountService;

    @Mock
    private VerificationDocumentService verificationDocumentService;

    @InjectMocks
    private VerificationController verificationController;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("testuser");
    }

    @Test
    void getVerificationStatus_Verified() {
        when(userAccountService.isUserVerified(1L)).thenReturn(true);

        ResponseEntity<VerificationStatusDTO> response =
                verificationController.getVerificationStatus(mockUser);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().verified());
        verify(userAccountService).isUserVerified(1L);
    }

    @Test
    void getVerificationStatus_NotVerified() {
        when(userAccountService.isUserVerified(1L)).thenReturn(false);

        ResponseEntity<VerificationStatusDTO> response =
                verificationController.getVerificationStatus(mockUser);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().verified());
        verify(userAccountService).isUserVerified(1L);
    }

    @Test
    void submitVerificationDocuments_Success() {
        MockMultipartFile front = new MockMultipartFile(
                "frontDocument", "front.pdf", "application/pdf", "front-bytes".getBytes()
        );
        MockMultipartFile back = new MockMultipartFile(
                "backDocument", "back.pdf", "application/pdf", "back-bytes".getBytes()
        );
        VerificationDocumentsResponseDTO serviceResponse = new VerificationDocumentsResponseDTO(
                "Documenti inviati con successo. Riceverai conferma via email."
        );

        when(verificationDocumentService.submitDocuments(1L, front, back)).thenReturn(serviceResponse);

        ResponseEntity<VerificationDocumentsResponseDTO> response =
                verificationController.submitVerificationDocuments(front, back, mockUser);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(serviceResponse.message(), response.getBody().message());
        verify(verificationDocumentService).submitDocuments(1L, front, back);
    }

    @Test
    void submitVerificationDocuments_ServiceValidationError_ThrowsException() {
        MockMultipartFile front = new MockMultipartFile(
                "frontDocument", "front.txt", "text/plain", "invalid".getBytes()
        );
        MockMultipartFile back = new MockMultipartFile(
                "backDocument", "back.pdf", "application/pdf", "back-bytes".getBytes()
        );

        when(verificationDocumentService.submitDocuments(anyLong(), any(), any()))
                .thenThrow(new ValidationException("frontDocument must be a PDF file"));

        assertThrows(ValidationException.class, () ->
                verificationController.submitVerificationDocuments(front, back, mockUser));
    }
}
