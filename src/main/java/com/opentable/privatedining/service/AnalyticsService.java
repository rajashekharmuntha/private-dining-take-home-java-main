package com.opentable.privatedining.service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.opentable.privatedining.dto.OccupancyReportDTO;
import com.opentable.privatedining.dto.OccupancySnapshotDTO;
import com.opentable.privatedining.exception.BusinessRuleException;
import com.opentable.privatedining.exception.RestaurantNotFoundException;
import com.opentable.privatedining.exception.SpaceNotFoundException;
import com.opentable.privatedining.model.Reservation;
import com.opentable.privatedining.model.Restaurant;
import com.opentable.privatedining.model.Space;
import com.opentable.privatedining.repository.ReservationRepository;
import com.opentable.privatedining.repository.RestaurantRepository;

@Service
public class AnalyticsService {
	

	// Operating windows constants.
	private LocalTime globalOpeningTime;
	private LocalTime globalClosingTime;

	private final RestaurantRepository restaurantRepository;
	private final ReservationRepository reservationRepository;

	public AnalyticsService(RestaurantRepository restaurantRepository, ReservationRepository reservationRepository,
			@Value("${reservation.operating-hours.open:09:00}") String open,
			@Value("${reservation.operating-hours.close:22:00}") String close,
			@Value("${reservation.default-duration-mins:90}") int duration) {
		this.restaurantRepository = restaurantRepository;
		this.reservationRepository = reservationRepository;
		try {
			this.globalOpeningTime = LocalTime.parse(open);
			this.globalClosingTime = LocalTime.parse(close);
		} catch (DateTimeParseException e) {
			this.globalOpeningTime = LocalTime.of(9, 0);
			this.globalClosingTime = LocalTime.of(22, 0);
		}
	}

	public OccupancyReportDTO getOccupancyReport(ObjectId restaurantId, UUID spaceId, LocalDateTime start,
			LocalDateTime end, Integer interval) {

		// Either interval should be 30 or 60, anything else defaulted to 30 minutes.
		int effectiveInterval = (interval == 60) ? 60 : 30;

		// Validating date time ranges. Validation and avoiding DOS.
		validateDateRange(start, end, effectiveInterval);

		// Fetch the Restaurant and existence check.
		Restaurant restaurant = restaurantRepository.findById(restaurantId)
				.orElseThrow(() -> new RestaurantNotFoundException(restaurantId));

		List<Reservation> allReservations = null;
		// Filter the spaces based on the optional spaceId.
		List<Space> spacesToAnalyze;
		if (spaceId != null) {
			spacesToAnalyze = restaurant.getSpaces().stream().filter(s -> s.getId().equals(spaceId)).toList();
			if (spacesToAnalyze.isEmpty()) {
				throw new SpaceNotFoundException(restaurantId, spaceId);
			}
			allReservations = reservationRepository.findConcurrentReservations(spaceId, start, end);
		} else {
			// In case no space id mentioned, all the spaces.
			spacesToAnalyze = restaurant.getSpaces();
			allReservations = reservationRepository.findInDateRange(restaurantId, start, end);
		}

		// 3. Generate snapshots
		List<OccupancySnapshotDTO> intervals = new ArrayList<>();

		// Pre-calculate Total Capacity for the report (constant across all snapshots).
		int totalReportCapacity = spacesToAnalyze.stream().mapToInt(Space::getMaxCapacity).sum();

		// Create a Set of valid Space IDs to filter reservations quickly in memory.
		Set<UUID> validSpaceIds = spacesToAnalyze.stream().map(Space::getId).collect(Collectors.toSet());

		LocalTime openingTime = Objects.nonNull(restaurant.getOpeningTime()) ? restaurant.getOpeningTime()
				: globalOpeningTime;
		LocalTime closingTime = Objects.nonNull(restaurant.getClosingTime()) ? restaurant.getClosingTime()
				: globalClosingTime;

		LocalDateTime cursor = start;

		while (!cursor.isAfter(end) && Objects.nonNull(allReservations)) {

			LocalTime currentT = cursor.toLocalTime();

			// --- DAILY WINDOW CHECK ---
			// If current time is before opening, jump to opening of THE SAME day
			if (currentT.isBefore(openingTime)) {
				cursor = cursor.with(openingTime);
			}
			// If current time is AFTER closing, jump to opening of THE NEXT day
			else if (currentT.isAfter(closingTime)) {
				cursor = cursor.plusDays(1).with(openingTime);
				// Important: After jumping, check if we've now exceeded the user's total 'end'
				// date
				if (cursor.isAfter(end))
					break;
			}

			// 2. In-Memory Filter (Zero DB calls inside this loop!)
			LocalDateTime snapshotTime = cursor;

			// Filter reservations that are active during THIS specific snapshot time
			// and belong to the spaces we are currently analyzing
			int currentHeadcount = allReservations.stream().filter(r -> validSpaceIds.contains(r.getSpaceId()))
					.filter(r -> !r.getStartTime().isAfter(snapshotTime) && r.getEndTime().isAfter(snapshotTime))
					.mapToInt(Reservation::getPartySize).sum();

			// Calculate occupancy percentage
			double percentage = (totalReportCapacity > 0) ? (double) currentHeadcount / totalReportCapacity * 100 : 0;

			// Round to 2 decimal places for a clean API response
			double roundedPct = Math.round(percentage * 100.0) / 100.0;

			intervals.add(new OccupancySnapshotDTO(snapshotTime, currentHeadcount, totalReportCapacity, roundedPct));

			// Increment by the user-requested (or defaulted) interval.
			cursor = cursor.plusMinutes(effectiveInterval);
		}
		

		return new OccupancyReportDTO(restaurantId, spaceId, intervals);
	}

	public OccupancyReportDTO getOccupancyReport(ObjectId restaurantObjectId, LocalDateTime start, LocalDateTime end,
			Integer interval) {
		return getOccupancyReport(restaurantObjectId, null, start, end, interval);
	}

	private void validateDateRange(LocalDateTime start, LocalDateTime end, int interval) {

		// Range Validation.
		if (start.isAfter(end)) {
			throw new BusinessRuleException("Start date must be before end date.");
		}

		// Time Span Limit, One month (31 Days).
		if (ChronoUnit.DAYS.between(start, end) > 31) {
			throw new BusinessRuleException("The requested range exceeds the 31-day limit.");
		}

		// Allowing maximum 1000 data points.
		long totalMinutes = ChronoUnit.MINUTES.between(start, end);
		long expectedDataPoints = totalMinutes / interval;

		if (expectedDataPoints > 1000) {
			throw new BusinessRuleException(
					"This request generates too many data points. Please increase your interval to 60 minutes or shorten your date range.");
		}
	}

}