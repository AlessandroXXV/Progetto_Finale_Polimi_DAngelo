package it.eably.backend.mapper;

import it.eably.backend.dto.booking.response.BookingResponseDTO;
import it.eably.backend.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for BookingMapper (MapStruct generated code).
 * 
 * COVERAGE FOCUS:
 * - Entity to DTO mapping
 * - Nested property mapping
 * - List mapping
 * - Null handling
 * 
 * @author Alessandro D'Angelo
 * @version 1.0
 */
class BookingMapperTest {
    
    private BookingMapper mapper;
    
    private User client;
    private User providerUser;
    private Profile provider;
    private AvailabilitySlot slot;
    private Booking booking;
    
    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(BookingMapper.class);
        
        // Create client
        client = new User();
        client.setId(1L);
        client.setUsername("client_user");
        client.setEmail("client@test.com");
        client.setPasswordHash("$2a$12$hash");
        client.setRole(UserRole.CLIENT);
        client.setIsActive(true);
        client.setIsVerified(true);
        
        // Create provider user
        providerUser = new User();
        providerUser.setId(2L);
        providerUser.setUsername("provider_user");
        providerUser.setEmail("provider@test.com");
        providerUser.setPasswordHash("$2a$12$hash");
        providerUser.setRole(UserRole.STUDENT);
        providerUser.setIsActive(true);
        providerUser.setIsVerified(true);
        
        // Create provider profile
        provider = new Profile();
        provider.setId(1L);
        provider.setUser(providerUser);
        provider.setTitle("Math Tutor");
        provider.setDescription("Expert in mathematics");
        provider.setHourlyRate(new BigDecimal("50.00"));
        provider.setDeliveryMode(DeliveryMode.ONLINE);
        provider.setIsActive(true);
        
        // Create availability slot
        slot = new AvailabilitySlot();
        slot.setId(1L);
        slot.setStudent(providerUser);
        slot.setDayOfWeek(DayOfWeek.MONDAY);
        slot.setStartTime(LocalTime.of(10, 0));
        slot.setEndTime(LocalTime.of(11, 0));
        slot.setStatus(SlotStatus.BOOKED);
        
        // Create booking
        booking = new Booking();
        booking.setId(1L);
        booking.setClient(client);
        booking.setProvider(providerUser);
        booking.setProfile(provider);
        booking.setAvailabilitySlot(slot);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setTotalAmount(new BigDecimal("50.00"));
        booking.setNotes("Test booking notes");
        booking.setBookedAt(LocalDateTime.of(2026, 3, 5, 10, 0));
        booking.setConfirmationCode("123456");
        booking.setPaymentIntentId("pi_test123");
    }
    
    @Test
    void testToResponseDTO_ConfirmationCodeHiddenUntilCompleted() {
        // Arrange: CONFIRMED status should hide confirmation code
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setConfirmationCode("123456");

        // Act
        BookingResponseDTO dto = mapper.toResponseDTO(booking);

        // Assert: Confirmation code should be null (hidden) when not COMPLETED
        assertNotNull(dto);
        assertEquals(BookingStatus.CONFIRMED, dto.status());
        assertNull(dto.confirmationCode()); // Hidden when not COMPLETED
    }

    @Test
    void testToResponseDTO_NullNotes() {
        // Arrange
        booking.setNotes(null);
        
        // Act
        BookingResponseDTO dto = mapper.toResponseDTO(booking);
        
        // Assert
        assertNotNull(dto);
        assertNull(dto.notes());
    }
    
    @Test
    void testToResponseDTO_NullConfirmationCode() {
        // Arrange
        booking.setConfirmationCode(null);
        
        // Act
        BookingResponseDTO dto = mapper.toResponseDTO(booking);
        
        // Assert
        assertNotNull(dto);
        assertNull(dto.confirmationCode());
    }
    
    @Test
    void testToResponseDTO_NullPaymentIntentId() {
        // Arrange
        booking.setPaymentIntentId(null);
        
        // Act
        BookingResponseDTO dto = mapper.toResponseDTO(booking);
        
        // Assert
        assertNotNull(dto);
        assertNull(dto.paymentIntentId());
    }
    
    @Test
    void testToResponseDTO_PaymentPendingStatus() {
        // Arrange
        booking.setStatus(BookingStatus.PAYMENT_PENDING);
        booking.setConfirmationCode(null);
        booking.setPaymentIntentId(null);
        
        // Act
        BookingResponseDTO dto = mapper.toResponseDTO(booking);
        
        // Assert
        assertNotNull(dto);
        assertEquals(BookingStatus.PAYMENT_PENDING, dto.status());
        assertNull(dto.confirmationCode());
        assertNull(dto.paymentIntentId());
    }
    
    @Test
    void testToResponseDTO_CompletedStatus() {
        // Arrange
        booking.setStatus(BookingStatus.COMPLETED);
        
        // Act
        BookingResponseDTO dto = mapper.toResponseDTO(booking);
        
        // Assert
        assertNotNull(dto);
        assertEquals(BookingStatus.COMPLETED, dto.status());
    }
    
    @Test
    void testToResponseDTO_CancelledStatus() {
        // Arrange
        booking.setStatus(BookingStatus.CANCELLED);
        
        // Act
        BookingResponseDTO dto = mapper.toResponseDTO(booking);
        
        // Assert
        assertNotNull(dto);
        assertEquals(BookingStatus.CANCELLED, dto.status());
    }
    
    @Test
    void testToResponseDTOList_MultipleBookings() {
        // Arrange
        Booking booking2 = new Booking();
        booking2.setId(2L);
        booking2.setClient(client);
        booking2.setProvider(providerUser);
        booking2.setProfile(provider);
        booking2.setAvailabilitySlot(slot);
        booking2.setStatus(BookingStatus.PAYMENT_PENDING);
        booking2.setTotalAmount(new BigDecimal("75.00"));
        booking2.setBookedAt(LocalDateTime.now());
        
        List<Booking> bookings = Arrays.asList(booking, booking2);
        
        // Act
        List<BookingResponseDTO> dtos = mapper.toResponseDTOList(bookings);
        
        // Assert
        assertNotNull(dtos);
        assertEquals(2, dtos.size());
        
        assertEquals(1L, dtos.get(0).id());
        assertEquals(BookingStatus.CONFIRMED, dtos.get(0).status());
        
        assertEquals(2L, dtos.get(1).id());
        assertEquals(BookingStatus.PAYMENT_PENDING, dtos.get(1).status());
    }
    
    @Test
    void testToResponseDTO_NestedProviderUserMapping() {
        // Act
        BookingResponseDTO dto = mapper.toResponseDTO(booking);
        
        // Assert - Verify nested provider.user.username mapping
        assertEquals("provider_user", dto.providerUsername());
    }
    
    @Test
    void testToResponseDTO_SlotDayOfWeekMapping() {
        // Arrange - Test different days
        slot.setDayOfWeek(DayOfWeek.FRIDAY);
        
        // Act
        BookingResponseDTO dto = mapper.toResponseDTO(booking);
        
        // Assert
        assertEquals("FRIDAY", dto.slotDayOfWeek());
    }
    
    @Test
    void testToResponseDTO_SlotTimeMapping() {
        // Arrange - Test different times
        slot.setStartTime(LocalTime.of(14, 30));
        slot.setEndTime(LocalTime.of(15, 30));
        
        // Act
        BookingResponseDTO dto = mapper.toResponseDTO(booking);
        
        // Assert
        assertEquals("14:30:00", dto.slotStartTime());
        assertEquals("15:30:00", dto.slotEndTime());
    }
}
