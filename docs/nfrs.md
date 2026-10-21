# Non-functional requirements

Every target has a **number**, a **way to check it**, and the **lab that proves it**. The status column says what's already proven and where. "Not yet evidenced" means the check exists but hasn't been run in the target environment yet. Anything not met by the defense goes into the [risk register](risk-register.md) with an owner and a date.

Status as of 2026-10-21, on branch `will_work`.

## Security

| ID | Target | How we check | Lab | Status |
| --- | --- | --- | --- | --- |
| NFR-S1 | 100% of `/api/**` calls without a valid JWT (missing, expired, forged, garbage) get `401` Problem Details | `SecurityIT` (`noToken_…`, `expiredToken_is401`, `tokenSignedWithAnotherKey_…`, `garbageToken_is401`), `CustomerApiIT.search_withoutBearer_unauthorized`, `InteractionApiIT.list_withoutBearer_unauthorized` | 51 | Met (CI green on PR #22) |
| NFR-S2 | 100% of ADMIN-only actions done by an AGENT get `403` and change nothing: `DELETE` a customer, close or reopen through `PUT`, `/actuator/metrics` | `SecurityIT.agentClosingCustomer_…`, `agentClosingCustomerThroughPut_…`, `agentReopeningClosedCustomer_…`, `healthIsPublic_butMetricsAreAdminOnly`, plus `CustomerServiceTest` | 51 | Met |
| NFR-S3 | Access tokens live at most **15 minutes**. The app **refuses to start** without `CRM_JWT_SECRET` of at least 32 bytes | `crm.security.jwt.ttl: 15m` in `application.yml`; startup check in `SecurityConfig.jwtSigningKey` | 51 | Met |
| NFR-S4 | **0** secrets, tokens, kubeconfigs, `.env` files or `*.tfstate` in git history | `.gitignore` (root and `infra/terraform/`), the Trivy `secret` scanner on every CI run, and a full-history secret scan before any visibility change | 48–51 | Met (history scan on 2026-10-21 found none) |
| NFR-S5 | **0** unaccepted fixable HIGH/CRITICAL findings in dependencies and images by the defense | Trivy repository and image reports (artifacts `trivy-repository-<sha>`, `trivy-images-<sha>`) | 51 | **Not met yet.** Trivy reports but doesn't block (`exit-code: "0"`). Exit condition is in the CI/CD runbook (R-08) |
| NFR-S6 | CodeQL (Java + TypeScript) runs on every PR with **0** open alerts | `codeql.yml` run results | 51 | **Blocked.** Upload fails on the private repo (R-03) |

## Traceability

| ID | Target | How we check | Lab | Status |
| --- | --- | --- | --- | --- |
| NFR-T1 | 100% of new interactions store the request's correlation id in the row **and** the event. The value comes from the `X-Correlation-Id` header, else the body, else `lab-request-001` | `InteractionServiceTest.create_correlationPrecedence_headerThenRequestThenDefault`, `InteractionApiIT.createAndList_forAmina_persistsRow` | 49 | Met |
| NFR-T2 | 100% of API log lines for a request carry `correlationId=…`, and the id is echoed in the response header | Log pattern in `application.yml`, `CorrelationIdFilterTest`, `HealthEndpointsIT.correlationId_isEchoedOnResponses` | 49/51 | Met. Two caveats are in R-10 and R-18 |
| NFR-T3 | 100% of deployed pods run an image pinned by `sha256` digest that maps to one Capstone CI run | `capstone-deploy` only accepts `sha256:<64 hex>`. Compare `kubectl get pods -o jsonpath='{..image}'` with the `image-digests-<sha>` artifact | 51 | Not yet evidenced (no deploy yet, R-01) |

## Recoverability

| ID | Target | How we check | Lab | Status |
| --- | --- | --- | --- | --- |
| NFR-R1 | Roll back to the previous digest pair in **10 minutes or less**, from deciding to roll back to the smoke going green again | Timed rehearsal: a new approved Capstone Deploy run with the previous digests (runbook "Rollback") | 51 | Not yet evidenced (R-01) |
| NFR-R2 | The schema rebuilds from an empty PostgreSQL 16 with **0** manual SQL steps | Every integration-test run starts an empty Testcontainers `postgres:16` and Flyway applies V1–V3 | 49–50 | Met |
| NFR-R3 | **0** committed interactions lost while Kafka is down | Experiment: stop Kafka (`docker compose stop kafka`), `POST` for `CUS-1001`, then check the row in PostgreSQL | 49 | **Not yet tested.** We expect the row to stay, but the HTTP call may be slow or return an error while the Kafka client waits (R-06). Team to confirm by running it |
| NFR-R4 | Data-loss window (backup point) | Restore drill, once database hosting is known | 51 | **Team to confirm.** Depends on who runs PostgreSQL on the cluster |
| NFR-R5 | A poison or unknown-version event never blocks the topic. It lands on `.DLT` after at most **3** attempts | `InteractionEventConsumerIT.unsupportedVersion_goesToDeadLetterTopic_…`, `CustomerLifecycleEventConsumerIT.unsupportedVersion_…` | 49 | Met for bad versions. A non-JSON record is not covered (R-12) |

## Correctness and idempotency

| ID | Target | How we check | Lab | Status |
| --- | --- | --- | --- | --- |
| NFR-C1 | Invalid input or an unknown customer creates **0** rows and **0** events | `InteractionApiIT.create_unknownCustomer_notFound_andNothingPersisted`, `create_invalidType_badRequest_andNothingPersisted`, `InteractionServiceTest.create_unknownCustomer_neverPersistsOrPublishes` | 49 | Met |
| NFR-C2 | Redelivering the same event causes **0** extra side effects | `InteractionEventConsumerIT.duplicateDelivery_appliesOnlyOnce`, `CustomerLifecycleEventConsumerIT.duplicateDelivery_isAuditedOnce` | 49 | Met |

## Performance

| ID | Target | How we check | Lab | Status |
| --- | --- | --- | --- | --- |
| NFR-P1 | p95 **500 ms or less** for `POST /api/v1/interactions` and `GET /api/v1/interactions?customerId=CUS-1001`, over **100** sequential calls with a valid token, synthetic data, local Compose | A timed run of 100 calls, e.g. a JMeter or Postman collection runner, or a curl loop. Results saved in `reports/` | 50–51 | **Not measured yet.** Team to confirm the tool and who runs it |
| NFR-P2 | Login responds in **1 s or less** (one BCrypt check) | Same timed run, 20 logins | 51 | Not measured yet |

## Operability

| ID | Target | How we check | Lab | Status |
| --- | --- | --- | --- | --- |
| NFR-O1 | Backend readiness reports `UP` within **150 s** of pod start (startup probe: 30 × 5 s), and the rollout finishes within the deploy's **300 s** timeout | Probes in `deploy/k3s/base/backend.yaml`; `kubectl rollout status --timeout=300s` in `capstone-deploy` | 51 | Probe endpoints proven by `HealthEndpointsIT`. In-cluster timing not yet evidenced |
| NFR-O2 | A database outage takes the pod out of service (readiness `DOWN`) **without** restarting it (liveness ignores the DB) | Health groups in `application.yml` (readiness = `readinessState,db`; liveness = `livenessState`) | 51 | Configured. Forced-fault demo not yet recorded |
| NFR-O3 | The post-deploy smoke finishes in **under 2 minutes**, and any failed step fails the deploy job | `scripts/smoke-crm.sh` (`set -euo pipefail`): readiness, frontend, login, then create, read and soft-delete a `CUS-1001` interaction | 51 | Local smoke passed (`reports/local-test-run-2026-10-21.md`). Cluster not yet |
| NFR-O4 | PR checks (Capstone CI) finish in **under 10 minutes** | Actions run duration | 51 | Met. About 3–4 minutes per run on 2026-10-21 |
| NFR-O5 | `mvn -B clean verify` is green twice in a row with the same results | Two back-to-back runs, locally and in CI | 49 | Partly. Capstone CI was green on 4 runs in a row on 2026-10-21 (different commits). A back-to-back double run on one commit isn't recorded yet |

## Accessibility

| ID | Target | How we check | Lab | Status |
| --- | --- | --- | --- | --- |
| NFR-A1 | Login, customer and interaction forms are usable by keyboard only. Every input has a `<label>`, and errors are announced (`role="alert"` / `aria-live`) | Templates use labels and `aria-live` regions; plus a manual keyboard-only pass of the Selenium happy path | 50 | Markup in place. **Keyboard pass not recorded yet.** Team to confirm who does it |
