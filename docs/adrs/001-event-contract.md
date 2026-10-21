# ADR 001: Kafka event contracts and publish-after-commit

**Status:** Accepted (built in Lab 49, extended with the lifecycle event in the customer-management work).
**Owner:** Backend (Keifer leads; see [team roles](../team-roles.md)).

## Context

When an agent records an interaction, the API saves it and then announces it on Kafka. We needed one event shape before anyone wrote publish code, and the sources disagreed. Lab 49 Step 2 has `eventId`, `actor`, and an integer `eventVersion`; the original starter record had no `eventId` or `actor` and stored the version as the string `"1"`. We went with the course shape, because without `eventId` a consumer can't spot a duplicate delivery.

Later, customer status changes (Ravi `PROSPECT -> ACTIVE`, any status `-> CLOSED`) also needed an audit trail, so a second event was added with the same rules.

## Decision

### Event 1: interaction recorded

- **Topic:** `crm.customer.interactions.v1` (setting `crm.kafka.topic`)
- **Key:** the customer's public id (`CUS-1001`), so one customer's events stay on one partition and in order.
- **Value:** JSON `CustomerInteractionRecordedV1` (`backend/.../messaging/CustomerInteractionRecordedV1.java`).

| Field | Type in Java | Notes |
| --- | --- | --- |
| `eventId` | `UUID` | New for every event. The consumer dedupes on it |
| `eventType` | `String` | Always `CustomerInteractionRecorded` |
| `eventVersion` | `int` | `1` |
| `occurredAt` | `Instant` (UTC) | Set when the event is built, on the request thread, just before the commit |
| `correlationId` | `String` | Same value stored on the row: `X-Correlation-Id` header, else the body's `correlationId`, else `lab-request-001` |
| `actor` | `String` | The JWT subject of the caller (`agent` or `admin`) |
| `customerId` | `String` | Public id like `CUS-1001`. Never the internal database key |
| `interactionId` | `String` | The interaction UUID, written as text so non-Java consumers don't need a UUID type |
| `interactionType` | `String` | `CALL`, `EMAIL`, `NOTE`, or `MEETING` |

The summary (the note text) is **not** in the event, and the publisher never logs it. Events carry identifiers only.

Example (values are synthetic):

```json
{
  "eventId": "6f1c2a9e-3b4d-4c8e-9a51-2d7f0b8e4c13",
  "eventType": "CustomerInteractionRecorded",
  "eventVersion": 1,
  "occurredAt": "2026-10-20T14:05:00Z",
  "correlationId": "lab-request-001",
  "actor": "agent",
  "customerId": "CUS-1001",
  "interactionId": "0b7e8f44-1d2c-4a6b-8e3f-5c9a7d1e2f60",
  "interactionType": "CALL"
}
```

Team to confirm: the exact JSON form of `occurredAt` (ISO text or a number) depends on the serializer settings. Check it once with the Kafka console consumer and update this example.

### Event 2: customer status changed

- **Topic:** `crm.customer.lifecycle.v1` (setting `crm.kafka.lifecycle-topic`), keyed by customer public id.
- **Value:** `CustomerStatusChangedV1`: `eventId`, `eventType` (`CustomerStatusChanged`), `eventVersion` (`1`), `occurredAt`, `correlationId`, `actor`, `customerId`, `fromStatus`, `toStatus`.
- Published only when the status really changes, from `PUT /api/v1/customers/{id}` or `DELETE /api/v1/customers/{id}` (which closes the customer). No names or other personal data.

### Rules for both events

**Publish after commit.** The service saves the row inside a `@Transactional` method. The publisher registers an after-commit hook (`TransactionSynchronization.afterCommit`), so Kafka only hears about rows that actually committed. A validation error (`400`) or an unknown customer (`CUS-9999`, `404`) publishes nothing. Tests: `InteractionEventPublisherTest.publish_insideTransaction_waitsForAfterCommit`, `CustomerLifecycleEventPublisherTest.rolledBackTransaction_neverPublishes`, `InteractionServiceTest.create_unknownCustomer_neverPersistsOrPublishes`.

**Consumers are idempotent.** `InteractionEventConsumer` (group `crm-interactions-audit`) and `CustomerLifecycleEventConsumer` (group `crm-customer-lifecycle-audit`) write one row per `eventId` into `processed_event` (Flyway V2). A redelivered event finds its row and is skipped. Test: `InteractionEventConsumerIT.duplicateDelivery_appliesOnlyOnce`.

**Bounded retries, then a dead-letter topic.** A failing record is retried twice, 500 ms apart, then copied to `<topic>.DLT` (`crm.customer.interactions.v1.DLT`, `crm.customer.lifecycle.v1.DLT`). An unknown `eventVersion` can never succeed, so it goes straight to the DLT. Test: `InteractionEventConsumerIT.unsupportedVersion_goesToDeadLetterTopic_andIsNotApplied`.

**Versioning.** Adding an optional field stays V1. Renaming or removing a field, or changing a type, means a new `...V2` event on a new `.v2` topic, with both running until consumers move.

## Consequences

- PostgreSQL stays the system of record. A lost event never means a wrong row.
- **Gap we accept:** if Kafka is down right after the commit, the row exists but its event can be lost. The send is fire-and-forget and there is no outbox yet. See risk R-06 in the [risk register](../risk-register.md). Team to confirm the team accepts this for the capstone.
- Ordering only holds per customer, which is all CAP-12 needs.
- The consumers record an audit row only. They don't trigger any other side effect yet.

## Rejected

- **Outbox table.** Safer (the event is saved in the same transaction and relayed later), but it adds a table, a poller, and tests we couldn't fit into the week. It's the upgrade path.
- **Publish before or inside the transaction.** Could announce a row that then rolls back.
- **Keep the starter shape.** Without `eventId`, consumers can't dedupe.
