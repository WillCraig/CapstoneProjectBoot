# Non-functional requirements

Each target has a number and a way to check it. Status is as of 2026-10-21 on `will_work`. Anything we can't meet by the defense goes in the [risk register](risk-register.md).

| ID | Target | How we check | Status |
| --- | --- | --- | --- |
| S1 | Every `/api/**` call without a valid JWT gets `401` | `SecurityIT`, `CustomerApiIT`, `InteractionApiIT` | Met |
| S2 | Every ADMIN-only action by an AGENT gets `403` and changes nothing | `SecurityIT`, `CustomerServiceTest` | Met |
| S3 | Tokens last 15 minutes at most, and the app won't start without a 32-byte `CRM_JWT_SECRET` | `application.yml`, `SecurityConfig` | Met |
| S4 | No secrets, kubeconfigs, `.env` files or `*.tfstate` in git history | `.gitignore`, Trivy secret scan, full-history scan | Met (scan on 2026-10-21 found none) |
| S5 | No unaccepted fixable HIGH or CRITICAL findings by the defense | Trivy repository and image reports | Not met. Trivy doesn't block yet (R-7) |
| S6 | CodeQL runs on every PR with no open alerts | `codeql.yml` runs | Blocked on the private repo (R-3) |
| T1 | Every new interaction stores the request's correlation id in the row and the event | `InteractionServiceTest`, `InteractionApiIT` | Met |
| T2 | Every API log line for a request carries the correlation id, and the response echoes it | `CorrelationIdFilterTest`, `HealthEndpointsIT` | Met, but see R-10 |
| T3 | Every deployed pod runs an image pinned by digest from one CI run | Compare pod images with the `image-digests-<sha>` artifact | No deploy yet (R-1) |
| R1 | Roll back to the previous digests in 10 minutes or less | Timed rehearsal with Capstone Deploy | No deploy yet (R-1) |
| R2 | The schema rebuilds from an empty PostgreSQL 16 with no manual SQL | Testcontainers runs apply V1-V3 from empty | Met |
| R3 | No committed interaction is lost while Kafka is down | Stop Kafka, `POST` for `CUS-1001`, check the row | Not tested yet (R-8) |
| R4 | An event with an unknown version lands on `.DLT` instead of blocking the topic | `InteractionEventConsumerIT`, `CustomerLifecycleEventConsumerIT` | Met |
| C1 | An invalid request or unknown customer creates 0 rows and 0 events | `InteractionApiIT`, `InteractionServiceTest` | Met |
| C2 | A redelivered event causes no extra side effects | The two consumer ITs | Met |
| P1 | p95 of 500 ms or less for creating and listing interactions, over 100 calls on local Compose | Timed run, results in `reports/` | Not measured yet |
| O1 | Backend is ready within 150 s and the rollout finishes within 300 s | Probes in `deploy/k3s/base/backend.yaml`, `kubectl rollout status --timeout=300s` | No deploy yet |
| O2 | The post-deploy smoke finishes in under 2 minutes, and a failed step fails the job | `scripts/smoke-crm.sh` | Passed locally, cluster not yet |
| O3 | PR checks finish in under 10 minutes | Actions run times | Met (about 3-4 minutes) |
| A1 | Forms work by keyboard only, every input has a label, and errors are announced | Templates use labels and `aria-live`, plus a manual keyboard pass | Markup done, keyboard pass not done |
