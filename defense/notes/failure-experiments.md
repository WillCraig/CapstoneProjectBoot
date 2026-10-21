# Failure experiments

Three were run on 2026-10-21. Tokens were not written down. The laptop API on port 8081 was already up (pid 42016). It was not restarted. The Kafka test used a second Postgres and Kafka on 15432 / 19092 and an API on 18081, then those were removed.

## 1. Bad token -> 401

Against `http://127.0.0.1:8081`.

`GET /api/v1/customers/CUS-1001` with `Authorization: Bearer not-a-real-jwt` and `X-Correlation-ID: defense-bad-token`.

Result: **401**

```json
{"type":"about:blank","title":"Unauthorized","status":401,"detail":"Bearer token is invalid or expired.","instance":"/api/v1/customers/CUS-1001","correlationId":"defense-bad-token"}
```

`WWW-Authenticate: Bearer error="invalid_token"`.

A call with no `Authorization` header, earlier the same hour, was also **401**, detail `A bearer token is required.`, and `WWW-Authenticate: Bearer`.

Matches `ProblemDetailAuthHandlers` and `SecurityIT.garbageToken_is401` / `noToken_is401ProblemDetail_withCorrelationId`.

## 2. Agent close -> 403

Logged in as `agent` (roles `["AGENT"]`, `expiresIn` 900). Token not stored.

`DELETE /api/v1/customers/CUS-1002` with `X-Correlation-ID: defense-agent-close`.

Result: **403**

```json
{"type":"about:blank","title":"Forbidden","status":403,"detail":"Your role does not allow this action.","instance":"/api/v1/customers/CUS-1002","correlationId":"defense-agent-close"}
```

`GET` before and after: Ravi Singh, status `PROSPECT`. The row did not change.

Same session, `PUT /api/v1/customers/CUS-1002` body `{"fullName":"Ravi Singh","status":"CLOSED"}`, correlation `defense-agent-put-close`.

Result: **403**, same detail sentence. Status still `PROSPECT`.

The UI hides Close for an agent (`close-requires-admin`). The 403 is the API. `CustomerService` throws `AccessDeniedException` on a close/reopen PUT, and `ProblemDetailAuthHandlers` replaces that message with the fixed sentence above. `SecurityIT.agentClosingCustomerThroughPut_is403ProblemDetail_andRowUnchanged` expects that sentence.

## 3. Kafka down at publish time

Not run on the laptop's `crm-kafka`. That container was left up.

Isolated compose project `crm-defense-exp`: Postgres `crm-defense-postgres` on 15432, Kafka `crm-defense-kafka` on 19092. Backend from this worktree:

```text
SPRING_PROFILES_ACTIVE=local SERVER_PORT=18081
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:15432/crm
SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:19092
```

Readiness on 18081: `{"status":"UP"}`.

Broker up. `POST /api/v1/interactions` for `CUS-1001`, type `NOTE`, correlation `defense-kafka-up`.

Result: **201**. Id `a1f52996-33b4-4448-aeee-38030d4305a6`.
SQL: one `customer_interaction` row, correlation `defense-kafka-up`.
`processed_event`: one row, `CustomerInteractionRecorded`, same correlation.
Log: `Publishing CustomerInteractionRecorded` then `Applied CustomerInteractionRecorded`, both with `correlationId=defense-kafka-up`.

Then `docker stop crm-defense-kafka` only.

`POST` again, correlation `defense-kafka-down`.

Result: **201**. Id `037e7c89-7b8e-4809-b754-70e2051240b2`.
SQL: both interaction rows present.
`processed_event`: still only `defense-kafka-up`. No row for `defense-kafka-down`.
Log: `Publishing CustomerInteractionRecorded` with `correlationId=defense-kafka-down`, then producer and consumer lines `Node 1 disconnected` to `localhost:19092`.

The API did not fail the request. The publisher calls `kafkaTemplate.send` after commit and does not wait on it (`InteractionEventPublisher`). About 40 seconds later the producer was still logging disconnects. We did not wait for the client to give up, and we did not start Kafka again, so this run does not prove the record is unrecoverable if the broker returns inside the producer timeout. It does prove a saved row with no consumer apply while the broker stays down. That is the gap in `docs/adrs/001-event-contract.md`.

Cleanup: stopped the 18081 process, `docker compose -p crm-defense-exp down -v`. Ports 18081, 15432, and 19092 were free afterward. pid 42016 and `crm-postgres` / `crm-kafka` were still up.

## Also seen, same laptop API (not the three above)

`POST /api/v1/interactions` with `interactionType` `TEXT`, correlation `defense-invalid-type`: **400**

```json
{"title":"Bad Request","status":400,"detail":"Validation failed","instance":"/api/v1/interactions","correlationId":"defense-invalid-type","errors":["interactionType: interactionType must be CALL, EMAIL, NOTE, or MEETING"]}
```

`POST` for `CUS-9999`: **404** `Unknown customer: CUS-9999`.

## Not run

| Experiment | Why |
| --- | --- |
| Kill the laptop API mid-rehearsal | That process belongs to the open demo. Use the fallback notes instead of stopping it. |
| Selenium `npm run e2e` | Needs port 4300 and a dedicated server. Last pass is the Oct 21 report, not this session. |
| `scripts/smoke-crm.sh` | Would write a row on the laptop DB. The Oct 21 report already quotes a pass. |
| Class cluster smoke | No kubeconfig. See `defense/notes/deployment-gap.md`. |
| CodeQL upload | Would mean changing GitHub settings. Not done. |
