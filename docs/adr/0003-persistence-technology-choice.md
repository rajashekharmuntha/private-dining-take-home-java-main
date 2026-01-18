# ADR 0003: Persistence Technology Choice

## Status
Accepted

## Context
The platform requires a storage solution capable of handling high-volume reservation writes and highly flexible metadata for dining spaces. Traditional fixed schemas would require frequent migrations as restaurant features (e.g., specific space amenities or variable capacity rules) evolve.

## Decision
We chose **MongoDB** as the primary persistence layer over a traditional RDBMS.

## Justification
1. **Document Model:** MongoDB allows nesting `Spaces` directly within `Restaurant` documents. This enables single-read access to a restaurant's full configuration, eliminating the need for complex multi-table JOINs and reducing latency.
2. **Horizontal Scaling:** Native sharding support satisfies the "high-traffic" requirement, allowing the system to scale out across multiple nodes as the volume of global reservations increases.
3. **Performance & Atomicity:** MongoDB provides atomic updates at the document level. For our use case, updating a single restaurant document (including its nested space availability) is faster and more efficient than coordinating transactions across multiple relational tables.
4. **Schema Flexibility:** The JSON-like structure allows different restaurants to have different space attributes without impacting the entire database schema.



## Implementation Details
* **Driver:** Spring Data MongoDB for seamless integration with the Spring Boot ecosystem.
* **Concurrency:** Implementation of `@Version` fields for optimistic locking to handle high-frequency concurrent reservation attempts.
* **Indexing:** Utilization of compound unique indexes (e.g., `customerEmail`, `restaurantId`, `spaceId`, `startTime`) to enforce data integrity at the database level.
