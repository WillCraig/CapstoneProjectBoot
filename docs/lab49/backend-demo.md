# Lab 49 — backend demo runbook

Reproduction guide for the CAP-12 interaction slice: a peer with Docker, JDK 21,
and Maven should be able to run every step below and see the same results.

## Step 1 — vertical slice and definition of done

Selected story: **CAP-12 — Record customer interaction** (`docs/backlog.md`).
As an agent I want to POST an interaction for `CUS-1001` so that the note
persists in PostgreSQL and emits a versioned Kafka event.

Acceptance criteria (numbered, from `docs/backlog.md`):

1. `POST /api/v1/interactions` for `CUS-1001` returns `201` with correlation `lab-request-001`.
2. The interaction row is visible in PostgreSQL.
3. A `CustomerInteractionRecordedV1` event is published to `crm.customer.interactions.v1`.
4. `CUS-9999` returns `404`, with no row written and no event published.

Change list for the slice:

- **API:** `POST` + `GET /api/v1/interactions`, DTO records only (no JPA entity in JSON), Bean Validation, Problem Details errors (`application/problem+json`).
- **Persistence:** Flyway `V1__crm_schema.sql` (customers + interactions, PostgreSQL `BIGSERIAL` / `UUID` / `TIMESTAMPTZ`, confirmed against real PostgreSQL 16 by the ITs) and `V2__processed_event.sql`; interaction PK is an app-generated UUID; index `ix_interaction_customer_created` serves the per-customer timeline, loaded newest-first via `findByCustomer_PublicIdOrderByCreatedAtDesc`.
- **Event:** `CustomerInteractionRecordedV1` (frozen below), partition key = customer public id, publish **after** the DB transaction commits (`docs/architecture.md`: PostgreSQL is the system of record).
- **Security stub:** demo Bearer filter (`lab-demo-token`) for now; real JWT is Lab 51. Event `actor` is the `demo-agent` placeholder until then.
- **Observability:** `X-Correlation-ID` threaded header → row → event → logs. Logs carry ids and correlation only, never the interaction summary.
- **Profiles:** one configuration for all runs; integration tests point at Testcontainers PostgreSQL 16 (no H2), demos point at Compose PostgreSQL via `SPRING_DATASOURCE_*` env vars.

Fixture plan:

| ID | Who / use |
| --- | --- |
| `CUS-1001` | Amina Khan (`ACTIVE`, seeded by Flyway V1) — primary create target |
| `CUS-1002` | Ravi Singh (`PROSPECT`, seeded by Flyway V1) — secondary / timeline scoping |
| `CUS-9999` | Does not exist — not-found paths |
| `lab-request-001` | `X-Correlation-ID` on HTTP and on the event |

Demo evidence checklist (definition of done):

- [x] Happy-path curl: `201` for `CUS-1001` with `lab-request-001`
- [x] Negative curls: invalid `interactionType` → `400`; `CUS-9999` → `404`; empty summary → `400`
- [x] SQL proof the row exists in PostgreSQL
- [x] Kafka proof: one V1 event per successful create, keyed by `CUS-1001`
- [x] Unit tests: service create/list, correlation precedence, no publish on not-found, publisher waits for after-commit, handler dedupe/version rules, MockMvc contract
- [x] HTTP / persistence / messaging integration tests (Testcontainers PostgreSQL + Kafka)
- [x] `mvn -B clean verify` green twice with identical results
- [x] No secrets, `target/`, or broker data committed

## Event contract V1 (frozen)

Topic: `crm.customer.interactions.v1` · key: customer public id (e.g. `CUS-1001`)

| Field | Type | Notes |
| --- | --- | --- |
| `eventId` | UUID | Fresh per event; consumer dedupe key |
| `eventType` | string | `CustomerInteractionRecorded` |
| `eventVersion` | int | `1` |
| `occurredAt` | instant | Publish time (UTC) |
| `correlationId` | string | From `X-Correlation-ID` (header > body > `lab-request-001`) |
| `actor` | string | `demo-agent` placeholder until Lab 51 JWT |
| `customerId` | string | Public id, e.g. `CUS-1001` |
| `interactionId` | string | Interaction UUID as string |
| `interactionType` | string | `CALL` / `EMAIL` / `NOTE` / `MEETING` |

