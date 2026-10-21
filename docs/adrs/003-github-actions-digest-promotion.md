# ADR 003: GitHub Actions builds once and promotes image digests

**Status:** Accepted (built in Lab 51). This replaces the first Lab 48 draft, which proposed a self-hosted runner. See "What changed" below.
**Owner:** Deployment (Will leads; see [team roles](../team-roles.md)).

## Context

CI/CD has to test and build both Angular and Spring Boot, scan the code and images, build images, and deploy to the shared k3s cluster. The course uses GitHub Actions. A deploy that rebuilds code can ship something nobody tested, so the image that passed the checks has to be the one that ships.

## Decision

- **GitHub Actions** for all CI/CD, in three workflows:
  - `capstone-ci.yml`: runs on every PR, on pushes to `main`, and on `v*` tags. Its jobs are `frontend` (`npm ci`, `ng test`, `ng build`), `backend` (`mvn -B clean verify`), `security-scan` (Trivy on the repo) and `container-images` (build and scan both images, then push to GHCR on `main` or a tag), plus `package-once` (the JAR's SHA-256) on pushes.
  - `codeql.yml`: CodeQL for Java and TypeScript.
  - `capstone-deploy.yml`: manual only, one GitHub Environment per k3s environment.
- **GitHub-hosted runners** (`ubuntu-latest`), the same as the Lab 51 starter.
- **Package once.** The image builds copy in the JAR and Angular bundle that the test jobs already checked. They don't run Maven or `ng build` again. Pushes to `main` publish images to GHCR and record their `sha256:` digests as the `image-digests-<sha>` artifact.
- **Promote digests, not tags.** `capstone-deploy.yml` only accepts `sha256:<64 hex>` digests as input, pins them into the Kustomize overlay, and never builds anything.

## What changed from the first draft

The `docs/11-actions-plan` draft chose a self-hosted runner because the team expected to run out of hosted Actions minutes. When Lab 51 was built, the workflows ended up on `ubuntu-latest`, and they run green there (Capstone CI took about 3–4 minutes per run on 2026-10-21). We never set up a self-hosted runner. **Team to confirm:** whether the minutes concern still applies. If it does, switching back is a `runs-on` change per job.

## Rejected

- **Self-hosted runner.** We'd have to maintain it, and it would hold deploy secrets. Hosted runners work, so we don't need it right now.
- **Bitbucket Pipelines or other CI.** Not the taught path.
- **Rebuilding inside the deploy job.** That breaks package-once, and the deployed image would no longer be the one that was scanned.
- **Deploying by hand from a laptop.** No approval record and no digest trail.

## Consequences

- A deploy can only happen after a green `main` run that produced digests. Today `main` doesn't have these workflows yet; they arrive when PR #22 merges.
- Trivy findings are reported but don't block yet (`exit-code: "0"`). The reason and the exit condition are in `docs/capstone-cicd-runbook.md`.
- CodeQL fails at the upload step on this private repo, because code scanning isn't available for it. The team's plan is in risk R-03 in the [risk register](../risk-register.md).
- The CI Node version is 20, but the brief says Node 22. **Team to confirm** which one to use (risk R-13).
