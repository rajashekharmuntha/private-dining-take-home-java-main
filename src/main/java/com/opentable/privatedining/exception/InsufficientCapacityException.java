package com.opentable.privatedining.exception;

import java.time.LocalDateTime;
import java.util.UUID;

import org.bson.types.ObjectId;

public class InsufficientCapacityException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public InsufficientCapacityException(String message) {
		super(message);
	}

	public InsufficientCapacityException(ObjectId restaurantId, UUID spaceId, LocalDateTime startTime,
			LocalDateTime endTime, int partySize) {
		super("Reservation conflict: the requested time slot (" + startTime + " to " + endTime
				+ ") with party size ("+ partySize + ") exceeds remaining capacity for this slot " + spaceId + " in restaurant " + restaurantId);
	}

}
