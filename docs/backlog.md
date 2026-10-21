# Capstone backlog (Lab 48)

Stories in priority order. Each one is a thin vertical slice with acceptance checks we can demo. "Workstream" points to the lead in [team roles](team-roles.md). Status as of 2026-10-21 on `will_work`. **Team to confirm** the order of the not-started stories.

| ID | Story | Workstream | Lab | Status |
| --- | --- | --- | --- | --- |
| CAP-12 | Record customer interaction | Backend | 49 | Done |
| CAP-13 | Interaction list and detail in Angular | Frontend | 50 | Done |
| CAP-14 | JWT login shell | Deployment/security + Frontend | 50–51 | Done |
| CAP-15 | Flyway interaction schema | Backend | 49–50 | Done |
| CAP-16 | Actions PR gate (Angular + Maven + scans) | Deployment | 51 | Done, except CodeQL (R-03) |
| CAP-17 | k3s deploy and smoke through Ingress | Deployment | 51 | Not started. Blocked on cluster access (R-01) |
| CAP-18 | Customer search, profile, create, update | Backend + Frontend | 50 | Done |
| CAP-19 | Admin-only close and reopen | Backend | 51 | Done |
| CAP-20 | Customer lifecycle event | Backend | 49–51 | Done |
| CAP-21 | OpenAPI document for the REST API | Backend | 51 | Not started. Team to confirm if needed (R-11) |
| CAP-22 | Unique correlation id per UI request | Frontend | 51 | Not started (R-10) |

---

## CAP-12: Record customer interaction

**As** an agent **I want** to record an interaction for `CUS-1001` **so that** it persists in PostgreSQL and emits `CustomerInteractionRecordedV1` on Kafka.

1. `POST /api/v1/interactions` for `CUS-1001`, with header `X-Correlation-Id: lab-request-001` and a valid AGENT token, returns `201` and the new interaction id.
2. One row in `customer_interaction` with that id and `correlation_id = lab-request-001`.
3. One `CustomerInteractionRecordedV1` on `crm.customer.interactions.v1`, keyed `CUS-1001`, carrying `lab-request-001`, published after commit. Fields are in [ADR 001](adrs/001-event-contract.md).
4. `CUS-9999` returns `404`, with no row and no event. An invalid `interactionType` returns `400`, with no row and no event.

Evidence: `InteractionApiIT`, `InteractionServiceTest`, `InteractionEventPublisherTest`, [Lab 49 demo](lab49/backend-demo.md).

## CAP-13: Interaction list and detail in Angular

**As** an agent **I want** to see a customer's interactions and open one **so that** I can pick up where the last contact left off.

1. Signed in, picking `CUS-1001` lists its interactions newest first from `GET /api/v1/interactions?customerId=CUS-1001`. Soft-deleted ones don't show.
2. Opening one (`/interactions/:id`) shows its type, summary, correlation id and created time, and lets the agent edit or delete it.
3. Loading, empty and error states each show a message. `CUS-9999` shows "not found".
4. `ng test` and `ng build` pass in CI.

Evidence: `interaction-pages.spec.ts`, `interaction-list-page.component.spec.ts`, the Selenium happy path (`frontend/e2e`), [Lab 50 demo](lab50/frontend-persistence-demo.md).

## CAP-14: JWT login shell

**As** an agent **I want** to sign in once **so that** every API call carries my token.

1. Opening any guarded page (e.g. `/customers`) without signing in redirects to `/login`.
2. After sign-in, the token is kept in memory only (never `localStorage`), and the interceptor adds `Authorization: Bearer …` to API calls only.
3. A `401` from the API clears the session and goes back to `/login`. A `403` keeps the session and shows the Problem Details message.
4. The API rejects a missing, expired or forged token with `401`. **The server is the gate, not Angular.** Roles are AGENT and ADMIN.

Evidence: `SecurityIT`, `auth-session.spec.ts`, `auth.interceptor.spec.ts`, `login-page.spec.ts`, [ADR 006](adrs/006-jwt-roles.md).

## CAP-15: Flyway interaction schema

**As** the team **we want** the schema managed by Flyway **so that** any environment can rebuild it from empty.

1. V1–V3 apply cleanly on an empty PostgreSQL 16 (every Testcontainers run does this).
2. Hibernate `ddl-auto: validate` passes against the result.
3. PostgreSQL types only. CHECK constraints reject an unknown customer status or interaction type.
4. Seeds only the synthetic `CUS-1001` and `CUS-1002`.

Evidence: `InteractionPersistenceIT`, [ADR 002](adrs/002-postgresql.md).

