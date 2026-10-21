# ADR 004: Shared k3s with Traefik Ingress

Status: Accepted for now. Platform TBD, ask the instructor.

## Context

The brief and the labs say to deploy to the shared k3s cluster. The rubric docx scores OpenShift Routes and `oc` rollback instead. We had to build against one of them.

## Decision

We build for k3s, since that's what the labs teach. Manifests live in `deploy/k3s/base`, with a Kustomize overlay for each of dev, test, stage and prod-like. One Traefik Ingress sends `/api` and `/actuator/health` to `crm-backend` and `/` to `crm-frontend`, so the browser sees one origin and we don't need CORS in the cluster.

Pods run as non-root with a read-only root filesystem. Settings come from the `crm-config` ConfigMap. The deploy workflow creates the `crm-secrets` Secret from GitHub Environment secrets.

Alternatives: we have no OpenShift Project or access. If OpenShift turns out to be graded, we swap the Ingress for a Route and `kubectl` for `oc`, and the images and workflows stay the same. A NodePort per service would open more ports and split the UI and API across origins.

## Consequences

The namespaces and hosts in the overlays are placeholders until the instructor gives us real ones. The backend Dockerfile uses a named user (`USER spring`), so Kubernetes can't prove it isn't root and will probably reject the pod on the first deploy. It's a one-line fix (R-6).
