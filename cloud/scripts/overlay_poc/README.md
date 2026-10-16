# Overlay Image Volume Proof of Concept

Scripts that publish a Liferay Workspace as an OCI overlay image and run the `liferay-default` chart against it on a Kubernetes 1.36 k3d cluster, so each step of the overlay's life can be watched: first install, update, removal of a configuration, removal of a module, a hotfix, and removal of the overlay itself.

## How the Overlay Works

The workspace's `publishOverlay` task publishes everything the workspace adds to Liferay as a single layer image that holds files alone, laid out as a Liferay home directory:

| Path in the Image | Source in the Workspace |
| --- | --- |
| `osgi/client-extensions/*.zip` | Client extension projects |
| `osgi/configs/*.config` | `configs/{common,docker,<environment>}/osgi/configs` |
| `osgi/modules/*.jar` | Module projects and `providedModules` |
| `osgi/war/*.war` | WAR and theme projects |
| `patching/*.zip` | `configs/{common,docker,<environment>}/patching` |
| Anything else | `configs/{common,docker,<environment>}` |

GitOps names the image by digest in `overlay.artifacts`, and the chart mounts each artifact as an image volume at `/mnt/liferay/overlays/<name>`. Nothing is copied to the persistent volume:

1. The `liferay-prepopulate-overlay` init container removes every symbolic link in `osgi/client-extensions`, `osgi/configs`, `osgi/modules`, and `osgi/war` that points into an overlay, then links each overlay file in. It never touches a regular file, so what the overlay contributes to the persistent volume is a function of the digests alone. Liferay's file install uninstalls a bundle whose file is gone at startup.

1. The `020_copy_overlay_files.sh` pre-configure script copies every other overlay file into the Liferay home directory, but only onto the container's own filesystem, which is recreated on every start. A file whose destination is on a volume, such as `portal-ext.properties` or `tomcat/conf/server.xml`, is skipped with a message.

1. The artifact with `hotfix: true` has its `patching` directory mounted at `/mnt/liferay/patching` in place of the persistent volume, where the image's own entrypoint installs it.

Liferay's file install notices a changed file by its name, size, and modification time, never by its content, so `publishOverlay` derives each file's modification time from its content. Identical inputs produce the same digest, and a changed JAR of the same size still looks changed. Module JARs are only reproducible when bnd is told to make them so, with `-reproducible: true`.

## Running It

```bash
./bootstrap.sh
```

Creates a k3d registry at `k3d-overlay-oci.localhost:5112`, a k3s 1.36 cluster that pulls from it, and imports `liferay/dxp:2026.q3.5`. A name under `.localhost` resolves to the host on the host and to the registry inside the cluster, so the reference `publishOverlay` prints is the one the cluster pulls.

The workspace resolves `com.liferay.gradle.plugins.workspace` from Maven local first, so publish the plugin from this branch before running it:

```bash
cd ../../../modules/sdk/gradle-plugins-workspace && ../../../gradlew publishToMavenLocal
```

Each scenario publishes an overlay, deploys it, and inspects the pod:

| Scenario | Commands |
| --- | --- |
| Install | `./publish_overlay.sh app v1 v1 && ./deploy_liferay.sh app` |
| Update the module and its configuration | `./publish_overlay.sh app v2 v2 && ./deploy_liferay.sh app` |
| Remove the configuration | `./publish_overlay.sh app v3 v2 && ./deploy_liferay.sh app` |
| Remove the module | `./publish_overlay.sh app v3 none && ./deploy_liferay.sh app` |
| Add a hotfix overlay | `./publish_overlay.sh hotfix hotfix none && ./deploy_liferay.sh app hotfix:hotfix` |
| Remove every overlay | `./deploy_liferay.sh` |

```bash
./inspect_liferay.sh
```

Prints what the link step did, what the main container copied or skipped, the linked directories on the persistent volume, the demo bundle's OSGi state, and the installed hotfix.

```bash
./teardown.sh
```

Deletes the cluster and the registry.