# Liferay GitOps Workspace

This workspace publishes client extensions as OCI artifacts and Helm charts that ArgoCD installs from an environment's `liferay.yaml`. It builds with an unreleased preview of the Liferay Workspace plugin.

## Build the Plugin Preview

The `packageClientExtension` and `publishClientExtension` tasks are not released yet. Build the plugin from a branch that has them and publish it to the local Maven repository:

```bash
cd modules/sdk/gradle-plugins-workspace

../../../gradlew -Psnapshot publishToMavenLocal
```

This publishes the version in the plugin's `bnd.bnd` with a `-SNAPSHOT` suffix. `settings.gradle` resolves `17.1.14-SNAPSHOT` through `mavenLocal()`, so update it when the plugin's version changes.

## Log In to the Registry

The plugin reads registry credentials from `~/.docker/config.json`. Pushing to GitHub Container Registry needs a token with the `write:packages` scope:

```bash
gh auth refresh --hostname github.com --scopes write:packages

gh auth token | docker login ghcr.io --password-stdin --username gamerson
```

## Publish

```bash
./gradlew publishClientExtension
```

Each client extension is pushed twice, under one version:

- `ghcr.io/gamerson/lcd-53966/cx/<serviceId>`: the files, which the workload mounts as an image volume.
- `ghcr.io/gamerson/lcd-53966/charts/<serviceId>`: the Helm chart, which pins the files by digest.

`build/client-extension-publish/summary.json` in each client extension lists both references and their digests. GitHub creates new packages as private. Make each one public in its package settings, or ArgoCD and the cluster cannot pull it.

`gradle.properties` sets where they go and what they are built from:

| Property | Value |
| --- | --- |
| `liferay.workspace.client.extension.chart` | `../../cloud/helm/client-extension`, the chart in this repository rather than the one embedded in the plugin |
| `liferay.workspace.client.extension.registry` | `ghcr.io/gamerson/lcd-53966` |

Override either with `-P`.

## Versions

A client extension's files and chart share one version, which is what `liferay.yaml` pins. The plugin takes the first of:

1. The client extension's Gradle version, for example `version=1.2.0` in `client-extensions/liferay-sample-global-css-1/gradle.properties`.
1. `liferay.workspace.client.extension.version`, in this workspace's `gradle.properties` or passed with `-P`.
1. A version derived from the content, such as `0.0.0-g1a2b3c4d5e6f`, which changes only when the files or configs do.

A published version cannot change. Publishing it again with the same content does nothing, and with different content the build fails. Only a version ending in `-SNAPSHOT` is republished.

## Install with GitOps

List the published charts in an environment's `liferay.yaml` in the GitOps repository. ArgoCD installs each one into that environment's DXP namespace:

```yaml
clientExtensions:
  - chart: oci://ghcr.io/gamerson/lcd-53966/charts/liferay-sample-global-css-1
    version: 0.1.0
    values:
      clientExtension:
        domain: global-css-1-able-nonproduction.runray.xyz
      network:
        enabled: true
        endpointRef: runray.xyz
  - chart: oci://ghcr.io/gamerson/lcd-53966/charts/liferay-sample-instance-settings
    version: 0.1.0
```

A client extension that runs a Deployment needs a domain, because DXP points the browser or its own requests there:

| Client Extension | Workload | Domain |
| --- | --- | --- |
| `liferay-sample-batch` | Job | Not needed |
| `liferay-sample-etc-spring-boot` | Deployment, called by DXP | Required |
| `liferay-sample-global-css-1` | Deployment, called by browsers | Required |
| `liferay-sample-instance-settings` | None, configuration only | Not needed |
