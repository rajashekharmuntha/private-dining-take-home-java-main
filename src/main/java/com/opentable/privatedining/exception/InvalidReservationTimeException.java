package com.opentable.privatedining.exception;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

import org.bson.types.ObjectId;

public class InvalidReservationTimeException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public InvalidReservationTimeException(String message) {
		super(message);
	}

	public InvalidReservationTimeException(ObjectId restaurantId, UUID spaceId, LocalTime startTime,
			LocalTime endTime) {
		super("Reservations must be between the time slot (" + startTime + " to " + endTime + ") for this slot "
				+ spaceId + " in restaurant " + restaurantId);
	}
}
