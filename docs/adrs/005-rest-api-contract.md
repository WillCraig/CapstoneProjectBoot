# ADR 005: REST API contract

Status: Accepted

## Context

Angular and the API have to agree on paths, fields and errors. The Lab 49 and 50 starters use `POST /api/v1/interactions` with the customer id in the body. Some slides show nested paths like `/api/customers/{id}/interactions`.

## Decision

We kept the starter style. Everything is under `/api/v1` and uses JSON DTOs, so entities never leave the API.

| Endpoint | Who can call it |
| --- | --- |
| `POST /api/v1/auth/login` | Anyone |
| `GET`, `POST /api/v1/customers` | AGENT |
| `GET`, `PUT /api/v1/customers/{customerId}` | AGENT. Closing or reopening through `PUT` needs ADMIN |
| `DELETE /api/v1/customers/{customerId}` | ADMIN. Sets `CLOSED` and keeps the row |
| `GET`, `POST /api/v1/interactions` | AGENT |
| `GET`, `PUT`, `DELETE /api/v1/interactions/{id}` | AGENT. Delete is a soft delete |

Customer ids are always the public id (`CUS-1001`). Errors are Problem Details (RFC 9457) with no stack traces. The API echoes `X-Correlation-Id`, or generates one if it's missing.

Alternatives: nested paths look cleaner, but we'd have had to rewrite the starter controller, tests and Angular service mid-week. SOAP isn't the taught path.

## Consequences

We don't have an OpenAPI document yet, so this table and the DTOs are the contract (CAP-21). A breaking change goes to `/api/v2` instead of changing v1.
