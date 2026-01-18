# System Design and Architecture

This document outlines the architectural patterns and design principles used in the Private Dining Reservation System.

## 1. Architectural Decisions (ADRs)
For detailed justifications on specific technical choices, please refer to our Architecture Decision Records:

* [ADR 0001: Concurrency Control Strategy](./docs/adr/0001-concurrency-control.md) - Focuses on Optimistic Locking and Spring Retry.
* [ADR 0002: Idempotency and Duplicate Detection](./docs/adr/0002-idempotency.md) - Explains the Compound Unique Index strategy.
* [ADR 0003: Persistence Technology Choice](./docs/adr/0003-persistence-choice.md) - Justifies the use of MongoDB and the Document Model.
* [ADR 0004: Hierarchical Configuration](./docs/adr/0004-hierarchical-config.md) - Details the cascading fallback logic for business rules.

---

## 2. Core Business Logic: Reservation V2
The V2 implementation introduces an **Optimized Capacity Engine**. Unlike standard reservation systems that block a whole "table," this system treats space capacity as a fluid resource.



### The Booking Lifecycle:
1.  **Resolution:** System resolves operating hours (Restaurant-level) and slot duration (Space-level).
2.  **Projection:** The system calculates the `endTime` based on the hierarchy defined in ADR 0004.
3.  **Concurrency Check:** Using a non-blocking query, the system sums the `partySize` of all existing reservations that overlap with the requested time window.
4.  **Validation:** If `current_bookings + requested_size <= total_capacity`, the booking proceeds.
5.  **Persistence:** The reservation is saved using a Version Check to ensure the capacity didn't change during the calculation.

---

## 3. Data Modeling
We utilize a denormalized **Document-per-Restaurant** model to optimize read performance.

* **Restaurant Collection:** Contains metadata and a nested list of `Space` objects. This allows us to fetch all room capacities for a restaurant in a single disk seek.
* **Reservation Collection:** Scalable flat collection indexed for rapid overlap queries.

---

## 4. Error Handling Strategy
The system uses a **Global Exception Handler** (`@ControllerAdvice`) to map technical exceptions to meaningful REST responses:

| Exception | HTTP Status | Business Meaning |
| :--- | :--- | :--- |
| `InsufficientCapacityException` | 409 Conflict | The requested party size exceeds available seats. |
| `DuplicateKeyException` | 409 Conflict | This exact user/time/space combination already exists. |
| `InvalidPartySizeException` | 400 Bad Request | Request is outside the Min/Max capacity of the space. |
| `RestaurantNotFoundException` | 404 Not Found | The target resource does not exist. |

---

## 5. Future Scalability
While current logic is handled in the Service layer, the architecture is prepared for:
* **Caching:** Resolved configurations (ADR 0004) can be cached in Redis.
* **Sharding:** The MongoDB collection is partitioned by `restaurantId` to support horizontal scaling.
