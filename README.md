# Private Dining Reservation System V2

## Features
-  Reservation availability management and optimized space utilization.
   -   Operating Windows: Enforcing reservations within defined hours (e.g., 9:00 AM to 10:00 PM).
   -   Slot Optimization: Utilizing time-block intervals to maximize table turnover and capacity.
   -   Flexible Capacity Management: Ensuring bookings respect the minimum and maximum capacity of a space while allowing concurrent reservations if the total headcount remains within limits.
   -   Optimistic Concurrency Control with Automatic Retries.
   -   Idempotency protection via Unique Database Constraints.
- Reporting and Analytics
   -   Provide restaurant owners with clear visibility into occupancy trends over time.
   -   Restaurant owners can use this analytics data to plan their restaurant avaibility more efficiently.
   -   High-performance Analytics with query guardrails.

## Setup & Running
1. **Prerequisites:** Java 17+, Maven, MongoDB.
2. **Bulding Application:**
   `mvn clean install`
3. **Running Test cases**
    `mvn test`
4. **Run Application:**
   The project can be started using `mvn spring-boot:run`, and runs with an embedded instance of MongoDB

## Testing Concurrency
I have included a specific integration test to demonstrate the Retry mechanism:
`mvn test -Dtest=ReservationServiceConcurrencyTest`

## API Documentation Swagger
http://<<hostname>>:<<port>>/swagger-ui/index.html (Here port is 8081)

## V2 APIs
- post /v2/reservations
- get /v2/analytics/restaurant/{restaurantId}/space/{spaceId}/occupancy
- get /v2/analytics/restaurant/{restaurantId}/occupancy

- 
sequenceDiagram
    participant User1
    participant User2
    participant Service
    participant MongoDB

    User1->>Service: Create ReservationV2
    Service->>MongoDB: Fetch Restaurant (version = 1)
    Service->>Service: Validate capacity

    Note over Service,MongoDB: Concurrent request by User2\nRestaurant version changes 1 → 2

    User2->>Service: Create ReservationV2
    Service->>MongoDB: Save ReservationV2 & Update Restaurant
    Service-->>User2: 201 Created

    Service->>MongoDB: Save ReservationV2 & Update Restaurant\n(expect version = 1)
    MongoDB-->>Service: OptimisticLockingFailureException

    Service->>Service: Retry attempt #1
    Service->>MongoDB: Fetch Restaurant (version = 2)
    Service->>Service: Validate capacity
    Service->>MongoDB: Save ReservationV2 for User1\nUpdate Restaurant (new version = 3)

    MongoDB-->>Service: Save successful
    Service-->>User1: 201 Created



AI Disclosure: > "I utilized AI (Gemini) to assist with generating initial test case templates, refining Spring Retry syntax, and comparing concurrency strategies (Optimistic Locking vs. Unique Indexes). All core business logic, comprehensive test suites, and final system implementations were developed, debugged, and verified by me to ensure full adherence to project requirements and system correctness."
