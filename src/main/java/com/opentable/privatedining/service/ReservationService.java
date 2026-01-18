package com.opentable.privatedining.service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

@Service
public class ReservationService {
	
	private static final Logger logger = LoggerFactory.getLogger(ReservationService.class);

	private final ReservationRepository reservationRepository;
	private final RestaurantService restaurantService;

	// Operating windows constants.
	private LocalTime globalOpeningTime;
	private LocalTime globalClosingTime;

	// Default slot duration.
	private final int configuredDefaultDuration;

	public ReservationService(ReservationRepository reservationRepository, RestaurantService restaurantService,
			@Value("${reservation.operating-hours.open:09:00}") String open,
			@Value("${reservation.operating-hours.close:22:00}") String close,
			@Value("${reservation.default-duration-mins:90}") int duration) {
		this.reservationRepository = reservationRepository;
		this.restaurantService = restaurantService;
		this.configuredDefaultDuration = duration;
		try {
			this.globalOpeningTime = LocalTime.parse(open);
			this.globalClosingTime = LocalTime.parse(close);
		} catch (DateTimeParseException e) {
			this.globalOpeningTime = LocalTime.of(9, 0);
			this.globalClosingTime = LocalTime.of(22, 0);
		}
	}

	public List<Reservation> getAllReservations() {
		return reservationRepository.findAll();
	}

	public Optional<Reservation> getReservationById(ObjectId id) {
		return reservationRepository.findById(id);
	}

	public Reservation createReservation(Reservation reservation) {

		// Validate restaurant existence.
		Restaurant restaurant = validateRestaurantExistence(reservation);

		// Validate space existence.
		Space space = validateSpaceExistence(reservation, restaurant);

		// Validate party size is within space capacity
		validatePartySize(reservation, space);

		// Check for overlapping reservations
		validateOverlappingIntervals(reservation);

		// Save reservation.
		return reservationRepository.save(reservation);
	}

	public boolean deleteReservation(ObjectId id) {
		Optional<Reservation> existingReservation = reservationRepository.findById(id);
		if (existingReservation.isPresent()) {
			reservationRepository.deleteById(id);
			return true;
		}
		return false;
	}

	public List<Reservation> getReservationsByRestaurant(ObjectId restaurantId) {
		return reservationRepository.findAll().stream()
				.filter(reservation -> reservation.getRestaurantId().equals(restaurantId)).toList();
	}

	public List<Reservation> getReservationsBySpace(ObjectId restaurantId, UUID spaceId) {
		return reservationRepository.findAll().stream()
				.filter(reservation -> reservation.getRestaurantId().equals(restaurantId)
						&& reservation.getSpaceId().equals(spaceId))
				.toList();
	}

	@Retryable(
		    retryFor = { OptimisticLockingFailureException.class }, 
		    maxAttempts = 3, 
		    backoff = @Backoff(delay = 100)
		)
	@Transactional
	public Reservation createReservationV2(Reservation reservationToBeCreated) {

		// Validate restaurant existence.
		Restaurant restaurant = validateRestaurantExistence(reservationToBeCreated);

		// Validate space existence.
		Space space = validateSpaceExistence(reservationToBeCreated, restaurant);

		// Validate party size is within space capacity.
		validatePartySize(reservationToBeCreated, space);

		// Validate reservation start time, it should be future.
		validateReservationStartTime(reservationToBeCreated);

		// Get slot duration.
		determineDurationAndUpdateEndTime(reservationToBeCreated, space);

		// Validate operating window for end time.
		validateOperatingHours(reservationToBeCreated, restaurant);

		// Validate requested size with available time.
		validateForAvailableSize(reservationToBeCreated, space);
		
		// Save restaurant, optimistic lock checking.
		restaurant.setLastReservationAt(LocalDateTime.now());
		restaurantService.updateRestaurant(restaurant.getId(),restaurant);
		logger.info("Reservation counts : " + reservationRepository.count());

		try {
	        return reservationRepository.save(reservationToBeCreated);
	    } catch (DuplicateKeyException e) {
	        // If the same user, same space, and same time exists...
	    	logger.info("DuplicateKeyException",e);
	        throw new BusinessRuleException("You already have a pending booking for this slot.");
	    }
	}
	
