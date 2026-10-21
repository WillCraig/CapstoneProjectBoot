# ADR 001: Kafka events after commit

## Context
When an agent records an interaction we save it and also tell Kafka about it. We needed one event shape, and it needed an `eventId` so consumers can spot duplicates.

## Decision
We publish `CustomerInteractionRecordedV1` and `CustomerStatusChangedV1`, keyed by customer id, only after the DB transaction commits. Consumers skip event ids they've already seen, and bad events go to a `.DLT` topic after 3 tries.

## Consequences
The database is always the source of truth. With no outbox table, an event can get lost if Kafka goes down right after a commit. We're OK with that for the capstone.
