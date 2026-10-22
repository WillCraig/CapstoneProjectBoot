# Technical Q&A

Say the four lines. Stop. About 60-90 seconds each. If we do not know, say "unknown" and the check we would run.

### Q: Where is the JWT validated?
- Claim: On every API request, in Spring Security, not in Angular.
- Evidence (path/id): `SecurityConfig` builds a `NimbusJwtDecoder` with `MacAlgorithm.HS256` and issuer `northstar-crm` (E17). `TokenService` sets a 15 minute TTL (`expiresIn` 900 on a live login). Missing or short `CRM_JWT_SECRET` throws at startup (`MIN_SECRET_BYTES` 32).
- Trade-off: HS256 means the API holds the signing key. We have no refresh token and no revoke list. A stolen token works until it expires.
- Next step / residual risk: A real IdP would replace `AuthController` and `DemoUserDirectory`. Controllers only see the JWT.
- Practice time (seconds): 70

### Q: What if Kafka is down when we publish?
- Claim: The interaction stays saved. The event can miss the consumer. There is no outbox.
- Evidence (path/id): `docs/adrs/001-event-contract.md` (E10). `InteractionEventPublisher.send` runs in `afterCommit` and does not fail the request. Isolated run 2026-10-21: broker up gave 201 plus a `processed_event` row; after `docker stop` on that broker, the next POST was still 201, the row existed, and `processed_event` had no row for `defense-kafka-down` (E29).
- Trade-off: Postgres is the source of truth. We can lose the audit event. The producer was still retrying when we stopped the test, so a broker that returns quickly might still get the record. We did not measure that.
- Next step / residual risk: An outbox table, written in the same transaction, with a publisher that retries from the table. Not built. Owner in the risk register: backend.
- Practice time (seconds): 80

### Q: How do you prove the UI wrote PostgreSQL?
- Claim: The UI calls the API. The API inserts a row. We can show the row.
- Evidence (path/id): `InteractionApiIT` hits HTTP, Postgres, and Kafka (E03). Flyway `V1__crm_schema.sql` seeds Amina and Ravi (E04). `docs/lab50/frontend-persistence-demo.md` records a 2026-10-20 browser edit and a SQL check. That write-up still describes the old demo token. Today's proof of the seed is the `psql` query in `defense/notes/demo-validation.md` (E27). Selenium (`frontend/e2e/crm-happy-path.e2e.mjs`) drives the real UI but is not in CI, and we did not re-run it for this pack (E25).
- Trade-off: A toast is not proof. The SQL is. The Oct 20 note and today's API check are not the same run.
- Next step / residual risk: During the demo, run the `lab-request-001` query after the click. Do not cite the Oct 20 UUID as today's row.
- Practice time (seconds): 75

### Q: What is the rollback unit?
- Claim: The pair of registry digests from run 39777251733. Not the git tag alone.
- Evidence (path/id): E31. Backend `sha256:ae571851d8c74da1d3f279d421a3576b9d9e4520523a1411b4a48f3e55c9581d`. Frontend `sha256:fa7e4cf7c53c36b83a867709ad8f72e2d983312c2e255c905fbe634f029a0ca1`. `capstone-deploy.yml` rejects anything that is not `sha256:` plus 64 hex chars. Rollback text: `docs/capstone-cicd-runbook.md` (E06).
- Trade-off: Tags are easy to say and easy to move. The digest is the bytes we scanned.
- Next step / residual risk: Nothing has been rolled back. You need two deploys before `kubectl rollout undo` has a previous ReplicaSet. Migrations V1-V3 are additive, so an older app image should still match the schema. That was not tried on a cluster.
- Practice time (seconds): 70

### Q: Which NFR is not met?
- Claim: Rollback within 10 minutes is not met, because we have not deployed. A few others are met on a laptop or in CI only.
- Evidence (path/id): `docs/nfrs.md` (E07). 401/403: `SecurityIT` and the live probes (E05). 15 minute tokens: `application.yml` and `expiresIn` 900. PR time: run 39777251733 went from 12:18:22Z to 12:21:47Z (8:18 to 8:21 AM ET), under 10 minutes (E23). "No secrets in git" is the policy plus Trivy secret scan, not a proof I re-ran today.
- Trade-off: Writing the target down was the Lab 48 point. Leaving the unmet one visible is the Lab 52 point.
- Next step / residual risk: One real deploy, then time a rollback. Until then the NFR stays open.
- Practice time (seconds): 65

