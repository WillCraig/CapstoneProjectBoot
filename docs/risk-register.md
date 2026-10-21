# Risk register

We add a row when we find a new blocker or cut scope. Owners are our proposal and still need the team's OK. Status as of 2026-10-21.

| ID | Risk | Likelihood / impact | What we're doing | Owner | Due |
| --- | --- | --- | --- | --- | --- |
| R-1 | No access to the shared k3s cluster, so no real deploy, smoke or rollback evidence | High / High | Ask the instructor. Until then, run Capstone Deploy as a dry run. The Lab 51 stand-in is the fallback | Will | 2026-10-22 |
| R-2 | The rubric scores OpenShift, but the brief and labs say k3s | Med / High | Ask the instructor which one is graded ([ADR 004](adrs/004-k3s-traefik-ingress.md)) | Will | 2026-10-22 |
| R-3 | CodeQL can't upload results on our private repo. Going public for a day would also publish the course brief, which is in git history | High / Med | Get the instructor's OK in writing before any visibility change | Will | 2026-10-22 |
| R-4 | PR #22 needs a teammate's approval before `main`, and images only publish from `main` | Med / High | Ask a teammate to review | Will | 2026-10-22 |
| R-5 | The GitHub Environments, secrets and variables for deploy don't exist yet | High / High | Create them in GitHub settings once we have cluster details. Never paste values into docs or chat | Will | 2026-10-22 |
| R-6 | The backend pod will probably fail `runAsNonRoot`, because the Dockerfile uses `USER spring` instead of a numeric UID | High / Med | Use a numeric UID before the first deploy | Will | 2026-10-22 |
| R-7 | Trivy reports HIGH and CRITICAL findings but doesn't fail the build | Med / Med | Triage, upgrade what we can, then make it blocking | Will | 2026-10-23 |
| R-8 | If Kafka is down right after a commit, the event can be lost because we have no outbox | Low / Med | Accepted ([ADR 001](adrs/001-event-contract.md)). Run the Kafka-down test and write down what happens | Keifer | 2026-10-23 |
| R-9 | No token revocation, refresh or login rate limit. A stolen token works until it expires | Low / Med | Accepted. Tokens last 15 minutes and stay in memory ([ADR 006](adrs/006-jwt-roles.md)) | Will | 2026-10-23 |
| R-10 | Angular sends `lab-request-001` as the correlation id on every request, so we can't tell UI requests apart in the logs | High / Low | Generate a new id per request (CAP-22) | Jason | 2026-10-23 |

## Scope cuts

- Outbox table. Too much for the week, so we accepted R-8.
- External identity provider. None was provided, so the API issues its own JWTs.
