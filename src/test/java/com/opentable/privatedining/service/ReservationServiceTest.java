package com.opentable.privatedining.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;

import com.opentable.privatedining.exception.BusinessRuleException;
import com.opentable.privatedining.exception.InsufficientCapacityException;
import com.opentable.privatedining.exception.InvalidPartySizeException;
import com.opentable.privatedining.exception.InvalidReservationTimeException;
import com.opentable.privatedining.exception.ReservationConflictException;
import com.opentable.privatedining.exception.RestaurantNotFoundException;
import com.opentable.privatedining.exception.SpaceNotFoundException;
import com.opentable.privatedining.model.Reservation;
import com.opentable.privatedining.model.Restaurant;
import com.opentable.privatedining.model.Space;
import com.opentable.privatedining.repository.ReservationRepository;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

	private static final Logger log = LoggerFactory.getLogger(ReservationServiceTest.class);
	
	@Mock
	private ReservationRepository reservationRepository;

	@Mock
	private RestaurantService restaurantService;

	private ReservationService reservationService;

	@BeforeEach
	void setUp() {
		// Manually call the constructor with mocks and test values
		reservationService = new ReservationService(reservationRepository, restaurantService, "09:00", "22:00", 90);
	}

	@Test
	void getAllReservations_ShouldReturnAllReservations() {
		// Given
		Reservation reservation1 = createTestReservation("customer1@example.com", 4);
		Reservation reservation2 = createTestReservation("customer2@example.com", 6);
		List<Reservation> reservations = Arrays.asList(reservation1, reservation2);

		when(reservationRepository.findAll()).thenReturn(reservations);

		// When
		List<Reservation> result = reservationService.getAllReservations();

		// Then
		assertEquals(2, result.size());
		assertEquals("customer1@example.com", result.get(0).getCustomerEmail());
		assertEquals("customer2@example.com", result.get(1).getCustomerEmail());
		verify(reservationRepository).findAll();
	}

	@Test
	void getReservationById_WhenReservationExists_ShouldReturnReservation() {
		// Given
		ObjectId reservationId = new ObjectId();
		Reservation reservation = createTestReservation("test@example.com", 4);
		reservation.setId(reservationId);

		when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));

		// When
		Optional<Reservation> result = reservationService.getReservationById(reservationId);

		// Then
		assertTrue(result.isPresent());
		assertEquals("test@example.com", result.get().getCustomerEmail());
		assertEquals(4, result.get().getPartySize());
		verify(reservationRepository).findById(reservationId);
	}

	@Test
	void getReservationById_WhenReservationNotFound_ShouldReturnEmpty() {
		// Given
		ObjectId reservationId = new ObjectId();
		when(reservationRepository.findById(reservationId)).thenReturn(Optional.empty());

		// When
		Optional<Reservation> result = reservationService.getReservationById(reservationId);

		// Then
		assertFalse(result.isPresent());
		verify(reservationRepository).findById(reservationId);
	}

	@Test
	void createReservation_WhenValidReservation_ShouldReturnSavedReservation() {
		// Given
		ObjectId restaurantId = new ObjectId();
		UUID spaceId = UUID.randomUUID();
		Reservation reservation = createTestReservation("customer@example.com", 4);
		reservation.setRestaurantId(restaurantId);
		reservation.setSpaceId(spaceId);

		com.opentable.privatedining.model.Restaurant restaurant = new com.opentable.privatedining.model.Restaurant(
				"Test Restaurant", "Address", "Cuisine", 50);
		Space space = new Space("Test Space", 2, 8);
		space.setId(spaceId);
		restaurant.setSpaces(List.of(space));

		Reservation savedReservation = createTestReservation("customer@example.com", 4);
		savedReservation.setId(new ObjectId());

		when(restaurantService.getRestaurantById(restaurantId)).thenReturn(Optional.of(restaurant));
		when(reservationRepository.findAll()).thenReturn(Arrays.asList());
		when(reservationRepository.save(reservation)).thenReturn(savedReservation);

		// When
		Reservation result = reservationService.createReservation(reservation);

		// Then
		assertNotNull(result);
		assertNotNull(result.getId());
		verify(restaurantService).getRestaurantById(restaurantId);
		verify(reservationRepository).save(reservation);
	}

	@Test
	void createReservation_WhenRestaurantNotFound_ShouldThrowException() {
		// Given
		ObjectId restaurantId = new ObjectId();
		Reservation reservation = createTestReservation("customer@example.com", 4);
		reservation.setRestaurantId(restaurantId);

		when(restaurantService.getRestaurantById(restaurantId)).thenReturn(Optional.empty());

		// When & Then
		assertThrows(RestaurantNotFoundException.class, () -> {
			reservationService.createReservation(reservation);
		});
		verify(restaurantService).getRestaurantById(restaurantId);
		verify(reservationRepository, never()).save(any(Reservation.class));
	}

	@Test
	void createReservation_WhenSpaceNotFound_ShouldThrowException() {
		// Given
		ObjectId restaurantId = new ObjectId();
		UUID spaceId = UUID.randomUUID();
		Reservation reservation = createTestReservation("customer@example.com", 4);
		reservation.setRestaurantId(restaurantId);
		reservation.setSpaceId(spaceId);

		com.opentable.privatedining.model.Restaurant restaurant = new com.opentable.privatedining.model.Restaurant(
				"Test Restaurant", "Address", "Cuisine", 50);

		when(restaurantService.getRestaurantById(restaurantId)).thenReturn(Optional.of(restaurant));

		// When & Then
		assertThrows(SpaceNotFoundException.class, () -> {
			reservationService.createReservation(reservation);
		});
		verify(restaurantService).getRestaurantById(restaurantId);
		verify(reservationRepository, never()).save(any(Reservation.class));
	}

	@Test
	void createReservation_WhenPartySizeBelowMinCapacity_ShouldThrowException() {
		// Given
		ObjectId restaurantId = new ObjectId();
		UUID spaceId = UUID.randomUUID();
		Reservation reservation = createTestReservation("customer@example.com", 1); // Below min capacity
		reservation.setRestaurantId(restaurantId);
		reservation.setSpaceId(spaceId);

		com.opentable.privatedining.model.Restaurant restaurant = new com.opentable.privatedining.model.Restaurant(
				"Test Restaurant", "Address", "Cuisine", 50);
		Space space = new Space("Test Space", 2, 8); // Min capacity is 2
		space.setId(spaceId);
		restaurant.setSpaces(List.of(space));

		when(restaurantService.getRestaurantById(restaurantId)).thenReturn(Optional.of(restaurant));

		// When & Then
		assertThrows(InvalidPartySizeException.class, () -> {
			reservationService.createReservation(reservation);
		});
		verify(reservationRepository, never()).save(any(Reservation.class));
	}

	@Test
	void createReservation_WhenPartySizeAboveMaxCapacity_ShouldThrowException() {
		// Given
		ObjectId restaurantId = new ObjectId();
		UUID spaceId = UUID.randomUUID();
		Reservation reservation = createTestReservation("customer@example.com", 10); // Above max capacity
		reservation.setRestaurantId(restaurantId);
		reservation.setSpaceId(spaceId);

		com.opentable.privatedining.model.Restaurant restaurant = new com.opentable.privatedining.model.Restaurant(
				"Test Restaurant", "Address", "Cuisine", 50);
		Space space = new Space("Test Space", 2, 8); // Max capacity is 8
		space.setId(spaceId);
		restaurant.setSpaces(List.of(space));

		when(restaurantService.getRestaurantById(restaurantId)).thenReturn(Optional.of(restaurant));

		// When & Then
		assertThrows(InvalidPartySizeException.class, () -> {
			reservationService.createReservation(reservation);
		});
		verify(reservationRepository, never()).save(any(Reservation.class));
	}

	@Test
	void createReservation_WhenOverlappingReservationExists_ShouldThrowException() {
		// Given
		ObjectId restaurantId = new ObjectId();
		UUID spaceId = UUID.randomUUID();

		LocalDateTime startTime = LocalDateTime.now().plusDays(1);
		LocalDateTime endTime = startTime.plusHours(2);

		Reservation newReservation = createTestReservation("customer@example.com", 4);
		newReservation.setRestaurantId(restaurantId);
		newReservation.setSpaceId(spaceId);
		newReservation.setStartTime(startTime);
		newReservation.setEndTime(endTime);

		// Existing overlapping reservation
		Reservation existingReservation = createTestReservation("other@example.com", 2);
		existingReservation.setRestaurantId(restaurantId);
		existingReservation.setSpaceId(spaceId);
		existingReservation.setStartTime(startTime.minusMinutes(30));
		existingReservation.setEndTime(endTime.minusMinutes(30));

		com.opentable.privatedining.model.Restaurant restaurant = new com.opentable.privatedining.model.Restaurant(
				"Test Restaurant", "Address", "Cuisine", 50);
		Space space = new Space("Test Space", 2, 8);
		space.setId(spaceId);
		restaurant.setSpaces(List.of(space));

		when(restaurantService.getRestaurantById(restaurantId)).thenReturn(Optional.of(restaurant));
		when(reservationRepository.findAll()).thenReturn(Arrays.asList(existingReservation));

		// When & Then
		assertThrows(ReservationConflictException.class, () -> {
			reservationService.createReservation(newReservation);
		});
		verify(reservationRepository, never()).save(any(Reservation.class));
	}

	@Test
	void deleteReservation_WhenReservationExists_ShouldReturnTrue() {
		// Given
		ObjectId reservationId = new ObjectId();
		Reservation reservation = createTestReservation("customer@example.com", 4);
		reservation.setId(reservationId);

		when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));

		// When
		boolean result = reservationService.deleteReservation(reservationId);

		// Then
		assertTrue(result);
		verify(reservationRepository).findById(reservationId);
		verify(reservationRepository).deleteById(reservationId);
	}

	@Test
	void deleteReservation_WhenReservationNotFound_ShouldReturnFalse() {
		// Given
		ObjectId reservationId = new ObjectId();
		when(reservationRepository.findById(reservationId)).thenReturn(Optional.empty());

		// When
		boolean result = reservationService.deleteReservation(reservationId);

		// Then
		assertFalse(result);
		verify(reservationRepository).findById(reservationId);
		verify(reservationRepository, never()).deleteById(reservationId);
	}

	@Test
	void getReservationsByRestaurant_ShouldReturnFilteredReservations() {
		// Given
		ObjectId restaurantId = new ObjectId();
		ObjectId otherRestaurantId = new ObjectId();

		Reservation reservation1 = createTestReservation("customer1@example.com", 4);
		reservation1.setRestaurantId(restaurantId);

		Reservation reservation2 = createTestReservation("customer2@example.com", 6);
		reservation2.setRestaurantId(otherRestaurantId);

		Reservation reservation3 = createTestReservation("customer3@example.com", 2);
		reservation3.setRestaurantId(restaurantId);

		List<Reservation> allReservations = Arrays.asList(reservation1, reservation2, reservation3);

		when(reservationRepository.findAll()).thenReturn(allReservations);

		// When
		List<Reservation> result = reservationService.getReservationsByRestaurant(restaurantId);

		// Then
		assertEquals(2, result.size());
		assertEquals("customer1@example.com", result.get(0).getCustomerEmail());
		assertEquals("customer3@example.com", result.get(1).getCustomerEmail());
		verify(reservationRepository).findAll();
	}

	@Test
	void getReservationsBySpace_ShouldReturnFilteredReservations() {
		// Given
		ObjectId restaurantId = new ObjectId();
		UUID spaceId = UUID.randomUUID();
		UUID otherSpaceId = UUID.randomUUID();

		Reservation reservation1 = createTestReservation("customer1@example.com", 4);
		reservation1.setRestaurantId(restaurantId);
		reservation1.setSpaceId(spaceId);

		Reservation reservation2 = createTestReservation("customer2@example.com", 6);
		reservation2.setRestaurantId(restaurantId);
		reservation2.setSpaceId(otherSpaceId);

		Reservation reservation3 = createTestReservation("customer3@example.com", 2);
		reservation3.setRestaurantId(restaurantId);
		reservation3.setSpaceId(spaceId);

		List<Reservation> allReservations = Arrays.asList(reservation1, reservation2, reservation3);

		when(reservationRepository.findAll()).thenReturn(allReservations);

		// When
		List<Reservation> result = reservationService.getReservationsBySpace(restaurantId, spaceId);

		// Then
		assertEquals(2, result.size());
		assertEquals("customer1@example.com", result.get(0).getCustomerEmail());
		assertEquals("customer3@example.com", result.get(1).getCustomerEmail());
		verify(reservationRepository).findAll();
	}

	private Reservation createTestReservation(String customerEmail, int partySize) {
		Reservation reservation = new Reservation();
		reservation.setCustomerEmail(customerEmail);
		reservation.setPartySize(partySize);
		reservation.setRestaurantId(new ObjectId());
		reservation.setSpaceId(UUID.randomUUID());
		reservation.setStartTime(LocalDateTime.now().plusDays(1));
		reservation.setEndTime(LocalDateTime.now().plusDays(1).plusHours(2));
		reservation.setStatus("CONFIRMED");
		return reservation;
	}

	// Create reservation v2 test cases.

	// 1. Success Test case, reservation should be created.
	@Test
	void createReservationV2_Success() {
		Restaurant restaurant = createMockRestaurant();
		Space space1 = restaurant.getSpaces().get(0);
		Space space2 = new Space("Space2",10,20,60);
		Space space3 = new Space("Space2",10,20,null);
		List<Space> mutableSpaces = new ArrayList<>(restaurant.getSpaces());
		mutableSpaces.add(space2);
		mutableSpaces.add(space3);
		restaurant.setSpaces(mutableSpaces);

		Reservation input1 = createTestReservation("rajmuntha@gmail.com", 10);
		input1.setRestaurantId(restaurant.getId());
		input1.setSpaceId(space1.getId());
		input1.setStartTime(tomorrow.atTime(18, 0)); // 6:00 PM
		input1.setEndTime(input1.getStartTime().plusMinutes(120));
		
		Reservation input2 = createTestReservation("rajmuntha@gmail.com", 10);
		input2.setRestaurantId(restaurant.getId());
		input2.setSpaceId(space2.getId());
		input2.setStartTime(tomorrow.atTime(18, 0)); // 6:00 PM
		input2.setEndTime(null);
		
		Reservation input3 = createTestReservation("rajmuntha@gmail.com", 10);
		input3.setRestaurantId(restaurant.getId());
		input3.setSpaceId(space3.getId());
		input3.setStartTime(tomorrow.atTime(18, 0)); // 6:00 PM
		input3.setEndTime(null);
		
		Reservation input4 = createTestReservation("rajmuntha@gmail.com", 10);
		input4.setRestaurantId(restaurant.getId());
		input4.setSpaceId(space2.getId());
		input4.setStartTime(tomorrow.atTime(18, 0)); // 6:00 PM
		input4.setEndTime(input4.getStartTime().plusMinutes(30));
		

		when(restaurantService.getRestaurantById(restaurant.getId())).thenReturn(Optional.of(restaurant));
		when(reservationRepository.findConcurrentReservations(any(), any(), any())).thenReturn(List.of());
		when(reservationRepository.save(any(Reservation.class))).thenAnswer(i -> i.getArgument(0));

		// Act
		Reservation result1 = reservationService.createReservationV2(input1);
		Reservation result2 = reservationService.createReservationV2(input2);
		Reservation result3 = reservationService.createReservationV2(input3);
		Reservation result4 = reservationService.createReservationV2(input4);

		// Assert
		assertNotNull(result1);
		assertNotNull(result2);
		assertNotNull(result3);
		assertNotNull(result4);
		
		// End time calculations test.
		// 18:00 + 120 minutes from request space slot = 20:00, Request is > Restaurant space slot duration.
		assertEquals(tomorrow.atTime(20, 0).truncatedTo(ChronoUnit.MINUTES),
				result1.getEndTime().truncatedTo(ChronoUnit.MINUTES));
		
		// 18:00 + 60 minutes from request = 19:00, With reservation space slot 90. No End time in request.
		assertEquals(tomorrow.atTime(19, 0).truncatedTo(ChronoUnit.MINUTES), 
				result2.getEndTime().truncatedTo(ChronoUnit.MINUTES));
		
		// 18:00 + 90 minutes default = 19:30, No reservation slot and No End time in request.
		assertEquals(tomorrow.atTime(19, 30).truncatedTo(ChronoUnit.MINUTES), 
				result3.getEndTime().truncatedTo(ChronoUnit.MINUTES));
		
		// 18:00 + 60 minutes,  reservation slot 60 minutes > End time in request (30 minutes).
		assertEquals(tomorrow.atTime(19, 0).truncatedTo(ChronoUnit.MINUTES), 
				result4.getEndTime().truncatedTo(ChronoUnit.MINUTES));
		
		// status confirmation.
		assertEquals("CONFIRMED", result1.getStatus());
		assertEquals("CONFIRMED", result2.getStatus());
		assertEquals("CONFIRMED", result3.getStatus());
		assertEquals("CONFIRMED", result4.getStatus());
		
		verify(reservationRepository, times(4)).save(any(Reservation.class));
	}

	// 2. Invalid party size validation failure.
	@Test
	void createReservationV2_ThrowsInvalidPartySize() {
		Restaurant restaurant = createMockRestaurant(); // Space capacity: 10-20
		Reservation input = createTestReservation("rajmuntha@gmail.com", 5);
		input.setRestaurantId(restaurant.getId());
		input.setSpaceId(restaurant.getSpaces().get(0).getId());

		when(restaurantService.getRestaurantById(any())).thenReturn(Optional.of(restaurant));

		assertThrows(InvalidPartySizeException.class, () -> reservationService.createReservationV2(input));
	}

	// 3. Operating hours window validation failure.
	@Test
	void createReservationV2_ThrowsInvalidReservationTime() {
		Restaurant restaurant = createMockRestaurant(); // Closes at 23:00
		
		// Start time validation.
		Reservation input = createTestReservation("rajmuntha@gmail.com", 10);
		input.setRestaurantId(restaurant.getId());
		input.setSpaceId(restaurant.getSpaces().get(0).getId());
		
		input.setStartTime(tomorrow.atTime(23, 15)); // Start at 23:15 (Too late)
		
		// End time validation.
		Reservation input2 = createTestReservation("rajmuntha@gmail.com", 10);
		input2.setRestaurantId(restaurant.getId());
		input2.setSpaceId(restaurant.getSpaces().get(0).getId());
		input2.setStartTime(tomorrow.atTime(22, 00));
		input2.setEndTime(input2.getStartTime().plusHours(2)); // Ends at 00:00 Too late.
		

		when(restaurantService.getRestaurantById(any())).thenReturn(Optional.of(restaurant));

		assertThrows(InvalidReservationTimeException.class, () -> reservationService.createReservationV2(input));
		assertThrows(InvalidReservationTimeException.class, () -> reservationService.createReservationV2(input2));
	}

	// 4. Concurrent booking and capacity validation failure.
	@Test
	@DisplayName("Should throw InsufficientCapacityException when space is full")
	void createReservationV2_InsufficientCapacity() {
		Restaurant restaurant = createMockRestaurant();
		Space space = restaurant.getSpaces().get(0); // Max capacity 20

		// Existing reservation of 8 people
		Reservation existing = new Reservation();
		existing.setPartySize(18);

		// New request for 10 people (10 + 10 = 20 > 18)
		Reservation input = createTestReservation("rajmuntha@gmail.com", 10);
		input.setRestaurantId(restaurant.getId());
		input.setSpaceId(restaurant.getSpaces().get(0).getId());
		input.setStartTime(tomorrow.atTime(18, 0));
		input.setEndTime(null);
		when(restaurantService.getRestaurantById(any())).thenReturn(Optional.of(restaurant));
		when(reservationRepository.findConcurrentReservations(any(), any(), any())).thenReturn(List.of(existing));

		assertThrows(InsufficientCapacityException.class, () -> reservationService.createReservationV2(input));
	}

	// 5. Concurrent booking and optimistic locking.
	@Test
	void createReservationV2_HandlesOptimisticLocking() {
		Restaurant restaurant = createMockRestaurant();
		when(restaurantService.getRestaurantById(any())).thenReturn(Optional.of(restaurant));
		when(reservationRepository.save(any())).thenThrow(OptimisticLockingFailureException.class);

		Reservation input = createTestReservation("rajmuntha@gmail.com", 10);
		input.setRestaurantId(restaurant.getId());
		input.setSpaceId(restaurant.getSpaces().get(0).getId());
		input.setStartTime(tomorrow.atTime(18, 0));
		input.setEndTime(null);
		assertThrows(OptimisticLockingFailureException.class, () -> reservationService.createReservationV2(input));
	}

	// 6. Reservation start date validation failure.
	@Test
	void createReservation_WhenDateInPast_ShouldThrowException() {
		Restaurant restaurant = createMockRestaurant();
		Reservation input = createTestReservation("rajmuntha@gmail.com", 10);
		input.setRestaurantId(restaurant.getId());
		input.setSpaceId(restaurant.getSpaces().get(0).getId());
		input.setStartTime(null);
		
		when(restaurantService.getRestaurantById(any())).thenReturn(Optional.of(restaurant));
		
		// Start date validation.
		assertThrows(BusinessRuleException.class, () -> {
			reservationService.createReservationV2(input);
			});
		
		input.setStartTime(today.minusDays(1).atTime(18, 0));
		// Start date should be future date validation.
		assertThrows(BusinessRuleException.class, () -> {
			reservationService.createReservationV2(input);
			});
		
		
	}
	
	private Restaurant createMockRestaurant() {
		Space space = new Space();
		space.setId(UUID.randomUUID());
		space.setName("Space1");
		space.setMinCapacity(10);
		space.setMaxCapacity(20);
		space.setSlotDurationMins(90);

		Restaurant restaurant = new Restaurant();
		restaurant.setId(new ObjectId());
		restaurant.setSpaces(List.of(space));
		restaurant.setOpeningTime(LocalTime.of(12, 0));
		restaurant.setClosingTime(LocalTime.of(23, 00));
		return restaurant;
	}
	
	private static final LocalDate today = LocalDate.now();
	private static final LocalDate tomorrow = LocalDate.now().plusDays(1);
	
}