### Q: How do you stop a duplicate event from doing the work twice?
- Claim: The consumer stores `eventId` in `processed_event` and skips a repeat.
- Evidence (path/id): `V2__processed_event.sql`. `InteractionEventConsumerIT.duplicateDelivery_appliesOnlyOnce` (E28). Bad versions go to `crm.customer.interactions.v1.DLT` (`docs/lab49/backend-demo.md`).
- Trade-off: Dedupe is in the consumer, after publish. It does not save you if the publish never happened (see Kafka down).
- Next step / residual risk: The HTTP create still makes a new interaction every time. Two clicks are two notes. We did not add an idempotency key on POST.
- Practice time (seconds): 70

### Q: What does `lab-request-001` prove?
- Claim: It ties one request to the log line, the row, the error body, and the event. It does not prove who the user is or that the cluster ran.
- Evidence (path/id): The Angular interceptor sets `X-Correlation-ID: lab-request-001` on API calls (`correlation.interceptor.ts`, E09). The filter reads `X-Correlation-Id`, keeps values that match `[A-Za-z0-9._:-]{1,64}`, otherwise makes a UUID, and echoes it (`CorrelationIdFilter`). Live 401/403/400 bodies echoed the id we sent (E20).
- Trade-off: A fixed demo id will collide in the logs if two people use it. That is fine for one demo and useless as a trace id in production.
- Next step / residual risk: Say "this is a demo constant." Do not say it proves a unique production request.
- Practice time (seconds): 70

### Q: Why is Angular not the security boundary?
- Claim: Hiding a button does not stop the HTTP call. The API checks the JWT and the role.
- Evidence (path/id): `auth-session.service.ts` keeps the token in a signal, not `localStorage` (E21). `authGuard` only redirects. The close button renders only when `session.isAdmin()` (`customer-detail-page.component.ts`). `SecurityIT` and the live DELETE both return 403 for `agent`, and Ravi stayed `PROSPECT` (E05, E19).
- Trade-off: A refresh signs the user out. That is so a script in the page cannot read a stored token.
- Next step / residual risk: Anyone can edit the signal in devtools and still get 403 from the API. We should say that out loud.
- Practice time (seconds): 65

### Q: Why isn't it deployed?
- Claim: We never got a kubeconfig or a student number, and the class fixes are not on main. The deploy workflow has zero runs.
- Evidence (path/id): E33, E37, E38. `~/.kube/config` is absent. `gh run list --workflow capstone-deploy.yml` was empty. Steps that are left are in `defense/notes/deployment-gap.md`.
- Trade-off: We can show digests and a laptop smoke note. We cannot show pods.
- Next step / residual risk: Checkout `feature/k3s-class-deploy`, create the two Secrets, dry-run `scripts/deploy-class.sh`, then deploy and smoke. Owner: deploy role. Blocked on the instructor file.
- Practice time (seconds): 80

### Q: Does CodeQL pass?
- Claim: Yes, now. It used to fail at the upload step because code scanning was not enabled. It was never a compile error in our Java.
- Evidence (path/id): `.github/workflows/codeql.yml` (E35). Up to Oct 21 2:25 PM ET it failed with `Code scanning is not enabled for this repository.` (run 39670104483). Every CodeQL run since the PR #23/#24 merges passes. Run 39777251809 on `ebda5ed` logged `Successfully uploaded results`.
- Trade-off: A green check means the scan ran and uploaded. It does not mean zero findings.
- Next step / residual risk: Read the alert list in the Security tab before claiming anything about findings. We have not reviewed it.
- Practice time (seconds): 80

