# Capstone CI/CD runbook — Lab 51

This runbook describes the Northstar CRM delivery spine from pull request to
digest-pinned k3s promotion. It separates what is implemented and verified in
this repository from settings that must be completed in GitHub or by the
shared-cluster instructor. Those external items are not silently claimed.

Previous evidence:

- Backend/Kafka: [`lab49/backend-demo.md`](lab49/backend-demo.md)
- Angular/PostgreSQL browser CRUD: [`lab50/frontend-persistence-demo.md`](lab50/frontend-persistence-demo.md)
- Environment and approval strategy: [`environment-strategy.md`](environment-strategy.md)

## Delivery shape

```text
Pull request
  -> Angular tests/build + Maven verify + Trivy reports + CodeQL
  -> container builds from verified artifacts + Trivy image scans

Push to main or a v* tag
  -> the same gates
  -> push the scanned images to GHCR
  -> record registry RepoDigests and the verified JAR checksum

Manual Capstone Deploy
  -> Terraform validate/plan + Ansible input checks
  -> GitHub Environment approval
  -> render one Kustomize overlay with the two supplied digests
  -> apply to k3s (unless dry-run)
  -> CUS-1001 Ingress smoke
  -> roll back by promoting the previous digests
```

No deployment step rebuilds the JAR, Angular bundle, or images.

## Workflows and gates

### Capstone CI — `.github/workflows/capstone-ci.yml`

| Job | Purpose | Evidence |
| --- | --- | --- |
| `Frontend (Angular)` | `npm ci`, 33 Karma tests, production build, upload verified bundle | Job log + `frontend-dist-<sha>` |
| `Backend (Maven)` | Java 21 `mvn -B clean verify`, upload verified JAR | Job log + `backend-verified-jar-<sha>` |
| `Security (dependency, secret, and config scan)` | Trivy filesystem HIGH/CRITICAL report | `trivy-repository-<sha>` |
| `Container images (build, scan, and digest)` | Build both images from verified artifacts, Trivy them, record IDs/digests | `trivy-images-<sha>`, `image-digests-<sha>` |
| `Package verified JAR once` | Download the verify-job JAR, SHA-256 it, upload the pair | `crm-package-<sha>` on pushes only |

The PostgreSQL service in the backend job follows the pasted starter. The
current integration tests also start their own Testcontainers PostgreSQL and
Kafka, and those dynamic test properties take precedence for the ITs.

### CodeQL — `.github/workflows/codeql.yml`

CodeQL runs on pull requests, pushes to `main`/`v*`, Mondays at 03:17 UTC,
and manual dispatch:

- Java/Kotlin: manual Maven compile under CodeQL tracing (`-DskipTests`; tests
  are already enforced by Capstone CI).
- JavaScript/TypeScript: `build-mode: none`.
- Results upload to GitHub code scanning when that repository feature and the
  workflow permissions are available.

### Required branch-protection checks

Repository settings cannot be proven from YAML. In GitHub branch protection
for `main`, require at least these completed checks before merge:

- `Frontend (Angular)`
- `Backend (Maven)`
- `Security (dependency, secret, and config scan)`
- `Container images (build, scan, and digest)`
- `Analyze (java-kotlin)`
- `Analyze (javascript-typescript)`

Also require the repository's normal non-author review. Do not mark this item
complete until those names appear as required checks in the GitHub UI and one
PR is blocked or admitted by them.

## Scan policy and residual risk

Tools:

- **CodeQL** supplies source-code SAST.
- **Trivy filesystem** supplies dependency, secret, and configuration reports.
- **Trivy image** scans the exact local images before any GHCR push.

A local Trivy baseline on 2026-10-20, filtered to fixable HIGH/CRITICAL
findings, reported:

| Image | Fixable HIGH/CRITICAL |
| --- | ---: |
| Backend | 44 |
| Frontend | 53 |