	@Recover
	public Reservation recover(OptimisticLockingFailureException e, Reservation reservation) {
	    // If it still fails after 3 tries, tell the user why.
	    throw new BusinessRuleException("The system is currently busy. Please try your booking again.");
	}

	// Validations.

	/**
	 * Check for overlapping reservations.
	 * 
	 * @param reservation
	 */
	private void validateOverlappingIntervals(Reservation reservation) {
		if (hasOverlappingReservation(reservation.getRestaurantId(), reservation.getSpaceId(),
				reservation.getStartTime(), reservation.getEndTime())) {
			throw new ReservationConflictException(reservation.getRestaurantId(), reservation.getSpaceId(),
					reservation.getStartTime(), reservation.getEndTime());
		}
	}

	/**
	 * Validate party size is within space capacity
	 * 
	 * @param reservation
	 * @param space
	 */
	private void validatePartySize(Reservation reservation, Space space) {
		if (reservation.getPartySize() < space.getMinCapacity()
				|| reservation.getPartySize() > space.getMaxCapacity()) {
			throw new InvalidPartySizeException(reservation.getPartySize(), space.getMinCapacity(),
					space.getMaxCapacity());
		}
	}

	/**
	 * Validate restaurant and space existence.
	 * 
	 * @param reservation
	 * @return
	 */
	private Restaurant validateRestaurantExistence(Reservation reservation) {
		// Validate the restaurant exists check.
		Optional<Restaurant> restaurantOpt = restaurantService.getRestaurantById(reservation.getRestaurantId());
		if (restaurantOpt.isEmpty()) {
			throw new RestaurantNotFoundException(reservation.getRestaurantId());
		}
		return restaurantOpt.get();
	}

	private Space validateSpaceExistence(Reservation reservation, Restaurant restaurant) {
		// If restaurant exists, check for space existence.
		Space space = restaurant.getSpaces().stream().filter(s -> s.getId().equals(reservation.getSpaceId()))
				.findFirst()
				.orElseThrow(() -> new SpaceNotFoundException(reservation.getRestaurantId(), reservation.getSpaceId()));
		return space;
	}

	private boolean hasOverlappingReservation(ObjectId restaurantId, UUID spaceId, LocalDateTime startTime,
			LocalDateTime endTime) {
		return reservationRepository.findAll().stream()
				.anyMatch(existing -> existing.getRestaurantId().equals(restaurantId)
						&& existing.getSpaceId().equals(spaceId)
						&& isTimeOverlapping(existing.getStartTime(), existing.getEndTime(), startTime, endTime));
	}

	private boolean hasOverlappingReservationExcluding(ObjectId restaurantId, UUID spaceId, LocalDateTime startTime,
			LocalDateTime endTime, ObjectId excludeReservationId) {
		return reservationRepository.findAll().stream()
				.anyMatch(existing -> !existing.getId().equals(excludeReservationId)
						&& existing.getRestaurantId().equals(restaurantId) && existing.getSpaceId().equals(spaceId)
						&& isTimeOverlapping(existing.getStartTime(), existing.getEndTime(), startTime, endTime));
	}

	private boolean isTimeOverlapping(LocalDateTime existingStart, LocalDateTime existingEnd, LocalDateTime newStart,
			LocalDateTime newEnd) {
		return newStart.isBefore(existingEnd) && newEnd.isAfter(existingStart);
	}

