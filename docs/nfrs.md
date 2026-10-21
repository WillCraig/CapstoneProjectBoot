# Non-functional requirements

Every target has a number and a way to check it. Where the team or instructor still owes us the number, the line says **FILL IN** with a suggestion. Evidence gets linked from `defense/evidence-index.md` as it lands.

## Security

| Target | How we check | Lab |
| --- | --- | --- |
| 100% of `/api/**` calls without a valid JWT get `401`; wrong role gets `403` | Auth tests for no token, bad token, wrong role; repeated in smoke | 51 |
| 0 secrets, tokens, kubeconfigs, `.env`, or `*.tfstate` in git | `.gitignore`, diff review per the AI usage plan, secret scan in CI | 48–51 |
| 0 open findings at or above the scan threshold. **FILL IN:** threshold (suggest High) | Dependency and image scan in Actions | 51 |

## Traceability

| Target | How we check | Lab |
| --- | --- | --- |
| 100% of interaction creates store the request's correlation id (default `lab-request-001`) in the row, the event, and the log line | Integration test, then console consumer and log grep in the demo | 49 |
| 100% of deployed pods run an image pinned by digest, and each digest maps to one Actions run | `kubectl get pods -o jsonpath` against the run summary | 51 |

## Recoverability

| Target | How we check | Lab |
| --- | --- | --- |
| Roll back to the previous digest in **FILL IN** minutes (suggest 10) | Timed rollback rehearsal before the stage-to-prod-like gate | 51 |
| Schema rebuilds from an empty Postgres 16 with 0 manual SQL steps | Fresh `docker compose up postgres`, start the API, Flyway applies | 50 |
| 0 committed interactions lost while Kafka is down | Stop Kafka, POST for `CUS-1001`, row is still there | 49 |
| Data loss window **FILL IN** (depends on where Postgres runs) | Restore drill, once hosting is known | 51 |

## Performance

| Target | How we check | Lab |
| --- | --- | --- |
| p95 under **FILL IN** ms (suggest 500) for `POST` and `GET /api/v1/interactions` at **FILL IN** concurrent users (suggest 10) on synthetic data | Short scripted load run against stage, results in `reports/` | 51 |

## Operability

| Target | How we check | Lab |
| --- | --- | --- |
| `/actuator/health` readiness reports UP within **FILL IN** seconds of pod start (suggest 60) | Readiness probe and `kubectl rollout status` | 51 |
| Post-deploy smoke finishes in under **FILL IN** minutes (suggest 2), and any non-2xx for `CUS-1001` fails the deploy | Smoke job in the CD run | 51 |
| 100% of API request log lines carry the correlation id | Grep logs for `lab-request-001` after the demo calls | 49 |
