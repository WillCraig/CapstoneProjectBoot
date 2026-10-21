# ADR 002: PostgreSQL as the system of record

**Status:** Accepted. The stack is frozen in [docs/architecture.md](../architecture.md).

## Context

Interactions have to survive restarts, and they're the truth if Kafka and the database ever disagree. The course stack names PostgreSQL, the starter already ships Flyway `V1__crm_schema.sql`, and local dev runs `postgres:16` in Compose.

## Decision

PostgreSQL 16 through Spring Data JPA, with Flyway owning the schema. Hibernate runs `ddl-auto: validate`, so it checks the schema but never changes it. Migrations use PostgreSQL types (`UUID`, `TIMESTAMPTZ`, `BIGSERIAL`, `VARCHAR`) and CHECK constraints for customer status and interaction type.

## Rejected

- **Oracle.** Not the taught path, licensed, and its types (`VARCHAR2`, `NUMBER`, `SYSDATE`) would leak into migrations and AI-drafted SQL.
- **Kafka as the store.** Events are notifications. We need 404 lookups and newest-first lists, which a topic can't answer.

## Consequences

- Every schema change is a new `V<n>__*.sql`. A merged migration is never edited.
- Integration tests and the CI backend job need a real Postgres 16.
- Where Postgres runs on the shared cluster is still open. **FILL IN:** shared cluster service or our own (instructor). That also decides backups (see recoverability in [docs/nfrs.md](../nfrs.md)).
