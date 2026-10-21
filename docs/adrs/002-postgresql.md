# ADR 002: PostgreSQL as the system of record

Status: Accepted

## Context

Customers and interactions have to survive restarts, and the database wins if it ever disagrees with Kafka. The course stack names PostgreSQL.

## Decision

We use PostgreSQL 16 through Spring Data JPA. Flyway owns the schema and Hibernate only validates it (`ddl-auto: validate`).

- `V1__crm_schema.sql` creates `customer` and `customer_interaction` and seeds `CUS-1001` and `CUS-1002`.
- `V2__processed_event.sql` adds `processed_event` so consumers can skip duplicate events.
- `V3__interaction_soft_delete.sql` adds `deleted_at` so deleted interactions stay for audit.

We never hard-delete a customer either. `DELETE /api/v1/customers/{id}` sets the status to `CLOSED`. The API only shows the public id, never the internal key.

Alternatives: Oracle isn't the taught path, and its types would leak into our migrations. H2 for tests would mean testing against a database we don't run, so integration tests use Testcontainers `postgres:16`.

## Consequences

Every schema change is a new migration file, and we don't edit one after it merges. `mvn clean verify` needs Docker running. Where PostgreSQL runs on the cluster is TBD, ask the instructor. The deploy reads it from `CRM_DATABASE_URL`.
