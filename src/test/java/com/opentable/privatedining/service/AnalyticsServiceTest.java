package com.opentable.privatedining.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.opentable.privatedining.dto.OccupancyReportDTO;
import com.opentable.privatedining.exception.BusinessRuleException;
import com.opentable.privatedining.exception.RestaurantNotFoundException;
import com.opentable.privatedining.exception.SpaceNotFoundException;
import com.opentable.privatedining.model.Reservation;
import com.opentable.privatedining.model.Restaurant;
import com.opentable.privatedining.model.Space;
import com.opentable.privatedining.repository.ReservationRepository;
import com.opentable.privatedining.repository.RestaurantRepository;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private RestaurantRepository restaurantRepository;
    @Mock
    private ReservationRepository reservationRepository;

    private AnalyticsService analyticsService;
    
    private final ObjectId restaurantId = new ObjectId();

    @BeforeEach
    void setUp() {
        // Manual instantiation to handle @Value properties
        analyticsService = new AnalyticsService(
            restaurantRepository, 
            reservationRepository, 
            "09:00", "22:00", 90
        );
    }
    
    @Test
    void getOccupancyReport_WhenRangeExceeds31Days_ShouldThrowException() {
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusDays(32);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> 
            analyticsService.getOccupancyReport(restaurantId, null, start, end, 60)
        );
    }
    
    
    @Test
    void getOccupancyReport_ShouldCalculateCorrectHeadcountAndSkipClosedHours() {
        // 1. Arrange Data
        Restaurant restaurant = new Restaurant();
        restaurant.setId(restaurantId);
        Space space = new Space("Main Room", 10, 10, 60);
        restaurant.setSpaces(List.of(space));
        restaurant.setOpeningTime(LocalTime.of(18, 0)); // Opens at 6 PM
        restaurant.setClosingTime(LocalTime.of(20, 0)); // Closes at 8 PM

        LocalDateTime start = LocalDateTime.of(2026, 1, 20, 17, 0); // Before opening
        LocalDateTime end = LocalDateTime.of(2026, 1, 20, 21, 0);   // After closing

        Reservation res = new Reservation();
        res.setSpaceId(space.getId());
        res.setPartySize(4);
        res.setStartTime(LocalDateTime.of(2026, 1, 20, 18, 0));
        res.setEndTime(LocalDateTime.of(2026, 1, 20, 19, 0));

        when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant));
        when(reservationRepository.findInDateRange(eq(restaurantId), any(), any()))
                .thenReturn(List.of(res));

        // 2. Act
        OccupancyReportDTO report = analyticsService.getOccupancyReport(restaurantId, null, start, end, 30);

        // 3. Assert
        // The first snapshot should jump to 18:00 (Opening)
        assertEquals(LocalDateTime.of(2026, 1, 20, 18, 0), report.getIntervals().get(0).getTimestamp());
        assertEquals(4, report.getIntervals().get(0).getCurrentHeadcount()); // 4 people
        assertEquals(40.0, report.getIntervals().get(0).getOccupancyPercentage()); // 4/10 = 40%

        // The snapshot at 19:30 should have 0 headcount (Reservation ended at 19:00)
        assertEquals(0, report.getIntervals().get(3).getCurrentHeadcount());
        
        // Ensure we didn't generate snapshots after 20:00 (Closing)
        boolean hasLateNight = report.getIntervals().stream()
                .anyMatch(s -> s.getTimestamp().toLocalTime().isAfter(LocalTime.of(20, 0)));
        assertFalse(hasLateNight, "Should not generate snapshots after closing time");
    }
    
    @Test
    void getOccupancyReport_WithSpecificSpace_ShouldFilterCapacity() {
        UUID specificSpaceId = UUID.randomUUID();
        Space s1 = new Space("Space1", 10, 10, 60); s1.setId(specificSpaceId);
        Space s2 = new Space("Space2", 20, 20, 60); s2.setId(UUID.randomUUID());
        
        Restaurant restaurant = new Restaurant();
        restaurant.setSpaces(List.of(s1, s2));
        
        Reservation reservation = new Reservation();
        reservation.setSpaceId(s1.getId());
        reservation.setRestaurantId(restaurantId);
        reservation.setStartTime(LocalDateTime.now());
        reservation.setEndTime(LocalDateTime.now().plusHours(1));

        when(restaurantRepository.findById(any())).thenReturn(Optional.of(restaurant));
        when(reservationRepository.findConcurrentReservations(eq(specificSpaceId), any(), any()))
                .thenReturn(List.of(reservation));

        OccupancyReportDTO report = analyticsService.getOccupancyReport(restaurantId, specificSpaceId, 
                LocalDateTime.now(), LocalDateTime.now().plusDays(1), 60);

        // Assert that capacity is 10 (Space1), not 30 (Total)
        assertEquals(10, report.getIntervals().get(0).getTotalCapacity());
    }
    
    @Test
    void getOccupancyReport_WhenStartIsAfterEnd_ShouldThrowBusinessRuleException() {
        // Arrange
        ObjectId restaurantId = new ObjectId();
        LocalDateTime start = LocalDateTime.of(2026, 1, 25, 18, 0);
        LocalDateTime end = LocalDateTime.of(2026, 1, 20, 18, 0); // End is BEFORE start

        // Act & Assert
        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> 
            analyticsService.getOccupancyReport(restaurantId, null, start, end, 30)
        );
    }
    
    @Test
    void getOccupancyReport_WhenRestaurantDoesNotExist_ShouldThrowRestaurantNotFoundException() {
        // Arrange
        ObjectId restaurantId = new ObjectId();
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = start.plusHours(2);

        // Mock the repository to return an empty Optional
        when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RestaurantNotFoundException.class, () -> 
            analyticsService.getOccupancyReport(restaurantId, null, start, end, 30)
        );
    }
    
    @Test
    void getOccupancyReport_WhenSpaceIdIsInvalid_ShouldThrowSpaceNotFoundException() {
        // Arrange
        ObjectId restaurantId = new ObjectId();
        UUID nonExistentSpaceId = UUID.randomUUID();
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = start.plusHours(2);

        Restaurant restaurant = new Restaurant();
        restaurant.setId(restaurantId);
        // Give the restaurant a different space
        Space existingSpace = new Space("Patio", 10, 10, 60);
        existingSpace.setId(UUID.randomUUID()); 
        restaurant.setSpaces(List.of(existingSpace));

        when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant));

        // Act & Assert
        assertThrows(SpaceNotFoundException.class, () -> 
            analyticsService.getOccupancyReport(restaurantId, nonExistentSpaceId, start, end, 30)
        );
    }
    
}