The main sources are the older Spring Boot 3.3.5 dependency set and dated
Alpine/Nginx packages. Therefore the Trivy steps currently publish SARIF
evidence with exit code 0 rather than pretending the baseline is clean.

**Residual-risk owner:** repository owner / capstone team.  
**Review date:** before Lab 52 defense.  
**Exit condition:** upgrade the Spring Boot parent and base images, rerun all
tests and Trivy, then change the image-scan threshold to fail on
HIGH/CRITICAL. Until that change is tested, a green CI run means “scans were
produced and reviewed,” not “zero known vulnerabilities.”

## Container identity and package-once rule

Image repositories:

- `ghcr.io/<owner>/capstone-crm-backend:<commit-sha>`
- `ghcr.io/<owner>/capstone-crm-frontend:<commit-sha>`

On pull requests both images are loaded and scanned locally, but never
pushed. On `main`/`v*` pushes, the same loaded images are pushed to GHCR with
the workflow `GITHUB_TOKEN` and the concrete `RepoDigests` are written to the
job summary and `image-digests-<sha>` artifact.

Promotion inputs must be the returned `ghcr.io/...@sha256:<digest>` values.
Tags are convenient labels for humans; the digest is the release identity.
The deploy workflow validates that both supplied digests match
`sha256:<64 lowercase hex characters>`.

## GitHub Environments, variables, and secrets

Create four GitHub Environments exactly named:

- `dev`
- `test`
- `stage`
- `prod-like`

Apply the reviewer rules in `environment-strategy.md`:

| Environment | Required reviewers |
| --- | --- |
| dev | Optional: author or teammate |
| test | One teammate |
| stage | Merge keeper |
| prod-like | Merge keeper + second teammate |

### Non-secret Environment variables

| Name | Meaning |
| --- | --- |
| `CRM_NAMESPACE` | Instructor-assigned k3s namespace for this environment |
| `CRM_INGRESS_HOST` | Public Traefik host for this environment |
| `CRM_DATABASE_URL` | JDBC URL visible to the backend pod |
| `CRM_KAFKA_BOOTSTRAP_SERVERS` | Kafka bootstrap endpoints visible to the backend pod |
| `GHCR_PULL_USERNAME` | Optional GHCR pull identity; defaults to repository owner |

### Environment secrets — names only

| Name | Used for |
| --- | --- |
| `KUBECONFIG_B64` | Teaching-cluster fallback credentials, base64-encoded |
| `CRM_DATABASE_USERNAME` | PostgreSQL username placed in `crm-secrets` |
| `CRM_DATABASE_PASSWORD` | PostgreSQL password placed in `crm-secrets` |
| `CRM_JWT_SECRET` | HS256 signing key for API JWTs (>= 32 random bytes), placed in `crm-secrets/jwt-secret` |
| `CRM_AGENT_PASSWORD` | Synthetic `agent` (AGENT) password, preferably a BCrypt hash, placed in `crm-secrets/agent-password` |
| `CRM_ADMIN_PASSWORD` | Synthetic `admin` (AGENT+ADMIN) password, preferably a BCrypt hash, placed in `crm-secrets/admin-password` |
| `CRM_SMOKE_AGENT_PASSWORD` | Plain-text `agent` password the smoke uses to log in (matches `CRM_AGENT_PASSWORD`) |
| `GHCR_PULL_TOKEN` | Read-only package token stored as the cluster image-pull secret |

Never put a value from this table in YAML, a runbook, a screenshot, or a log.
The workflow does not echo them. The ephemeral CI PostgreSQL service uses
trust authentication and has no reusable credential.

### OIDC position

The deploy workflow requests `id-token: write`, so a cloud OIDC login action
can be inserted when the cluster provider is known. Its expected subject for
an environment-scoped token is:

```text
repo:WillCraig/CapstoneProjectBoot:environment:<environment>
```

