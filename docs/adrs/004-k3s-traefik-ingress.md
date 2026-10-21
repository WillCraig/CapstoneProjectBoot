# ADR 004: Run on shared k3s behind Traefik Ingress, with Kustomize overlays

**Status:** Accepted for the build. **Open question for the instructor:** the platform (see below).
**Owner:** Deployment (Will leads).

## Context

The capstone brief (`CAPSTONE-BRIEF-AND-RUBRIC.md`) and the Lab 48–51 guides say to deploy to the **shared k3s cluster** and list OpenShift as not the taught path. The scored rubric (`Java_Angular_Fullstack_Capstone_Rubric.docx`) says the opposite: it names **OpenShift** (Routes, `oc` rollback) and treats k3s as "not the taught path". We have to pick one to build against, and we can't change the course documents.

## Decision

- Build for **k3s** because the labs teach it: Deployments and Services, plus one **Traefik** `Ingress` (`ingressClassName: traefik`) that sends `/api` and `/actuator/health` to `crm-backend` and `/` to `crm-frontend`. The browser sees one origin, so production needs no CORS entry.
- Manifests live in `deploy/k3s/base`, with one **Kustomize overlay** per environment (`dev`, `test`, `stage`, `prod-like`). Overlays only change the namespace, the Ingress host, and replicas (1 in dev and test, 2 in stage and prod-like).
- Pods are hardened: they run as non-root with a read-only root filesystem, drop all Linux capabilities, and don't mount a service account token. The backend has startup, readiness and liveness probes on Actuator.
- Settings come from the `crm-config` ConfigMap and secrets from the `crm-secrets` Secret. The deploy workflow creates the Secret from GitHub Environment secrets.

## Open question (Will to ask the instructor)

> "The brief and labs say to deploy to shared k3s. The rubric docx scores OpenShift Routes and `oc` rollback, and marks k3s down. Which one is graded? If it's OpenShift, can you give us a Project and access?"

We haven't switched anything to OpenShift without an answer. If OpenShift is required, the change is limited to swapping the `Ingress` for a `Route` and `kubectl` for `oc`, and checking the security context. The images, digests and workflows stay the same.

## Rejected

- **OpenShift Routes (for now).** We have no Project or access, and the brief says not to use it. We'll revisit this once the instructor answers.
- **Laptop k3s as the release target.** It wouldn't prove anything about the shared cluster. Local runs use Docker Compose for Postgres and Kafka only.
- **NodePort or LoadBalancer per service.** That exposes more ports, and the UI and API would end up on different origins.

## Consequences

- The namespaces (`northstar-crm-<env>`) and hosts (`crm-<env>.example.invalid`) in the overlays are placeholders. The deploy overrides them from the GitHub Environment variables `CRM_NAMESPACE` and `CRM_INGRESS_HOST`. **Team to confirm** the real values with the instructor.
- The backend container asks for `runAsNonRoot`, but `backend/Dockerfile` names its user (`USER spring`) rather than giving a numeric UID. Kubernetes can't prove a named user isn't root, so the first deploy will probably fail. The fix is one line (risk R-07).
