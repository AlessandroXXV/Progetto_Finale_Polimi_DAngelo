package it.eably.backend.dto;

import it.eably.backend.dto.admin.response.AdminUserBookingStatsDTO;
import it.eably.backend.model.BookingStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AdminUserBookingStatsDTOTest {

    @Test
    void countForStatus_ReturnsMatchingCounters() {
        AdminUserBookingStatsDTO stats = new AdminUserBookingStatsDTO(
                5,
                7,
                9,
                2,
                3,
                4,
                5
        );

        assertEquals(2, stats.countForStatus(BookingStatus.PAYMENT_PENDING));
        assertEquals(3, stats.countForStatus(BookingStatus.CONFIRMED));
        assertEquals(4, stats.countForStatus(BookingStatus.COMPLETED));
        assertEquals(5, stats.countForStatus(BookingStatus.CANCELLED));
    }
}

