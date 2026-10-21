# GitHub Actions plan (planned in Lab 48, built in Lab 51)

Three workflows in `.github/workflows/`. Angular and Spring Boot are both in scope, and each has its own gate. All jobs run on GitHub-hosted `ubuntu-latest` runners ([ADR 003](adrs/003-github-actions-digest-promotion.md)). Step-by-step operation and secret names: [CI/CD runbook](capstone-cicd-runbook.md).

## By trigger

| Trigger | Jobs | Gates | Evidence |
| ------- | ---- | ----- | -------- |
| `pull_request` | `capstone-ci`: `frontend` (`npm ci`, `ng test --watch=false --browsers=ChromeHeadless`, `ng build --configuration=production`), `backend` (`mvn -B clean verify` with Testcontainers plus a `postgres:16` service), `security-scan` (Trivy fs: vuln, secret, config), `container-images` (build both images from the verified artifacts, then Trivy image scan, without pushing). `codeql`: Java + TypeScript | A failing Angular or Maven test, or a build error, fails the run. The image job `needs` all three earlier jobs | Run logs, `frontend-dist-<sha>`, `backend-verified-jar-<sha>`, `trivy-repository-<sha>`, `trivy-images-<sha>` artifacts |
| push to `main` | Same jobs, then push both images to GHCR, record the registry digests, and `package-once` (JAR plus `SHA256SUMS`) | Same as PR | `image-digests-<sha>` and `crm-package-<sha>` artifacts, digests in the job summary |
| push of a `v*` tag | Same publication path as `main`. No automatic deploy | Same as PR | Digest pair to hand to Capstone Deploy |
| weekly (Mon 03:17 UTC) and manual | `codeql` refresh | n/a | Code scanning results (blocked, see below) |
| manual `workflow_dispatch` | `capstone-deploy`: choose the environment and both digests. Terraform fmt/validate, Ansible preflight, render the digest-pinned Kustomize overlay, then (if not a dry run) apply, wait for rollout, and smoke `CUS-1001` | **GitHub Environment approval.** Digest format check. Rollout within 300 s. Smoke must pass | Rendered-manifest artifact, approval record, rollout and smoke output |

## Rules

- **Angular and Java are separate gates**, so one can't hide the other.
- **Package once:** image builds copy the JAR and bundle the test jobs already checked. The deploy never runs `mvn`, `ng build` or `docker build`. The same digest moves dev → test → stage → prod-like. Only ConfigMap values, Secrets, hosts and replicas change.
- **Promotion identity is the `sha256` digest pair**, not a tag.
- **Rollback** is a new approved Capstone Deploy run with the previous digest pair (runbook "Rollback").
- **Secrets** are GitHub Environment secrets only, and workflows never echo them. Workflow permissions default to `contents: read`. Only the image job gets `packages: write`.

## Owners

| Job | Lead |
| --- | --- |
| `frontend` | Jason (Frontend) |
| `backend` | Keifer (Backend) |
| `security-scan`, `codeql`, `container-images`, `package-once`, `capstone-deploy` | Will (Deployment) |

## Known gaps (tracked in the [risk register](risk-register.md))

- **Trivy doesn't block yet** (`exit-code: "0"`). The fixable HIGH/CRITICAL baseline and the exit condition are in the runbook (R-08).
- **CodeQL** fails at the upload step, because code scanning isn't available on this private repo (R-03).
- **Node 20** in CI, but the brief says Node 22. Team to confirm (R-13).
- **Required checks** are a GitHub setting and can't be proven from the YAML. The check names GitHub actually shows are `frontend`, `backend`, `Security (dependency, secret, and config scan)`, `Container images (build, scan, and digest)`, `Analyze (java-kotlin)` and `Analyze (javascript-typescript)`. The runbook lists `Frontend (Angular)` and `Backend (Maven)`, which don't match because those two jobs have no `name:`. Fix one side before setting up branch protection. **Team to confirm** which checks are required on `main`.
- **Not run yet:** the `main` push path and Capstone Deploy. `main` doesn't have these workflows until PR #22 merges (R-04).
