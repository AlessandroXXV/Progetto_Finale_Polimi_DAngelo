package it.eably.backend.mapper;

import it.eably.backend.dto.profile.response.ProfilePublicDTO;
import it.eably.backend.dto.profile.response.ProfileResponseDTO;
import it.eably.backend.model.DeliveryMode;
import it.eably.backend.model.Profile;
import it.eably.backend.model.User;
import it.eably.backend.model.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProfileMapperTest {

    private ProfileMapper mapper;
    private User user;
    private Profile profile;

    @BeforeEach
    void setUp() {
        mapper = new ProfileMapperImpl();

        user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setRole(UserRole.STUDENT);
        user.setIsActive(true);

        profile = new Profile();
        profile.setId(10L);
        profile.setUser(user);
        profile.setTitle("Math Tutor");
        profile.setDescription("Expert in math");
        profile.setHourlyRate(new BigDecimal("50.00"));
        profile.setDeliveryMode(DeliveryMode.ONLINE);
        profile.setAddress("Via Roma 1");
        profile.setIsActive(true);
        profile.setCreatedAt(LocalDateTime.of(2024, 1, 1, 10, 0));
        profile.setUpdatedAt(LocalDateTime.of(2024, 6, 1, 10, 0));
    }

    @Test
    void toResponseDTO_NullProfile_ReturnsNull() {
        assertNull(mapper.toResponseDTO(null));
    }

    @Test
    void toResponseDTO_NullUser_UserFieldsAreNull() {
        profile.setUser(null);

        ProfileResponseDTO dto = mapper.toResponseDTO(profile);

        assertNotNull(dto);
        assertNull(dto.userId());
        assertNull(dto.username());
        assertNull(dto.email());
        assertEquals("Math Tutor", dto.title());
    }

    @Test
    void toResponseDTOList_NullList_ReturnsNull() {
        assertNull(mapper.toResponseDTOList(null));
    }

    @Test
    void toResponseDTOList_WithProfiles_MapsList() {
        Profile profile2 = new Profile();
        profile2.setId(20L);
        profile2.setUser(user);
        profile2.setTitle("Science Teacher");
        profile2.setHourlyRate(new BigDecimal("40.00"));
        profile2.setDeliveryMode(DeliveryMode.IN_PERSON);
        profile2.setIsActive(true);

        List<ProfileResponseDTO> result = mapper.toResponseDTOList(Arrays.asList(profile, profile2));

        assertEquals(2, result.size());
        assertEquals(10L, result.get(0).id());
        assertEquals(20L, result.get(1).id());
        assertEquals("Science Teacher", result.get(1).title());
    }

    // ── toPublicDTO ───────────────────────────────────────────────────────────

    @Test
    void toPublicDTO_NullProfile_ReturnsNull() {
        assertNull(mapper.toPublicDTO(null));
    }

    @Test
    void toPublicDTO_NullUser_UsernameIsNull() {
        profile.setUser(null);

        ProfilePublicDTO dto = mapper.toPublicDTO(profile);

        assertNotNull(dto);
        assertNull(dto.username());
        assertEquals("Math Tutor", dto.title());
    }

    // ── toPublicDTOList ───────────────────────────────────────────────────────

    @Test
    void toPublicDTOList_NullList_ReturnsNull() {
        assertNull(mapper.toPublicDTOList(null));
    }

    @Test
    void toPublicDTOList_WithProfiles_MapsList() {
        Profile profile2 = new Profile();
        profile2.setId(20L);
        profile2.setUser(user);
        profile2.setTitle("Science Teacher");
        profile2.setHourlyRate(new BigDecimal("40.00"));
        profile2.setDeliveryMode(DeliveryMode.IN_PERSON);
        profile2.setIsActive(true);

        List<ProfilePublicDTO> result = mapper.toPublicDTOList(Arrays.asList(profile, profile2));

        assertEquals(2, result.size());
        assertEquals(10L, result.get(0).id());
        assertEquals(20L, result.get(1).id());
        assertEquals("testuser", result.get(0).username());
        assertEquals("Science Teacher", result.get(1).title());
    }
}
