# ADR 003: GitHub Actions builds once and promotes digests

Status: Accepted

## Context

CI/CD has to test and build both Angular and Spring Boot, scan the code and images, and deploy to k3s. If the deploy rebuilds anything, we could ship something nobody tested.

## Decision

We use GitHub Actions on GitHub-hosted `ubuntu-latest` runners, with three workflows:

- `capstone-ci.yml` runs on PRs, pushes to `main` and `v*` tags. Jobs: `frontend`, `backend`, `security-scan`, `container-images`, and `package-once` on pushes.
- `codeql.yml` scans Java and TypeScript.
- `capstone-deploy.yml` is manual only, with one GitHub Environment per k3s environment.

The image builds copy in the JAR and Angular bundle that the test jobs already built. Pushes to `main` publish the images to GHCR and save their digests as an artifact. The deploy only accepts `sha256:` digests and never builds.

Alternatives: our first draft picked a self-hosted runner to save Actions minutes. Hosted runners take about 3-4 minutes per CI run, so we dropped that idea. Bitbucket Pipelines isn't the taught path.

## Consequences

We can't deploy until `main` has a green run with digests, and that needs PR #22 merged. Trivy reports findings but doesn't block yet (R-7). CodeQL fails at the upload step on our private repo (R-3).
