# ADR 0002: Idempotency and Duplicate Detection

## Status
Accepted

## Context
In high-traffic environments, users may trigger duplicate reservation requests (e.g., double-clicking a "Book" button) or network retries may resend the same payload. Without protection, this creates duplicate data and incorrect capacity counts.

## Decision
We implemented **Server-Side Idempotency** using a MongoDB **Compound Unique Index** on the `Reservation` collection.

The unique key is defined as:
`{ "customerEmail": 1, "restaurantId": 1, "spaceId": 1, "startTime": 1 }`

## Justification
1. **Data Integrity:** This acts as the "final line of defense" at the database level. It ensures that even during a race condition, the database will reject the duplicate entry.
2. **Performance:** Using an index is faster and more efficient than performing a "check-then-insert" logic in the application code.
3. **Synergy with Locking:** While Optimistic Locking prevents overbooking the space capacity, this index specifically prevents the same user from booking the same slot multiple times.



## Implementation Details
* **Spring Data Integration:** Enabled via `@CompoundIndex` in the Java model and `auto-index-creation: true` in configuration.
* **Exception Handling:** The service layer catches `DuplicateKeyException` and translates it into a meaningful `409 Conflict` response for the client.

## Consequences
* **Pros:** Guaranteed uniqueness and protection against race conditions that might bypass service-layer validation.
* **Cons:** Minimal write overhead for index maintenance.
* **Note:** For the current project scope, using the combination of email, space, and time is a sufficient constraint to define a unique booking attempt.
