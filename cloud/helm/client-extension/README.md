# Liferay Client Extension Chart

This chart deploys one Liferay client extension to Kubernetes. It renders a `ClientExtension` resource for the DXP operator and the workload that runs the client extension: a Deployment, Job or CronJob, with an optional Service, Ingress and ServiceAccount.

The DXP operator does the rest. It delivers the configuration to DXP, mirrors DXP's routes and the OAuth2 credentials DXP issues into the client extension's namespace, checks that the workload mounts them, and rolls the workload when they change. `kubectl get clientextensions` reports where each client extension stands.

## Requirements

- Kubernetes 1.33 or newer when the files are mounted from an artifact, because that uses image volumes. Running your own image works on older versions.

- The DXP operator, installed with the `liferay-dxp-operator` chart.

- When the client extension runs in a namespace other than DXP's, DXP's namespace must list that namespace in its `cx.liferay.com/allowed-client-extension-namespaces` annotation. The chart renders the RoleBinding the operator needs in the client extension's namespace.

## Choosing a Mode

A Liferay Workspace build produces a zip, and the same files in `build/liferay-client-extension-build`: a Dockerfile, `LCP.json`, a `*.client-extension-config.json`, and the client extension's own files. The chart can run them in either of two ways.

| Mode | Image | Client Extension Files | Values to Supply |
| --- | --- | --- | --- |
| Mounted artifact | The stock base image the Dockerfile names, such as `liferay/caddy` or `liferay/jar-runner` | An OCI artifact mounted into the base image | Only the environment binding |
| Your own image | An image you build from the Dockerfile | Inside the image | Everything the build decided, plus the environment binding |

Both modes render the same `ClientExtension` and are handled the same way by the operator.

## Mounted Artifact

The Liferay Workspace's `publishClientExtension` task turns a client extension project into a chart ready to install, with no image build. It publishes the build zip's files as a single layer OCI artifact and publishes a copy of this chart, named after the client extension, with everything the build decided baked into its defaults: the configuration, the base image, the artifact pinned by digest and where it mounts, and the workload shape from `LCP.json`.

```bash
./gradlew :client-extensions:able:publishClientExtension \
	--chart-version 1.0.0 \
	--registry ghcr.io/acme
```

This publishes the files to `ghcr.io/acme/cx/able` and the chart to `oci://ghcr.io/acme/charts/able`. The task reads registry credentials from `~/.docker/config.json` and its credential helpers, so a `docker login` beforehand is enough. In CI, set the registry once in `gradle.properties` or with `-P` instead, since a command line option applies only to the task named just before it:

| Property | Option | Default |
| --- | --- | --- |
| `liferay.workspace.client.extension.chart.version` | `--chart-version` | `0.1.0` |
| `liferay.workspace.client.extension.cluster.registry` | `--cluster-registry` | The registry. Set it when the cluster reaches the registry under another name. |
| `liferay.workspace.client.extension.plain.http` | `--plain-http` | `false` |
| `liferay.workspace.client.extension.registry` | `--registry` | None; required |

`packageClientExtension` does the same without pushing, writing the chart archive and both OCI layouts to `build/client-extension-oci` for inspection.

Install the chart with only the environment binding:

```bash
helm install able oci://ghcr.io/acme/charts/able \
	--namespace cx \
	--set clientExtension.domain=able.example.com \
	--set clientExtension.dxpNamespace=liferay \
	--set clientExtension.virtualInstanceId=liferay.com \
	--version 1.0.0
```

## Your Own Image

Build the Dockerfile the usual way and push the image:

```bash
cd build/liferay-client-extension-build

docker build --tag ghcr.io/acme/able:1.0.0 .

docker push ghcr.io/acme/able:1.0.0
```

Then supply what `publishClientExtension` would otherwise read from the build, in a values file:

```yaml
clientExtension:
    configs:
        # The contents of able.client-extension-config.json. JSON is valid
        # YAML, so the file can be pasted here unchanged.
    domain: able.example.com
    dxpNamespace: liferay
    serviceId: able
    virtualInstanceId: liferay.com
image:
    repository: ghcr.io/acme/able
    tag: 1.0.0
service:
    enabled: true
    targetPort: 58081
workload:
    containerPort: 58081
    kind: Deployment
    livenessProbe:
        httpGet:
            path: /ready
            port: 58081
    readinessProbe:
        httpGet:
            path: /ready
            port: 58081
    startupProbe:
        failureThreshold: 60
        httpGet:
            path: /ready
            port: 58081
        periodSeconds: 5
```

