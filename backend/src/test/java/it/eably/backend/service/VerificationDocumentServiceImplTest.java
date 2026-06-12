package it.eably.backend.service;

import it.eably.backend.dto.common.response.VerificationDocumentsResponseDTO;
import it.eably.backend.exception.ResourceNotFoundException;
import it.eably.backend.exception.ValidationException;
import it.eably.backend.model.User;
import it.eably.backend.repository.UserRepository;
import it.eably.backend.service.impl.VerificationDocumentServiceImpl;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VerificationDocumentServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private UserRepository userRepository;

    private VerificationDocumentServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new VerificationDocumentServiceImpl(
                mailSender,
                userRepository,
                "docs@eably.it",
                "noreply@eably.it"
        );
    }

    @Test
    void submitDocuments_Success() {
        User user = new User();
        user.setId(1L);
        user.setUsername("verified-user");
        user.setFullName("Verified User");
        user.setEmail("verified@test.com");

        MockMultipartFile front = new MockMultipartFile(
                "frontDocument", "front.pdf", "application/pdf", "front".getBytes()
        );
        MockMultipartFile back = new MockMultipartFile(
                "backDocument", "back.pdf", "application/pdf", "back".getBytes()
        );

        MimeMessage message = new MimeMessage((jakarta.mail.Session) null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(mailSender.createMimeMessage()).thenReturn(message);

        VerificationDocumentsResponseDTO response = service.submitDocuments(1L, front, back);

        assertNotNull(response.message());
        verify(mailSender).send(message);
    }

    @Test
    void submitDocuments_UserNotFound_ThrowsResourceNotFoundException() {
        when(userRepository.findById(55L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                service.submitDocuments(55L, mock(MultipartFile.class), mock(MultipartFile.class)));
    }

    @Test
    void submitDocuments_InvalidContentType_ThrowsValidationException() {
        User user = new User();
        user.setId(2L);
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));

        MockMultipartFile invalid = new MockMultipartFile(
                "frontDocument", "front.txt", "text/plain", "front".getBytes()
        );
        MockMultipartFile back = new MockMultipartFile(
                "backDocument", "back.pdf", "application/pdf", "back".getBytes()
        );

        ValidationException ex = assertThrows(ValidationException.class,
                () -> service.submitDocuments(2L, invalid, back));
        assertEquals("frontDocument must be a PDF file", ex.getMessage());
    }

    @Test
    void submitDocuments_FileTooLarge_ThrowsValidationException() {
        User user = new User();
        user.setId(4L);
        when(userRepository.findById(4L)).thenReturn(Optional.of(user));

        byte[] oversized = new byte[4 * 1024 * 1024]; // 4MB > 3MB limit
        MockMultipartFile front = new MockMultipartFile(
                "frontDocument", "front.pdf", "application/pdf", oversized
        );
        MockMultipartFile back = new MockMultipartFile(
                "backDocument", "back.pdf", "application/pdf", "back".getBytes()
        );

        ValidationException ex = assertThrows(ValidationException.class,
                () -> service.submitDocuments(4L, front, back));
        assertEquals("frontDocument exceeds maximum size of 3MB", ex.getMessage());
    }

    @Test
    void submitDocuments_AttachmentReadFailure_ThrowsValidationException() throws Exception {
        User user = new User();
        user.setId(3L);
        user.setUsername("io-user");
        user.setEmail("io@test.com");

        MultipartFile broken = mock(MultipartFile.class);
        MultipartFile back = mock(MultipartFile.class);

        when(userRepository.findById(3L)).thenReturn(Optional.of(user));
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((jakarta.mail.Session) null));
        when(broken.isEmpty()).thenReturn(false);
        when(back.isEmpty()).thenReturn(false);
        when(broken.getContentType()).thenReturn("application/pdf");
        when(back.getContentType()).thenReturn("application/pdf");
        when(broken.getOriginalFilename()).thenReturn("front.pdf");
        when(broken.getBytes()).thenThrow(new IOException("disk-error"));

        ValidationException ex = assertThrows(ValidationException.class,
                () -> service.submitDocuments(3L, broken, back));
        assertTrue(ex.getMessage().contains("Unable to send verification documents"));
    }
}
