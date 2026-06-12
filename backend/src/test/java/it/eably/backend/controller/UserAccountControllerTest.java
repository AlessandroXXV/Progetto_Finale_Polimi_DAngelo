package it.eably.backend.controller;

import it.eably.backend.dto.user.response.UserResponseDTO;
import it.eably.backend.dto.user.request.UserUpdateDTO;
import it.eably.backend.exception.ConflictException;
import it.eably.backend.exception.ResourceNotFoundException;
import it.eably.backend.exception.ValidationException;
import it.eably.backend.model.User;
import it.eably.backend.model.UserRole;
import it.eably.backend.service.def.UserAccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserAccountController.
 */
@ExtendWith(MockitoExtension.class)
class UserAccountControllerTest {

    @Mock
    private UserAccountService userAccountService;

    @InjectMocks
    private UserAccountController userAccountController;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("testuser");
    }

    @Test
    void testUpdateProfile_Success() {
        UserUpdateDTO updateDTO = new UserUpdateDTO("newuser", null, null, "John Doe", null);

        User updatedUser = new User();
        updatedUser.setId(1L);
        updatedUser.setUsername("newuser");
        updatedUser.setEmail("test@test.com");
        updatedUser.setPasswordHash("hash");
        updatedUser.setRole(UserRole.CLIENT);
        updatedUser.setIsActive(true);
        updatedUser.setIsVerified(false);
        updatedUser.setFullName("John Doe");

        when(userAccountService.updateUserProfile(anyLong(), any(UserUpdateDTO.class)))
            .thenReturn(updatedUser);

        ResponseEntity<UserResponseDTO> response =
            userAccountController.updateProfile(updateDTO, mockUser);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("newuser", response.getBody().username());
        assertEquals("John Doe", response.getBody().fullName());

        verify(userAccountService).updateUserProfile(eq(1L), eq(updateDTO));
    }

    @Test
    void testUpdateProfile_UsernameTaken_ThrowsConflictException() {
        UserUpdateDTO updateDTO = new UserUpdateDTO("existinguser", null, null, null, null);

        when(userAccountService.updateUserProfile(anyLong(), any(UserUpdateDTO.class)))
            .thenThrow(new ConflictException("Username already taken"));

        assertThrows(ConflictException.class, () ->
            userAccountController.updateProfile(updateDTO, mockUser));
    }

    @Test
    void testUpdateProfile_InvalidEmail_ThrowsValidationException() {
        UserUpdateDTO updateDTO = new UserUpdateDTO(null, "invalidemail", null, null, null);

        when(userAccountService.updateUserProfile(anyLong(), any(UserUpdateDTO.class)))
            .thenThrow(new ValidationException("Invalid email format"));

        assertThrows(ValidationException.class, () ->
            userAccountController.updateProfile(updateDTO, mockUser));
    }

    @Test
    void testUploadProfileImage_Success() {
        MockMultipartFile imageFile = new MockMultipartFile(
            "image", "test.jpg", "image/jpeg", "fake-image-data".getBytes()
        );

        doNothing().when(userAccountService).uploadProfileImage(anyLong(), any(MultipartFile.class));

        ResponseEntity<Void> response =
            userAccountController.uploadProfileImage(imageFile, mockUser);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(userAccountService).uploadProfileImage(eq(1L), eq(imageFile));
    }

    @Test
    void testUploadProfileImage_InvalidFormat_ThrowsValidationException() {
        MockMultipartFile imageFile = new MockMultipartFile(
            "image", "test.gif", "image/gif", "fake-image-data".getBytes()
        );

        doThrow(new ValidationException("Invalid image format"))
            .when(userAccountService).uploadProfileImage(anyLong(), any(MultipartFile.class));

        assertThrows(ValidationException.class, () ->
            userAccountController.uploadProfileImage(imageFile, mockUser));
    }

    @Test
    void testGetProfileImage_Success() {
        byte[] imageBytes = "fake-image-data".getBytes();
        UserAccountService.ProfileImageData imageData =
            new UserAccountService.ProfileImageData(imageBytes, "image/jpeg");

        when(userAccountService.getProfileImageData(1L)).thenReturn(imageData);

        ResponseEntity<byte[]> response = userAccountController.getProfileImage(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertArrayEquals(imageBytes, response.getBody());
        assertEquals("image/jpeg", response.getHeaders().getContentType().toString());
        assertEquals("no-cache, no-store, must-revalidate", response.getHeaders().getCacheControl());

        verify(userAccountService).getProfileImageData(1L);
    }

    @Test
    void testGetProfileImage_NotFound_ThrowsResourceNotFoundException() {
        when(userAccountService.getProfileImageData(1L))
            .thenThrow(new ResourceNotFoundException("User has no profile image"));

        assertThrows(ResourceNotFoundException.class, () ->
            userAccountController.getProfileImage(1L));
    }

    @Test
    void testDeleteProfileImage_Success() {
        doNothing().when(userAccountService).deleteProfileImage(anyLong());

        ResponseEntity<Void> response =
            userAccountController.deleteProfileImage(mockUser);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(userAccountService).deleteProfileImage(eq(1L));
    }

    @Test
    void testDeleteProfileImage_NotFound_ThrowsResourceNotFoundException() {
        doThrow(new ResourceNotFoundException("User has no profile image"))
            .when(userAccountService).deleteProfileImage(anyLong());

        assertThrows(ResourceNotFoundException.class, () ->
            userAccountController.deleteProfileImage(mockUser));
    }

}