The audience and role mapping are provider-specific and are **not configured
in this repository yet**. Until the instructor identifies that provider,
`KUBECONFIG_B64` is the documented fallback. GHCR publication itself uses the
short-lived workflow `GITHUB_TOKEN`, not a pasted personal access token.

## Terraform and Ansible stages

Terraform lives in `infra/terraform/` and models only the four environment
namespaces. It is safe by default:

- `manage_namespaces` defaults to `false` because the instructor assigns the
  shared-cluster namespaces.
- `terraform fmt -check`, `terraform init -backend=false`, and
  `terraform validate` always run in the manual deploy workflow.
- After environment approval and kubeconfig setup, `terraform plan` runs with
  `manage_namespaces=false`.
- There is deliberately no `terraform apply` step and no committed backend or
  state. Configure an approved remote backend before changing the default.

Ansible lives in `infra/ansible/site.yml`. It is an idempotent no-change
preflight: it validates the target environment, namespace, Ingress host, and
digest formats, then prints only non-secret values. The deploy workflow runs
both syntax-check and the playbook before rendering manifests.

Local mirror commands:

```bash
terraform fmt -check -recursive infra/terraform
terraform -chdir=infra/terraform init -backend=false -input=false
terraform -chdir=infra/terraform validate
ansible-playbook -i localhost, infra/ansible/site.yml --syntax-check
```

## k3s manifests

Base: `deploy/k3s/base/`

- Backend and frontend Deployments and ClusterIP Services
- Shared Traefik Ingress: `/api` → backend, `/` → frontend
- `crm-config` for non-secret database/Kafka endpoints
- References to runtime-created `crm-secrets` and `ghcr-pull-secret`
- Actuator HTTP probes: startup + liveness on `/actuator/health/liveness`,
  readiness on `/actuator/health/readiness` (includes the PostgreSQL check);
  the Ingress also routes `/actuator/health` to the backend for the smoke
- Probes, resources, non-root security contexts, dropped capabilities, and
  read-only root filesystems with writable `/tmp` mounts where required

Overlays: `deploy/k3s/overlays/{dev,test,stage,prod-like}/`

- Placeholder namespace and `.example.invalid` Ingress host until assignment
- One replica in dev/test and two in stage/prod-like

Render any overlay locally without contacting a cluster:

```bash
kubectl kustomize deploy/k3s/overlays/dev
```

The production frontend uses a relative `/api` base, so the same Ingress host
serves Angular and Spring. Development keeps using `http://localhost:8081`.

## Manual promotion procedure

1. Merge through the protected PR checks.
2. Wait for the `main` or `v*` Capstone CI run.
3. Open its `image-digests-<sha>` artifact or job summary and copy the two
   registry `RepoDigests`. Do not invent a digest and do not promote a tag
   alone.
4. Run **Capstone Deploy** manually with:
   - the target environment,
   - backend and frontend digests,
   - optional GHCR owner,
   - `dry_run: true` for the first pass.
5. Inspect the uploaded rendered-manifest artifact. Confirm the namespace,
   host, image digests, endpoint values, replica counts, and that no Secret
   values are present.
6. Rerun with `dry_run: false`. The selected GitHub Environment must approve
   before cluster work can proceed.
7. The workflow creates/updates `ghcr-pull-secret` and `crm-secrets`, applies
   the rendered manifests, waits for both rollouts, and runs the smoke.
8. Record the Actions run URL, both digests, environment, and smoke result in
   the Lab 52 evidence pack. Do not commit kubeconfig or generated Secrets.

## Smoke

`scripts/smoke-crm.sh` requires `CRM_BASE_URL` and either `CRM_SMOKE_PASSWORD`
(logs in as `CRM_SMOKE_USERNAME`, default `agent`) or a pre-issued `CRM_SMOKE_TOKEN`. It:

1. Requires `/actuator/health/readiness` to report `UP`.
2. Requests the frontend root (skip with `CRM_SMOKE_SKIP_FRONTEND=true` for a
   backend-only laptop run).
