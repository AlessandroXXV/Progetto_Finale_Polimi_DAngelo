package it.eably.backend.controller;

import it.eably.backend.dto.profile.request.ProfileRequestDTO;
import it.eably.backend.dto.profile.response.ProfilePublicDTO;
import it.eably.backend.dto.profile.response.ProfileResponseDTO;
import it.eably.backend.mapper.ProfileMapper;
import it.eably.backend.model.DeliveryMode;
import it.eably.backend.model.Profile;
import it.eably.backend.model.User;
import it.eably.backend.service.def.ProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileControllerTest {

    @Mock
    private ProfileService profileService;

    @Mock
    private ProfileMapper profileMapper;

    @InjectMocks
    private ProfileController profileController;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("testuser");
        mockUser.setEmail("test@test.com");
    }

    // ── create ───────────────────────────────────────────────────────────────

    @Test
    void createProfile_Success() {
        Profile profile = new Profile();
        profile.setId(1L);
        ProfileResponseDTO responseDTO = buildResponseDTO();

        when(profileService.createProfile(anyLong(), any())).thenReturn(profile);
        when(profileMapper.toResponseDTO(any())).thenReturn(responseDTO);

        ResponseEntity<ProfileResponseDTO> response =
                profileController.createProfile(buildRequest(), mockUser);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(profileService).createProfile(eq(1L), any());
    }

    // ── update ───────────────────────────────────────────────────────────────

    @Test
    void updateProfile_Success() {
        Profile profile = new Profile();
        ProfileResponseDTO responseDTO = buildResponseDTO();

        when(profileService.updateProfile(anyLong(), anyLong(), any())).thenReturn(profile);
        when(profileMapper.toResponseDTO(any())).thenReturn(responseDTO);

        ResponseEntity<ProfileResponseDTO> response =
                profileController.updateProfile(1L, buildRequest(), mockUser);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(profileService).updateProfile(eq(1L), eq(1L), any());
    }

    // ── delete ───────────────────────────────────────────────────────────────

    @Test
    void deleteProfile_Success() {
        doNothing().when(profileService).deleteProfile(anyLong(), anyLong());

        ResponseEntity<Void> response = profileController.deleteProfile(1L, mockUser);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(profileService).deleteProfile(1L, 1L);
    }

    // ── get by id ────────────────────────────────────────────────────────────

    @Test
    void getProfileById_Success() {
        Profile profile = new Profile();

        when(profileService.getProfileById(1L)).thenReturn(profile);
        when(profileMapper.toPublicDTO(any())).thenReturn(buildPublicDTO());

        ResponseEntity<ProfilePublicDTO> response = profileController.getProfileById(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(profileService).getProfileById(1L);
        verify(profileMapper).toPublicDTO(profile);
    }

    // ── get all active ────────────────────────────────────────────────────────

    @Test
    void getAllActiveProfiles_NoFilters() {
        when(profileService.getAllActiveProfiles()).thenReturn(List.of());
        when(profileMapper.toPublicDTOList(anyList())).thenReturn(List.of());

        ResponseEntity<List<ProfilePublicDTO>> response = profileController.getAllActiveProfiles(null, null, null, null, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(profileService).getAllActiveProfiles();
        verify(profileMapper).toPublicDTOList(List.of());
    }

    @Test
    void getAllActiveProfiles_WithFilters() {
        when(profileService.searchActiveProfiles(eq("math"), eq(BigDecimal.valueOf(40)), eq(0), eq("10:00"), eq("12:00")))
                .thenReturn(List.of(new Profile()));
        when(profileMapper.toPublicDTOList(anyList())).thenReturn(List.of());

        ResponseEntity<List<ProfilePublicDTO>> response =
                profileController.getAllActiveProfiles("math", BigDecimal.valueOf(40), 0, "10:00", "12:00");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(profileService).searchActiveProfiles("math", BigDecimal.valueOf(40), 0, "10:00", "12:00");
        verify(profileMapper).toPublicDTOList(anyList());
    }

    // ── get my profiles ───────────────────────────────────────────────────────

    @Test
    void getMyProfiles_ReturnsProfilesForAuthenticatedUser() {
        when(profileService.getProfilesByUserId(1L)).thenReturn(List.of());
        when(profileMapper.toResponseDTOList(anyList())).thenReturn(List.of());

        ResponseEntity<List<ProfileResponseDTO>> response = profileController.getMyProfiles(mockUser);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(profileService).getProfilesByUserId(1L);
    }

    // ── get by student ────────────────────────────────────────────────────────

    @Test
    void getProfilesByStudent_Success() {
        when(profileService.getProfilesByUserId(7L)).thenReturn(List.of());
        when(profileMapper.toPublicDTOList(anyList())).thenReturn(List.of());

        ResponseEntity<List<ProfilePublicDTO>> response = profileController.getProfilesByStudent(7L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(profileService).getProfilesByUserId(7L);
        verify(profileMapper).toPublicDTOList(List.of());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private ProfileRequestDTO buildRequest() {
        return new ProfileRequestDTO("Math Tutor", "Description", BigDecimal.valueOf(50), DeliveryMode.ONLINE, "Address");
    }

    private ProfileResponseDTO buildResponseDTO() {
        return new ProfileResponseDTO(
                1L, 1L, "testuser", "test@test.com", "Math Tutor", "Description",
                BigDecimal.valueOf(50), DeliveryMode.ONLINE, "Address", true,
                LocalDateTime.now(), LocalDateTime.now()
        );
    }

    private ProfilePublicDTO buildPublicDTO() {
        return new ProfilePublicDTO(
                1L, "testuser", "Math Tutor", "Description",
                BigDecimal.valueOf(50), DeliveryMode.ONLINE, "Address", true
        );
    }
}
