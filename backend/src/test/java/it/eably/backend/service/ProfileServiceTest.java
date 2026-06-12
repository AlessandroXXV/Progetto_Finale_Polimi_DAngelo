package it.eably.backend.service;

import it.eably.backend.dto.profile.request.ProfileRequestDTO;
import it.eably.backend.exception.ResourceNotFoundException;
import it.eably.backend.exception.ValidationException;
import it.eably.backend.model.DeliveryMode;
import it.eably.backend.model.Profile;
import it.eably.backend.model.SlotStatus;
import it.eably.backend.model.User;
import it.eably.backend.model.UserRole;
import it.eably.backend.repository.ProfileRepository;
import it.eably.backend.repository.UserRepository;
import it.eably.backend.service.def.ProfileService;
import it.eably.backend.service.impl.ProfileServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ProfileService.
 *
 * Tests cover:
 * - Profile creation with validation and multi-profile limit (max 5)
 * - Profile updates (by profileId)
 * - Profile retrieval
 * - Profile listing
 * - Soft delete and reactivation
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProfileServiceImpl profileService;

    private User user;
    private Profile profile;
    private ProfileRequestDTO profileRequest;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPasswordHash("$2a$12$hashedpassword");
        user.setRole(UserRole.STUDENT);
        user.setIsActive(true);
        user.setIsVerified(true);
        user.setStripeConnected(true);

        profile = new Profile();
        profile.setId(1L);
        profile.setUser(user);
        profile.setTitle("Math Tutor");
        profile.setDescription("Experienced math tutor");
        profile.setHourlyRate(new BigDecimal("25.00"));
        profile.setDeliveryMode(DeliveryMode.ONLINE);
        profile.setAddress(null);
        profile.setIsActive(true);

        profileRequest = new ProfileRequestDTO(
                "Math Tutor",
                "Experienced math tutor",
                new BigDecimal("25.00"),
                DeliveryMode.ONLINE,
                null
        );
    }

    // ========== CREATE PROFILE TESTS ==========

    @Test
    void createProfile_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(profileRepository.findAllByUserId(1L)).thenReturn(Collections.emptyList());
        when(profileRepository.countByUserIdAndIsActiveTrue(1L)).thenReturn(0L);
        when(profileRepository.save(any(Profile.class))).thenReturn(profile);

        Profile result = profileService.createProfile(1L, profileRequest);

        assertNotNull(result);
        assertEquals("Math Tutor", result.getTitle());
        assertEquals(new BigDecimal("25.00"), result.getHourlyRate());
        assertEquals(DeliveryMode.ONLINE, result.getDeliveryMode());
        assertTrue(result.getIsActive());

        verify(userRepository).findById(1L);
        verify(profileRepository).countByUserIdAndIsActiveTrue(1L);
        verify(profileRepository).save(any(Profile.class));
    }

    @Test
    void createProfile_UserNotFound_ThrowsException() {
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> profileService.createProfile(1L, profileRequest));

        assertEquals("User not found with ID: 1", exception.getMessage());
        verify(profileRepository, never()).save(any(Profile.class));
    }

    @Test
    void createProfile_StripeNotConnected_ThrowsException() {
        user.setStripeConnected(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        ValidationException exception = assertThrows(ValidationException.class,
                () -> profileService.createProfile(1L, profileRequest));

        assertTrue(exception.getMessage().contains("Complete Stripe onboarding"));
        verify(profileRepository, never()).save(any(Profile.class));
    }

    @Test
    void createProfile_MaxProfilesReached_ThrowsException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(profileRepository.findAllByUserId(1L)).thenReturn(Collections.emptyList());
        when(profileRepository.countByUserIdAndIsActiveTrue(1L)).thenReturn((long) ProfileService.MAX_PROFILES_PER_USER);

        ValidationException exception = assertThrows(ValidationException.class,
                () -> profileService.createProfile(1L, profileRequest));

        assertTrue(exception.getMessage().contains("Maximum number of profiles reached"));
        verify(profileRepository, never()).save(any(Profile.class));
    }

    @Test
    void createProfile_InPersonMode_RequiresAddress() {
        ProfileRequestDTO inPersonRequest = new ProfileRequestDTO(
                "Math Tutor", "desc", new BigDecimal("25.00"), DeliveryMode.IN_PERSON, null);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(profileRepository.findAllByUserId(1L)).thenReturn(Collections.emptyList());
        when(profileRepository.countByUserIdAndIsActiveTrue(1L)).thenReturn(0L);

        ValidationException exception = assertThrows(ValidationException.class,
                () -> profileService.createProfile(1L, inPersonRequest));

        assertTrue(exception.getMessage().contains("Address is required"));
        verify(profileRepository, never()).save(any(Profile.class));
    }

    @Test
    void createProfile_HybridMode_RequiresAddress() {
        ProfileRequestDTO hybridRequest = new ProfileRequestDTO(
                "Math Tutor", "desc", new BigDecimal("25.00"), DeliveryMode.HYBRID, null);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(profileRepository.findAllByUserId(1L)).thenReturn(Collections.emptyList());
        when(profileRepository.countByUserIdAndIsActiveTrue(1L)).thenReturn(0L);

        ValidationException exception = assertThrows(ValidationException.class,
                () -> profileService.createProfile(1L, hybridRequest));

        assertTrue(exception.getMessage().contains("Address is required"));
        verify(profileRepository, never()).save(any(Profile.class));
    }

    @Test
    void createProfile_OnlineMode_AddressNotRequired() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(profileRepository.findAllByUserId(1L)).thenReturn(Collections.emptyList());
        when(profileRepository.countByUserIdAndIsActiveTrue(1L)).thenReturn(0L);
        when(profileRepository.save(any(Profile.class))).thenReturn(profile);

        Profile result = profileService.createProfile(1L, profileRequest);

        assertNotNull(result);
        assertNull(result.getAddress());
        verify(profileRepository).save(any(Profile.class));
    }

    @Test
    void createProfile_WithAddress_Success() {
        ProfileRequestDTO requestWithAddress = new ProfileRequestDTO(
                "Math Tutor", "desc", new BigDecimal("25.00"), DeliveryMode.IN_PERSON, "123 Main St");

        Profile profileWithAddress = new Profile();
        profileWithAddress.setId(1L);
        profileWithAddress.setUser(user);
        profileWithAddress.setTitle("Math Tutor");
        profileWithAddress.setHourlyRate(new BigDecimal("25.00"));
        profileWithAddress.setDeliveryMode(DeliveryMode.IN_PERSON);
        profileWithAddress.setAddress("123 Main St");
        profileWithAddress.setIsActive(true);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(profileRepository.findAllByUserId(1L)).thenReturn(Collections.emptyList());
        when(profileRepository.countByUserIdAndIsActiveTrue(1L)).thenReturn(0L);
        when(profileRepository.save(any(Profile.class))).thenReturn(profileWithAddress);

        Profile result = profileService.createProfile(1L, requestWithAddress);

        assertNotNull(result);
        assertEquals("123 Main St", result.getAddress());
        verify(profileRepository).save(any(Profile.class));
    }

    // ========== UPDATE PROFILE TESTS ==========

    @Test
    void updateProfile_Success() {
        ProfileRequestDTO updateRequest = new ProfileRequestDTO(
                "Senior Math Tutor", "Expert", new BigDecimal("35.00"), DeliveryMode.HYBRID, "456 Oak Ave");

        Profile updatedProfile = new Profile();
        updatedProfile.setId(1L);
        updatedProfile.setUser(user);
        updatedProfile.setTitle("Senior Math Tutor");
        updatedProfile.setDescription("Expert");
        updatedProfile.setHourlyRate(new BigDecimal("35.00"));
        updatedProfile.setDeliveryMode(DeliveryMode.HYBRID);
        updatedProfile.setAddress("456 Oak Ave");
        updatedProfile.setIsActive(true);

        when(profileRepository.findById(1L)).thenReturn(Optional.of(profile));
        when(profileRepository.save(any(Profile.class))).thenReturn(updatedProfile);

        Profile result = profileService.updateProfile(1L, 1L, updateRequest);

        assertNotNull(result);
        assertEquals("Senior Math Tutor", result.getTitle());
        assertEquals(new BigDecimal("35.00"), result.getHourlyRate());
        assertEquals("456 Oak Ave", result.getAddress());

        verify(profileRepository).findById(1L);
        verify(profileRepository).save(any(Profile.class));
    }

    @Test
    void updateProfile_ProfileNotFound_ThrowsException() {
        when(profileRepository.findById(anyLong())).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> profileService.updateProfile(1L, 1L, profileRequest));

        assertEquals("Profile not found with ID: 1", exception.getMessage());
        verify(profileRepository, never()).save(any(Profile.class));
    }

    @Test
    void updateProfile_NotOwner_ThrowsException() {
        User otherUser = new User();
        otherUser.setId(99L);
        profile.setUser(otherUser);

        when(profileRepository.findById(1L)).thenReturn(Optional.of(profile));

        ValidationException exception = assertThrows(ValidationException.class,
                () -> profileService.updateProfile(1L, 1L, profileRequest));

        assertTrue(exception.getMessage().contains("not authorized"));
        verify(profileRepository, never()).save(any(Profile.class));
    }

    @Test
    void updateProfile_ChangingToInPersonMode_RequiresAddress() {
        ProfileRequestDTO updateRequest = new ProfileRequestDTO(
                "Math Tutor", "desc", new BigDecimal("25.00"), DeliveryMode.IN_PERSON, null);

        when(profileRepository.findById(1L)).thenReturn(Optional.of(profile));

        ValidationException exception = assertThrows(ValidationException.class,
                () -> profileService.updateProfile(1L, 1L, updateRequest));

        assertTrue(exception.getMessage().contains("Address is required"));
        verify(profileRepository, never()).save(any(Profile.class));
    }

    // ========== GET PROFILE TESTS ==========

    @Test
    void getProfileById_Success() {
        when(profileRepository.findById(1L)).thenReturn(Optional.of(profile));

        Profile result = profileService.getProfileById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Math Tutor", result.getTitle());
        verify(profileRepository).findById(1L);
    }

    @Test
    void getProfileById_NotFound_ThrowsException() {
        when(profileRepository.findById(anyLong())).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> profileService.getProfileById(1L));

        assertEquals("Profile not found with ID: 1", exception.getMessage());
    }

    @Test
    void getProfilesByUserId_Success() {
        when(profileRepository.findAllByUserId(1L)).thenReturn(List.of(profile));

        List<Profile> result = profileService.getProfilesByUserId(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getUser().getId());
        verify(profileRepository).findAllByUserId(1L);
    }

    @Test
    void getProfilesByUserId_Empty() {
        when(profileRepository.findAllByUserId(anyLong())).thenReturn(Collections.emptyList());

        List<Profile> result = profileService.getProfilesByUserId(1L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ========== LIST PROFILES TESTS ==========

    @Test
    void getAllActiveProfiles_Success() {
        Profile profile2 = new Profile();
        profile2.setId(2L);
        profile2.setUser(user);
        profile2.setTitle("Physics Tutor");
        profile2.setIsActive(true);

        when(profileRepository.findByIsActiveTrue()).thenReturn(Arrays.asList(profile, profile2));

        List<Profile> result = profileService.getAllActiveProfiles();

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(profileRepository).findByIsActiveTrue();
    }

    @Test
    void getAllActiveProfiles_EmptyList() {
        when(profileRepository.findByIsActiveTrue()).thenReturn(Collections.emptyList());

        List<Profile> result = profileService.getAllActiveProfiles();

        assertTrue(result.isEmpty());
        verify(profileRepository).findByIsActiveTrue();
    }

    @Test
    void searchActiveProfiles_WithAvailabilityAndRates_UsesRepositoryFlags() {
        when(profileRepository.searchActiveProfiles(
                eq(true), eq("math"),
                eq(true), eq(new BigDecimal("50.00")),
                eq(true),
                eq(true), eq(DayOfWeek.MONDAY),
                eq(true), eq(LocalTime.parse("10:00")),
                eq(true), eq(LocalTime.parse("12:00")),
                eq(SlotStatus.AVAILABLE)
        )).thenReturn(List.of(profile));

        List<Profile> result = profileService.searchActiveProfiles("math", new BigDecimal("50.00"), 0, "10:00", "12:00");

        assertEquals(1, result.size());
        verify(profileRepository).searchActiveProfiles(
                eq(true), eq("math"),
                eq(true), eq(new BigDecimal("50.00")),
                eq(true),
                eq(true), eq(DayOfWeek.MONDAY),
                eq(true), eq(LocalTime.parse("10:00")),
                eq(true), eq(LocalTime.parse("12:00")),
                eq(SlotStatus.AVAILABLE)
        );
    }

    @Test
    void searchActiveProfiles_OnlyCategory_DisablesAvailabilityFlags() {
        when(profileRepository.searchActiveProfiles(
                eq(true), eq("physics"),
                eq(false), isNull(),
                eq(false),
                eq(false), isNull(),
                eq(false), isNull(),
                eq(false), isNull(),
                eq(SlotStatus.AVAILABLE)
        )).thenReturn(List.of(profile));

        List<Profile> result = profileService.searchActiveProfiles("physics", null, null, null, null);

        assertEquals(1, result.size());
        verify(profileRepository).searchActiveProfiles(
                eq(true), eq("physics"),
                eq(false), isNull(),
                eq(false),
                eq(false), isNull(),
                eq(false), isNull(),
                eq(false), isNull(),
                eq(SlotStatus.AVAILABLE)
        );
    }

    @Test
    void searchActiveProfiles_InvalidTimeRange_ThrowsValidationException() {
        ValidationException exception = assertThrows(ValidationException.class,
                () -> profileService.searchActiveProfiles("math", new BigDecimal("20.00"),
                        null, "12:00", "10:00"));

        assertTrue(exception.getMessage().contains("endTime must be after startTime"));
        verify(profileRepository, never()).searchActiveProfiles(
                anyBoolean(), any(), anyBoolean(), any(), anyBoolean(), anyBoolean(),
                any(), anyBoolean(), any(), anyBoolean(), any(), any());
    }

    // ========== SOFT DELETE TESTS ==========

    @Test
    void deleteProfile_Success() {
        when(profileRepository.findById(1L)).thenReturn(Optional.of(profile));
        when(profileRepository.save(any(Profile.class))).thenReturn(profile);

        profileService.deleteProfile(1L, 1L);

        verify(profileRepository).findById(1L);
        verify(profileRepository).save(any(Profile.class));
    }

    @Test
    void deleteProfile_ProfileNotFound_ThrowsException() {
        when(profileRepository.findById(anyLong())).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> profileService.deleteProfile(1L, 1L));

        assertEquals("Profile not found with ID: 1", exception.getMessage());
        verify(profileRepository, never()).save(any(Profile.class));
    }

    @Test
    void deleteProfile_NotOwner_ThrowsException() {
        User otherUser = new User();
        otherUser.setId(99L);
        profile.setUser(otherUser);

        when(profileRepository.findById(1L)).thenReturn(Optional.of(profile));

        ValidationException exception = assertThrows(ValidationException.class,
                () -> profileService.deleteProfile(1L, 1L));

        assertTrue(exception.getMessage().contains("not authorized"));
        verify(profileRepository, never()).save(any(Profile.class));
    }

    // ========== REACTIVATE TESTS ==========

    @Test
    void reactivateProfile_Success() {
        profile.setIsActive(false);
        when(profileRepository.findById(1L)).thenReturn(Optional.of(profile));
        when(profileRepository.save(any(Profile.class))).thenReturn(profile);

        profileService.reactivateProfile(1L, 1L);

        verify(profileRepository).findById(1L);
        verify(profileRepository).save(any(Profile.class));
    }

    @Test
    void reactivateProfile_ProfileNotFound_ThrowsException() {
        when(profileRepository.findById(anyLong())).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> profileService.reactivateProfile(1L, 1L));

        assertEquals("Profile not found with ID: 1", exception.getMessage());
        verify(profileRepository, never()).save(any(Profile.class));
    }
}
