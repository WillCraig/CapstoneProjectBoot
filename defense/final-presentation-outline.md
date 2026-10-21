# Final presentation outline

11 slides. No PDF in the repo. Build the deck from this file.
Speaker on every slide: TBD. About 12 minutes of talking, then the demo is its own block inside slide 5, then questions.
Evidence IDs are in `defense/evidence-index.md`. No claim without an ID.

## 1. Users and the problem

- Service agents look up a customer and write down the call, email, or note.
- Northstar CRM is that slice, not a full CRM.
- Synthetic people only: Amina Khan `CUS-1001`, Ravi Singh `CUS-1002`.
- Success for the slice is CAP-12.

Evidence: E01, E02

Speaker notes: Start with the agent, not the tools. One sentence on who uses it and what "done" means. Point at Amina so the demo later is the same person.

## 2. What we are willing to call done

- Note saved for `CUS-1001`, row in Postgres, one `CustomerInteractionRecorded` event.
- Unknown id `CUS-9999` does not write a row.
- Admin closes a customer. An agent cannot.
- Cluster deploy is not in the done column.

Evidence: E02, E07, E08

Speaker notes: Read the done list, then say the deploy line out loud so we do not get caught later. The NFR page is where the unmet rollback target lives.

## 3. Architecture, from the ADRs

- Angular 19 calls Spring Boot 3.3 on Java 21.
- JWT at the API. Postgres 16 is the record. Flyway V1-V3.
- Kafka after the commit. Topics `crm.customer.interactions.v1` and `crm.customer.lifecycle.v1`.
- No outbox. A down broker can drop the event.
- GitHub Actions builds once. The deploy unit is the digest. Target cluster is k3s, not OpenShift, until someone says otherwise.

Evidence: E01, E10, E11, E12

Speaker notes: Three decisions, not a tool tour. Postgres because that is the course database and the tests use it. Kafka after commit so a rollback does not publish. Digests so we do not rebuild on the way to the cluster. Mention OpenShift as an open question, not as a feature.

## 4. The slice, before we click

- Sign in as `agent`.
- Search Amina, open `CUS-1001`.
- Record a NOTE. Correlation `lab-request-001`.
- Show the SQL row and the Kafka line.
- Then a 400 and a 403. Those are curls, on purpose.

Evidence: E03, E04, E09, E21

Speaker notes: Tell them the path so a missed click is not a surprise. Say the close button will be missing for the agent and that the 403 comes from curl. The form cannot submit a bad interaction type.

## 5. Live demo

- Follow `defense/demo-script.md`. About 11 minutes.
- Speaker talks. Operator drives. Names are TBD.
- If the API dies, use `defense/notes/fallback-pack.md`.
- Keep the 403 even if we cut the 404.

Evidence: E05, E27, E29, demo script

Speaker notes: Do not narrate every click. Say what the agent is trying to do, then let the operator catch up. The correlation id on the list page is the same id we will grep in the log.

## 6. Security

- `POST /api/v1/auth/login`. HS256. 15 minutes. No cookie.
- `agent` is AGENT. `admin` is AGENT and ADMIN.
- 401 if the token is missing or bad. 403 if the role is wrong. Both are Problem Details with `correlationId`.
- Close and reopen are ADMIN in `CustomerService` and, for DELETE, in `SecurityConfig`. Commit `8e573e9`.
- Angular stores the token in memory. The guard is UX.

Evidence: E17, E18, E19, E20, E21

Speaker notes: Offer to show the 403 body, not the token. If they ask where validation happens, say `NimbusJwtDecoder` in `SecurityConfig`. If they ask about a stolen token, say it works until the 15 minutes are up. We have no revoke list.

## 7. CI and the digests

- Run 39649860445 on `0235018`, pushed 2026-10-21 2:07 PM ET. Conclusion success.
- Frontend 43 tests and a prod build. Backend `mvn verify`, 99 tests, Postgres service plus Testcontainers.
- Trivy fs and image scans ran with `exit-code` 0.
- Images: backend `sha256:4329fe9b…1cd3`, frontend `sha256:599a2986…5f72`.
- JAR sha256 `3b260400cb99…10f7c7` from the package-once job.
- CodeQL on that same push failed. It is a different workflow.

Evidence: E23, E30, E31, E32, E35, E39

Speaker notes: Read the two digests slowly or point at them. Say green CI means tests passed and reports were uploaded. It does not mean Trivy found zero issues. Then say CodeQL is red because code scanning is not enabled, and do not blend that into the green run.

## 8. Deploy: not run, and the plan

- Capstone Deploy has never been started. Dry-run is the default. It only accepts digests.
- No kubeconfig on this laptop. Student number not confirmed.
- Fixes for the class cluster are on `feature/k3s-class-deploy` (uid 10001, in-namespace Kafka, `deploy-class.sh`).
- Those manifests pin the digests from slide 7.
- Rollback is written. It has not been executed.

Evidence: E06, E33, E37, E38

Speaker notes: This is the gap. Do not apologize for a full minute. State it, show the four commits, and say the next command is `STUDENT=studentNN DRY_RUN=true scripts/deploy-class.sh` once we have the yaml. `kubectl rollout undo` is the emergency path after a real deploy, not something we can demo today.

## 9. What the evidence does not cover

- No cluster smoke and no rollback.
- Trivy does not block.
- CodeQL has no result list.
- Selenium is local only.
- PR #23 (confetti) and PR #24 (show password) are open. They are not in this demo.
- Branch protection requires one review. It does not require the CI jobs.
- The laptop database has two extra customers. We will not use them.

Evidence: E34, E35, E39, E40, E25, E27

Speaker notes: Pick three, not all of them, unless someone asks. The three that matter are deploy, Trivy exit 0, and CodeQL. The open PRs are so nobody thinks confetti is part of the defended build.

## 10. Retro and next steps

- Five actions in `defense/retrospective.md`. Each has a role and a measure.
- First one is the class deploy and the CUS-1001 smoke line.
- Then a recorded fallback, required CI checks, a CodeQL decision, and a Trivy fail-after-upgrade.
- Kafka without an outbox stays an accepted risk until the backend role builds one.

Evidence: E08, E10, retro

Speaker notes: Do not read the whole retro. Say what we would do on Monday: kubeconfig, dry-run, deploy, smoke. Mention that we have not had the team retro meeting yet, so the actions are from the repo review.

## 11. Questions

- Answers are in `defense/technical-q-and-a.md`.
- If we do not know, say unknown and name the file or command we would check.
- Do not open `application-local.yml` or a kubeconfig.

Evidence: Q&A file, E17, E29, E35

Speaker notes: Likely questions are JWT, Kafka down, why it is not deployed, CodeQL, and Trivy. The 403 curl can stay in a terminal. Unset the token before you sit down.
