# Environment strategy

| Env | k3s namespace | Approvals | Config source |
| --- | ----------------- | --------- | ------------- |
| dev | `northstar-crm-dev` | optional | ConfigMap/Secret |
| test | `northstar-crm-test` | one teammate | ConfigMap/Secret |
| stage | `northstar-crm-stage` | required | ConfigMap/Secret |
| prod-like | `northstar-crm-prod-like` | required | ConfigMap/Secret |

The real namespaces depend on cluster access. TBD, ask the instructor.

## Promotion rule
Digest from Actions → k3s; no laptop-only builds as release identity.
