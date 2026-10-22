# ADR 003: GitHub Actions, build once

## Context
We need CI that tests Angular and Spring Boot, scans the code, and deploys to k3s without rebuilding anything.

## Decision
GitHub Actions on GitHub-hosted runners. CI tests and builds on every PR. Merges to `main` push the images to GHCR and record their digests. A manual deploy workflow with an approval step deploys those digests. We picked this over Bitbucket Pipelines, which isn't the taught path.

## Consequences
What we deploy is exactly what we tested. PR #22 is merged. The deploy is on hold until we have class cluster access.
