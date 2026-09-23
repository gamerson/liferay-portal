# Client Extension Spike Scripts

Scripts that stand up a k3d cluster running the DXP operator against a real
Liferay, deploy the sample client extensions into it, and assert the
configuration handshake end to end.

Every script is idempotent. Re-running one against an environment that already
satisfies it reconciles what is missing and reports the rest as already done.

## The Whole Thing

```bash
./run-all.sh
```

Cluster, operator, Liferay, sample images, four scenarios, the end to end
checks, and the status report. Expect this to take a while on a cold cluster --
Liferay boots against an empty database on the first run.

## Building Blocks

| Script | What it does |
| --- | --- |
| `bootstrap.sh` | Creates the k3d cluster, MariaDB, MinIO, the operator, and Liferay. |
| `patch-coredns.sh` | Resolves `*.localtest.me` to the cluster load balancer, in-cluster and out. |
| `stage-modules.sh` | Builds the portal modules under test and stages them for the init container overlay. |
| `build-samples.sh` | Builds a container image per sample from its `dist` zip and imports them. |
| `deploy-samples.sh` | `deploy-samples.sh <cx-namespace> <liferay-namespace> [sample ...]` |
| `redeploy-operator.sh` | Rebuilds the operator image and rolls it. |
| `report.sh` | Writes `../STATUS_REPORT.md`. |

## Virtual Instances

```bash
./add-virtual-instance.sh virtual1.localtest.me
```

Creates the instance, routes its virtual host to the Liferay service, and waits
for the agent to publish `<web-id>-lxc-dxp-metadata`.

The web ID doubles as the virtual host and as the value the operator matches on,
so it has to be a resolvable hostname. Anything under `localtest.me` resolves
through the CoreDNS wildcard.

There is no headless route to this. `CompanyServiceImpl.addCompany` carries
`@JSONWebService(mode = JSONWebServiceMode.IGNORE)`, so
`/api/jsonws/company/add-company` answers 404 and no REST resource exposes
company creation; `add_virtual_instance.py` drives the Portal Instances portlet
form instead.

## Cross Namespace Delivery

```bash
./allow-namespace.sh liferay-vi2
```

Delivery into a namespace other than Liferay's own is consented to by the
Liferay side, not claimed by the client extension. Without this the
ClientExtension sits at `Degraded` with `EnvironmentUnusable`, naming the
namespace it wanted and the `LiferayEnvironment` that did not list it.

## Checks

```bash
./verify-handshake.sh [cx-namespace] [virtual-instance-id]
```

Walks both legs of the handshake and then proves the result is live:

1. every ClientExtension reached `Ready`
2. the agent published the dxp metadata, and the operator mirrored it into the
   client extension namespace with matching content (skipped when the two
   namespaces are the same, where there is deliberately no mirror)
3. Liferay wrote `ext-init` back, and the operator mirrored it into a Secret
4. a token minted through the authorization code + PKCE flow is accepted by the
   microservice endpoints

Step 4 is the one that cannot be faked by the control plane alone: it fails if
the workload is holding credentials Liferay has since reissued.

```bash
./verify-object-action.sh [cx-namespace] [virtual-instance-id]
```

Creates a `Sample` entry, updates it to fire `onAfterUpdate`, and waits for the
handler's own log to carry this run's probe value. Requires
`liferay-sample-batch` to have been deployed, since that is what provisions the
object definition and its actions.

The assertion is the microservice's log rather than the action status Liferay
records. That status reports that the request was dispatched and stays
`success` even when the endpoint answers 500.

## Scenarios

```bash
./scenario-second-instance.sh [web-id] [cx-namespace]
```

One Liferay, two virtual instances, the same four samples deployed twice. It
adds the instance, consents to the namespace, deploys, verifies the handshake,
and then compares the mirrored credentials across the two instances -- the same
`serviceId` must not share a client ID, because Liferay registers a separate
OAuth2 application per virtual instance.

Client extension hostnames are kept apart by the instance's own subdomain
(`<sample>.vi2.localtest.me`); the CoreDNS wildcard resolves any depth.

## Settings

Everything in `lib.sh` is overridable from the environment. The ones worth
knowing:

| Variable | Default | Notes |
| --- | --- | --- |
| `CLUSTER_NAME` | `cx-spike` | |
| `LIFERAY_NAMESPACE` | `liferay-prod` | |
| `VIRTUAL_INSTANCE_ID` | `liferay.localtest.me` | The default instance's web ID. |
| `PUBLIC_DOMAIN_SUFFIX` | `localtest.me` | Set per instance to keep hostnames apart. |
| `ADMIN_PASSWORD` | `test` | |
| `SAMPLES_DIR` | the sample workspace | |

## Teardown

```bash
./teardown.sh
```

Note that `PORTAL_MODULES_DIR` is bind mounted into the cluster. Never delete
and recreate that directory while the cluster exists -- the node keeps the old
inode and the mount goes empty.
