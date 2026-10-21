# Environment strategy

We have four environments on the shared k3s cluster, each in its own namespace. Environments are places, not branches.

| Env | k3s namespace | Approvals | Config source |
| --- | ----------------- | --------- | ------------- |
| dev | `northstar-crm-dev` | optional | `crm-config` ConfigMap and `crm-secrets` Secret |
| test | `northstar-crm-test` | one teammate | same |
| stage | `northstar-crm-stage` | required | same |
| prod-like | `northstar-crm-prod-like` | required, two teammates | same |

Dev and test run one replica of each app, and stage and prod-like run two. The namespace names and the `crm-<env>.example.invalid` hosts are placeholders. The deploy replaces them with the `CRM_NAMESPACE` and `CRM_INGRESS_HOST` variables from each GitHub Environment. The real values are TBD, ask the instructor. We haven't created the four GitHub Environments yet (R-5).

## Promotion rule

Digest from Actions → k3s; no laptop-only builds as release identity. `capstone-deploy` rejects anything that isn't a `sha256` digest.

The image is the same in every environment. Only the ConfigMap values, the Secrets, the replica count and the Ingress host change. Each environment gets its own `CRM_JWT_SECRET`, so a dev token doesn't work in prod-like.

We only promote from stage to prod-like when the stage smoke test passes on that digest and we've rehearsed a rollback.

## Data

We use synthetic fixtures only in every environment: `CUS-1001`, `CUS-1002`, `CUS-9999` and `lab-request-001`.

## Local development

Local isn't one of the four. `docker compose up -d` starts PostgreSQL and Kafka, the backend runs on 8081 with `SPRING_PROFILES_ACTIVE=local`, and `ng serve` runs on 4200. The `local` profile uses throwaway values that never go to the cluster.
