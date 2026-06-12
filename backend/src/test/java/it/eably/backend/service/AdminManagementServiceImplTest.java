package it.eably.backend.service;

import it.eably.backend.dto.admin.request.AdminUserActionDTO;
import it.eably.backend.dto.admin.response.AdminUserDetailDTO;
import it.eably.backend.dto.admin.response.AdminUserListDTO;
import it.eably.backend.exception.ResourceNotFoundException;
import it.eably.backend.exception.ValidationException;
import it.eably.backend.model.Booking;
import it.eably.backend.model.BookingStatus;
import it.eably.backend.model.DeliveryMode;
import it.eably.backend.model.Profile;
import it.eably.backend.model.User;
import it.eably.backend.model.UserRole;
import it.eably.backend.repository.BookingRepository;
import it.eably.backend.repository.ProfileRepository;
import it.eably.backend.repository.UserRepository;
import it.eably.backend.mapper.AdminMapper;
import it.eably.backend.service.impl.AdminManagementServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminManagementServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private ProfileRepository profileRepository;

    @Spy
    private AdminMapper adminMapper;

    @InjectMocks
    private AdminManagementServiceImpl adminManagementService;

    private User clientUser;
    private User studentUser;

    @BeforeEach
    void setUp() {
        clientUser = buildUser(1L, "client1", UserRole.CLIENT, true, true);
        studentUser = buildUser(2L, "student1", UserRole.STUDENT, true, false);
    }

    @Test
    void getUsers_FiltersAndSortsByIdDesc() {
        User adminUser = buildUser(3L, "admin1", UserRole.ADMIN, false, true);
        when(userRepository.findAll()).thenReturn(List.of(clientUser, adminUser, studentUser));
        when(profileRepository.countProfilesGroupedByUserId()).thenReturn(List.of());
        when(bookingRepository.countClientBookingsGroupedByUserId()).thenReturn(List.of());
        when(bookingRepository.countProviderBookingsGroupedByUserId()).thenReturn(List.of());

        List<AdminUserListDTO> result = adminManagementService.getUsers("all", null, null);

        assertEquals(3, result.size());
        assertEquals(3L, result.get(0).id());
        assertEquals(2L, result.get(1).id());
        assertEquals(1L, result.get(2).id());
    }

    @Test
    void getUsers_WithRoleAndFlags_ReturnsMatchingUsersOnly() {
        User studentVerified = buildUser(4L, "student2", UserRole.STUDENT, true, true);
        when(userRepository.findAll()).thenReturn(List.of(clientUser, studentUser, studentVerified));
        when(profileRepository.countProfilesGroupedByUserId()).thenReturn(List.of());
        when(bookingRepository.countClientBookingsGroupedByUserId()).thenReturn(List.of());
        when(bookingRepository.countProviderBookingsGroupedByUserId()).thenReturn(List.of());

        List<AdminUserListDTO> result = adminManagementService.getUsers("student", true, true);

        assertEquals(1, result.size());
        assertEquals(4L, result.getFirst().id());
        assertEquals("STUDENT", result.getFirst().role());
    }

    @Test
    void getUsers_InvalidRole_ThrowsValidationException() {
        ValidationException ex = assertThrows(ValidationException.class,
                () -> adminManagementService.getUsers("not-a-role", null, null));

        assertEquals("Invalid role filter: not-a-role", ex.getMessage());
        verify(userRepository, never()).findAll();
    }

    @Test
    void getUsers_MapsNullRoleToNullString() {
        User rolelessUser = buildUser(5L, "roleless", null, true, false);
        when(userRepository.findAll()).thenReturn(List.of(rolelessUser));
        when(profileRepository.countProfilesGroupedByUserId()).thenReturn(List.of());
        when(bookingRepository.countClientBookingsGroupedByUserId()).thenReturn(List.of());
        when(bookingRepository.countProviderBookingsGroupedByUserId()).thenReturn(List.of());

        List<AdminUserListDTO> result = adminManagementService.getUsers("", null, null);

        assertEquals(1, result.size());
        assertNull(result.getFirst().role());
    }

    @Test
    void getUserDetails_AggregatesDistinctBookingsAndSortsServices() {
        LocalDateTime createdAt = LocalDateTime.now().minusDays(3);
        LocalDateTime updatedAt = LocalDateTime.now().minusHours(4);

        studentUser.setStripeAccountId("acct_123");
        studentUser.setStripeConnected(true);
        studentUser.setCreatedAt(createdAt);
        studentUser.setUpdatedAt(updatedAt);

        Profile oldService = new Profile();
        oldService.setId(10L);
        oldService.setTitle("Matematica base");
        oldService.setDeliveryMode(DeliveryMode.ONLINE);
        oldService.setIsActive(true);
        oldService.setHourlyRate(new BigDecimal("25.00"));

        Profile newService = new Profile();
        newService.setId(20L);
        newService.setTitle("Fisica avanzata");
        newService.setDeliveryMode(null);
        newService.setIsActive(false);
        newService.setHourlyRate(new BigDecimal("35.00"));

        Booking clientRequested = bookingWithIdAndStatus(100L, BookingStatus.PAYMENT_PENDING);
        Booking duplicatedCompleted = bookingWithIdAndStatus(200L, BookingStatus.COMPLETED);
        Booking providerCancelled = bookingWithIdAndStatus(300L, BookingStatus.CANCELLED);
        Booking providerWithoutId = bookingWithIdAndStatus(null, BookingStatus.CONFIRMED);

        when(userRepository.findById(2L)).thenReturn(Optional.of(studentUser));
        when(profileRepository.findAllByUserId(2L)).thenReturn(List.of(oldService, newService));
        when(bookingRepository.findByClientId(2L)).thenReturn(List.of(clientRequested, duplicatedCompleted));
        when(bookingRepository.findByProviderId(2L)).thenReturn(List.of(duplicatedCompleted, providerCancelled, providerWithoutId));

        AdminUserDetailDTO result = adminManagementService.getUserDetails(2L);

        assertEquals(2L, result.id());
        assertEquals("STUDENT", result.role());
        assertEquals(createdAt, result.createdAt());
        assertEquals(updatedAt, result.updatedAt());

        assertEquals(2, result.bookingStats().totalAsClient());
        assertEquals(3, result.bookingStats().totalAsProvider());
        assertEquals(3, result.bookingStats().totalDistinctBookings());
        // REQUESTED removed; that booking now appears as PAYMENT_PENDING
        assertEquals(1, result.bookingStats().paymentPending());
        assertEquals(1, result.bookingStats().completed());
        assertEquals(1, result.bookingStats().cancelled());
        assertEquals(0, result.bookingStats().confirmed());

        assertEquals(2, result.services().size());
        assertEquals(20L, result.services().getFirst().id());
        assertEquals(10L, result.services().get(1).id());
        assertNull(result.services().getFirst().deliveryMode());

        assertTrue(result.stripeInfo().hasStripeAccount());
        assertTrue(result.stripeInfo().stripeConnected());
        assertEquals("acct_123", result.stripeInfo().stripeAccountId());
    }

    @Test
    void getUserDetails_WithoutStripeAccount_SetsHasStripeAccountFalse() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(clientUser));
        when(profileRepository.findAllByUserId(1L)).thenReturn(List.of());
        when(bookingRepository.findByClientId(1L)).thenReturn(List.of());
        when(bookingRepository.findByProviderId(1L)).thenReturn(List.of());

        AdminUserDetailDTO result = adminManagementService.getUserDetails(1L);

        assertFalse(result.stripeInfo().hasStripeAccount());
        assertNull(result.stripeInfo().stripeAccountId());
    }

    @Test
    void getUserDetails_UserNotFound_ThrowsResourceNotFoundException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> adminManagementService.getUserDetails(999L));
    }

    @Test
    void setUserVerified_UpdatesAndReturnsActionDto() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(clientUser));
        when(userRepository.save(clientUser)).thenReturn(clientUser);

        AdminUserActionDTO result = adminManagementService.setUserVerified(1L, false);

        assertEquals(1L, result.id());
        assertFalse(result.isVerified());
        assertTrue(result.isActive());
    }

    @Test
    void setUserActive_UpdatesAndReturnsActionDto() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(studentUser));
        when(userRepository.save(studentUser)).thenReturn(studentUser);

        AdminUserActionDTO result = adminManagementService.setUserActive(2L, false);

        assertEquals(2L, result.id());
        assertFalse(result.isActive());
        assertFalse(result.isVerified());
    }

    @Test
    void setUserActive_UserNotFound_ThrowsResourceNotFoundException() {
        when(userRepository.findById(500L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> adminManagementService.setUserActive(500L, true));
    }

    private User buildUser(Long id, String username, UserRole role, boolean isActive, boolean isVerified) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(username + "@test.com");
        user.setPasswordHash("hash");
        user.setRole(role);
        user.setIsActive(isActive);
        user.setIsVerified(isVerified);
        user.setStripeConnected(false);
        return user;
    }

    private Booking bookingWithIdAndStatus(Long id, BookingStatus status) {
        Booking booking = new Booking();
        booking.setId(id);
        booking.setStatus(status);
        return booking;
    }
}


