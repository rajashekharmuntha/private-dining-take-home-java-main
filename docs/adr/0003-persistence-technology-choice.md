\# ADR 0003: Persistence Technology Choice



\## Status

Accepted



\## Context

The application requires a scalable storage solution that handles flexible metadata for dining spaces and high-volume write operations.



\## Decision

We chose \*\*MongoDB\*\* over a traditional RDBMS.



\## Justification

1\. \*\*Document Model:\*\* Allows nesting `Spaces` within `Restaurant` documents, enabling single-read access to full availability data.

2\. \*\*Horizontal Scaling:\*\* Native sharding support satisfies the "high-traffic" requirement.

3\. \*\*Performance:\*\* Atomic document updates are faster than multi-table RDBMS joins for this specific use case.

