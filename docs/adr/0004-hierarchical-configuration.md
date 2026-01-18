# ADR 0004: Hierarchical Configuration for Business Rules

## Status
Accepted

## Context
A private dining platform serves diverse restaurants and unique dining spaces. A "one-size-fits-all" approach to operating hours and reservation durations is insufficient. We need a deterministic way to resolve business settings that balances restaurant-specific requirements, system-wide defaults, and user-requested durations.

## Decision
We implemented a **Cascading Fallback Strategy** to resolve configuration values. The resolution logic differs slightly for Operating Hours versus Slot Durations.

### 1. Operating Hours Resolution (Restaurant-Centric)
Operating windows are maintained at the establishment level to define the valid booking range.
* **Primary:** `Restaurant` entity level (Specific to the venue).
* **Secondary:** `application.yml` properties (`reservation.operating-hours.open/close`).
* **Tertiary:** Hard-coded System Defaults (**09:00** to **22:00**) as a final fail-safe.

### 2. Slot Duration Resolution (Space/User-Centric)
The final duration used to calculate the `endTime` is determined by comparing configuration against intent:

1. **Resolve Minimum Configured Duration:**
   * Check **Space Level** configuration (`slotDurationMins`).
   * If null, check **`application.yml`** properties (`reservation.default-duration-mins`).
2. **Compare with User Request:**
   * Calculate `userRequestedDuration` from the `endTime` provided in the payload.
   * **Final Duration** = `Math.max(Minimum Configured, userRequestedDuration)`.
3. **Safety Floor:**
   * Regardless of configuration, the system enforces a minimum of **30 minutes**.



## Justification
1. **Operational Reality:** Operating hours are typically consistent across a restaurant, whereas different spaces (e.g., a large private hall vs. a small chef's table) require different turnover times (Slot Durations).
2. **Business Protection:** Using `Math.max` for duration ensures that users cannot bypass capacity constraints by requesting artificially short durations.
3. **Resilience:** The hierarchy ensures the system remains functional even if a restaurant or space profile is partially incomplete by falling back to global or system defaults.

## Implementation Details
* **Logic:** Centralized within the `ReservationService` using a null-coalescing pattern.
* **End-Time Calculation:** The `endTime` is programmatically calculated and updated on the `Reservation` model before persisting to the database.
* **Validation:** Validation against operating hours is performed *after* the final duration is resolved to ensure the calculated `endTime` does not spill over into closed hours.

## Consequences
* **Pros:** * High degree of customization for restaurant partners.
    * Predictable behavior for missing data.
    * Prevents accidental "short-slotting" that could lead to overbooking.
* **Cons:** * Complexity: Increases the logic required before a simple `save` operation.
    * User Feedback: Users may see their `endTime` adjusted automatically if their requested duration was shorter than the space's minimum requirement.
* **Mitigation:** The API response returns the fully calculated `ReservationDTO` so the caller is immediately informed of the actual booked time slot.
