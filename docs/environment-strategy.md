# Northstar CRM Environment Strategy

Four environments on the shared k3s cluster. Each one gets **its own namespace**, so config and mistakes never spill from one into another. Environments are places, not branches. We promote by **image digest** from GitHub Actions: the same artifact moves forward, and only ConfigMap values, Secrets, Ingress hosts and replica counts change.

| Env | k3s namespace (placeholder in `deploy/k3s/overlays/<env>`) | Replicas (backend / frontend) | Approval (GitHub Environment) | Config source |
| --- | --- | --- | --- | --- |
| dev | `northstar-crm-dev` | 1 / 1 | optional (author or teammate) | `crm-config` ConfigMap + `crm-secrets` Secret in the namespace |
| test | `northstar-crm-test` | 1 / 1 | required (one teammate) | same |
| stage | `northstar-crm-stage` | 2 / 2 | required (merge keeper) | same |
| prod-like | `northstar-crm-prod-like` | 2 / 2 | required (merge keeper + second teammate) | same |

The namespace names and hosts (`crm-<env>.example.invalid`) are **placeholders**. At deploy time they're replaced by the GitHub Environment variables `CRM_NAMESPACE` and `CRM_INGRESS_HOST`. **Team to confirm** the real values once the instructor assigns them. No two environments may share a namespace.

**Team to confirm:** the four GitHub Environments and their reviewers haven't been created in the repo settings yet (risk R-05). The merge keeper is still unassigned ([team roles](team-roles.md)).

## Promotion rule

Digest from Actions → k3s. Don't rebuild the image per environment. Laptop builds are never a release identity. `capstone-deploy` rejects anything that isn't `sha256:<64 hex>`.

## What differs between envs

The image digest is the same everywhere. What changes: ConfigMap values (the database URL and Kafka servers, filled from `CRM_DATABASE_URL` and `CRM_KAFKA_BOOTSTRAP_SERVERS`), the Secret values (database user and password, `CRM_JWT_SECRET`, demo-account passwords), replica counts, and Ingress hosts. Each environment gets its **own** JWT secret, so a token from dev doesn't work in prod-like.

## Data rule

Synthetic fixtures only, in every environment: `CUS-1001` Amina Khan (ACTIVE), `CUS-1002` Ravi Singh (PROSPECT), `CUS-9999` (not found), correlation id `lab-request-001`. No real customer data.

## Stage → prod-like gate

Promote only when all of these are true:

1. Stage smoke (`scripts/smoke-crm.sh`) is green against the digest pair we plan to promote.
2. The named approvers in the table above have approved.
3. Rollback (re-deploy the previous digest pair) has been rehearsed within NFR-R1 (10 minutes or less).

## Local development

Not one of the four environments. `docker compose up -d` (PostgreSQL 16 + Kafka) plus the backend on 8081 with `SPRING_PROFILES_ACTIVE=local` and `ng serve` on 4200. The `local` profile's values are labeled throwaway defaults, never used on the cluster.
