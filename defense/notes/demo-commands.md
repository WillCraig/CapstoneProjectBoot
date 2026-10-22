# Northstar CRM demo: commands, in order

Run everything from the repo root on main (on this laptop: ~/TEK/CapstoneProjectBoot). API is http://localhost:8081, UI is http://localhost:4200 (use "localhost", not 127.0.0.1, for the UI).

## 0. Before anyone walks in (about 10 min early)

Check what is already running (don't start a second copy):

    docker ps --format '{{.Names}}  {{.Status}}'
    lsof -nP -iTCP:8081 -iTCP:4200 -sTCP:LISTEN

If nothing is up, start it. Terminal 1:

    cd ~/TEK/CapstoneProjectBoot
    git switch main && git pull
    cp -n .env.example .env
    docker compose up -d

Terminal 2 (backend, leave it visible, the Kafka log lines show up here):

    cd ~/TEK/CapstoneProjectBoot/backend
    SPRING_PROFILES_ACTIVE=local SERVER_PORT=8081 mvn spring-boot:run

Terminal 3 (frontend):

    cd ~/TEK/CapstoneProjectBoot/frontend
    npm ci
    npx ng serve

Health check (expect {"status":"UP"}):

    curl -sS http://localhost:8081/actuator/health/readiness; echo

Fixture check (expect Amina ACTIVE, Ravi PROSPECT):

    docker exec crm-postgres psql -U crm -d crm -c "SELECT public_id, full_name, status FROM customer WHERE public_id IN ('CUS-1001','CUS-1002');"

## 1. Get a token right before you start (it dies in 15 minutes)

In the curl terminal. This asks for the password without showing it (the local agent password is in README.md):

    read -rs 'PW?agent password: '; echo
    TOKEN=$(curl -sS -X POST http://localhost:8081/api/v1/auth/login \
      -H 'Content-Type: application/json' \
      -d "{\"username\":\"agent\",\"password\":\"$PW\"}" \
      | python3 -c 'import json,sys; print(json.load(sys.stdin)["accessToken"])')
    unset PW
    [ -n "$TOKEN" ] && echo "token ready" || echo "login failed"

Never run `echo $TOKEN`.

The script `bash defense/demo-walkthrough.sh` (run from the repo root) does steps 3 to 7 below in one go, with a check mark per step. Use the raw curls here if the script breaks.

## 2. UI part (click, no commands)

- http://localhost:4200/login, user agent, Sign in. Header should say "agent (AGENT)".
- Look up a customer, search Amina, open Profile on CUS-1001.
- Point at "Only an ADMIN can close a customer."
- Record an interaction: NOTE, summary "Resolved login question", create.

## 3. Prove the note is in Postgres

    docker exec crm-postgres psql -U crm -d crm -c "SELECT c.public_id, i.interaction_type, i.correlation_id, i.deleted_at IS NULL AS visible FROM customer_interaction i JOIN customer c ON c.customer_id = i.customer_id WHERE i.correlation_id = 'lab-request-001' ORDER BY i.created_at DESC LIMIT 5;"

Expect CUS-1001, NOTE, lab-request-001, visible = t.

## 4. Prove the Kafka event went out

First look at Terminal 2 for "Publishing" and "Applied" with correlationId=lab-request-001. If you need more:

    docker exec crm-kafka /opt/kafka/bin/kafka-console-consumer.sh \
      --bootstrap-server localhost:9092 \
      --topic crm.customer.interactions.v1 \
      --from-beginning --timeout-ms 8000

Point at correlationId and customerId. The summary is not in the event on purpose.

## 5. Bad input gets a clean 400

    curl -sS -i -X POST http://localhost:8081/api/v1/interactions \
      -H "Authorization: Bearer $TOKEN" \
      -H 'Content-Type: application/json' \
      -H 'X-Correlation-Id: lab-request-001' \
      -d '{"customerId":"CUS-1001","interactionType":"TEXT","summary":"bad type","correlationId":"lab-request-001"}'

Expect 400, "Validation failed", and a message listing CALL, EMAIL, NOTE, MEETING.

## 6. THE KEY MOMENT: agent tries to close Ravi, gets 403

    curl -sS -i -X DELETE http://localhost:8081/api/v1/customers/CUS-1002 \
      -H "Authorization: Bearer $TOKEN" \
      -H 'X-Correlation-Id: lab-request-001'

Expect 403, "Your role does not allow this action.", correlationId echoed. Then show Ravi didn't change:

    curl -sS http://localhost:8081/api/v1/customers/CUS-1002 \
      -H "Authorization: Bearer $TOKEN"; echo

Expect status PROSPECT.

Optional, same 403 through the "sneaky" route (edit status to CLOSED):

    curl -sS -i -X PUT http://localhost:8081/api/v1/customers/CUS-1002 \
      -H "Authorization: Bearer $TOKEN" \
      -H 'Content-Type: application/json' \
      -H 'X-Correlation-Id: lab-request-001' \
      -d '{"fullName":"Ravi Singh","status":"CLOSED"}'

## 7. Optional extras (skip if past 9:30)

Unknown customer, expect 404:

    curl -sS -i http://localhost:8081/api/v1/customers/CUS-9999 \
      -H "Authorization: Bearer $TOKEN"

Fake token, expect 401:

    curl -sS -i http://localhost:8081/api/v1/customers/CUS-1001 \
      -H 'Authorization: Bearer not-a-real-jwt'

## 8. CI and digests (browser)

https://github.com/WillCraig/CapstoneProjectBoot/actions/runs/39777251733
Read the two digest lines. Say "checks run on every PR" and "Trivy scans and saves a report".

## 9. Deploy gap (file)

Open defense/notes/deployment-gap.md. Not deployed yet; fix is on feature/k3s-class-deploy.

## 10. Clean up

    unset TOKEN

## If something breaks

- UI dies: keep going with the curls above.
- API dies: stop typing, open defense/notes/fallback-pack.md and read the saved 403 from defense/notes/failure-experiments.md.
- Kafka console empty: "The DB row is the source of truth; the event can be late."
