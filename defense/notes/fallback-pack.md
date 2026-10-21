# Fallback pack

Use this when the live click-through breaks. One person calls the switch. Do not argue in front of the panel.

Say this:

> The live cluster is not deployed. We are switching to the laptop API. If that is down too, we use the notes in defense/notes and the Capstone CI run 39649860445, plus the Lab 50 SQL note for CUS-1001. Here is what that evidence does and does not prove.

What it proves: the slice ran on a laptop (health, login, CUS-1001, a saved interaction, a Kafka apply when the broker was up, 401 and 403). CI on `0235018` built images and printed digests.

What it does not prove: a pod on the class cluster, an Ingress smoke, or a rollback.

## When to switch

| What broke | Switch to |
| --- | --- |
| UI on 4200 | API curls below. Say the page is down. |
| API on 8081 | Stop curling. Open this file and `reports/local-test-run-2026-10-21.md`. |
| Kafka console empty | The DB row is the source of truth (`docs/adrs/001-event-contract.md`). Say the event may be late or lost. |
| Projector or login | Read the CI digest lines and the 403 body in `failure-experiments.md`. |

No screenshot folder. The lab said not to commit screenshots, and none were taken.

## Checklist (open these before the panel)

- [ ] This file
- [ ] `defense/notes/failure-experiments.md` (401 and 403 bodies)
- [ ] `defense/notes/demo-validation.md`
- [ ] `reports/local-test-run-2026-10-21.md` (smoke line and Selenium line)
- [ ] `docs/lab50/frontend-persistence-demo.md` SQL section (dated 2026-10-20, old demo token)
- [ ] Actions run https://github.com/WillCraig/CapstoneProjectBoot/actions/runs/39649860445
- [ ] `docs/capstone-cicd-runbook.md` rollback section
- [ ] `defense/notes/deployment-gap.md`

## Curls if the UI dies and the API is still up

Get a token in the shell. Do not echo it.

```bash
TOKEN=$(curl -sS -X POST http://127.0.0.1:8081/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"agent","password":"<from README>"}' \
  | python3 -c 'import json,sys; print(json.load(sys.stdin)["accessToken"])')

curl -sS -H "Authorization: Bearer $TOKEN" \
  http://127.0.0.1:8081/api/v1/customers/CUS-1001

curl -i -X POST http://127.0.0.1:8081/api/v1/interactions \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -H 'X-Correlation-Id: lab-request-001' \
  -d '{"customerId":"CUS-1001","interactionType":"NOTE","summary":"Resolved login question","correlationId":"lab-request-001"}'
```

Expect 201, then the SQL in `demo-validation.md`.

Deny, still as agent:

```bash
curl -i -X DELETE http://127.0.0.1:8081/api/v1/customers/CUS-1002 \
  -H "Authorization: Bearer $TOKEN" \
  -H 'X-Correlation-Id: lab-request-001'
```

Expect 403 and Ravi still `PROSPECT`.

`unset TOKEN` when you are done.

## If they ask for a cluster command

`kubectl` on this machine has no current context. Do not run `rollout undo` against nothing. Show the command in the runbook and say it has not been run.