## CAP-16: Actions PR gate

**As** the team **we want** every PR tested on both halves **so that** a broken UI or API can't merge.

1. Every PR runs `frontend` (`npm ci`, `ng test`, `ng build`) and `backend` (`mvn -B clean verify`) on hosted `ubuntu-latest` runners.
2. A failing Angular or Maven test turns the run red, and the image jobs don't run.
3. Trivy (repository and images) and CodeQL (Java + TypeScript) run, and their reports are attached to the run.
4. Branch protection on `main` requires a non-author approval. **Team to confirm** which check names are set as required in GitHub settings (the list is in the CI/CD runbook).

Evidence: Capstone CI runs on PR #22. CodeQL currently fails at upload (R-03).

## CAP-17: k3s deploy and smoke through Ingress

**As** the team **we want** an approved deploy with a smoke check **so that** a bad release is caught and rolled back before anyone relies on it.

1. Capstone Deploy takes the two `sha256` digests from a green `main` run, waits for Environment approval, applies the overlay, and both rollouts finish within 300 s.
2. `scripts/smoke-crm.sh` through the Ingress host checks readiness and the frontend, logs in as `agent`, and creates, reads and soft-deletes one `CUS-1001` interaction with `lab-request-001`.
3. A failed smoke fails the job and prints the pod and event diagnostics.
4. A rollback is rehearsed by running Capstone Deploy again with the previous digest pair, timed against NFR-R1 (10 minutes or less).

## CAP-18: Customer search, profile, create, update

**As** an agent **I want** to find and maintain customers **so that** I can onboard Ravi and keep records current.

1. `GET /api/v1/customers?q=amina` finds `CUS-1001`. An empty query lists everyone by name.
2. `POST /api/v1/customers` returns `201` with a server-generated `CUS-####` and a `Location` header.
3. `PUT /api/v1/customers/CUS-1002` with `ACTIVE` moves Ravi from PROSPECT to ACTIVE in the database.
4. `CUS-9999` returns `404`. An invalid status returns `400` and nothing is saved.

Evidence: `CustomerApiIT`, `CustomerServiceTest`, `customer-pages.spec.ts`, [customer management notes](customer-management.md).

## CAP-19: Admin-only close and reopen

**As** an admin **I want** only admins to close or reopen customers **so that** agents can't remove a customer from service by mistake.

1. `DELETE /api/v1/customers/{id}` as ADMIN sets `CLOSED` and keeps the row. As AGENT it returns `403` and changes nothing.
2. `PUT` to or from `CLOSED` as AGENT returns `403`. Renaming with the same status is allowed.
3. Angular hides the close option from agents, but the API enforces the rule regardless.

Evidence: `SecurityIT` admin/agent close tests, `CustomerServiceTest`.

## CAP-20: Customer lifecycle event

**As** the audit team **we want** each status change announced **so that** there is a trail of who changed what.

1. A real status change publishes one `CustomerStatusChangedV1` on `crm.customer.lifecycle.v1` after commit, with `fromStatus`, `toStatus`, `actor` and `correlationId`.
2. A name-only update, or closing a customer that's already closed, publishes nothing.
3. The consumer writes one audit row per `eventId`. Duplicates and unknown versions are handled (unknown versions go to `.DLT`).

Evidence: `CustomerLifecycleEventPublisherTest`, `CustomerLifecycleEventConsumerIT`, `CustomerServiceTest`.

## CAP-21: OpenAPI document (not started)

**As** a reviewer **I want** a machine-readable API contract **so that** Angular and the API can't drift.

1. An OpenAPI 3 document is served or committed, and it matches the table in [ADR 005](adrs/005-rest-api-contract.md).
2. It shows the Bearer auth scheme and the Problem Details error responses.

## CAP-22: Unique correlation id per UI request (not started)

**As** an operator **I want** each UI request to have its own correlation id **so that** I can follow one click through the logs.

1. The Angular interceptor sends a new id for each request, unless a page sets one on purpose (the demo can still send `lab-request-001`).
2. The id appears in the response header, the log lines, and (for creates) the row and the event.

## Scope decisions

| Topic | Decision |
| --- | --- |
| Interaction delete | **Soft delete** (`deleted_at`, Flyway V3). The row stays for audit |
| Customer delete | **Close** (`status = CLOSED`), ADMIN only. The row stays |
| Customer search and profile | In scope and built (CAP-18) |
| Kafka consumer | Audit consumer: dedupe on `eventId`, bounded retries, DLT |
| Outbox | Out of scope for the capstone (R-06) |
