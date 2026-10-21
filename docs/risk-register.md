# Risk register

A living list. Add a row whenever we cut scope or find a new blocker. Each risk has an owner and a date, because the brief says unowned security and deployment gaps block full credit. **Owners below are proposals based on [team roles](team-roles.md). Team to confirm.**

Scale: Low / Med / High. Status as of 2026-10-21.

## Open

| ID | Risk | Likelihood / impact | Mitigation | Owner | Due |
| --- | --- | --- | --- | --- | --- |
| R-01 | No access to the shared k3s cluster yet, and no namespaces or Ingress hosts. No real deploy, smoke or rollback evidence | High / High | Ask the instructor today. Meanwhile, a dry-run Capstone Deploy (renders manifests without a cluster) as evidence. The Lab 51 documented stand-in is the fallback | Will | 2026-10-22 |
| R-02 | The rubric docx scores OpenShift; the brief and labs say k3s | Med / High | Ask the instructor which is graded ([ADR 004](adrs/004-k3s-traefik-ingress.md)). Don't change platforms without an answer | Will | 2026-10-22 |
| R-03 | CodeQL can't upload results on this private repo (code scanning not available for a personal private repo) | High / Med | The team's plan is a short public window, after the full-history secret scan and a check for course material in history. Needs team and instructor OK | Will | 2026-10-22 |
| R-04 | PR #22 needs a non-author approval before `main`. Images and digests only get published from `main` | Med / High | Ask a teammate to review today. The deploy waits for this | Will | 2026-10-22 |
| R-05 | GitHub Environments, secrets and variables for deploy aren't set up (`KUBECONFIG_B64`, `GHCR_PULL_TOKEN`, `CRM_DATABASE_*`, `CRM_JWT_SECRET`, `CRM_AGENT_PASSWORD`, `CRM_ADMIN_PASSWORD`, `CRM_SMOKE_AGENT_PASSWORD`, `CRM_NAMESPACE`, `CRM_INGRESS_HOST`, `CRM_DATABASE_URL`, `CRM_KAFKA_BOOTSTRAP_SERVERS`) | High / High | Create them in the GitHub UI once cluster details arrive. Never paste values into chat or docs | Will | 2026-10-22 |
| R-06 | If Kafka is down right after a commit, the row is saved but its event can be lost (no outbox), and the HTTP call may hang or fail while the Kafka client waits | Low / Med | Accepted for the capstone ([ADR 001](adrs/001-event-contract.md)). Run the NFR-R3 experiment and record what happens. Outbox is the upgrade path | Keifer | 2026-10-23 |
| R-07 | The backend pod will probably fail `runAsNonRoot` on the first deploy, because `backend/Dockerfile` uses a named user (`USER spring`) instead of a numeric UID | High / Med | One-line fix (numeric UID in the Dockerfile, or `runAsUser` in the manifest) before the first real deploy | Will | 2026-10-22 |
| R-08 | Trivy reports HIGH/CRITICAL findings but doesn't block (`exit-code: "0"`) | Med / Med | Triage the reports, upgrade what can be upgraded, then switch to blocking. The exit condition is in `docs/capstone-cicd-runbook.md` | Will | 2026-10-23 |
| R-09 | No token revocation, refresh or login rate limit. A stolen token works until it expires | Low / Med | Accepted. 15-minute lifetime, token held in memory only, HTTPS through Ingress ([ADR 006](adrs/006-jwt-roles.md)) | Will | Accepted, review 2026-10-23 |
| R-10 | The Angular app sends the same correlation id (`lab-request-001`) on every request, so UI requests can't be told apart in the logs | High / Low | Keep it for the demo script, and generate a unique id per request otherwise (CAP-22) | Jason | 2026-10-23 |
| R-11 | No OpenAPI document. The rubric names "REST / OpenAPI" | Med / Low | The contract table in [ADR 005](adrs/005-rest-api-contract.md) for now. Add springdoc if the instructor wants it (CAP-21) | Keifer | Team to confirm |
| R-12 | The Kafka consumers use `JsonDeserializer` without an `ErrorHandlingDeserializer`. A non-JSON record may keep failing instead of reaching the `.DLT` | Low / Med | Wrap the deserializer, and add a test that sends a non-JSON record | Keifer | Team to confirm |
| R-13 | CI uses Node 20, but the brief says Node 22 | Med / Low | Ask the instructor, then change `node-version` in `capstone-ci.yml` and re-run CI | Jason | 2026-10-22 |
| R-14 | The live demo fails in front of the panel | Med / High | Rehearse twice. Keep a recorded fallback and the local smoke output ready. Plan one deliberate failure and its fix | Will | 2026-10-23 |
| R-15 | A secret or token gets committed or shown in a screenshot | Low / High | `.gitignore`, Trivy secret scan, review the diff before every push (AI usage plan), check screenshots before saving | Everyone | Ongoing |
| R-16 | The course brief and rubric files are in git history (committed 2026-10-07). Going public would publish them | High / Med | Get the instructor's OK in writing before any visibility change, or use a separate clean public repo for the scan | Will | Before any visibility change |
| R-17 | Business-rule gaps: an agent can rename a `CLOSED` customer; interactions can still be recorded on a `CLOSED` customer; status transitions aren't otherwise restricted | Med / Low | Decide whether these need rules. If yes, add them in `CustomerService` / `InteractionService` with tests | Keifer | Team to confirm |
| R-18 | The correlation id can differ between logs and data. With no header, logs get a generated UUID while the row and event get `lab-request-001`. An unsafe header is replaced in logs but stored as-is, and anything over 64 characters will fail the insert | Low / Low | Make `InteractionService` use the same id the filter chose, and add a test | Keifer | Team to confirm |
| R-19 | Some teammate branches have diverged (e.g. a test-merge branch that deletes most of the backend, and a separate customer API) | Med / Med | Bring work in file by file onto `will_work`, never by merging whole branches. Every merge goes through a reviewed PR | Will | Ongoing |
| R-20 | It's unclear which Lab 48 items are graded versus practice | Low / Med | Treat this `docs/` pack as the graded path. Confirm with the instructor | Will | 2026-10-22 |

## Retired (from the first Lab 48 draft)

| Was | Why it's closed |
| --- | --- |
| No JWT issuer defined | Closed: the API issues its own HS256 tokens ([ADR 006](adrs/006-jwt-roles.md)) |
| Kafka consumer side effect undefined | Closed: audit consumers with dedupe and DLT were built ([ADR 001](adrs/001-event-contract.md)) |
| Self-hosted Actions runner not proven | Retired: CI runs on hosted `ubuntu-latest` ([ADR 003](adrs/003-github-actions-digest-promotion.md)) |
| Kafka/PostgreSQL won't run on a laptop | Closed: `compose.yaml` and Testcontainers work. Docker Desktop is the documented setup |

## Scope cuts

When we drop or defer work, add a row: what we cut, why, who decided, and the date.

| What | Why | Decided by | Date |
| --- | --- | --- | --- |
| Outbox pattern | Too much for the week. Publish-after-commit accepted (R-06) | Team to confirm | 2026-10-21 |
| External identity provider | None provided. Self-issued JWT instead | Team to confirm | 2026-10-21 |
