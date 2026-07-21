# Local Operator Testing

Scripts to run the Liferay cloud operator end to end on a throwaway
[k3d](https://k3d.io) cluster: build the image, install the chart, apply a
`LiferayEnvironment`, and watch the reconcile loop.

## Prerequisites

`k3d`, `docker`, `kubectl`, and `helm` on your `PATH`. `python3` is needed only
for the mock provisioning server.

## Quick Start

```bash
cd cloud/operator/hack
./e2e.sh          # cluster up -> build+load -> install -> apply -> verify
./99-teardown.sh  # delete the cluster when done
```

`e2e.sh` deliberately leaves the cluster running so you can inspect it.

## The Scripts

Each script is idempotent and can be run on its own, in order:

| Script | What it does |
|---|---|
| `01-cluster-up.sh` | Create the k3d cluster (`liferay-operator-test`). |
| `02-build-load.sh` | Build the operator image and import it into the cluster. |
| `03-install.sh` | `helm upgrade --install` the chart into `liferay-system`. |
| `04-apply-samples.sh` | Apply the fixtures in `manifests/`. |
| `05-verify.sh` | Print the CR status, identity Secret, events, and operator logs. |
| `06-enable-mock.sh` | Deploy the in-cluster mock and point the operator at it. |
| `99-teardown.sh` | Delete the cluster (`KEEP_CLUSTER=1` keeps it, removing only the release + fixtures). |

Every default (cluster name, image tag, namespaces) is an overridable
environment variable — see `lib.sh`.

## Fixtures

`manifests/` contains the test environment:

- `00-namespace.yaml` — the `acme-prod` namespace (its UID becomes the
  `environmentId`).
- `10-activation-secret.yaml` — a placeholder one-time activation code.
- `20-liferay-statefulset.yaml` — a `pause`-image StatefulSet standing in for
  the Liferay workload the operator scales.
- `30-liferayenvironment.yaml` — the `LiferayEnvironment` CR that drives
  reconciliation.

## What To Expect (default: real provisioning host)

By default the operator targets `https://provisioning.liferay.com`, which does
not exist yet, so the reconcile loop runs as far as it can and then reports the
failure. After `04-apply-samples.sh` you should see:

- `status.environmentId` populated with the `acme-prod` namespace UID.
- A Secret `default-cne-identity` created — the generated per-environment
  cluster keypair.
- The activation POST failing (connection/404), so `status.phase = Degraded`
  and the `Activated` condition reports the error.

This confirms the controller is installed, watching, creating owned resources,
signing requests, and writing status — i.e. the reconcile wiring works. To make
the loop succeed, point it at the mock (below).

## Mocking The Provisioning Server

`mock-provisioning/server.py` implements the three contract endpoints with canned
responses (no dependencies). `06-enable-mock.sh` deploys it inside the cluster
and reconfigures the operator to use it:

```bash
./06-enable-mock.sh
./05-verify.sh
```

The mock is wired end to end: the operator reads `PROVISIONING_BASE_URL` (set by
the chart's `provisioning.baseURL` value) and signs each request as an RS256 JWT
with the environment's private key. With the mock returning
`maxClusterNodes=2`, the reconcile loop now completes:

- activation succeeds → `Activated = True`, `status.activatedAt` set;
- entitlements succeed → `ProvisioningReachable = True`,
  `status.license.maxClusterNodes = 2`;
- the clamp runs → the `liferay-dxp` StatefulSet is scaled to **2** (from the
  CR's `desiredReplicas: 3`) and `ReplicasClamped = True`;
- `status.phase = Ready`.

Then the validating webhook has a ceiling to enforce. A scale above it is
rejected:

```bash
kubectl --context k3d-liferay-operator-test -n acme-prod \
    scale statefulset/liferay-dxp --replicas=5
# Error ... admission webhook "vstatefulsetscale.licensing.liferay.com" denied
# the request: replicas 5 exceeds licensed maxClusterNodes 2 ...
```

To run the mock server directly on your host instead (e.g. for quick iteration):

```bash
MAX_CLUSTER_NODES=2 PORT=8888 mock-provisioning/server.py
```
