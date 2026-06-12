package it.eably.backend.dto;

import it.eably.backend.dto.user.response.UserResponseDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the 8-argument secondary constructor of {@link UserResponseDTO},
 * which delegates to the canonical constructor and is not exercised by any
 * functional flow. The remaining DTO record members (getters, equals,
 * hashCode, toString) are already covered by controller/service/mapper tests,
 * so the previous exhaustive coverage tests were removed as redundant.
 */
class UserResponseDTOTest {

    @Test
    void userResponseDTO_eightArgConstructor_delegatesToCanonical() {
        UserResponseDTO dto = new UserResponseDTO(
                1L, "testuser", "test@test.com", "CLIENT", true, true,
                "John Doe", "Male"
        );

        assertEquals(1L, dto.id());
        assertEquals("testuser", dto.username());
        assertEquals("test@test.com", dto.email());
        assertEquals("CLIENT", dto.role());
        assertTrue(dto.isActive());
        assertTrue(dto.isVerified());
        assertEquals("John Doe", dto.fullName());
        assertEquals("Male", dto.gender());
    }
}
