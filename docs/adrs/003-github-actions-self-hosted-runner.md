# ADR 003: GitHub Actions on a self-hosted runner, deploying digests to k3s

**Status:** Accepted (team policy).

## Context

CI/CD has to test and build Angular and Spring Boot, scan, build images, and deploy to the shared k3s cluster. The course uses GitHub Actions, and its Lab 51 starter runs on `ubuntu-latest`. Our Actions minutes on this account are used up, so hosted runners aren't an option right now.

## Decision

GitHub Actions for all CI/CD, on our own self-hosted runner. Deploys promote an image digest built once on `main` to k3s with `kubectl`. This is a team rule. If the instructor wants hosted runners, switching back is a `runs-on` change per job.

## Rejected

- **GitHub-hosted `ubuntu-latest`.** What the course uses. Out because of billing, kept as the fallback.
- **Bitbucket Pipelines or other CI.** Not the taught path.
- **Deploying by hand from a laptop.** No approval record and no digest trail.

## Consequences

- We maintain the runner: Docker, JDK 21, Node, headless Chrome for `ng test`, and `kubectl`. Owner is Security / deploy. **FILL IN:** where the runner is hosted.
- The runner holds secrets during deploy jobs, so it serves only this private repo, never fork PRs, and cleans its workspace after each job.
- If the runner is down, CI stops. The fallback is `ubuntu-latest` for that run, if minutes allow.
- A hello-world workflow on the runner gets proven before Lab 51 CI work starts.