The event never carries the interaction summary (asserted by `InteractionApiIT`).

## Consumer (resilience)

`InteractionEventConsumer` (group `crm-interactions-audit`) applies each event
via `InteractionEventHandler`:

- `eventVersion != 1` → `UnsupportedEventVersionException`, non-retryable, straight to DLT.
- Dedupe: a `processed_event` row (Flyway `V2__processed_event.sql`, PK = `eventId`) records
  each applied event; a redelivery with the same `eventId` is a logged no-op.
- Other failures: bounded retry (2 × 500 ms) then dead-letter to
  `crm.customer.interactions.v1.DLT` via `DeadLetterPublishingRecoverer`.
- Logs carry ids + `correlationId` only.

## Run the demo

Prerequisites: JDK 21, Maven, Docker. From the repo root:

```bash
cp .env.example .env     # local only, never commit .env
docker compose up -d     # PostgreSQL 16 on 5432, Kafka on 9092
cd backend && mvn -q package -DskipTests
java -jar target/crm-backend-0.0.1-SNAPSHOT.jar   # API on http://localhost:8080
```

> Local note (this workstation): ports 5432/8080 are taken by an unrelated stack,
> so the evidence run used the pre-existing `crm49-postgres` (15432) and
> `crm49-kafka` (19092) containers with `SPRING_DATASOURCE_URL`,
> `SPRING_KAFKA_BOOTSTRAP_SERVERS`, and `SERVER_PORT=18080` overrides. The app
> needed no code or config changes for that.

Happy path (expect `201`):

```bash
TOKEN=<demo token from DemoBearerFilter / README>
curl -i -X POST "http://localhost:8080/api/v1/interactions" \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN" \
  -H 'X-Correlation-ID: lab-request-001' \
  -d '{"customerId":"CUS-1001","interactionType":"NOTE","summary":"Requested address update","correlationId":"lab-request-001"}'
```

Observed response body:

```json
{"id":"dfe59792-1001-42ab-a2a5-22eb7553b5c4","customerId":"CUS-1001","interactionType":"NOTE","summary":"Requested address update","correlationId":"lab-request-001","createdAt":"2026-10-20T12:07:55.226575Z"}
```

Timeline (expect the same interaction back):

```bash
curl -s -H "Authorization: Bearer $TOKEN" "http://localhost:8080/api/v1/interactions?customerId=CUS-1001"
```

SQL proof — interaction row, and the consumer's processed row a second later:

```sql
SELECT c.public_id, i.interaction_type, i.correlation_id
FROM customer_interaction i JOIN customer c ON c.customer_id = i.customer_id
WHERE i.correlation_id = 'lab-request-001';
-- CUS-1001 | NOTE | lab-request-001   (1 row)

SELECT event_type, customer_id, correlation_id
FROM processed_event WHERE correlation_id = 'lab-request-001';
-- CustomerInteractionRecorded | CUS-1001 | lab-request-001   (1 row)
```

Kafka proof:

```bash
kafka-console-consumer.sh --bootstrap-server localhost:9092 \
  --topic crm.customer.interactions.v1 --from-beginning --timeout-ms 8000 | grep lab-request-001
```

Observed event (note `interactionId` matches the `201` response above):

```json
{"eventId":"55df0e83-95e9-4841-8a67-d799a76542ff","eventType":"CustomerInteractionRecorded","eventVersion":1,"occurredAt":1792498075.240068000,"correlationId":"lab-request-001","actor":"demo-agent","customerId":"CUS-1001","interactionId":"dfe59792-1001-42ab-a2a5-22eb7553b5c4","interactionType":"NOTE"}
```

## Failure experiments

| # | Experiment | Observed | Evidence |
| - | ---------- | -------- | -------- |
| 1 | POST `interactionType: "TEXT"` | `400` Problem Detail: `Validation failed`, `errors: ["interactionType: … must be CALL, EMAIL, NOTE, or MEETING"]`; no row, no event | Live curl + `InteractionControllerTest`, `InteractionApiIT` |
| 2 | POST for `CUS-9999` | `404` Problem Detail: `Unknown customer: CUS-9999`; nothing persisted | Live curl + controller/IT tests; service unit test proves publish is never called |
| 5 | `mvn clean verify` twice | Identical: `Tests run: 29, Failures: 0, Errors: 0` + `BUILD SUCCESS` both runs | Verify-twice run, 2026-10-20 |
| 6 | POST with empty summary | `400` Problem Detail with `errors: ["summary: must not be blank"]`; no event | Live curl + `InteractionControllerTest` |
| 7 | Same `eventId` delivered twice | Applied once; second delivery logged as duplicate and skipped | `InteractionEventConsumerIT.duplicateDelivery_appliesOnlyOnce`, handler unit test |

