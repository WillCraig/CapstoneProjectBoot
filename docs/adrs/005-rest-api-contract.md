# ADR 005: REST API contract shape

**Status:** Accepted (built in Labs 49–50 and the customer-management work).
**Owner:** Backend (Keifer leads) with Frontend (Jason leads), because both sides depend on it.

## Context

Angular and the API have to agree on paths, fields and error shapes, or both sides break. The Lab 49/50 starters use `POST /api/v1/interactions` with the customer id in the body. Some course slides show nested paths like `/api/customers/{id}/interactions`. An earlier assistant-drafted sample (branch `48-grok`) proposed the nested style. We kept the starter style because the starter tests and labs already used it.

## Decision

All paths sit under `/api/v1`. They use JSON DTOs, and JPA entities never leave the API.

| Method and path | Who | Success | Errors |
| --- | --- | --- | --- |
| `POST /api/v1/auth/login` | anyone | `200` with `accessToken`, `tokenType`, `expiresIn`, `username`, `roles` | `400`, `401` |
| `GET /api/v1/customers?q=` | AGENT | `200` list, by name (search matches id or name) | `401` |
| `POST /api/v1/customers` | AGENT | `201` with `Location: /api/v1/customers/CUS-####` | `400`, `401` |
| `GET /api/v1/customers/{customerId}` | AGENT | `200` | `401`, `404` (`CUS-9999`) |
| `PUT /api/v1/customers/{customerId}` | AGENT; closing or reopening needs ADMIN | `200` | `400`, `401`, `403`, `404` |
| `DELETE /api/v1/customers/{customerId}` | ADMIN | `204` (sets status `CLOSED`, row kept) | `401`, `403`, `404` |
| `GET /api/v1/interactions?customerId=` | AGENT | `200` newest first | `401`, `404` |
| `POST /api/v1/interactions` | AGENT | `201` with the new interaction | `400`, `401`, `404` |
| `GET` / `PUT` / `DELETE /api/v1/interactions/{id}` | AGENT | `200` / `200` / `204` (soft delete) | `400`, `401`, `404` |

- `{customerId}` is always the public id (`CUS-1001`). The server generates it when a customer is created.
- Errors are **Problem Details** (RFC 9457) with `type`, `title`, `status`, `detail` and `instance`, and no stack traces. That covers `401` and `403` too (`ProblemDetailAuthHandlers`).
- Requests can send `X-Correlation-Id`. The API echoes it back on the response, or generates one if it's missing or unsafe.

## Rejected

- **Nested paths** (`/api/customers/{id}/interactions`). Cleaner on paper, but we'd have had to rewrite the starter's controller, tests and Angular service mid-week.
- **SOAP / XML.** Not the taught path.

## Consequences

- There's **no OpenAPI document yet**. The table above and the DTO records are the contract. The rubric names "REST / OpenAPI", so adding one is a backlog item (CAP-21). **Team to confirm** whether it's needed for the defense.
- `POST /api/v1/interactions` returns `201` without a `Location` header (customers do send one). It's a small inconsistency, recorded here so nobody is surprised.
- A breaking change means `/api/v2`, not editing v1.
