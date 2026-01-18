package com.opentable.privatedining.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.opentable.privatedining.model.Reservation;

@Repository
public interface ReservationRepository extends MongoRepository<Reservation, ObjectId> {
	
	List<Reservation> findBySpaceId(UUID spaceId);
	
	@Query("{ 'spaceId': ?0, 'startTime': { $lt: ?2 }, 'endTime': { $gt: ?1 } }")
    List<Reservation> findConcurrentReservations(UUID spaceId, LocalDateTime start, LocalDateTime end);
	
	@Query("{ 'restaurantId': ?0, 'startTime': { '$lt': ?2 }, 'endTime': { '$gt': ?1 } }")
    List<Reservation> findInDateRange(ObjectId restaurantId, LocalDateTime start, LocalDateTime end);
}