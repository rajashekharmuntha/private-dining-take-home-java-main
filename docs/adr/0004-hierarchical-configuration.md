\# ADR 0004: Hierarchical Configuration for Business Rules



\## Status

Accepted



\## Context

A private dining platform serves diverse restaurants. A "one-size-fits-all" approach to operating hours and slot durations is insufficient. For example, a "Rooftop Bar" space may have different operating hours than the "Main Dining Room" within the same restaurant. We need a way to resolve these settings that is both flexible for the user and safe for the system.



\## Decision

We implemented a \*\*Hierarchical Fallback (Cascading) Strategy\*\* for resolving business configurations, specifically for opening/closing hours and reservation slot durations.



\### Lookup Order (Precedence):

1\. \*\*Space Level:\*\* Most specific. If defined, this overrides everything else.

2\. \*\*Restaurant Level:\*\* If the Space has no specific config, the parent Restaurant's settings are used.

3\. \*\*Global Level:\*\* If neither is defined, the system falls back to `application.yml` defaults.



\## Justification

1\. \*\*Flexibility:\*\* Allows restaurant owners to manage unique spaces (e.g., late-night rooms) without forcing the entire establishment to follow the same schedule.

2\. \*\*Robustness:\*\* By having a "Global Level" fallback, the system avoids `NullPointerExceptions` or undefined behavior if a restaurant profile is incomplete.

3\. \*\*Code Maintainability:\*\* Centralizes the "Resolution Logic" in a single helper or service method, keeping the core `ReservationService` clean.







\## Implementation Details

\- \*\*Logic:\*\* The resolution uses a "Null-Coalescing" pattern (using Java `Optional`).

\- \*\*Validation:\*\* Once the configuration is resolved, the requested `startTime` and `endTime` are validated against the resolved hours.

\- \*\*Reporting:\*\* Analytics use the resolved slot duration to group data consistently.



\## Consequences

\- \*\*Pros:\*\* High degree of customization for partners; system-wide consistency via global defaults.

\- \*\*Cons:\*\* Slightly more complex data fetching as the system must check three potential sources.

\- \*\*Mitigation:\*\* Results of the resolution can be cached per request context to minimize overhead.

