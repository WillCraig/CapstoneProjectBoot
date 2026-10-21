#!/usr/bin/env bash
set -euo pipefail

# Post-deploy smoke for the synthetic Amina fixture: readiness, frontend, login,
# then create/read/soft-delete one CUS-1001 interaction. Credentials and the
# issued JWT come from the environment and are never printed. The created
# interaction is soft-deleted at the end; its audit row remains by design.
#
#   CRM_BASE_URL         Ingress origin (or http://localhost:8080 for a laptop run)
#   CRM_SMOKE_USERNAME   synthetic AGENT user (default: agent)
#   CRM_SMOKE_PASSWORD   that user's password  -- or --
#   CRM_SMOKE_TOKEN      an already-issued JWT (skips the login step)
#   CRM_SMOKE_SKIP_FRONTEND=true  when only the backend is running locally
: "${CRM_BASE_URL:?set CRM_BASE_URL to the Ingress origin, without a trailing path}"

CUSTOMER_ID="${CRM_SMOKE_CUSTOMER_ID:-CUS-1001}"
CORRELATION_ID="${CRM_SMOKE_CORRELATION_ID:-lab-request-001}"
RUN_MARKER="${GITHUB_RUN_ID:-local}"
SUMMARY="Lab 51 smoke ${RUN_MARKER}"
BASE_URL="${CRM_BASE_URL%/}"
API="${BASE_URL}/api/v1/interactions"

cleanup_needed=false
interaction_id=""

cleanup() {
  if [[ "$cleanup_needed" == "true" && -n "$interaction_id" ]]; then
    curl --fail --silent --show-error \
      --request DELETE \
      --header "Authorization: Bearer ${CRM_SMOKE_TOKEN}" \
      --header "X-Correlation-ID: ${CORRELATION_ID}" \
      "${API}/${interaction_id}" >/dev/null || true
  fi
}
trap cleanup EXIT

echo "Checking backend readiness at ${BASE_URL}/actuator/health/readiness"
readiness="$(curl --fail --silent --show-error "${BASE_URL}/actuator/health/readiness")"
printf '%s' "$readiness" | grep -Fq '"status":"UP"'

if [[ "${CRM_SMOKE_SKIP_FRONTEND:-false}" != "true" ]]; then
  echo "Checking frontend at ${BASE_URL}/"
  curl --fail --silent --show-error "${BASE_URL}/" >/dev/null
fi

if [[ -z "${CRM_SMOKE_TOKEN:-}" ]]; then
  : "${CRM_SMOKE_PASSWORD:?set CRM_SMOKE_PASSWORD (or CRM_SMOKE_TOKEN) from the environment secret}"
  echo "Logging in as ${CRM_SMOKE_USERNAME:-agent}"
  login_body="$(
    CRM_SMOKE_USERNAME="${CRM_SMOKE_USERNAME:-agent}" CRM_SMOKE_PASSWORD="$CRM_SMOKE_PASSWORD" python3 -c \
      'import json,os; print(json.dumps({"username": os.environ["CRM_SMOKE_USERNAME"], "password": os.environ["CRM_SMOKE_PASSWORD"]}))'
  )"
  CRM_SMOKE_TOKEN="$(
    printf '%s' "$login_body" | curl --fail --silent --show-error \
      --request POST \
      --header 'Content-Type: application/json' \
      --header "X-Correlation-ID: ${CORRELATION_ID}" \
      --data @- \
      "${BASE_URL}/api/v1/auth/login" \
      | python3 -c 'import json,sys; print(json.load(sys.stdin)["accessToken"])'
  )"
  [[ -n "$CRM_SMOKE_TOKEN" ]]
fi

echo "Creating a ${CUSTOMER_ID} smoke interaction"
create_response="$(
  curl --fail --silent --show-error \
    --request POST \
    --header 'Content-Type: application/json' \
    --header "Authorization: Bearer ${CRM_SMOKE_TOKEN}" \
    --header "X-Correlation-ID: ${CORRELATION_ID}" \
    --data "{\"customerId\":\"${CUSTOMER_ID}\",\"interactionType\":\"NOTE\",\"summary\":\"${SUMMARY}\",\"correlationId\":\"${CORRELATION_ID}\"}" \
    "$API"
)"

interaction_id="$(
  printf '%s' "$create_response" | python3 -c 'import json,sys; print(json.load(sys.stdin)["id"])'
)"
[[ -n "$interaction_id" ]]
cleanup_needed=true

echo "Reading the ${CUSTOMER_ID} timeline"
list_response="$(
  curl --fail --silent --show-error \
    --get \
    --header "Authorization: Bearer ${CRM_SMOKE_TOKEN}" \
    --header "X-Correlation-ID: ${CORRELATION_ID}" \
    --data-urlencode "customerId=${CUSTOMER_ID}" \
    "$API"
)"
printf '%s' "$list_response" | grep -Fq "$interaction_id"
printf '%s' "$list_response" | grep -Fq "$SUMMARY"

echo "Soft-deleting the smoke interaction"
curl --fail --silent --show-error \
  --request DELETE \
  --header "Authorization: Bearer ${CRM_SMOKE_TOKEN}" \
  --header "X-Correlation-ID: ${CORRELATION_ID}" \
  "${API}/${interaction_id}" >/dev/null
cleanup_needed=false

echo "Smoke passed for ${CUSTOMER_ID} with correlation ${CORRELATION_ID}"
