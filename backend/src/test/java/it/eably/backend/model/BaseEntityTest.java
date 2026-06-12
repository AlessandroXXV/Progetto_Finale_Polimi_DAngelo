package it.eably.backend.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for BaseEntity abstract class concrete methods.
 * Uses User as a concrete subclass to test all BaseEntity functionality.
 */
class BaseEntityTest {

    private User entityA;
    private User entityB;

    @BeforeEach
    void setUp() {
        entityA = new User();
        entityA.setId(1L);
        entityA.setUsername("userA");
        entityA.setEmail("a@example.com");
        entityA.setPasswordHash("hash");
        entityA.setRole(UserRole.STUDENT);
        entityA.setIsActive(true);

        entityB = new User();
        entityB.setId(1L);
        entityB.setUsername("userB");
        entityB.setEmail("b@example.com");
        entityB.setPasswordHash("hash");
        entityB.setRole(UserRole.STUDENT);
        entityB.setIsActive(true);
    }

    // isNew() tests

    @Test
    void isNew_NullId_ReturnsTrue() {
        User entity = new User();
        // id not set, defaults to null
        assertTrue(entity.isNew());
    }

    @Test
    void isNew_WithId_ReturnsFalse() {
        assertFalse(entityA.isNew());
    }

    // equals() tests

    @Test
    void equals_DifferentId_ReturnsFalse() {
        entityB.setId(2L);
        assertNotEquals(entityA, entityB);
    }

    @Test
    void equals_DifferentClass_ReturnsFalse() {
        Profile profile = new Profile();
        profile.setId(1L);
        assertNotEquals(entityA, profile);
    }

    // hashCode() tests

    @Test
    void hashCode_DifferentIds_DifferentHashes() {
        entityB.setId(999L);
        assertNotEquals(entityA.hashCode(), entityB.hashCode());
    }

    // toString() tests

    @Test
    void toString_NullFields_DoesNotThrow() {
        User entity = new User();
        assertDoesNotThrow(() -> entity.toString());
    }

    // Getter/setter tests for BaseEntity fields

    @Test
    void getSetVersion() {
        entityA.setVersion(42L);
        assertEquals(42L, entityA.getVersion());
    }

}
