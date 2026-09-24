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

Set `SKIP_BOOTSTRAP=true` to rerun the scenarios and checks against a cluster that is already up.

Cluster, operator, Liferay, sample images, four scenarios, the end to end
checks, and the status report. Expect this to take a while on a cold cluster --
Liferay boots against an empty database on the first run.

## Building Blocks

| Script | What it does |
| --- | --- |
| `bootstrap.sh` | Creates the OCI registry, a Kubernetes 1.36 k3d cluster, MariaDB, the operator, and Liferay. |
| `patch-coredns.sh` | Resolves `*.localtest.me` to the cluster load balancer, in-cluster and out. |
| `stage-modules.sh` | Builds the portal modules under test and stages them for the init container overlay. |
| `build-samples.sh` | Pulls the stock Liferay base images and imports them. Nothing sample specific is built. |
| `package-cx-chart.sh` | `package-cx-chart.sh <sample> [version]` — publishes a zip as an artifact image and a chart, as CI would. |
| `deploy-samples.sh` | `deploy-samples.sh <cx-namespace> <liferay-namespace> [sample ...]` — publishes, then installs from `oci://` with only the environment binding. |
| `redeploy-operator.sh` | Rebuilds the operator image and rolls it. |
| `report.sh` | Writes `../STATUS_REPORT.md`. |

## From Zip To Cluster

Nothing is built from a client extension's Dockerfile. `cx_artifact.py` reads it instead: the `FROM` names the stock base image the workload runs, and each `COPY` becomes a mount. The zip's files are published as a single layer OCI image, and the chart mounts it into the unmodified base image as an image volume.

| Base image | Mount |
| --- | --- |
| `liferay/caddy` | `static/` at `/public_html` |
| `liferay/jar-runner` | the jar at `/opt/liferay/jar-runner.jar` |
| `liferay/batch` | `batch/` or `site-initializer/` under `/opt/liferay` |
| `liferay/node-runner` | the whole zip at `/opt/liferay` |

An image volume can only mount a directory. A `COPY` whose destination is a file therefore also gets a directory of its own inside the layer, holding the file under its destination name as a hard link, and that directory is mounted over the destination's parent. `/opt/liferay` is empty in every runner image, so nothing is hidden.

Mounts are read only, so nothing can run against the files in the cluster. A `RUN` step is therefore replayed at package time instead: when the zip is copied whole to one destination, `package-cx-chart.sh` runs the step inside the zip's own base image against the unpacked zip, then builds the layer from the result. The node sample needs this — its zip carries one of its six dependencies, and `npm install` is where the rest come from. Any instruction that cannot be replayed is reported as a warning rather than dropped silently.

`LCP.json` declares its probes with no initial delay and leaves start-up time to the platform. Kubernetes doesn't, so the chart also gets a `startupProbe` on the same endpoint, which holds liveness and readiness off until the process has answered once. Without it the kubelet kills a Spring Boot client extension before Tomcat is listening.

A chart is republished only when its inputs change: the zip, `cx_artifact.py`, `package-cx-chart.sh`, or the chart itself.

Image volumes need Kubernetes 1.33 or newer and a containerd that implements them, which is why the cluster runs 1.36.

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
