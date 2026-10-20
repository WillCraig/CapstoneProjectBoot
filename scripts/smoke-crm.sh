#!/usr/bin/env bash
set -euo pipefail

# Post-deploy smoke for the synthetic Amina fixture. The bearer token is read
# from the environment and is never printed. The created interaction is
# soft-deleted at the end; its audit row remains in PostgreSQL by design.
: "${CRM_BASE_URL:?set CRM_BASE_URL to the Ingress origin, without a trailing path}"
: "${CRM_SMOKE_TOKEN:?set CRM_SMOKE_TOKEN from the environment secret}"

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

echo "Checking frontend at ${BASE_URL}/"
curl --fail --silent --show-error "${BASE_URL}/" >/dev/null

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
