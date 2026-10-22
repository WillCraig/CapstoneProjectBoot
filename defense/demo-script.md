# Demo script — Northstar CRM

**Total:** about 11 minutes, plus questions after.
**Fixtures:** `CUS-1001` Amina Khan, `CUS-1002` Ravi Singh, correlation `lab-request-001`.
**Roles:** Speaker: TBD / Operator: TBD.
**Where:** UI http://localhost:4200 and API http://localhost:8081. Stack notes: `defense/notes/demo-validation.md`.

If the laptop API dies, say the failover line in `defense/notes/fallback-pack.md` and stop clicking.

## Pre-seed (before they walk in)

| # | Confirm | What we know |
| --- | --- | --- |
| 1 | Amina (`CUS-1001`) searchable | API GET returned Amina Khan, ACTIVE. UI search was not clicked in this session. |
| 2 | Ravi (`CUS-1002`) searchable | API GET returned Ravi Singh, PROSPECT. |
| 3 | Agent login works | Login as `agent` returned 200, `expiresIn` 900. Do not mint the token early. It dies in 15 minutes. Password is the local one in `README.md`. Do not put it on a slide. |
| 4 | Kafka console bookmarked | Command in `defense/notes/demo-validation.md`. Binary path checked on `crm-kafka`. |
| 5 | SQL ready | `docker exec crm-postgres psql -U crm -d crm` works. Query is in the validation notes. |
| 6 | Fallback pack open | `defense/notes/fallback-pack.md`. No screenshot folder. |

Also: the lookup list has two extra local customers (`CUS-1003`, `CUS-1004`). Search Amina. Do not scroll the full list on the projector.

Sign in as **agent** for the whole demo. The close button will be hidden. The 403 is a curl.

## Timed beats

From 5:15 to 9:15 the operator can run `bash defense/demo-walkthrough.sh` from the repo root instead of typing curls. It asks for the agent password (from `README.md`, it does not show), never prints the token, and puts a check mark on each step. Raw commands are in `defense/notes/demo-commands.md`. A saved green run is in `defense/notes/walkthrough-run.txt`.

| Time | Speaker | Operator | Evidence |
| --- | --- | --- | --- |
| 0:00 | Agents need one place to find a customer and write down the call. That is the slice. Success is CAP-12: save a note for Amina, keep it in Postgres, publish one event. | Show the home screen, or the title slide if the UI is not up yet. | E02, E01 |
| 0:45 | Angular talks to Spring. Spring checks the JWT, writes Postgres, then Kafka. We deploy image digests, not a rebuild. | If a diagram slide exists, leave it up. Otherwise stay on the sign-in page. | E01, E10, E12 |
| 1:20 | Sign in as the agent. The token stays in memory. A refresh logs you out. The page is not the security check. | Open http://localhost:4200/login. Username `agent`. Password from `README.md`. Click **Sign in**. Expect the header `agent (AGENT)` and the home heading "What would you like to do?" | E18, E21 |
| 2:20 | Look up Amina. She is the seeded active customer, `CUS-1001`. | Click **Look up a customer**. Search `Amina`. Click **Search**. Open **Profile** on the `CUS-1001` row. Expect name Amina Khan, id `CUS-1001`, status ACTIVE. | E04, E27 |
| 3:20 | An agent does not get a Close button. The line on the page is only a hint. The API is what blocks it. We will show the 403 at the end so we do not close Ravi by mistake. | Point at `Only an ADMIN can close a customer.` (`data-testid="close-requires-admin"`). Do not hunt for a button. | E19, E21 |
| 4:00 | Record a note. The app sends correlation `lab-request-001` on API calls. | Click **Record an interaction**. Type `NOTE` if it is not already selected. Summary: `Resolved login question`. Click create. Expect the detail page for a new id, customer `CUS-1001`. | E03, E09 |
| 5:15 | The row is in Postgres. That is the source of truth. | Script step 4 (steps 1-3 are health, seed rows, sign-in; press Enter through them). Or run the SQL in `demo-validation.md` for `lab-request-001`. Expect `CUS-1001`, `NOTE`, that correlation, visible true. | E04, E11 |
| 6:15 | After commit we publish `CustomerInteractionRecorded` on `crm.customer.interactions.v1`. The summary is not in the event. | In the backend terminal, find `Publishing` and `Applied` with `correlationId=lab-request-001`. If the terminal is not on screen, script step 5 runs the kafka-console-consumer. Point at `correlationId` and `customerId`. | E10, E09 |
| 7:15 | Bad input does not get a stack trace. It gets Problem Details. The form's dropdown cannot send a bad type, so this one is a curl. | Script step 6. It sends `POST /api/v1/interactions` type `TEXT`, header `X-Correlation-Id: lab-request-001`. Expect **400**, `detail` `Validation failed`, `errors` mentioning CALL, EMAIL, NOTE, or MEETING. | E05, `defense/notes/failure-experiments.md` |
| 8:15 | Same agent, close Ravi. The UI hid the button. The API still says no. | Script step 7. It sends `DELETE /api/v1/customers/CUS-1002` with the agent token and `X-Correlation-Id: lab-request-001`. Expect **403**, `detail` `Your role does not allow this action.`, and the correlation echoed. Then it reads Ravi back and shows `Ravi is still PROSPECT`. | E19, E20 |
| 9:15 | Optional if you are ahead: `CUS-9999` is not a customer. | Script step 8, `GET /api/v1/customers/CUS-9999`. Expect **404** `Unknown customer: CUS-9999`. Step 9 (fake token, **401**) follows. Skip both if you are past 9:30. | E02 |
| 9:30 | CI on main run 39777251733 (merge of PR #25, `ebda5ed`) passed. Frontend 56 tests, backend 99. Images were pushed by digest. Trivy wrote reports and did not fail the job. CodeQL passes on main now. | Open the Actions run. Read the two digest lines. Do not open a kubeconfig. | E23, E31, E35, E39 |
| 10:20 | We have not deployed. The class fixes are on `feature/k3s-class-deploy`, not main. Rollback is a new deploy of the previous digests, or `kubectl rollout undo` after a real rollout. Neither has been run. | Show `defense/notes/deployment-gap.md` or the rollback section of `docs/capstone-cicd-runbook.md`. | E06, E33, E37, E38 |
| 10:50 | What we can stand behind is the slice, the tests, and the digests. What we cannot stand behind is a running cluster. Next step is the student namespace, then smoke `CUS-1001`. | Stop. Leave the profile or the Actions tab up for questions. `unset TOKEN`. | E07, E08 |

## Controlled failure

The 8:15 beat is the one we keep if we have to cut. 403, correlation echoed, Ravi unchanged.

If that call fails because the API is down, read the saved 403 body in `defense/notes/walkthrough-run.txt` (step 7, Oct 22) or `defense/notes/failure-experiments.md` (Oct 21). Say it was captured on this laptop, not on the cluster.

## If you are long at 7:00

Drop the architecture sentence you already said, drop `CUS-9999`, and keep sign-in, Amina, the note, the 403, and the digest. Do not drop the 403.

## Close, if they ask "one more week"

Get the kubeconfig, deploy the class overlay, smoke CUS-1001, and only then practice rollback. Until that smoke line exists, the deploy slide stays a plan.
