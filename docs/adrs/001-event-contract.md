# ADR 001: Kafka event contracts and publish after commit

Status: Accepted

## Context

When an agent records an interaction, the API saves it and then announces it on Kafka. The Lab 49 guide and the starter code disagreed on the event shape. The starter had no `eventId`, and without one a consumer can't tell a redelivery from a new event. Later, customer status changes needed the same kind of audit trail.

## Decision

We publish two JSON events, both keyed by the customer's public id (`CUS-1001`):

- `CustomerInteractionRecordedV1` on `crm.customer.interactions.v1`
- `CustomerStatusChangedV1` on `crm.customer.lifecycle.v1`, only when the status really changes

Every event has `eventId`, `eventType`, `eventVersion` (1), `occurredAt`, `correlationId`, `actor` and `customerId`. The interaction event adds `interactionId` and `interactionType`. The status event adds `fromStatus` and `toStatus`. We don't put the note text or names in events.

The publisher sends only after the transaction commits, so a `400` or a `404` publishes nothing. Consumers store each `eventId` in `processed_event` and skip repeats. A record that fails three times (first try plus two retries), or has an unknown version, goes to the topic's `.DLT`. A breaking change gets a new `V2` event on a `.v2` topic.

Alternatives: an outbox table is safer, but it was more than we could build this week, so it's our upgrade path. Publishing inside the transaction could announce a row that later rolls back.

## Consequences

PostgreSQL stays the source of truth. If Kafka goes down right after a commit, the row is saved but its event can be lost. We accept that for the capstone (R-8 in the [risk register](../risk-register.md)). Ordering only holds per customer, which is all CAP-12 needs.

Tests: `InteractionEventPublisherTest`, `InteractionEventConsumerIT`, `CustomerLifecycleEventPublisherTest`, `CustomerLifecycleEventConsumerIT`.
