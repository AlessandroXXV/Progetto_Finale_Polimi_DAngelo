package it.eably.backend.service;

import it.eably.backend.dto.user.request.UserUpdateDTO;
import it.eably.backend.exception.ConflictException;
import it.eably.backend.exception.ResourceNotFoundException;
import it.eably.backend.exception.ValidationException;
import it.eably.backend.model.User;
import it.eably.backend.model.UserRole;
import it.eably.backend.repository.UserRepository;
import it.eably.backend.service.def.UserAccountService;
import it.eably.backend.service.impl.UserAccountServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserAccountService.
 * 
 * Tests cover:
 * - Partial profile updates
 * - Username/email uniqueness validation
 * - Password hashing
 * - Profile image upload/download/deletion
 * - Verification status checks
 * 
 * Target: 100% line and branch coverage.
 */
@ExtendWith(MockitoExtension.class)
class UserAccountServiceTest {
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @InjectMocks
    private UserAccountServiceImpl userAccountService;
    
    private User user;
    
    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPasswordHash("$2a$12$hashedpassword");
        user.setRole(UserRole.CLIENT);
        user.setIsActive(true);
        user.setIsVerified(false);
    }
    
    // ========== UPDATE USER PROFILE TESTS ==========
    
    @Test
    void testUpdateUserProfile_UpdateUsername_Success() {
        UserUpdateDTO updateDTO = new UserUpdateDTO("newuser", null, null, null, null);
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByUsernameAndIdNot("newuser", 1L)).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(user);
        
        User result = userAccountService.updateUserProfile(1L, updateDTO);
        
        assertNotNull(result);
        assertEquals("newuser", result.getUsername());
        
        verify(userRepository).save(any(User.class));
    }
    
    @Test
    void testUpdateUserProfile_UpdateEmail_Success() {
        UserUpdateDTO updateDTO = new UserUpdateDTO(null, "new@test.com", null, null, null);
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailAndIdNot("new@test.com", 1L)).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(user);
        
        User result = userAccountService.updateUserProfile(1L, updateDTO);
        
        assertNotNull(result);
        assertEquals("new@test.com", result.getEmail());
        
        verify(userRepository).save(any(User.class));
    }
    
    @Test
    void testUpdateUserProfile_UpdatePassword_Success() {
        UserUpdateDTO updateDTO = new UserUpdateDTO(null, null, "newpassword", null, null);
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newpassword")).thenReturn("$2a$12$newhash");
        when(userRepository.save(any(User.class))).thenReturn(user);
        
        User result = userAccountService.updateUserProfile(1L, updateDTO);
        
        assertNotNull(result);
        assertEquals("$2a$12$newhash", result.getPasswordHash());
        
        verify(passwordEncoder).encode("newpassword");
        verify(userRepository).save(any(User.class));
    }
    
    @Test
    void testUpdateUserProfile_UpdateFullName_Success() {
        UserUpdateDTO updateDTO = new UserUpdateDTO(null, null, null, "John Doe", null);
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        
        User result = userAccountService.updateUserProfile(1L, updateDTO);
        
        assertNotNull(result);
        assertEquals("John Doe", result.getFullName());
        
        verify(userRepository).save(any(User.class));
    }
    
    @Test
    void testUpdateUserProfile_UpdateGender_Success() {
        UserUpdateDTO updateDTO = new UserUpdateDTO(null, null, null, null, "Male");
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        
        User result = userAccountService.updateUserProfile(1L, updateDTO);
        
        assertNotNull(result);
        assertEquals("Male", result.getGender());
        
        verify(userRepository).save(any(User.class));
    }
    
    @Test
    void testUpdateUserProfile_AllFieldsNull_Success() {
        UserUpdateDTO updateDTO = new UserUpdateDTO(null, null, null, null, null);
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        
        User result = userAccountService.updateUserProfile(1L, updateDTO);
        
        assertNotNull(result);
        
        verify(userRepository).save(any(User.class));
    }
    
    @Test
    void testUpdateUserProfile_UserNotFound_ThrowsException() {
        UserUpdateDTO updateDTO = new UserUpdateDTO("newuser", null, null, null, null);
        
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        
        assertThrows(ResourceNotFoundException.class, () -> {
            userAccountService.updateUserProfile(1L, updateDTO);
        });
        
        verify(userRepository, never()).save(any());
    }
    
    @Test
    void testUpdateUserProfile_UsernameTaken_ThrowsConflictException() {
        UserUpdateDTO updateDTO = new UserUpdateDTO("existinguser", null, null, null, null);
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByUsernameAndIdNot("existinguser", 1L)).thenReturn(true);
        
        assertThrows(ConflictException.class, () -> {
            userAccountService.updateUserProfile(1L, updateDTO);
        });
        
        verify(userRepository, never()).save(any());
    }
    
    @Test
    void testUpdateUserProfile_EmailTaken_ThrowsConflictException() {
        UserUpdateDTO updateDTO = new UserUpdateDTO(null, "existing@test.com", null, null, null);
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailAndIdNot("existing@test.com", 1L)).thenReturn(true);
        
        assertThrows(ConflictException.class, () -> {
            userAccountService.updateUserProfile(1L, updateDTO);
        });
        
        verify(userRepository, never()).save(any());
    }
    
    @Test
    void testUpdateUserProfile_InvalidEmailFormat_ThrowsValidationException() {
        UserUpdateDTO updateDTO = new UserUpdateDTO(null, "invalidemail", null, null, null);
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        
        assertThrows(ValidationException.class, () -> {
            userAccountService.updateUserProfile(1L, updateDTO);
        });
        
        verify(userRepository, never()).save(any());
    }
    
    @Test
    void testUpdateUserProfile_EmptyUsername_SkipsUpdate() {
        UserUpdateDTO updateDTO = new UserUpdateDTO("   ", null, null, null, null);
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        
        User result = userAccountService.updateUserProfile(1L, updateDTO);
        
        assertNotNull(result);
        assertEquals("testuser", result.getUsername()); // Original username unchanged
        
        verify(userRepository, never()).existsByUsernameAndIdNot(anyString(), anyLong());
    }
    
    @Test
    void testUpdateUserProfile_EmptyEmail_SkipsUpdate() {
        UserUpdateDTO updateDTO = new UserUpdateDTO(null, "   ", null, null, null);
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        
        User result = userAccountService.updateUserProfile(1L, updateDTO);
        
        assertNotNull(result);
        assertEquals("test@example.com", result.getEmail()); // Original email unchanged
        
        verify(userRepository, never()).existsByEmailAndIdNot(anyString(), anyLong());
    }
    
    @Test
    void testUpdateUserProfile_EmptyPassword_SkipsUpdate() {
        UserUpdateDTO updateDTO = new UserUpdateDTO(null, null, "   ", null, null);
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        
        User result = userAccountService.updateUserProfile(1L, updateDTO);
        
        assertNotNull(result);
        assertEquals("$2a$12$hashedpassword", result.getPasswordHash()); // Original hash unchanged
        
        verify(passwordEncoder, never()).encode(anyString());
    }
    
    // ========== UPLOAD PROFILE IMAGE TESTS ==========
    
    @Test
    void testUploadProfileImage_JPEG_Success() {
        byte[] imageBytes = "fake-image-data".getBytes();
        MultipartFile imageFile = new MockMultipartFile(
            "image", "test.jpg", "image/jpeg", imageBytes
        );
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        
        userAccountService.uploadProfileImage(1L, imageFile);
        
        assertNotNull(user.getProfileImageData());
        assertTrue(user.getProfileImageData().startsWith("data:image/jpeg;base64,"));
        
        verify(userRepository).save(user);
    }
    
    @Test
    void testUploadProfileImage_PNG_Success() {
        byte[] imageBytes = "fake-image-data".getBytes();
        MultipartFile imageFile = new MockMultipartFile(
            "image", "test.png", "image/png", imageBytes
        );
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        
        userAccountService.uploadProfileImage(1L, imageFile);
        
        assertNotNull(user.getProfileImageData());
        assertTrue(user.getProfileImageData().startsWith("data:image/png;base64,"));
        
        verify(userRepository).save(user);
    }
    
    @Test
    void testUploadProfileImage_WEBP_Success() {
        byte[] imageBytes = "fake-image-data".getBytes();
        MultipartFile imageFile = new MockMultipartFile(
            "image", "test.webp", "image/webp", imageBytes
        );
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        
        userAccountService.uploadProfileImage(1L, imageFile);
        
        assertNotNull(user.getProfileImageData());
        assertTrue(user.getProfileImageData().startsWith("data:image/webp;base64,"));
        
        verify(userRepository).save(user);
    }
    
    @Test
    void testUploadProfileImage_InvalidFormat_ThrowsValidationException() {
        byte[] imageBytes = "fake-image-data".getBytes();
        MultipartFile imageFile = new MockMultipartFile(
            "image", "test.gif", "image/gif", imageBytes
        );
        
        assertThrows(ValidationException.class, () -> {
            userAccountService.uploadProfileImage(1L, imageFile);
        });
        
        verify(userRepository, never()).save(any());
    }
    
    @Test
    void testUploadProfileImage_NullContentType_ThrowsValidationException() {
        byte[] imageBytes = "fake-image-data".getBytes();
        MultipartFile imageFile = new MockMultipartFile(
            "image", "test.jpg", null, imageBytes
        );
        
        assertThrows(ValidationException.class, () -> {
            userAccountService.uploadProfileImage(1L, imageFile);
        });
        
        verify(userRepository, never()).save(any());
    }
    
    @Test
    void testUploadProfileImage_SizeExceeded_ThrowsValidationException() {
        byte[] imageBytes = new byte[6 * 1024 * 1024]; // 6MB
        MultipartFile imageFile = new MockMultipartFile(
            "image", "test.jpg", "image/jpeg", imageBytes
        );
        
        assertThrows(ValidationException.class, () -> {
            userAccountService.uploadProfileImage(1L, imageFile);
        });
        
        verify(userRepository, never()).save(any());
    }
    
    @Test
    void testUploadProfileImage_UserNotFound_ThrowsException() {
        byte[] imageBytes = "fake-image-data".getBytes();
        MultipartFile imageFile = new MockMultipartFile(
            "image", "test.jpg", "image/jpeg", imageBytes
        );
        
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        
        assertThrows(ResourceNotFoundException.class, () -> {
            userAccountService.uploadProfileImage(1L, imageFile);
        });
        
        verify(userRepository, never()).save(any());
    }
    
    // ========== GET PROFILE IMAGE DATA TESTS ==========

    @Test
    void testGetProfileImageData_JPEG_Success() {
        String base64Data = "ZmFrZS1pbWFnZS1kYXRh"; // "fake-image-data" in Base64
        user.setProfileImageData("data:image/jpeg;base64," + base64Data);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserAccountService.ProfileImageData result = userAccountService.getProfileImageData(1L);

        assertNotNull(result);
        assertTrue(result.imageBytes().length > 0);
        assertEquals("image/jpeg", result.contentType());
    }

    @Test
    void testGetProfileImageData_PNG_Success() {
        String base64Data = "ZmFrZS1pbWFnZS1kYXRh";
        user.setProfileImageData("data:image/png;base64," + base64Data);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserAccountService.ProfileImageData result = userAccountService.getProfileImageData(1L);

        assertNotNull(result);
        assertTrue(result.imageBytes().length > 0);
        assertEquals("image/png", result.contentType());
    }

    @Test
    void testGetProfileImageData_WithoutDataURIPrefix_DefaultsToJPEG() {
        user.setProfileImageData("ZmFrZS1pbWFnZS1kYXRh");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserAccountService.ProfileImageData result = userAccountService.getProfileImageData(1L);

        assertNotNull(result);
        assertTrue(result.imageBytes().length > 0);
        assertEquals("image/jpeg", result.contentType());
    }

    @Test
    void testGetProfileImageData_UserNotFound_ThrowsException() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userAccountService.getProfileImageData(1L));
    }

    @Test
    void testGetProfileImageData_NoImage_ThrowsException() {
        user.setProfileImageData(null);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThrows(ResourceNotFoundException.class, () -> userAccountService.getProfileImageData(1L));
    }

    @Test
    void testGetProfileImageData_EmptyImage_ThrowsException() {
        user.setProfileImageData("   ");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThrows(ResourceNotFoundException.class, () -> userAccountService.getProfileImageData(1L));
    }
    
    // ========== DELETE PROFILE IMAGE TESTS ==========
    
    @Test
    void testDeleteProfileImage_Success() {
        user.setProfileImageData("data:image/jpeg;base64,abc");
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        
        userAccountService.deleteProfileImage(1L);
        
        assertNull(user.getProfileImageData());
        
        verify(userRepository).save(user);
    }
    
    @Test
    void testDeleteProfileImage_UserNotFound_ThrowsException() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        
        assertThrows(ResourceNotFoundException.class, () -> {
            userAccountService.deleteProfileImage(1L);
        });
        
        verify(userRepository, never()).save(any());
    }
    
    @Test
    void testDeleteProfileImage_NoImage_ThrowsException() {
        user.setProfileImageData(null);
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        
        assertThrows(ResourceNotFoundException.class, () -> {
            userAccountService.deleteProfileImage(1L);
        });
        
        verify(userRepository, never()).save(any());
    }
    
    @Test
    void testDeleteProfileImage_EmptyImage_ThrowsException() {
        user.setProfileImageData("   ");
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        
        assertThrows(ResourceNotFoundException.class, () -> {
            userAccountService.deleteProfileImage(1L);
        });
        
        verify(userRepository, never()).save(any());
    }
    
    // ========== IS USER VERIFIED TESTS ==========
    
    @Test
    void testIsUserVerified_True() {
        user.setIsVerified(true);
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        
        boolean result = userAccountService.isUserVerified(1L);
        
        assertTrue(result);
    }
    
    @Test
    void testIsUserVerified_False() {
        user.setIsVerified(false);
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        
        boolean result = userAccountService.isUserVerified(1L);
        
        assertFalse(result);
    }
    
    @Test
    void testIsUserVerified_Null_ReturnsFalse() {
        user.setIsVerified(null);
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        
        boolean result = userAccountService.isUserVerified(1L);
        
        assertFalse(result);
    }
    
    @Test
    void testIsUserVerified_UserNotFound_ThrowsException() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        
        assertThrows(ResourceNotFoundException.class, () -> {
            userAccountService.isUserVerified(1L);
        });
    }
}
