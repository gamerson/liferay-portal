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

## What To Expect (no provisioning backend)

The operator ships without a provisioning client wired in, so the reconcile loop
runs as far as it can offline and then stops cleanly. After `04-apply-samples.sh`
you should see:

- `status.environmentId` populated with the `acme-prod` namespace UID.
- A Secret `default-cne-identity` created — the generated per-environment
  cluster keypair.
- Condition `ProvisioningReachable = False`, reason `NoProvisioningClient`.
- `status.phase = Pending`.

This confirms the controller is installed, watching, creating owned resources,
and writing status — i.e. the reconcile wiring works. **Activation,
entitlements, replica clamping, and add-on download stay dormant** because they
all depend on a provisioning response.

The validating webhook is installed and serving (self-signed cert), but it
allows every scale for now: with no entitlement, `maxClusterNodes` is unset, so
there is no ceiling to enforce yet.

## Mocking The Provisioning Server

To exercise the dormant paths (activation, entitlements, the replica clamp, and
the webhook denial), the operator needs to reach a provisioning backend.
`mock-provisioning/server.py` implements the three contract endpoints with canned
responses and needs no dependencies:

```bash
MAX_CLUSTER_NODES=3 PORT=8888 mock-provisioning/server.py
```

It is **not yet consumed.** The operator's provisioning client
(`resources/internal/provisioning/client.go`) is still an interface stub, and
`main.go` passes `nil`. Wiring the mock in is the next step and needs:

1. A concrete HTTP `provisioning.Client` that reads a base URL from
   configuration (e.g. a `PROVISIONING_BASE_URL` env var) and signs each request
   as a JWT with the environment's private key.
2. A chart/values knob to set that base URL, plus a way to run the mock reachable
   from the cluster (a Deployment + Service in the cluster, or a host-mapped
   endpoint).

Once that exists, pointing `PROVISIONING_BASE_URL` at the mock makes the loop
complete: the StatefulSet gets clamped to `MAX_CLUSTER_NODES`, and a
`kubectl scale` above it is rejected by the webhook.
