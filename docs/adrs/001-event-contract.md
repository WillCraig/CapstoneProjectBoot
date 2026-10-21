# ADR 001: Event contract for recorded interactions

**Status:** Proposed. **FILL IN:** confirm the frozen field list with the instructor, or note that the team froze it without an answer.

## Context

When an agent records an interaction, the API saves it and then announces it on Kafka. We need one event shape before anyone writes publish code, and the sources disagree. Lab 49 Step 2 has `eventId`, `actor`, and an integer `eventVersion`. The starter record in `backend/.../messaging/CustomerInteractionRecordedV1.java` has no `eventId` or `actor` and stores the version as the string `"1"`. We go with the course shape.

## Decision

- **Topic:** `crm.customer.interactions.v1`
- **Message key:** the customer id string (`CUS-1001`), so one customer's events stay on one partition and in order.
- **Value:** JSON `CustomerInteractionRecordedV1`.

| Field | JSON type | Notes |
| --- | --- | --- |
| `eventId` | string (UUID) | New for every event. Consumers dedupe on it |
| `eventType` | string | Always `CustomerInteractionRecorded` |
| `eventVersion` | number | `1` |
| `occurredAt` | string (ISO-8601, UTC) | When the row was committed |
| `correlationId` | string | From the request; defaults to `lab-request-001` |
| `actor` | string | Who recorded it. **FILL IN:** value to use until real JWT lands (suggest the demo user's subject) |
| `customerId` | string | Public id like `CUS-1001`. Never the database key or a UUID |
| `interactionId` | string (UUID) | Same as `interaction_id` in PostgreSQL |
| `interactionType` | string | `CALL`, `EMAIL`, `NOTE`, or `MEETING` |

```json
{
  "eventId": "6f1c2a9e-3b4d-4c8e-9a51-2d7f0b8e4c13",
  "eventType": "CustomerInteractionRecorded",
  "eventVersion": 1,
  "occurredAt": "2026-10-20T14:05:00Z",
  "correlationId": "lab-request-001",
  "actor": "demo-agent",
  "customerId": "CUS-1001",
  "interactionId": "0b7e8f44-1d2c-4a6b-8e3f-5c9a7d1e2f60",
  "interactionType": "CALL"
}
```

**Publish after commit.** The service saves the interaction, commits, then publishes. A validation failure or an unknown customer (`CUS-9999`, 404) publishes nothing. No outbox for the capstone.

**When Kafka is down.** The row is already committed, so the API still returns `201`. The failed publish is logged at ERROR with the interaction id and correlation id so we can find it. **FILL IN:** confirm the team accepts this gap (the instructor question on after-commit vs outbox covers it too).

**Versioning.** Adding an optional field stays V1. Renaming or removing a field, or changing a type, means a new `CustomerInteractionRecordedV2` on `crm.customer.interactions.v2`, with both running until consumers move.

**Consumer.** **FILL IN:** what the consumer does with a good event, or whether a log-line consumer is an accepted substitute (instructor). Whatever it does, it has to be idempotent on `eventId`, because Kafka can deliver twice.

## Consequences

- The starter record changes before Lab 49 publish work: add `eventId` and `actor`, make `eventVersion` an `int`. `customerId` stays a `String`.
- PostgreSQL stays the system of record. A lost event never means a wrong row.
- Events can go missing while Kafka is down. An outbox is the upgrade if that turns out to matter.
- Ordering only holds per customer, which is all CAP-12 needs.

## Rejected

- **Outbox table.** Safer, but it adds a table, a poller, and tests we can't fit into the week.
- **Publish before or inside the transaction.** Can announce a row that then rolls back.
- **Keep the starter shape.** Without `eventId`, consumers can't dedupe.
