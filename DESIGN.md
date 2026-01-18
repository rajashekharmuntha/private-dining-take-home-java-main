\# Technical Design: Private Dining Reservation System



\## 1. Business Logic Assumptions

\- \*\*Slotting:\*\* Reservation durations are standardized (30/60 min) to optimize table turnover and simplify capacity math.

\- \*\*Operating Hours:\*\* Validation is enforced at the service layer to ensure `startTime` and `endTime` reside within restaurant bounds.



\## 2. Analytics \& Reporting

To ensure system stability under high load, the following guardrails are implemented:

\- \*\*Date Range Limit:\*\* Analytics queries are restricted to a maximum of 31 days to prevent long-running collection scans.

\- \*\*Pagination/Caps:\*\* Report results are capped at 1000 records to protect the JVM from OutOfMemory (OOM) errors.



\## 3. Data Integrity

\- \*\*Idempotency:\*\* A Compound Unique Index (`customerEmail`, `restaurantId`, `spaceId`, `startTime`) is used to prevent accidental duplicate submissions.

\- \*\*Race Conditions:\*\* Managed via `@Version` increments on the Restaurant/Space aggregate root.



High-Traffic Concurrency Strategy

Explain why you didn't just use a simple if (capacity > 0) check.



Optimistic Locking: Justify using @Version. Explain that it prevents "lost updates" without the performance penalty of database-level row locks (Pessimistic Locking).



Transparent Retries: Explain that Spring Retry provides a seamless UX. If a collision occurs, the system self-heals rather than showing the user an error.



Idempotency: Mention the Compound Unique Index (customerEmail + spaceId + startTime). Justify this as a defense against "double-click" submissions and network retries.





"Assumptions and Guardrails"



"Time Slot" Strategy

Granularity vs. Performance trade-off.Decision: Standardized slot duration (e.g., 60 minutes).Justification: While "free-form" booking (any start/end time) is flexible, fixed slots allow the system to pre-calculate capacity and use indexed lookups. It prevents "fragmentation" of restaurant time where 15-minute gaps are left that no one can book.Scalability: By normalizing all bookings to 30/60m blocks, your reporting queries become $O(1)$ or $O(N)$ lookups instead of complex time-overlap calculations.



"Analytics Safety Rails"

The 31-day and 1000-record limits are excellent production-grade decisions.



Decision: Strict 31-day range and 1000-record pagination/limit for reports.



Justification (The "Denial of Service" Prevention): In high-traffic systems, an "All Time" report can crash the database or cause an OutOfMemory (OOM) error.



Performance: By capping the range, you ensure that the query always hits the startTime index efficiently and returns in predictable time (sub-100ms).

