# ADR 002: PostgreSQL

## Context
Customers and interactions have to stick around, and the course stack says PostgreSQL.

## Decision
PostgreSQL 16 with Spring Data JPA. Flyway owns the schema (V1-V3) and Hibernate only validates it. Tests run against a real Postgres through Testcontainers, not H2. We picked this over Oracle, which isn't the taught path.

## Consequences
Every schema change is a new migration file, and we don't edit old ones. Running `mvn verify` needs Docker.
