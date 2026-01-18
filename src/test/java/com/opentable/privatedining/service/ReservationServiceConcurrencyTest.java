package com.opentable.privatedining.service;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.opentable.privatedining.model.Reservation;
import com.opentable.privatedining.model.Restaurant;
import com.opentable.privatedining.model.Space;
import com.opentable.privatedining.repository.ReservationRepository;
import com.opentable.privatedining.repository.RestaurantRepository;

@SpringBootTest
class ReservationServiceConcurrencyTest {

    private static final Logger log = LoggerFactory.getLogger(ReservationServiceConcurrencyTest.class);

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private RestaurantService restaurantService;

    @Autowired
    private ReservationRepository reservationRepository;
    
    @Autowired
    private RestaurantRepository restaurantRepository;

    @Test
    void createReservation_ShouldHandleConcurrencyWithRetry() throws InterruptedException {
        // Clean up before test
        reservationRepository.deleteAll();

        int numberOfThreads = 2;
        ExecutorService service = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numberOfThreads);

        // Setup Restaurant in DB
        Restaurant restaurant = new Restaurant("Sunset Grill", "45 Ocean Dr", "Seafood", 150);
        ObjectId restaurantId = new ObjectId();
        restaurant.setId(restaurantId);
        
        Space space = new Space("Main Hall", 10, 100);
        UUID spaceId = UUID.randomUUID();
        space.setId(spaceId);
        restaurant.setSpaces(List.of(space));
        
        restaurantRepository.save(restaurant);
        
        long count = reservationRepository.count();

      
        LocalDateTime startTime = LocalDateTime.now().plusDays(1).withHour(18).withMinute(0);

        Reservation res1 = createTestReservation("user1@test.com", 10, restaurantId, spaceId, startTime);
        Reservation res2 = createTestReservation("user2@test.com", 10, restaurantId, spaceId, startTime);

        // Execute threads
        service.execute(() -> runTask(latch, doneLatch, res1));
        service.execute(() -> runTask(latch, doneLatch, res2));

        latch.countDown(); // Start
        doneLatch.await(); // Wait

        assertEquals(count+2, reservationRepository.count(), "Both should succeed via retry");
    }

    private void runTask(CountDownLatch latch, CountDownLatch doneLatch, Reservation res) {
        try {
            latch.await();
            reservationService.createReservationV2(res);
            log.info("Successfully saved reservation for: {}", res.getCustomerEmail());
        } catch (Exception e) {
            log.error("Failed for {}: {}", res.getCustomerEmail(), e.getMessage());
        } finally {
            doneLatch.countDown();
        }
    }

    private Reservation createTestReservation(String email, int size, ObjectId rId, UUID sId, LocalDateTime start) {
        Reservation r = new Reservation();
        r.setCustomerEmail(email);
        r.setPartySize(size);
        r.setRestaurantId(rId);
        r.setSpaceId(sId);
        r.setStartTime(start);
        r.setEndTime(start.plusHours(2));
        r.setStatus("CONFIRMED");
        return r;
    }
}