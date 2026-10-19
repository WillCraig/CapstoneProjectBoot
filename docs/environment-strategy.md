# Northstar CRM Environment Strategy

Four environments on the shared k3s cluster. Each env gets its own namespace so we never share config or blast radius. We promote by **image digest** from GitHub Actions — the same artifact moves forward; only ConfigMaps, Secrets, and replica counts change.

| Env | k3s namespace | Approvals | Config source |
| --- | --- | --- | --- |
| dev | pending instructor | optional (author or teammate) | ConfigMap + Secret in the namespace |
| test | pending instructor | required (one teammate) | ConfigMap + Secret in the namespace |
| stage | pending instructor | required (merge keeper) | ConfigMap + Secret in the namespace |
| prod-like | pending instructor | required (merge keeper + second teammate) | ConfigMap + Secret in the namespace |

Namespace names are filled in when the instructor assigns them. Until then every row stays **pending instructor**, and no two environments share a namespace.

## Promotion rule

Digest from Actions → k3s. Do not rebuild the image per environment. Laptop-only builds are not a release identity.

## What differs between envs

Same image digest everywhere. What changes: ConfigMap values, Secret references, replica counts, and Ingress hostnames once we have them.

## Data rule

Below prod-like, only synthetic fixtures: `CUS-1001` Amina Khan (ACTIVE), `CUS-1002` Ravi Singh (PROSPECT), correlation id `lab-request-001`. No real customer data.

## Stage → prod-like gate

Promote only when all of these are true:

1. Stage smoke is green against the digest we plan to promote
2. Named approvers as in the table above
3. Rollback command rehearsed (redeploy the previous digest)
