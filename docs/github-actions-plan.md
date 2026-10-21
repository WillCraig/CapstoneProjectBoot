# GitHub Actions plan

| Trigger | Jobs | Evidence |
| ------- | ---- | -------- |
| pull_request | Angular `npm ci` / `ng test` / `ng build`; Maven verify; Trivy and CodeQL scans | Actions logs |
| main | Same, plus push images to GHCR and record digests | digest artifacts |
| tag / env | Manual k3s deploy with approval, then smoke test | Environment approval + logs |

## Notes
- Package-once: the deploy never rebuilds, it just deploys the digests CI made.
- The Angular build and tests run in CI next to the Maven ones.
