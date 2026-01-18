\# ADR 0001: Concurrency Control Strategy



\## Status

Accepted



\## Context

The system must handle high-traffic scenarios where multiple users attempt to book the same space simultaneously. We need to prevent overbooking while maintaining high system throughput.



\## Decision

We will implement \*\*Optimistic Concurrency Control (OCC)\*\* using Spring Data MongoDB's `@Version` field, combined with an application-level \*\*Retry Mechanism\*\* using `@Retryable`.



\## Consequences

\- \*\*Pros:\*\* No database deadlocks; higher performance under low-to-medium contention; simple to implement.

\- \*\*Cons:\*\* In very high contention, multiple retries may occur.

\- \*\*Mitigation:\*\* We implement an exponential backoff strategy to reduce database pressure during retries.

