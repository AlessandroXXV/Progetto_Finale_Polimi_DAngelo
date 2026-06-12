package it.eably.backend.controller;

import it.eably.backend.dto.admin.response.AdminStatsResponseDTO;
import it.eably.backend.dto.admin.request.AdminUserActionDTO;
import it.eably.backend.dto.admin.response.AdminUserDetailDTO;
import it.eably.backend.dto.admin.response.AdminUserListDTO;
import it.eably.backend.service.def.AdminManagementService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private AdminManagementService adminManagementService;

    @InjectMocks
    private AdminController adminController;

    @Test
    void getStats_ReturnsAggregatedCounts() {
        AdminStatsResponseDTO stats = new AdminStatsResponseDTO(11L, 27L);
        when(adminManagementService.getStats()).thenReturn(stats);

        ResponseEntity<AdminStatsResponseDTO> response = adminController.getStats();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(11L, response.getBody().totalUsers());
        assertEquals(27L, response.getBody().totalBookings());
    }

    @Test
    void getUsers_ForwardsFilters() {
        List<AdminUserListDTO> users = List.of(
                new AdminUserListDTO(1L, "u1", "u1@test.com", "U1", "CLIENT", true, true, false, 0, 1, 0)
        );
        when(adminManagementService.getUsers("client", true, true)).thenReturn(users);

        ResponseEntity<List<AdminUserListDTO>> response = adminController.getUsers("client", true, true);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals(1L, response.getBody().getFirst().id());
    }

    @Test
    void getUserDetails_ReturnsDto() {
        AdminUserDetailDTO detail = new AdminUserDetailDTO(
                7L,
                "user7",
                "user7@test.com",
                "User Seven",
                "F",
                "STUDENT",
                true,
                true,
                true,
                "acct_7",
                null,
                null,
                null,
                null,
                List.of()
        );
        when(adminManagementService.getUserDetails(7L)).thenReturn(detail);

        ResponseEntity<AdminUserDetailDTO> response = adminController.getUserDetails(7L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(7L, response.getBody().id());
    }

    @Test
    void verifyAndUnverifyUser_ReturnActionDto() {
        AdminUserActionDTO verified = new AdminUserActionDTO(3L, true, true);
        AdminUserActionDTO unverified = new AdminUserActionDTO(3L, true, false);
        when(adminManagementService.setUserVerified(3L, true)).thenReturn(verified);
        when(adminManagementService.setUserVerified(3L, false)).thenReturn(unverified);

        ResponseEntity<AdminUserActionDTO> verifyResponse = adminController.verifyUser(3L);
        ResponseEntity<AdminUserActionDTO> unverifyResponse = adminController.unverifyUser(3L);

        assertEquals(true, verifyResponse.getBody().isVerified());
        assertEquals(false, unverifyResponse.getBody().isVerified());
    }

    @Test
    void suspendAndActivateUser_ReturnActionDto() {
        AdminUserActionDTO suspended = new AdminUserActionDTO(4L, false, true);
        AdminUserActionDTO active = new AdminUserActionDTO(4L, true, true);
        when(adminManagementService.setUserActive(4L, false)).thenReturn(suspended);
        when(adminManagementService.setUserActive(4L, true)).thenReturn(active);

        ResponseEntity<AdminUserActionDTO> suspendResponse = adminController.suspendUser(4L);
        ResponseEntity<AdminUserActionDTO> activateResponse = adminController.activateUser(4L);

        assertEquals(false, suspendResponse.getBody().isActive());
        assertEquals(true, activateResponse.getBody().isActive());
    }
}