	/**
	 * // Validate operating window for end time.
	 * 
	 * @param reservation
	 * @param restaurant
	 */
	private void validateOperatingHours(Reservation reservation, Restaurant restaurant) {
		LocalTime effectiveOpeningTime = Objects.nonNull(restaurant.getOpeningTime()) ? restaurant.getOpeningTime()
				: globalOpeningTime;
		LocalTime effectiveClosingTime = Objects.nonNull(restaurant.getClosingTime()) ? restaurant.getClosingTime()
				: globalClosingTime;
		LocalTime startTime = reservation.getStartTime().toLocalTime();
		if (startTime.isBefore(effectiveOpeningTime) || startTime.isAfter(effectiveClosingTime)) {
			throw new InvalidReservationTimeException(reservation.getRestaurantId(), reservation.getSpaceId(),
					effectiveOpeningTime, effectiveClosingTime);
		}

		LocalTime endTime = reservation.getEndTime().toLocalTime();
		if (endTime.isBefore(effectiveOpeningTime) || endTime.isAfter(effectiveClosingTime)) {
			throw new InvalidReservationTimeException(reservation.getRestaurantId(), reservation.getSpaceId(),
					effectiveOpeningTime, effectiveClosingTime);
		}
	}

	/**
	 * Get the difference between two times in minutes.
	 * 
	 * @param startTime
	 * @param endTime
	 * @return
	 */
	private long getDurationInMinutes(LocalDateTime startTime, LocalDateTime endTime) {
		if (startTime == null || endTime == null)
			return 0;
		return java.time.Duration.between(startTime, endTime).toMinutes();
	}

	/**
	 * Validate requested size with available time.
	 * 
	 * @param reservationToBeCreated
	 * @param space
	 */
	private void validateForAvailableSize(Reservation reservationToBeCreated, Space space) {
		// Fetch current booked capacity.
		int currentBookedCapacity = reservationRepository.findConcurrentReservations(space.getId(),
				reservationToBeCreated.getStartTime(), reservationToBeCreated.getEndTime()).stream()
				.mapToInt(Reservation::getPartySize).sum();

		logger.info("Current Booking : " + currentBookedCapacity);
		
		// Validate requested size with available time.
		if (currentBookedCapacity + reservationToBeCreated.getPartySize() > space.getMaxCapacity()) {
			logger.info("No space for  : " + reservationToBeCreated.getPartySize());
			throw new InsufficientCapacityException(reservationToBeCreated.getRestaurantId(),
					reservationToBeCreated.getSpaceId(), reservationToBeCreated.getStartTime(),
					reservationToBeCreated.getEndTime(), reservationToBeCreated.getPartySize());
		}
		
		logger.info("Enough space for  : " + reservationToBeCreated.getPartySize());
	}

	/**
	 * Validation of reservation start time, it should be future date.
	 * 
	 * @param reservationToBeCreated
	 */
	private void validateReservationStartTime(Reservation reservation) {

		if (Objects.isNull(reservation.getStartTime())) {
			throw new BusinessRuleException("Reservation start time is required.");
		}

		if (LocalDateTime.now().isAfter(reservation.getStartTime())) {
			throw new BusinessRuleException("Reservation must be in the future.");
		}

	}

	/**
	 * Get slot duration out of User requested, Restaurant space level configuration
	 * or System default.
	 * 
	 * @param reservation
	 * @param space
	 */
	private void determineDurationAndUpdateEndTime(Reservation reservation, Space space) {
		int partyDuration = configuredDefaultDuration;
		int userRequestedDuration = 0;
		// 1. User provides duration -> Use it if it's >= Space's minimum.
		if (Objects.nonNull(reservation.getEndTime())) {
			userRequestedDuration = (int) getDurationInMinutes(reservation.getStartTime(), reservation.getEndTime());
		}

		// 2. User provides nothing -> Use Space default
		if (Objects.nonNull(space.getSlotDurationMins())) {
			// If exists in Space entity.
			Integer minSpaceDuration = Objects.nonNull(space.getSlotDurationMins()) ? space.getSlotDurationMins() : 0;
			partyDuration = Math.max(userRequestedDuration, minSpaceDuration);
		}
		// 3. System Fallback (Default).
		else {
			partyDuration = Math.max(userRequestedDuration, configuredDefaultDuration);
		}
		// final check, irrespective of the configuration, minimum slot should be 30min.
		partyDuration = partyDuration < 30 ? 30 : partyDuration;

		// Set end time based on the duration.
		LocalDateTime calculatedEndTime = reservation.getStartTime().plusMinutes(partyDuration);
		reservation.setEndTime(calculatedEndTime);

	}
}