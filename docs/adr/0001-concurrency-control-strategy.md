# ADR 0001: Concurrency Control Strategy

## Status
Accepted

## Context
The system must handle high-traffic scenarios where multiple users attempt to book the same limited capacity simultaneously. We need a strategy to prevent overbooking (ensuring the sum of party sizes never exceeds space capacity) while maintaining high system throughput and avoiding database deadlocks.

## Decision
We implemented **Optimistic Concurrency Control (OCC)** using Spring Data MongoDB's `@Version` field, combined with an application-level **Retry Mechanism** using Spring Retry (`@Retryable`).



## Justification
1. **Throughput:** Unlike Pessimistic Locking (which locks rows and makes other users wait), Optimistic Locking allows the database to process requests without blocking, significantly improving performance in a web environment.
2. **Conflict Handling:** By using `@Version`, MongoDB detects if a document was modified by another thread between the time we read it and the time we attempt to save.
3. **Resilience:** The `@Retryable` mechanism automatically handles `OptimisticLockingFailureException`. If a collision occurs, the system transparently retries the business logic (re-calculating available capacity) to ensure the booking can still succeed if seats are available.

## Implementation Details
* **Model:** Added a `Long version` field annotated with `@Version` to the core entities.
* **Service:** Wrapped the `createReservationV2` method with `@Retryable` to handle version conflicts.
* **Validation:** The capacity check is re-executed on every retry to ensure data consistency against the latest database state.

## Consequences
* **Pros:** * Eliminates the risk of database deadlocks.
    * Higher performance under low-to-medium contention.
    * Maintains a clean, stateless service layer.
* **Cons:** In scenarios of extreme contention for the same time slot, a request may fail after exhausting the maximum retry attempts.
