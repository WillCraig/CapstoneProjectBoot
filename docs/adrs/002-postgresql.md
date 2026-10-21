# ADR 002: PostgreSQL as the system of record

**Status:** Accepted (built in Labs 49–50).
**Owner:** Backend (Keifer leads; see [team roles](../team-roles.md)).

## Context

Customers and interactions have to survive restarts, and they're the truth if Kafka and the database ever disagree. The course stack names PostgreSQL, and local dev runs `postgres:16` in `compose.yaml`.

## Decision

PostgreSQL 16 through Spring Data JPA, with **Flyway owning the schema**. Hibernate runs `ddl-auto: validate`, so it checks the schema at startup but never changes it.

| Migration | What it does |
| --- | --- |
| `V1__crm_schema.sql` | `customer` (internal `BIGSERIAL` key plus unique `public_id` like `CUS-1001`, `status` CHECK `PROSPECT/ACTIVE/CLOSED`) and `customer_interaction` (`UUID` key, `interaction_type` CHECK `CALL/EMAIL/NOTE/MEETING`, `summary` up to 1024 characters, `correlation_id`, `TIMESTAMPTZ` times). Index on `(customer_id, created_at DESC)` for the newest-first timeline. Seeds only the synthetic `CUS-1001` Amina Khan (ACTIVE) and `CUS-1002` Ravi Singh (PROSPECT) |
| `V2__processed_event.sql` | `processed_event`, one row per consumed `eventId`, so redelivered Kafka events are skipped |
| `V3__interaction_soft_delete.sql` | Adds `deleted_at` so deleting an interaction hides it but keeps the audit row |

Customers are never hard-deleted either: `DELETE /api/v1/customers/{id}` sets `status = CLOSED`, so interactions never point at a missing customer.

The API only ever shows the public id (`CUS-1001`). The internal numeric key stays inside the database.

## Rejected

- **Oracle.** Not the taught path, licensed, and its types (`VARCHAR2`, `NUMBER`, `SYSDATE`) would leak into migrations and AI-drafted SQL.
- **Kafka as the store.** Events are notifications. We need `404` lookups, search, and newest-first lists, which a topic can't answer.
- **H2 for tests.** Tests would pass against a database we don't run. Integration tests use Testcontainers `postgres:16`, and the CI backend job uses a `postgres:16` service.

## Consequences

- Every schema change is a new `V<n>__*.sql`. A merged migration is never edited.
- `mvn clean verify` needs Docker for Testcontainers.
- Where PostgreSQL runs on the shared cluster is not decided by us. The deploy reads `CRM_DATABASE_URL` from the GitHub Environment. **Team to confirm** with the instructor: a shared database service, or our own. That also decides backups (see the recoverability rows in [docs/nfrs.md](../nfrs.md)).
- `V1` still starts with a leftover starter `TODO` comment about types. It's harmless, and per the rule above we don't edit a merged migration.
