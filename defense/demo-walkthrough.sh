#!/usr/bin/env bash
# Northstar CRM demo walkthrough. Press Enter to move between steps.
# Usage, from the repo root: bash defense/demo-walkthrough.sh
# Run it AFTER you saved the note in the UI.
# Nothing here changes data except the 400/403 attempts, which the API rejects.

API="${API:-http://localhost:8081}"
CID="lab-request-001"
AUTO="${AUTO:-0}"   # AUTO=1 skips the pauses (for testing)

bold=$(tput bold 2>/dev/null); dim=$(tput dim 2>/dev/null)
green=$(tput setaf 2 2>/dev/null); red=$(tput setaf 1 2>/dev/null)
blue=$(tput setaf 4 2>/dev/null); reset=$(tput sgr0 2>/dev/null)

step()  { echo; echo "${bold}${blue}== $1 ==${reset}"; echo "$2"; }
show()  { echo "${dim}\$ $1${reset}"; }
ok()    { echo "${green}✔ $1${reset}"; }
bad()   { echo "${red}✘ $1${reset}"; }
pause() { [ "$AUTO" = "1" ] && return; echo; read -r -p "${dim}Press Enter to continue...${reset}" _; }
pretty(){ python3 -m json.tool 2>/dev/null || cat; }
psqlq() { docker exec crm-postgres psql -U crm -d crm -At -c "$1"; }

# call METHOD PATH [JSON]: prints status + pretty body, sets $STATUS and $BODY
call() {
  local method="$1" path="$2" data="$3"
  local args=(-sS -o /tmp/demo_body.$$ -w '%{http_code}' -X "$method" "$API$path"
              -H "X-Correlation-Id: $CID")
  [ -n "$TOKEN" ] && args+=(-H "Authorization: Bearer $TOKEN")
  [ -n "$data" ] && args+=(-H 'Content-Type: application/json' -d "$data")
  STATUS=$(curl "${args[@]}")
  BODY=$(cat /tmp/demo_body.$$); rm -f /tmp/demo_body.$$
  echo "HTTP ${bold}$STATUS${reset}"
  [ -n "$BODY" ] && echo "$BODY" | pretty
}

trap 'unset TOKEN' EXIT

# ---------------------------------------------------------------
step "1. Is the app up?" "Checking the API's readiness endpoint."
show "curl $API/actuator/health/readiness"
if curl -sS "$API/actuator/health/readiness" | grep -q '"UP"'; then
  ok "API is up"
else
  bad "API is not answering. Start the backend first (see defense/notes/demo-commands.md)."; exit 1
fi
pause

# ---------------------------------------------------------------
step "2. Our two test customers" "Amina should be ACTIVE, Ravi should be PROSPECT."
show "SELECT public_id, full_name, status FROM customer ..."
docker exec crm-postgres psql -U crm -d crm -c \
  "SELECT public_id, full_name, status FROM customer WHERE public_id IN ('CUS-1001','CUS-1002') ORDER BY public_id;"
SEEDS=$(psqlq "SELECT string_agg(public_id || '=' || status, ',' ORDER BY public_id) FROM customer WHERE public_id IN ('CUS-1001','CUS-1002');")
[ "$SEEDS" = "CUS-1001=ACTIVE,CUS-1002=PROSPECT" ] && ok "Amina is ACTIVE, Ravi is PROSPECT" || bad "Seed rows look wrong: $SEEDS"
pause

# ---------------------------------------------------------------
step "3. Sign in as the agent" "The token stays in this shell only. It is never printed."
if [ -z "$DEMO_PASSWORD" ]; then
  read -r -s -p "agent password: " DEMO_PASSWORD; echo
fi
show "POST $API/api/v1/auth/login  (user: agent)"
TOKEN=$(curl -sS -X POST "$API/api/v1/auth/login" -H 'Content-Type: application/json' \
  -d "{\"username\":\"agent\",\"password\":\"$DEMO_PASSWORD\"}" \
  | python3 -c 'import json,sys; print(json.load(sys.stdin).get("accessToken",""))' 2>/dev/null)
