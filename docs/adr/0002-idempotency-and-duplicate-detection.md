\# ADR 0002: Idempotency and Duplicate Detection



\## Status

Accepted



\## Context

In high-traffic environments, users may accidentally trigger multiple reservation requests (e.g., double-clicking the "Book" button). Additionally, network-level retries from the client or infrastructure could result in the same payload being sent multiple times. Without protection, this leads to duplicate data and inconsistent capacity counts.



\## Decision

We will enforce \*\*Server-Side Idempotency\*\* using a MongoDB \*\*Compound Unique Index\*\* on the `Reservation` collection.



The unique key is defined as:

`{ "customerEmail": 1, "restaurantId": 1, "spaceId": 1, "startTime": 1 }`



\## Justification

1\. \*\*Data Integrity:\*\* This index acts as a "final line of defense" at the database level, ensuring that even if application-level checks fail during a race condition, the database will reject the duplicate entry.

2\. \*\*Performance:\*\* Checking for existing records via a unique index is significantly faster than performing a `find()` followed by an `insert()` in the application code.

3\. \*\*Optimistic Locking Synergy:\*\* While Optimistic Locking handles the \*capacity\* collision (two people taking the last seat), this index handles the \*identity\* collision (the same person taking the same seat twice).







\## Implementation Details

\- \*\*Exception Handling:\*\* The service layer will catch `org.springframework.dao.DuplicateKeyException`.

\- \*\*User Feedback:\*\* Instead of a generic 500 error, the system will translate this exception into a `BusinessRuleException` with a `409 Conflict` or `400 Bad Request` status, informing the user that the reservation already exists.



\## Consequences

\- \*\*Pros:\*\* Guaranteed data uniqueness; protects against race conditions that bypass service-layer validation.

\- \*\*Cons:\*\* Index maintenance has a negligible write overhead.

\- \*\*Note:\*\* If a user wishes to book the same room for the same time for two different parties, they would need to use a different email address, or the system would need to include a `correlationId`. For the current scope, the email-based constraint is sufficient.