```bash
helm install able oci://<registry>/liferay-client-extension \
	--namespace cx \
	--values able-values.yaml
```

The configuration can also be passed straight from the build instead of pasted:

```bash
helm install able oci://<registry>/liferay-client-extension \
	--namespace cx \
	--set-json "clientExtension.configs=$(cat build/liferay-client-extension-build/able.client-extension-config.json)" \
	--values able-values.yaml
```

Leave `artifact.reference` empty in this mode. The chart then runs the image as is and mounts nothing into it but DXP's metadata.

### Taking Values From the Build

| Value | Where It Comes From |
| --- | --- |
| `clientExtension.configs` | `*.client-extension-config.json` |
| `clientExtension.serviceId` | The workspace project name. DXP names the ConfigMap it writes the OAuth2 credentials to after it, so it must match. |
| `image.repository`, `image.tag` | The image you pushed. Use a new tag for every build, or a digest, because a Deployment does not roll when a tag is reused. |
| `service.enabled`, `service.targetPort` | Enable for a Deployment; `targetPort` is `loadBalancer.targetPort` in `LCP.json`. |
| `workload.containerPort` | `loadBalancer.targetPort` in `LCP.json`. |
| `workload.env` | `env` in `LCP.json`. Leave out `LIFERAY_ROUTES_CLIENT_EXTENSION` and `LIFERAY_ROUTES_DXP`; the chart sets them. |
| `workload.kind` | `kind` in `LCP.json`. |
| `workload.livenessProbe`, `workload.readinessProbe` | `livenessProbe` and `readinessProbe` in `LCP.json`. |
| `workload.replicas` | `scale` in `LCP.json`. |
| `workload.schedule` | `schedule` in `LCP.json`, for a CronJob. |
| `workload.startupProbe` | The readiness probe's handler, with `failureThreshold: 60` and `periodSeconds: 5`. `LCP.json` probes have no initial delay, so without a startup probe the kubelet kills a JVM client extension before it is listening. |

## Environment Binding

These values differ per environment, and are the only ones to supply in the mounted artifact mode.

| Value | Description |
| --- | --- |
| `clientExtension.domain` | The host the client extension is reached at, by browsers through the Ingress and by DXP through the Service. |
| `clientExtension.dxpNamespace` | The namespace DXP runs in. Leave it empty when the client extension runs in DXP's namespace. |
| `clientExtension.virtualInstanceId` | The virtual instance to deliver to. Defaults to `liferay.com`. |
| `ingress.*` | The Ingress, when browsers reach the client extension from outside the cluster. |

## Behavior to Know

- A batch client extension reads its OAuth2 application from `LIFERAY_BATCH_OAUTH_APP_ERC`. `publishClientExtension` sets it; with your own image, set `workload.env.LIFERAY_BATCH_OAUTH_APP_ERC` to the identifier after `~` in the payload's `OAuth2ProviderApplicationHeadlessServerConfiguration~<identifier>` key.

- A configuration only client extension, one whose Dockerfile is `FROM liferay/noop`, has nothing to run. Set `workload.enabled` to `false`; the chart then renders no workload and the operator reports it as configuration only. With your own image, there is nothing to build.

- A Job's pod template is immutable, so the chart keeps a Job on upgrade rather than replacing it, which would rerun it. Delete the Job before upgrading to a new image.

- The ext-init ConfigMap, which carries the OAuth2 credentials, is mounted only when the configuration declares an OAuth2 application, the same test the operator applies. DXP writes it for no other client extension, and a pod mounting it would wait for it forever.

- The workload always mounts DXP's routes at `/etc/liferay/lxc/dxp-metadata` and sets `LIFERAY_ROUTES_DXP` to that path, and mounts the OAuth2 credentials at `/etc/liferay/lxc/ext-init-metadata` with `LIFERAY_ROUTES_CLIENT_EXTENSION` when it needs them. The operator checks this wiring and reports a missing piece in `status.workloadIssues`.
