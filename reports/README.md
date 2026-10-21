# reports/ — sanitized evidence

The brief asks for "sanitized scan and pipeline evidence in `reports/`". Everything here
must be safe to show on a projector: synthetic fixtures only (CUS-1001 Amina, CUS-1002
Ravi, CUS-9999), and **never** a token, password, kubeconfig, Secret manifest, or real
personal data. Link each file from `defense/evidence-index.md`.

## What belongs here

| Evidence | File naming | Source |
| --- | --- | --- |
| Local test runs (backend + Angular + Selenium) | `local-test-run-<date>.md` | `mvn -B clean verify`, `ng test`, `npm run e2e` |
| CI run summary | `ci-run-<date>.md` | Actions run URL, job results, commit SHA |
| CodeQL results | `codeql-<date>.md` | Security tab summary (counts by severity, triage notes) |
| Trivy repo + image scans | `trivy-<date>.md` | SARIF artifacts from `capstone-ci.yml` (HIGH/CRITICAL table + accepted risks) |
| Image digests | `image-digests-<date>.txt` | `image-digests` artifact (`sha256:` digests only) |
| k3s deploy | `deploy-<env>-<date>.md` | Capstone Deploy run URL, digests deployed, `kubectl rollout status` output |
| Smoke | `smoke-<env>-<date>.md` | Output of `scripts/smoke-crm.sh` (it never prints credentials) |
| Rollback | `rollback-<env>-<date>.md` | Previous digest redeployed + smoke re-run |

Sanitize before committing: strip `Authorization` headers and JWTs (`eyJ...`), cluster
hostnames/IPs if the instructor asks, and any non-synthetic data.

## Status (2026-10-21)

| Evidence | Status |
| --- | --- |
| Local test run | ✅ [local-test-run-2026-10-21.md](local-test-run-2026-10-21.md) |
| CI run (Java + Angular) | ⏳ needs the branch pushed and a PR / main run |
| CodeQL | ⏳ runs on push / PR (`codeql.yml`) |
| Trivy repo + image scans | ⏳ produced by `capstone-ci.yml` on push / PR |
| Image digests | ⏳ only on push to `main` (GHCR push) |
| k3s deploy (non-dry-run) | ⏳ needs digests, GitHub Environment secrets (incl. new `CRM_JWT_SECRET`, `CRM_AGENT_PASSWORD`, `CRM_ADMIN_PASSWORD`, `CRM_SMOKE_AGENT_PASSWORD`) and the assigned namespace/host |
| Smoke against the Ingress | ⏳ runs inside the deploy workflow |
| Rollback | ⏳ redeploy the previous digest pair, then smoke |
