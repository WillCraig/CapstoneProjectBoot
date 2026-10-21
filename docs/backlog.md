# Capstone backlog (Lab 48)

Stories in priority order. Each one fits a session and has checks we can demo. Owners are workstreams. **FILL IN:** team confirms the order.

## CAP-12: Record customer interaction

**As** an agent **I want** to record an interaction for `CUS-1001` **so that** it persists in PostgreSQL and emits `CustomerInteractionRecordedV1` on Kafka.
Owner: Backend / Kafka. Lab 49.

1. `POST /api/v1/interactions` for `CUS-1001` with correlation `lab-request-001` returns `201` and the new interaction id.
2. One row in `customer_interaction` with that id and correlation id.
3. One `CustomerInteractionRecordedV1` on `crm.customer.interactions.v1`, keyed `CUS-1001`, carrying `lab-request-001`, published after commit. Fields per [ADR 001](adrs/001-event-contract.md).
4. `CUS-9999` returns `404`, with no row and no event.

## CAP-13: Interaction list and detail in Angular

**As** an agent **I want** to see a customer's interactions and open one **so that** I can pick up where the last contact left off.
Owner: Frontend. Lab 50.

1. Signed in, opening `CUS-1001` lists its interactions newest first from `GET /api/v1/interactions?customerId=CUS-1001`.
2. Selecting one shows type, summary, correlation id, and created time.
3. Loading, empty, and error states each show a message. `CUS-9999` shows not found.
4. `ng test` and `ng build` pass.

## CAP-14: JWT login shell

**As** an agent **I want** to sign in once **so that** every API call carries my token.
Owner: Security / deploy. Labs 50–51.

1. Visiting `/interactions` without signing in redirects to `/login`.
2. After sign-in the token is held in memory only, and the interceptor adds `Authorization: Bearer …` to API calls.
3. A `401` from the API sends the user back to login.
4. The API rejects a missing or bad token with `401`. The server is the gate, not Angular. **FILL IN:** token issuer and role names (instructor).

## CAP-15: Flyway interaction table

**As** the team **we want** the interaction schema managed by Flyway **so that** any environment can rebuild it from empty.
Owner: Database. Lab 50.

1. `V1__crm_schema.sql` applies clean on an empty Postgres 16 (`docker compose up postgres`, then start the API).
2. Hibernate `ddl-auto: validate` passes against it.
3. PostgreSQL types only. CHECK constraints reject an unknown customer status or interaction type.
4. Seeds only the synthetic `CUS-1001` and `CUS-1002`.

## CAP-16: Actions PR gate

**As** the team **we want** every PR tested on both halves **so that** a broken UI or API can't merge.
Owner: Security / deploy. Lab 51.

1. Every PR runs `frontend` (`npm ci`, `ng test`, `ng build`) and `backend` (`mvn -B clean verify`) on the self-hosted runner.
2. A failing Angular or Maven test turns the run red.
3. The SAST and dependency scan runs, and its result shows in the run.
4. **FILL IN:** whether these can be required checks to merge (instructor).

## CAP-17: k3s smoke through Ingress

**As** the team **we want** a smoke check after every deploy **so that** a bad release rolls back before anyone relies on it.
Owner: Security / deploy. Lab 51.

1. After deploy, the rollout completes and readiness is green.
2. Smoke lists interactions for `CUS-1001` through the Traefik Ingress host with `lab-request-001` and gets `200`.
3. `CUS-9999` through the Ingress gets `404`.
4. If smoke fails, the job rolls back to the previous digest and the run shows it.

## Scope decisions

| Topic | Where we stand |
| --- | --- |
| Interaction update and delete | Not part of CAP-12. Lab 50 asks for update and delete, or a designed soft delete. **FILL IN:** hard or soft delete (Database workstream), confirmed with the instructor |
| Customer search and profile | CAP-12 and CAP-13 take a customer id; there's no customer API yet. **FILL IN:** whether search, profile, or customer create/update is in scope (instructor) |
| Kafka consumer | CAP-12 is producer only. **FILL IN:** what the consumer does with a good event, or whether a log-line consumer is accepted (instructor) |