3. Logs in through `POST /api/v1/auth/login` (the JWT is never printed).
4. Creates a synthetic Amina (`CUS-1001`) interaction with correlation
   `lab-request-001`.
5. Reads the timeline and requires the returned ID and unique summary.
6. Soft-deletes that smoke interaction, including on early failure after a
   successful create.

Manual equivalent:

```bash
CRM_BASE_URL=https://<ingress-host> \
CRM_SMOKE_PASSWORD='<read from the environment secret store>' \
bash scripts/smoke-crm.sh
```

Do not paste the password or a token into shell history or a ticket. Prefer the workflow's
Environment secret.

## Rollback

Preferred rollback is a new approved **Capstone Deploy** run using the
previous pair of registry digests. That keeps rollback in the same audit and
approval trail as promotion.

Emergency in-cluster rollback, when the previous ReplicaSets still exist:

```bash
kubectl -n <namespace> rollout undo deployment/crm-backend
kubectl -n <namespace> rollout undo deployment/crm-frontend
kubectl -n <namespace> rollout status deployment/crm-backend
kubectl -n <namespace> rollout status deployment/crm-frontend
```

Then rerun the Ingress smoke against `CUS-1001`. Database migrations are not
automatically reversed; V1–V3 are additive, so older application images remain
schema-compatible for this lab. Never roll back by rebuilding an old commit.

## Failure experiments

| # | Experiment | Expected / observed disposition |
| - | ---------- | --------------------------------- |
| 1 | Break an Angular unit test | The `Frontend (Angular)` job turns red; image and package jobs do not run because they need it. This is the intended PR merge block once branch protection is enabled. |
| 2 | Deploy rebuilds the JAR | Policy failure by inspection: `capstone-deploy.yml` contains no Maven/npm build and only accepts supplied digests. |
| 3 | Skip environment approval | Policy failure by design: the only deploy job is bound to a named GitHub Environment. It remains externally unverified until the four environments and reviewers are configured. |

Experiments 1 and 3 need a real PR/GitHub settings pass for final evidence;
the YAML dependencies and environment binding were inspected locally.

## Local verification mirrors

```bash
cd frontend
npm ci
CHROME_BIN='/Applications/Google Chrome.app/Contents/MacOS/Google Chrome' \
  npx ng test --watch=false --browsers=ChromeHeadless
npx ng build --configuration=production

cd ../backend
mvn -B -ntp clean verify

cd ..
docker build -t northstar-crm-backend:lab51-local backend
docker build -t northstar-crm-frontend:lab51-local frontend
kubectl kustomize deploy/k3s/overlays/dev >/dev/null
terraform -chdir=infra/terraform validate
ansible-playbook -i localhost, infra/ansible/site.yml --syntax-check
```

Backend Testcontainers commands need a running Docker engine. Docker Desktop
and GitHub-hosted runners both work with no extra settings; see
[README.md — Run it locally](../README.md#run-it-locally) for the local steps.

## Still external before Lab 51 can be called deployed

- Push the Lab 51 commits and open a PR so Capstone CI and CodeQL produce
  real GitHub run URLs.
- Enable branch protection with the check names above.
- Let one `main` or tag run publish both GHCR images and capture concrete
  registry digests.
- Create the four GitHub Environments, reviewers, variables, and named
  secrets.
- Replace placeholder namespaces and `.example.invalid` hosts with the
  instructor assignments.
- Provide reachable PostgreSQL and Kafka endpoints for the backend pods.
- Run Capstone Deploy first as a manifest dry run, then for real in dev.
- Rehearse rollback with the previous digest pair.

## Reflection

1. Angular is proven gated when its separate job, production bundle artifact,
   frontend image build, and required PR check all reference the same commit.
2. Environment approvals limit blast radius by separating “the artifact is
   releasable” from “a named person accepts this environment change now.”
3. The rollback unit is the **pair of registry digests**. Tags and commits
   identify provenance for humans, but only digests identify the exact images.