### Q: What does Trivy block?
- Claim: Nothing, right now. It writes a HIGH/CRITICAL report and exits 0.
- Evidence (path/id): `exit-code: "0"` on the fs scan and both image scans in `.github/workflows/capstone-ci.yml` (E30, E39). The security job on run 39777251733 succeeded. The runbook's 2026-10-20 local count was 44 fixable highs/criticals on the backend image and 53 on the frontend. We did not re-count that today.
- Trade-off: Failing the build on that baseline would have blocked every PR before an upgrade. A green run means "the scan ran," not "the image is clean."
- Next step / residual risk: Bump Spring Boot 3.3.5 and the base images, retest, then set the image scan `exit-code` to 1. Until then, say the residual risk the way the runbook does.
- Practice time (seconds): 70

### Q: OpenShift or k3s?
- Claim: The running manifests are k3s. The course brief says OpenShift. We have not been told to switch.
- Evidence (path/id): `docs/architecture.md` ("the labs use k3s"). `deploy/k3s/`. Traefik Ingress in `deploy/k3s/base/ingress.yaml`. Risk register line: ask the instructor (E08). There is no `openshift/` tree.
- Trade-off: k3s matches the class cluster the unmerged runbook describes. It does not match the word OpenShift on the rubric.
- Next step / residual risk: Ask in the room if k3s counts. If they want OpenShift, that is new work, not a rename.
- Practice time (seconds): 60

### Q: Why PostgreSQL?
- Claim: The course stack is Postgres. Flyway owns the schema. Tests use Postgres, not H2.
- Evidence (path/id): `docs/adrs/002-postgresql.md` (E11). Image `postgres:16` in `compose.yaml` and in the CI service. `ddl-auto: validate`. `InteractionPersistenceIT` and `InteractionApiIT` in the 99-test run (E22).
- Trade-off: `mvn verify` needs Docker. That is why a laptop without Docker cannot reproduce the ITs.
- Next step / residual risk: Do not edit V1-V3 in place. Add V4 if the schema changes.
- Practice time (seconds): 55

### Q: What if the panel asks to see the JWT?
- Claim: We do not put it on the slide, in the chat, or in the notes.
- Evidence (path/id): Login returns `accessToken`. We redacted it in the experiment notes. `ProblemDetailAuthHandlers` does not echo the token. Logs are supposed to skip passwords, tokens, and summaries (`docs/lab51/security-and-observability.md`).
- Trade-off: They cannot "see" the signature. They can see `expiresIn`, the 401 body, and the code that checks HS256.
- Next step / residual risk: If one lands in a screenshot, stop the projector, do not commit the file, and treat that local token as burned (15 minute TTL).
- Practice time (seconds): 50

### Q: What fails closed?
- Claim: No token or a bad token is 401. Wrong role is 403. Unknown customer is 404 and writes nothing. Readiness goes down when Postgres is down. Liveness does not, so a DB blip does not restart-loop the pod.
- Evidence (path/id): E20, E05. `application.yml` readiness group includes `db`. Liveness group is `livenessState` only. Probes: `deploy/k3s/base/backend.yaml`. `HealthEndpointsIT` is in the verify run.
- Trade-off: `/actuator/health` is public so the probe and the smoke can call it. `show-details` is `never`. `/actuator/metrics` is ADMIN (`SecurityIT.healthIsPublic_butMetricsAreAdminOnly`).
- Next step / residual risk: We have not watched a pod leave the Service when Postgres dies, because we have not deployed.
- Practice time (seconds): 75

### Q: Do the CI checks block a merge?
- Claim: Not as required status checks. A review does.
- Evidence (path/id): E34. Branch protection on `main`: one approving review, stale reviews dismissed, last push needs approval. `required_status_checks` is null. The runbook's list of required check names is a plan. Two of those names are not the job names in the YAML: the run showed `frontend` and `backend`, while the runbook says `Frontend (Angular)` and `Backend (Maven)`. The Trivy and image job names do match.
- Trade-off: A red CodeQL check did not stop PR #22. Capstone CI was green on that merge anyway. CodeQL passes now, but nothing makes it required.
- Next step / residual risk: After the panel, require the Capstone CI job names, and CodeQL with them.
- Practice time (seconds): 60