Experiments 3 (break idempotency on purpose) and 4 (stop Kafka mid-publish) were
not run destructively. Their code paths are still covered: dedupe on/off behavior
is asserted by the handler unit test and the consumer IT, and publish-after-commit
timing is asserted by `InteractionEventPublisherTest`. A broker-down publish is
fire-and-forget after the DB commit (send failures surface in logs, the `201`
stands) — the transactional outbox that would close that gap is a known
limitation below, not implemented in Lab 49.

An unsupported-version event (`eventVersion: 99`) was also exercised end to end:
it lands on `crm.customer.interactions.v1.DLT` and gets no `processed_event` row
(`InteractionEventConsumerIT`).

## Full verification

```bash
cd backend
mvn -B clean verify   # 29 tests, BUILD SUCCESS — run twice, identical results
```

The integration tests start their own PostgreSQL 16 and Kafka containers
(Testcontainers), so no local Compose ports are needed. On this machine Docker
runs under Colima, which Testcontainers does not auto-detect — export first:

```bash
export DOCKER_HOST=unix://$HOME/.colima/default/docker.sock
export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock
```

(Docker Desktop / Linux need neither. The pom pins Docker API `1.41` because
Testcontainers' client defaults to 1.32, which current Docker engines reject.)

## Test inventory (29)

| Class | Count | Proves |
| --- | --- | --- |
| `InteractionServiceTest` | 7 | Create/list, correlation precedence, persist-before-publish, no publish on not-found |
| `InteractionEventPublisherTest` | 2 | V1 payload shape, customer-id key, no send before after-commit |
| `InteractionControllerTest` | 7 | 201/200/400/404/401, Problem Details bodies, correlation header pass-through |
| `InteractionApiIT` | 5 | Real HTTP + PostgreSQL row + Kafka event on the wire; failures persist nothing |
| `InteractionPersistenceIT` | 2 | Flyway V1 seeds, timeline ordering scoped per customer |
| `InteractionEventHandlerTest` | 3 | Apply once, duplicate no-op, unsupported version rejected |
| `InteractionEventConsumerIT` | 3 | Consume from real broker, duplicate applied once, version 99 → DLT |

## Known limitations

- **No transactional outbox.** If the JVM dies between DB commit and the Kafka
  send, that event is lost (the row is safe; PostgreSQL is the system of record).
  An outbox table + relay is the production-grade fix, deferred beyond Lab 49.
- **Demo auth.** A static bearer token gates `/api/**`; `actor` is `demo-agent`.
  Lab 51 replaces both with JWT.
- **Malformed-payload DLT.** Unsupported *versions* are dead-lettered, but bytes
  that fail JSON deserialization would need an `ErrorHandlingDeserializer` to
  reach the DLT cleanly. Not wired up yet.
- **Migration V1 comment.** The `-- TODO: confirm PostgreSQL types` line in
  `V1__crm_schema.sql` stays: editing an applied Flyway migration breaks its
  checksum. The types are confirmed by the ITs against PostgreSQL 16.

## Reflection

1. **Publish timing was the decision that mattered most.** Publishing after the
   DB commit guarantees consumers never see an event for a rolled-back
   interaction, and it shaped the code (transaction synchronization in the
   publisher) and even the tests (a `@Transactional` IT would never publish).
2. **End-to-end evidence is the chain, not one check:** the `201` response id
   equals the SQL row, equals the Kafka event's `interactionId`, equals the
   consumer's `processed_event` row — plus 29 automated tests repeating it.
3. **Hardest failure to diagnose was tooling, not code:** Testcontainers against
   Colima failed first on socket detection, then a Docker API version floor,
   then the Ryuk socket mount. Each fix is now recorded above so it is a
   one-minute setup next time.

Angular client for these contracts is Lab 50; JWT hardening is Lab 51.
