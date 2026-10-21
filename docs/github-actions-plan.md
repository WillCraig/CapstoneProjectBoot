# GitHub Actions plan

We have three workflows in `.github/workflows/`, all on GitHub-hosted `ubuntu-latest` runners ([ADR 003](adrs/003-github-actions-digest-promotion.md)). Angular and Spring Boot each get their own gate, so one can't hide a failure in the other. The [CI/CD runbook](capstone-cicd-runbook.md) has the step-by-step and secret names.

| Trigger | Jobs | Gate | Evidence |
| ------- | ---- | ---- | -------- |
| `pull_request` | `capstone-ci`: `frontend` (`npm ci`, `ng test`, `ng build`), `backend` (`mvn -B clean verify`), `security-scan` (Trivy), `container-images` (build and scan, no push). `codeql` for Java and TypeScript | Any failing test or build fails the run. Images only build after the other jobs pass | Actions logs, frontend and JAR artifacts, Trivy reports |
| push to `main` | Same jobs, then push both images to GHCR, record digests, and `package-once` | Same as PR | `image-digests-<sha>` and `crm-package-<sha>` artifacts |
| `v*` tag | Same as `main`. No automatic deploy | Same as PR | Digests to hand to the deploy |
| manual | `capstone-deploy`: pick the environment and two digests, validate Terraform and Ansible, render the overlay, apply, wait for rollout, smoke `CUS-1001` | GitHub Environment approval, digest format check, rollout within 300 s, smoke must pass | Rendered manifests, approval record, smoke output |

## Notes

- Package once. The images use the JAR and bundle the test jobs already built. The deploy never runs `mvn`, `ng build` or `docker build`.
- The same digest moves from dev to test to stage to prod-like. Rollback means deploying the previous digests again.
- Secrets live only in GitHub Environments. Workflows default to `contents: read`, and only the image job gets `packages: write`.
- Jason owns `frontend`, Keifer owns `backend`, and Will owns the scan, image and deploy jobs.

## Known gaps

- Trivy doesn't block yet (R-7).
- CodeQL fails at the upload step on the private repo (R-3).
- The `main` path and the deploy haven't run yet, because `main` gets these workflows when PR #22 merges.
- The runbook lists the required checks as `Frontend (Angular)` and `Backend (Maven)`, but GitHub shows them as `frontend` and `backend`. We need to fix one side before turning on branch protection.
