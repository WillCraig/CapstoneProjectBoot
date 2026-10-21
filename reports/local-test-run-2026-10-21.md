# Local test run — 2026-10-21 (Will's Mac, Colima Docker)

Recorded on branch `feature/customer-crud` before it was pushed (later fast-forwarded into `will_work` and pushed, 1cc1761). Times are America/New_York (EDT).

## Backend — `mvn -B clean verify` (Java 21, Spring Boot 3.3.5, Testcontainers PostgreSQL 16 + Kafka 3.8)

**92 tests, 0 failures, 0 errors, 0 skipped — BUILD SUCCESS**

| Test class | Tests | Layer |
| --- | --- | --- |
| `security.SecurityIT` | 13 | JWT + RBAC negative/positive paths, CORS, actuator access |
| `observability.CorrelationIdFilterTest` | 3 | correlation id read/generate/sanitize, MDC cleared |
| `observability.HealthEndpointsIT` | 3 | readiness/liveness UP, correlation echo |
| `api.CustomerControllerTest` | 9 | HTTP contract (real JWT) |
| `api.InteractionControllerTest` | 10 | HTTP contract (real JWT) |
| `api.CustomerApiIT` | 7 | HTTP → PostgreSQL |
| `api.InteractionApiIT` | 6 | HTTP → PostgreSQL → Kafka (actor = JWT subject) |
| `service.CustomerServiceTest` | 11 | lifecycle event published only on real status change |
| `service.InteractionServiceTest` | 9 | service rules |
| `messaging.InteractionEventPublisherTest` | 3 | after-commit, actor |
| `messaging.InteractionEventHandlerTest` | 3 | version check, dedupe |
| `messaging.InteractionEventConsumerIT` | 3 | apply once, duplicate no-op, DLT |
| `messaging.CustomerLifecycleEventPublisherTest` | 3 | V1 shape, after-commit, rollback → no send |
| `messaging.CustomerLifecycleEventHandlerTest` | 3 | version check, dedupe |
| `messaging.CustomerLifecycleEventConsumerIT` | 3 | API status change → topic → audited; duplicate; DLT |
| `repo.InteractionPersistenceIT` | 3 | JPA/Flyway |

Colima needs `DOCKER_HOST=unix://$HOME/.colima/default/docker.sock` and
`TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock`; GitHub runners do not.

## Frontend — Angular 19

- `npx ng test --watch=false --browsers=ChromeHeadless`: **30 specs, 30 SUCCESS**
- `npx ng build --configuration=production`: success

## Selenium UI test — `npm run e2e` (headless Chrome 156)

Stack: isolated throwaway PostgreSQL + Kafka containers, backend jar with
`SPRING_PROFILES_ACTIVE=local`, `npm run e2e:serve` (port 4300, `/api` proxied).

```
✔ agent records an interaction for Amina and sees it on her timeline (2446ms)
ℹ tests 1  ℹ pass 1  ℹ fail 0
```

Run twice back to back; both passed (the test soft-deletes its own interaction).

## Smoke script against the local backend

`CRM_BASE_URL=http://localhost:8095 CRM_SMOKE_SKIP_FRONTEND=true bash scripts/smoke-crm.sh`
(password supplied via env, not shown):

```
Checking backend readiness at http://localhost:8095/actuator/health/readiness
Logging in as agent
Creating a CUS-1001 smoke interaction
Reading the CUS-1001 timeline
Soft-deleting the smoke interaction
Smoke passed for CUS-1001 with correlation lab-request-001
```

Manual checks: no token → 401; old `lab-demo-token` → 401; AGENT `DELETE /customers/CUS-1002` →

```json
{"type":"about:blank","title":"Forbidden","status":403,"detail":"Your role does not allow this action.","instance":"/api/v1/customers/CUS-1002","correlationId":"e2e-403-check"}
```

## Log excerpt (sanitized: no tokens/passwords exist in logs; pid/thread trimmed)

```
07:42:35.555 INFO [correlationId=a101d98a-…] ProblemDetailAuthHandlers : 401 GET /api/v1/customers (A bearer token is required.)
07:42:35.669 WARN [correlationId=e2e-403-check] ProblemDetailAuthHandlers : 403 DELETE /api/v1/customers/CUS-1002 user=agent
07:42:35.682 INFO [correlationId=817c52d8-…] ProblemDetailAuthHandlers : 401 GET /api/v1/customers (Bearer token is invalid or expired.)
07:43:30.173 INFO [correlationId=lab-request-001] AuthController : Login succeeded for username=agent roles=[AGENT]
07:43:30.774 INFO [correlationId=lab-request-001] InteractionEventPublisher : Publishing CustomerInteractionRecorded v1 eventId=10f37f1c-… customerId=CUS-1001
07:43:30.801 INFO [correlationId=lab-request-001] InteractionEventHandler : Applied CustomerInteractionRecorded v1 eventId=10f37f1c-… customerId=CUS-1001
07:43:43.173 INFO [correlationId=8ce56230-…] AuthController : Login succeeded for username=admin roles=[AGENT, ADMIN]
07:43:43.208 INFO [correlationId=evidence-lifecycle-001] CustomerLifecycleEventPublisher : Publishing CustomerStatusChanged v1 eventId=86a89a70-… customerId=CUS-1002 PROSPECT->ACTIVE actor=admin
07:43:43.232 INFO [correlationId=evidence-lifecycle-001] CustomerLifecycleEventHandler : Audited CustomerStatusChanged v1 eventId=86a89a70-… customerId=CUS-1002 PROSPECT->ACTIVE actor=admin
```

(Ravi was set back to PROSPECT afterwards; that change produced its own audited event.)

## Not covered locally

CI run, CodeQL, Trivy, image digests, k3s deploy, Ingress smoke, rollback — see
[README.md](README.md#status-2026-10-21).
