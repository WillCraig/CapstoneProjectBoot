# Capstone backlog

Stories in priority order. Status as of 2026-10-21 on `will_work`.

| ID | Story | Lead | Status |
| --- | --- | --- | --- |
| CAP-12 | Record customer interaction | Keifer | Done |
| CAP-13 | Interaction list and detail in Angular | Jason | Done |
| CAP-14 | JWT login shell | Will, Jason | Done |
| CAP-15 | Flyway interaction schema | Keifer | Done |
| CAP-16 | Actions PR gate | Will | Done except CodeQL (R-3) |
| CAP-17 | k3s deploy and smoke through Ingress | Will | Blocked on cluster access (R-1) |
| CAP-18 | Customer search, profile, create and update | Keifer, Jason | Done |
| CAP-19 | Admin-only close and reopen | Keifer | Done |
| CAP-20 | Customer lifecycle event | Keifer | Done |
| CAP-21 | OpenAPI document | Keifer | Not started. Ask the instructor if it's needed |
| CAP-22 | Unique correlation id per UI request | Jason | Not started (R-10) |

## CAP-12: Record customer interaction

As an agent I want to POST an interaction for `CUS-1001`, so that the note persists in PostgreSQL and emits a versioned Kafka event.

Acceptance:

1. `POST /api/v1/interactions` for `CUS-1001` with `X-Correlation-Id: lab-request-001` returns `201`.
2. The row is in `customer_interaction` with `correlation_id = lab-request-001`.
3. `CustomerInteractionRecordedV1` is published on `crm.customer.interactions.v1` after the commit.
4. `CUS-9999` returns `404`, and a bad `interactionType` returns `400`. Neither saves a row or sends an event.

## Additional seeds

### CAP-13: Interaction list and detail in Angular

As an agent I want to see a customer's interactions and open one, so I can pick up where the last contact left off.

- Picking `CUS-1001` lists its interactions newest first. Deleted ones don't show.
- Opening one shows its details and lets the agent edit or delete it.
- Loading, empty and error states each show a message.

### CAP-14: JWT login shell

As an agent I want to sign in once, so every API call carries my token.

- A guarded page redirects to `/login` when signed out.
- The token stays in memory, and the interceptor adds it to API calls only.
- A `401` sends the user back to `/login`. A `403` keeps the session and shows the error.

### CAP-15: Flyway interaction schema

As a team we want Flyway to own the schema, so any environment can rebuild it from empty.

- V1-V3 apply cleanly on an empty PostgreSQL 16, and Hibernate validation passes.
- Only the synthetic `CUS-1001` and `CUS-1002` are seeded.

### CAP-16: Actions PR gate

As a team we want every PR to test both the frontend and the backend, so a broken UI or API can't merge.

- Every PR runs `npm ci`, `ng test`, `ng build` and `mvn -B clean verify`.
- A failing test turns the run red and the image jobs don't run.
- Trivy and CodeQL run and attach their reports.

### CAP-17: k3s deploy and smoke through Ingress

As a team we want an approved deploy with a smoke test, so we catch a bad release and roll it back.

- Capstone Deploy takes the digests from a green `main` run, waits for approval, and both rollouts finish within 300 s.
- `scripts/smoke-crm.sh` logs in and creates, reads and deletes a `CUS-1001` interaction through the Ingress.
- We rehearse a rollback to the previous digests in 10 minutes or less.

### CAP-18: Customer search, profile, create and update

As an agent I want to find and update customers, so I can onboard Ravi and keep records current.

- `GET /api/v1/customers?q=amina` finds `CUS-1001`.
- `POST /api/v1/customers` returns `201` with a new `CUS-####` id.
- `PUT /api/v1/customers/CUS-1002` with `ACTIVE` moves Ravi from PROSPECT to ACTIVE.

### CAP-19: Admin-only close and reopen

As an admin I want only admins to close or reopen customers, so an agent can't take a customer out of service by mistake.

- `DELETE` as ADMIN sets `CLOSED` and keeps the row. As AGENT it returns `403`.
- `PUT` into or out of `CLOSED` as AGENT returns `403`.

### CAP-20: Customer lifecycle event

As a team we want each status change announced, so there's an audit trail.

- A real status change publishes one `CustomerStatusChangedV1` after the commit.
- A name-only update publishes nothing.

### CAP-21: OpenAPI document

As a reviewer I want a machine-readable API contract, so Angular and the API can't drift apart.

- An OpenAPI 3 document matches [ADR 005](adrs/005-rest-api-contract.md) and shows Bearer auth.

### CAP-22: Unique correlation id per UI request

As an operator I want each UI request to have its own correlation id, so I can follow one click through the logs.

- The interceptor sends a new id per request, unless a page sets `lab-request-001` on purpose for the demo.
