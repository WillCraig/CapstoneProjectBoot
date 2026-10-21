# Evidence index

Paths are from the repo root. Checked 2026-10-21 while writing this pack.
`main` is `0235018` (PR #22, merged 2026-10-21 18:07:19Z / 2:07 PM ET).
This branch is `defense/lab52-pack` (Lab 48 docs merged with that `main`).

Every slide in `defense/final-presentation-outline.md` uses an ID from this table.

## Minimum rows from the lab guide

| ID | Claim | Artifact | Lab | Scrubbed? |
| --- | --- | --- | --- | --- |
| E01 | C4-style context (one file, not a `docs/architecture/` folder) | `docs/architecture.md` | 48 | Y |
| E02 | CAP-12 acceptance | `docs/backlog.md` (status: done) | 48 | Y |
| E03 | Interaction API | `docs/lab49/backend-demo.md`, `backend/src/test/java/com/northstar/crm/api/InteractionApiIT.java` | 49 | Y |
| E04 | UI to PostgreSQL | `docs/lab50/frontend-persistence-demo.md` (2026-10-20 browser note; auth steps in that file are old). Seed rows: `backend/src/main/resources/db/migration/V1__crm_schema.sql` | 50 | Y |
| E05 | JWT deny (401 / 403) | `docs/lab51/security-and-observability.md`, `backend/src/test/java/com/northstar/crm/security/SecurityIT.java` (17 tests), `defense/notes/failure-experiments.md` | 51 | Y |
| E06 | Rollback | `docs/capstone-cicd-runbook.md` section "Rollback". Not executed. Class steps: `feature/k3s-class-deploy` `docs/lab51/class-cluster-deploy.md` (not merged) | 51 | Y |
| E07 | NFRs | `docs/nfrs.md` | 48 | Y |
| E08 | Risk register | `docs/risk-register.md` (names an owner per line) | 48 | Y |
| E09 | Correlation `lab-request-001` | `frontend/src/app/core/correlation.interceptor.ts` (`LAB_CORRELATION_ID`). Header constant: `X-Correlation-Id` in `backend/src/main/java/com/northstar/crm/observability/CorrelationId.java` | 49-51 | Y |

There is no `docs/architecture/` directory. E01 is the single architecture file plus the three ADRs below.

## Planning

| ID | Claim | Artifact | Lab | Scrubbed? |
| --- | --- | --- | --- | --- |
| E10 | Events after commit, no outbox | `docs/adrs/001-event-contract.md` | 48 | Y |
| E11 | PostgreSQL 16, Flyway, not Oracle | `docs/adrs/002-postgresql.md`, migrations `V1` `V2` `V3` under `backend/src/main/resources/db/migration/` | 48 | Y |
| E12 | Build once, deploy digests | `docs/adrs/003-github-actions-digest-promotion.md` | 48 | Y |
| E13 | Team roles | `docs/team-roles.md`. Brandon's piece is still "TBD" in that file | 48 | Y |
| E14 | AI usage plan | `docs/ai-usage-plan.md`. The "Must verify" boxes are still unchecked | 48 | Y |
| E15 | Four environments, approvals | `docs/environment-strategy.md`. Namespaces are placeholders | 48 | Y |
| E16 | Lab 48 index | `docs/lab48/README.md` | 48 | Y |

## Security

| ID | Claim | Artifact | Lab | Scrubbed? |
| --- | --- | --- | --- | --- |
| E17 | HS256, 15 min TTL, startup refuses a short or missing key | `backend/src/main/resources/application.yml` (`ttl` default `15m`), `SecurityConfig.jwtSigningKey` (32 byte minimum), `TokenService` | 51 | Y |
| E18 | Login and roles | `POST /api/v1/auth/login`. Users `agent` (AGENT) and `admin` (AGENT+ADMIN) in `application.yml`. BCrypt: `DemoUserDirectory` | 51 | Y |
| E19 | API is the boundary. Close/reopen needs ADMIN | `SecurityConfig` (`DELETE /api/v1/customers/**` is ADMIN). `CustomerService.update` throws if status moves to or from `CLOSED` without ADMIN. Commit `8e573e9` | 51 | Y |
| E20 | 401 vs 403 body | `ProblemDetailAuthHandlers` (fixed sentences, plus `correlationId`). Live probes in `defense/notes/failure-experiments.md` | 51 | Y |
| E21 | Angular keeps the token in memory. Guards are UX | `frontend/src/app/core/auth/auth-session.service.ts`, `auth.guard.ts`. Close button hidden for agents in `customer-detail-page.component.ts` | 51 | Y |

## Tests and the laptop

| ID | Claim | Artifact | Lab | Scrubbed? |
| --- | --- | --- | --- | --- |
| E22 | Backend verify, this machine, 2026-10-21 17:48 ET | `mvn -B clean verify` in `backend/`. `Tests run: 99, Failures: 0, Errors: 0, Skipped: 0`. `SecurityIT`: 17. BUILD SUCCESS. Docker Desktop 29.8.0, Java 21.0.12.1 | 49-51 | Y |
| E23 | Same counts on CI | Capstone CI run [39649860445](https://github.com/WillCraig/CapstoneProjectBoot/actions/runs/39649860445) on `0235018`. Backend job: `Tests run: 99`. Frontend job: `TOTAL: 43 SUCCESS` | 51 | Y |
| E24 | Frontend unit tests, this machine, same day | `npm ci` then `npx ng test --watch=false --browsers=ChromeHeadless`. `TOTAL: 43 SUCCESS` (Chrome Headless 156, Node 26.8.1) | 50 | Y |
| E25 | Selenium happy path exists, not in CI | `frontend/e2e/crm-happy-path.e2e.mjs`, `frontend/e2e/README.md`. Last recorded pass: `reports/local-test-run-2026-10-21.md` (1 test, pass). Not re-run for this pack | 50 | Y |
| E26 | Local smoke, earlier the same day | `reports/local-test-run-2026-10-21.md` quotes `Smoke passed for CUS-1001 with correlation lab-request-001`. Script: `scripts/smoke-crm.sh`. Not re-run for this pack | 51 | Y |
| E27 | Seed customers on the laptop DB | 2026-10-21 query on the already-running `crm-postgres`: `CUS-1001` Amina Khan ACTIVE, `CUS-1002` Ravi Singh PROSPECT. Two extra local rows (`CUS-1003`, `CUS-1004`) are not in Flyway | 50 | Y |
| E28 | Duplicates | `V2__processed_event.sql`. `InteractionEventConsumerIT.duplicateDelivery_appliesOnlyOnce` | 49 | Y |
| E29 | Kafka down after commit | `defense/notes/failure-experiments.md` (isolated stack, 2026-10-21 17:52 ET) | 49 | Y |

## CI, images, deploy

| ID | Claim | Artifact | Lab | Scrubbed? |
| --- | --- | --- | --- | --- |
| E30 | PR jobs: frontend, backend, Trivy fs, images | `.github/workflows/capstone-ci.yml`. Trivy steps set `exit-code: "0"`. Images push only when `github.event_name == 'push'` | 51 | Y |
| E31 | Digests from run 39649860445 | `ghcr.io/willcraig/capstone-crm-backend@sha256:4329fe9b6c4641f6d90c5864db260a9a712ce19bbf4a8c8229e349e11c5e1cd3` and `ghcr.io/willcraig/capstone-crm-frontend@sha256:599a298636df759334c1ddcde9f58b8e1652c270730681f68e6403ccb4c85f72`. Copied from that run's "Record image IDs and registry digests" log | 51 | Y |
| E32 | JAR checksum from the same run | `3b260400cb99baef70f248018dc2e106333ff51cd87c715f6e6c17447810f7c7  crm-backend-0.0.1-SNAPSHOT.jar` (package-once job) | 51 | Y |
| E33 | Manual deploy workflow, dry-run default, never run | `.github/workflows/capstone-deploy.yml` (`dry_run` default `true`). `gh run list --workflow capstone-deploy.yml` returned no runs | 51 | Y |
| E34 | Branch protection does not require the CI jobs | `GET /repos/WillCraig/CapstoneProjectBoot/branches/main/protection` on 2026-10-21: `required_status_checks` is null. One approving review is required (`required_approving_review_count` 1) | 51 | Y |
| E35 | CodeQL fails at upload | `.github/workflows/codeql.yml`. Run [39649860453](https://github.com/WillCraig/CapstoneProjectBoot/actions/runs/39649860453) (push to `main`, same minute as E23) failed. Run [39670104483](https://github.com/WillCraig/CapstoneProjectBoot/actions/runs/39670104483) (PR #24, 2026-10-21 20:25Z) failed with `Code scanning is not enabled for this repository.` | 51 | Y |
| E36 | Repo visibility at check time | `gh repo view` on 2026-10-21 returned `visibility: PUBLIC`. When it changed was not looked up. Code scanning was still off on the later CodeQL run | 51 | Y |
| E37 | Class cluster work is not on main | Branch `feature/k3s-class-deploy`, 4 commits ahead of `0235018`: `f679691` numeric UID 10001, `060e886` class overlay + `scripts/deploy-class.sh`, `078e15a` deploy workflow class target, `2c121c3` `docs/lab51/class-cluster-deploy.md` | 51 | Y |
| E38 | No kubeconfig here | `~/.kube/config` absent. `kubectl config current-context` -> `current-context is not set`. Student number not confirmed | 51 | Y |
| E39 | Trivy does not fail the build | `exit-code: "0"` in E30. Runbook baseline (2026-10-20, not re-counted today): 44 fixable HIGH/CRITICAL on the backend image, 53 on the frontend. `docs/capstone-cicd-runbook.md` | 51 | Y |
| E40 | Open PRs, not in this tree | PR #23 confetti (`feature/delete-confetti`, open). PR #24 show/hide password (`feature/show-password`, open). `gh pr view` 2026-10-21 | 50 | Y |
| E41 | How to start and fall back | `README.md` "Run it locally". `defense/notes/demo-validation.md`, `defense/notes/fallback-pack.md`, `defense/notes/deployment-gap.md` | 52 | Y |

GHCR package visibility was not checked. `gh api /users/WillCraig/packages` returned 403 (`read:packages` is not on this token).

## Non-claims / limitations

We will not say these are done:

- A cluster deploy. Capstone Deploy has no runs. `~/.kube/config` is not on this machine. The class overlay is not merged.
- A rollback. The commands are written. Nothing has been rolled back.
- CodeQL results. The analyze step errors because code scanning is not enabled. A green check was not captured.
- Trivy as a gate. It uploads a report and exits 0. A green CI run does not mean zero highs.
- Selenium in CI. It is a local script. The Oct 21 note says it passed once. We did not re-run it for this pack.
- Browser click-through today. API reads of Amina and Ravi were done. The Angular pages were not driven.
- OpenShift. The brief names it. We ship k3s manifests. `docs/architecture.md` says ask the instructor.
- Required CI checks. Protection requires a review. It does not require the Capstone CI jobs.
- GitHub Environments, reviewers, and deploy secrets. Named in the runbook. Not verified as created.
- An outbox. Kafka down still returned 201 and kept the row. See E29.
- PR #23 and PR #24. Not merged. The demo does not include confetti or a show-password control.
- `final-presentation.pdf`. Not in the repo. Slides come from the outline.
- A rehearsed demo or a fallback video. Script only.
- Brandon's scope. `docs/team-roles.md` still says TBD.
- Speaker and operator. Not named in the repo.