unset DEMO_PASSWORD
if [ -n "$TOKEN" ]; then ok "Signed in as agent (token good for 15 minutes)"; else bad "Login failed"; exit 1; fi
pause

# ---------------------------------------------------------------
step "4. The note is in Postgres" "Postgres is the source of truth. Looking for correlation $CID."
show "SELECT ... FROM customer_interaction WHERE correlation_id = '$CID'"
docker exec crm-postgres psql -U crm -d crm -c \
  "SELECT c.public_id, i.interaction_type, i.correlation_id, i.deleted_at IS NULL AS visible
   FROM customer_interaction i JOIN customer c ON c.customer_id = i.customer_id
   WHERE i.correlation_id = '$CID' ORDER BY i.created_at DESC LIMIT 3;"
NOTES=$(psqlq "SELECT count(*) FROM customer_interaction i JOIN customer c ON c.customer_id = i.customer_id
   WHERE i.correlation_id = '$CID' AND c.public_id = 'CUS-1001' AND i.interaction_type = 'NOTE' AND i.deleted_at IS NULL;")
[ "${NOTES:-0}" -gt 0 ] && ok "Found a visible NOTE for CUS-1001" || bad "No visible NOTE for CUS-1001 yet. Save one in the UI first."
pause

# ---------------------------------------------------------------
step "5. The event went out on Kafka" "After the save, we publish CustomerInteractionRecorded. The summary is NOT in the event."
show "kafka-console-consumer --topic crm.customer.interactions.v1"
docker exec crm-kafka /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 --topic crm.customer.interactions.v1 \
  --from-beginning --timeout-ms 8000 2>/dev/null | grep "$CID" | grep 'CUS-1001' | tail -1 > /tmp/demo_event.$$
if [ -s /tmp/demo_event.$$ ]; then pretty < /tmp/demo_event.$$; ok "Event found for CUS-1001"; else bad "No event found for $CID"; fi
rm -f /tmp/demo_event.$$
pause

# ---------------------------------------------------------------
step "6. Bad input gets a clean error" "Sending a type of TEXT, which isn't allowed. Expect 400, not a stack trace."
show "POST $API/api/v1/interactions  {interactionType: TEXT}"
call POST /api/v1/interactions \
  "{\"customerId\":\"CUS-1001\",\"interactionType\":\"TEXT\",\"summary\":\"bad type\",\"correlationId\":\"$CID\"}"
[ "$STATUS" = "400" ] && ok "Rejected with 400" || bad "Expected 400, got $STATUS"
pause

# ---------------------------------------------------------------
step "7. An agent tries to close Ravi" "The UI already hides the Close button. This goes straight at the API. Expect 403."
show "DELETE $API/api/v1/customers/CUS-1002  (as agent)"
call DELETE /api/v1/customers/CUS-1002
[ "$STATUS" = "403" ] && ok "Blocked with 403" || bad "Expected 403, got $STATUS"
echo
echo "And Ravi didn't change:"
show "GET $API/api/v1/customers/CUS-1002"
call GET /api/v1/customers/CUS-1002 >/dev/null
RAVI=$(echo "$BODY" | python3 -c 'import json,sys; print(json.load(sys.stdin).get("status",""))' 2>/dev/null)
[ "$RAVI" = "PROSPECT" ] && ok "Ravi is still PROSPECT" || bad "Ravi status is '$RAVI'"
pause

# ---------------------------------------------------------------
step "8. Unknown customer" "Optional. Expect 404."
show "GET $API/api/v1/customers/CUS-9999"
call GET /api/v1/customers/CUS-9999
[ "$STATUS" = "404" ] && ok "404 as expected" || bad "Expected 404, got $STATUS"

pause

# ---------------------------------------------------------------
step "9. A fake token" "No real sign-in, no access. Expect 401."
show "GET $API/api/v1/customers/CUS-1001  (Authorization: Bearer not-a-real-jwt)"
REAL="$TOKEN"; TOKEN="not-a-real-jwt"
call GET /api/v1/customers/CUS-1001
TOKEN="$REAL"; unset REAL
[ "$STATUS" = "401" ] && ok "Rejected with 401" || bad "Expected 401, got $STATUS"

echo
ok "Demo done. Token cleared."
