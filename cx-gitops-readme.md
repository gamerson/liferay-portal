# Client Extensions with GitOps on AWS

These steps reproduce the client extension demo on an AWS environment created by the Liferay Cloud Native bootstrap. ArgoCD installs every client extension listed in an environment's `liferay.yaml`, and the DXP operator connects each one to DXP. Everything is built from the `cxGitopsPoc` branch.

The steps were run end to end on 2026-10-29 against the `gma-lcd54120` deployment (AWS bootstrap 4.0.0, EKS 1.37), environment `able-nonproduction`.

## What Gets Installed

| Piece | Built From | What It Adds |
| --- | --- | --- |
| `liferay-platform-components` chart | `cloud/helm/platform-components` | An ApplicationSet that turns each `clientExtensions` entry in a `liferay.yaml` into an ArgoCD Application, and the `liferay-client-extension` AppProject those Applications run in |
| `liferay-aws` chart | `cloud/helm/aws`, which wraps `cloud/helm/default` | When client extension delivery is on: the configuration for DXP's Kubernetes agent, and ConfigMap create and update for DXP's service account |
| DXP operator | The bootstrap, unchanged | The `ClientExtension` controller. The platform chart turns it on by default. |
| Workspace plugin | `modules/sdk/gradle-plugins-workspace` | `packageClientExtension` and `publishClientExtension` |
| Sample client extensions | `workspaces/liferay-gitops-workspace` | Five samples, each published as a Helm chart plus, when it has files, an OCI artifact |

The five samples:

| Client Extension | Workload | Needs a Domain |
| --- | --- | --- |
| `liferay-sample-batch` | Job that imports a `Sample` object definition with four object actions | No |
| `liferay-sample-custom-element-2` | Deployment on `liferay/caddy` | Yes |
| `liferay-sample-etc-spring-boot` | Deployment on `liferay/jar-runner` | Yes |
| `liferay-sample-global-css-1` | Deployment on `liferay/caddy` | Yes |
| `liferay-sample-instance-settings` | None, configuration only | No |

## Variables

| Variable | Meaning | Demo Value |
| --- | --- | --- |
| `${GITHUB_USER}` | Your GitHub user name | `gamerson` |
| `${REGISTRY}` | Where charts and artifacts are pushed, under your GitHub user | `ghcr.io/gamerson/lcd-53966` |
| `${DEPLOYMENT}` | `deployment_name` in the bootstrap's `config.json` | `gma-lcd54120` |
| `${DOMAIN}` | Domain with a wildcard TLS certificate | `runray.xyz` |
| `${PROJECT}` | Project directory in the GitOps repository | `able` |
| `${ENV}` | Environment under `${PROJECT}` | `nonproduction` |
| `${NS}` | The environment's namespace, `liferay-${PROJECT}-${ENV}` | `liferay-able-nonproduction` |

## Prerequisites

- **The AWS bootstrap's own prerequisites:**
	- Administrator access to an AWS account through SSO.
	- A fork of `LiferayCloud/cloud-native-gitops-boilerplate`.
	- A DNS provider where you can create CNAME records.
	- Two AWS Secrets Manager secrets: `liferay-certificates-<slug>`, which holds the wildcard certificate for `${DOMAIN}`, and `liferay-credentials-<name>`, which holds the GitOps repository login.
- **One Liferay environment provisioned**, with `enabled: true` in `liferay/projects/${PROJECT}/environments/${ENV}/infrastructure.yaml`.
- **Read access to `gs://liferay-cloud-native-bootstrap-nonprd`**, where CI uploads release candidate bootstrap tarballs.
- **Tools:** `aws`, `gcloud`, `gh`, `docker`, `helm`, `kubectl`, `jq`, `yq` (mikefarah), `dig`, and a JDK that can run the portal's Gradle build.

Image volumes need Kubernetes 1.35 or later. The bootstrap creates its EKS cluster at the newest version EKS offers, which was 1.37 for the demo.

## Step 1: Check Out the Branch

```bash
git fetch https://github.com/gamerson/liferay-portal.git cxGitopsPoc:cxGitopsPoc

git worktree add ../wt-cx-gitops cxGitopsPoc

cd ../wt-cx-gitops
```

A new worktree has no Gradle distribution under `tools/`, and `gradlew` fails with `FileNotFoundException` until it does:

```bash
curl \
	--location \
	--output tools/gradle-8.5.LIFERAY-PATCHED-1-bin.zip \
	--url https://releases-cdn.liferay.com/tools/gradle/gradle-8.5.LIFERAY-PATCHED-1-bin.zip
```

Run every later step from the root of this worktree unless it says otherwise.

## Step 2: Log In to GitHub Container Registry

Pushing needs a token with the `write:packages` scope. The workspace plugin reads registry credentials from Docker's configuration, and Helm keeps its own login, so log in to both:

```bash
gh auth refresh --hostname github.com --scopes write:packages

gh auth token | docker login ghcr.io --password-stdin --username "${GITHUB_USER}"

gh auth token | helm registry login ghcr.io --password-stdin --username "${GITHUB_USER}"
```

Without a Docker credential helper, `docker login` stores the token in plain text in `~/.docker/config.json`. Run `docker logout ghcr.io` when you are done.

## Step 3: Publish the Platform Charts

Package both charts under versions of your own, so they never collide with a released chart. The versions below are the ones the demo used:

```bash
mkdir --parents /tmp/cx-charts

helm package cloud/helm/platform-components --destination /tmp/cx-charts --version 0.3.1-cx.2

helm dependency build cloud/helm/aws

helm package cloud/helm/aws --destination /tmp/cx-charts --version 4.0.1-cx.1

helm push /tmp/cx-charts/liferay-platform-components-0.3.1-cx.2.tgz "oci://${REGISTRY}"

helm push /tmp/cx-charts/liferay-aws-4.0.1-cx.1.tgz "oci://${REGISTRY}"
```

GitHub creates every new package as private, and ArgoCD pulls charts anonymously. Make both packages public:

1. Open `https://github.com/users/${GITHUB_USER}/packages/container/<package>/settings`. `<package>` is the package path with each `/` written as `%2F`, for example `lcd-53966%2Fliferay-aws`.
1. Under **Danger Zone**, choose **Change visibility**, then **Public**.

GitHub sets visibility per package, so later versions of a public package are public too.

## Step 4: Build the Workspace Plugin Preview

`packageClientExtension` and `publishClientExtension` are not released yet. Publish the plugin from this branch to the local Maven repository:

```bash
cd modules/sdk/gradle-plugins-workspace

../../../gradlew -Psnapshot publishToMavenLocal
```

This publishes `17.1.14-SNAPSHOT`, which `workspaces/liferay-gitops-workspace/settings.gradle` resolves through `mavenLocal()`. Keep `-Psnapshot`. Without it, the build publishes a plain `17.1.14`, and every other workspace that checks `mavenLocal()` first would pick up the preview.

## Step 5: Publish the Client Extensions

```bash
cd workspaces/liferay-gitops-workspace

./gradlew publishClientExtension -Pliferay.workspace.client.extension.registry="${REGISTRY}"
```

`-P` overrides the demo registry that the workspace's `gradle.properties` sets. Each client extension sets `version=0.0.1` in its own `gradle.properties`, so everything publishes as `0.0.1`. The Spring Boot artifact is about 34 MB, so the run takes about a minute.

Each client extension is pushed as:

- `${REGISTRY}/cx/<serviceId>`: its files, which the workload mounts as an image volume. `liferay-sample-instance-settings` has no files, so it has no artifact.
- `${REGISTRY}/charts/<serviceId>`: its Helm chart, which pins those files by digest.

Make all nine new packages public, as in [Step 3](#step-3-publish-the-platform-charts): five under `charts/` and four under `cx/`. A private chart fails to sync in ArgoCD. A private artifact leaves the pod in `ImagePullBackOff`.

Each client extension's `build/client-extension-publish/summary.json` records what was published. This turns them into the block for [Step 7](#step-7-list-the-client-extensions-in-liferayyaml):

```bash
jq --slurp '{clientExtensions: [.[] | {chart: .chart.reference, version: .chart.version}]}' \
	client-extensions/*/build/client-extension-publish/summary.json | yq --prettyPrint
```

## Step 6: Run the Bootstrap with the Chart Overrides

The demo used the release candidate AWS bootstrap 4.0.0, built from `release/LCD-54120` at `e231156`. Its DXP operator, 0.2.0, already includes the `ClientExtension` controller, and the platform chart turns it on with `operatorApplications.dxpOperator.values.clientExtensions.enabled: true`.

```bash
gcloud storage cp \
	gs://liferay-cloud-native-bootstrap-nonprd/bootstrap/release-LCD-54120/e231156/liferay-aws-bootstrap-4.0.0.tar.gz \
	gs://liferay-cloud-native-bootstrap-nonprd/bootstrap/release-LCD-54120/e231156/liferay-aws-bootstrap-4.0.0.tar.gz.sha256 \
	.

sha256sum --check liferay-aws-bootstrap-4.0.0.tar.gz.sha256

mkdir --parents liferay-aws-bootstrap-4.0.0

tar --directory liferay-aws-bootstrap-4.0.0 --extract --file liferay-aws-bootstrap-4.0.0.tar.gz --gzip
```

In the bootstrap's `config.json`, add `artifacts.charts` to point the two charts at your registry. The bootstrap deep-merges `config.json` over its `config.json.defaults`, so every other chart and image keeps the release candidate's. The demo's whole file, with your values in place of the variables:

```json
{
  "account_id": "<aws account id>",
  "artifacts": {
    "charts": {
      "liferay-aws": {
        "registry": "${REGISTRY}",
        "version": "4.0.1-cx.1"
      },
      "liferay-platform-components": {
        "registry": "${REGISTRY}",
        "version": "0.3.1-cx.2"
      }
    }
  },
  "deployment_name": "${DEPLOYMENT}",
  "marketplace": false,
  "options": {
    "auto_approve": true,
    "parallelism": 10
  },
  "platformComponents": {
    "values": {
      "argoWorkflows": {
        "gateway": {
          "enabled": true,
          "hostname": "argo-${DEPLOYMENT}.${DOMAIN}",
          "tls": {
            "externalSecretKey": "liferay-certificates-<slug>"
          }
        }
      },
      "argocd": {
        "gateway": {
          "enabled": true,
          "hostname": "argocd-${DEPLOYMENT}.${DOMAIN}",
          "tls": {
            "externalSecretKey": "liferay-certificates-<slug>"
          }
        }
      },
      "gitops": {
        "repository": {
          "credentialsSecretName": "liferay-credentials-<name>",
          "url": "https://github.com/<you>/cloud-native-gitops-boilerplate"
        }
      },
      "operatorApplications": {
        "dxpOperator": {
          "values": {
            "debug": true,
            "downloadPollInterval": "5s",
            "heartbeatInterval": "15s",
            "provisioning": {
              "baseURL": "https://api.one-uat.liferay.com"
            },
            "retry": {
              "initialDelay": "5s",
              "maxDelay": "2m"
            }
          }
        }
      }
    }
  },
  "region": "ca-central-1",
  "terraform": {
    "platform": {
      "keda_config": {
        "enabled": false
      },
      "observability_config": {
        "enabled": true
      }
    }
  },
  "tfstate": {
    "bucket_name": "${DEPLOYMENT}-tfstate"
  }
}
```

Run the bootstrap. It is safe to rerun on a deployment that already exists:

```bash
./liferay-aws-bootstrap-4.0.0/cloud/scripts/setup_aws.sh config.json
```

On an existing deployment, DXP's Application switches to `liferay-aws` `4.0.1-cx.1`, and `liferay-default-0` restarts once. Check that the new platform pieces exist:

```bash
kubectl get applicationset client-extension-applicationset --namespace argocd-system

kubectl get appproject liferay-client-extension --namespace argocd-system
```

## Step 7: List the Client Extensions in liferay.yaml

In the GitOps repository, add a top-level `clientExtensions` list to `liferay/projects/${PROJECT}/environments/${ENV}/liferay.yaml`, and add the `liferay-default` values below. Keep the file's other values as they are.

```yaml
clientExtensions:
  - chart: oci://${REGISTRY}/charts/liferay-sample-batch
    version: 0.0.1
  - chart: oci://${REGISTRY}/charts/liferay-sample-custom-element-2
    version: 0.0.1
    values:
      clientExtension:
        domain: custom-element-2-${PROJECT}-${ENV}.${DOMAIN}
      network:
        enabled: true
        endpointRef: ${DOMAIN}
  - chart: oci://${REGISTRY}/charts/liferay-sample-etc-spring-boot
    version: 0.0.1
    values:
      clientExtension:
        domain: etc-spring-boot-${PROJECT}-${ENV}.${DOMAIN}
      network:
        enabled: true
        endpointRef: ${DOMAIN}
  - chart: oci://${REGISTRY}/charts/liferay-sample-global-css-1
    version: 0.0.1
    values:
      clientExtension:
        domain: global-css-1-${PROJECT}-${ENV}.${DOMAIN}
      network:
        enabled: true
        endpointRef: ${DOMAIN}
  - chart: oci://${REGISTRY}/charts/liferay-sample-instance-settings
    version: 0.0.1

liferay-default:
  customEnv:
    x-web-server-protocol:
      - name: LIFERAY_WEB_PERIOD_SERVER_PERIOD_PROTOCOL
        value: https
  network:
    enabled: true
    endpointRef: ${DOMAIN}
    forceHttpsRedirect: true
    hostnames:
      - liferay-${PROJECT}-${ENV}.${DOMAIN}
  replicaCount: 1
```

Write the values out in full, since ArgoCD does not expand variables. Notes on the values:

- **Domains.** A client extension that runs a Deployment needs a domain. DXP builds the extension's base URL from it, and the chart adds it to the extension's HTTPRoute. Keep each domain one label under `${DOMAIN}`, because the wildcard certificate covers only one label.
- **`endpointRef`.** This names the Gateway's HTTPS listener, which takes its name from the endpoint in the environment's `infrastructure.yaml`. The ApplicationSet fills in `network.gatewayName`.
- **`web.server.protocol`.** DXP advertises `http` to client extensions unless this property is set. With `forceHttpsRedirect`, every call an extension makes to DXP over `http` gets a 301. Spring Boot then cannot fetch DXP's signing keys, and the batch Job cannot get a token.
- **`replicaCount: 1`.** DXP must be running to write the ConfigMaps the operator waits for.

Commit and push. For each entry, ArgoCD creates an Application named `${PROJECT}-${ENV}-<serviceId>` and installs it into `${NS}`.

## Step 8: Write DNS Records

List each hostname and its Gateway's load balancer:

```bash
kubectl get httproute --namespace "${NS}" --output json \
	| jq --raw-output '.items[] | .spec.hostnames // [] | .[]'

kubectl get gateway "${PROJECT}-${ENV}-gateway" --namespace "${NS}" --output jsonpath='{.status.addresses[0].value}{"\n"}'
```

Create a CNAME from each of these to that load balancer's host name:

- `liferay-${PROJECT}-${ENV}.${DOMAIN}`
- `custom-element-2-${PROJECT}-${ENV}.${DOMAIN}`
- `etc-spring-boot-${PROJECT}-${ENV}.${DOMAIN}`
- `global-css-1-${PROJECT}-${ENV}.${DOMAIN}`

Check each one through a public resolver, because the local one can cache an earlier miss:

```bash
dig +short @1.1.1.1 "global-css-1-${PROJECT}-${ENV}.${DOMAIN}"
```

## Step 9: Verify

All five client extensions should reach `Ready`. The phase can be `Pending` for a few minutes while DXP starts and writes its metadata.

```bash
kubectl get clientextensions --namespace "${NS}"

kubectl get pods,jobs --namespace "${NS}"
```

The custom element, Spring Boot, and global CSS Deployments should be running, and the batch Job should be complete. Instance settings has no workload.

Check each path between DXP and the client extensions:

| Path | How to Check |
| --- | --- |
| Browser to Caddy | Find the CSS file with `kubectl exec deploy/liferay-sample-global-css-1 --namespace "${NS}" -- ls /public_html`, then `curl --head https://global-css-1-${PROJECT}-${ENV}.${DOMAIN}/<file>`. It returns 200. |
| Browser to Caddy and Spring Boot | Sign in to `https://liferay-${PROJECT}-${ENV}.${DOMAIN}`, add the **Liferay Sample Custom Element 2** widget to a page, and publish it. The widget's scripts load from Caddy with CORS headers, and its dad joke comes from Spring Boot, called with the signed-in user's OAuth2 token. |
| Spring Boot to DXP | `kubectl logs deploy/liferay-sample-etc-spring-boot --namespace "${NS}"` shows a client ID such as `id-…`, not `null`. |
| Batch Job to DXP | The Job's log shows the import as `COMPLETED`, and **Control Panel > Objects** lists **Sample**. |
| DXP to Spring Boot | Under **Control Panel > Object > Samples**, add an entry, then edit and save it, because the actions trigger on update only. Spring Boot's log shows `ObjectAction1RestController` and `ObjectAction2RestController` receiving the entry. The two Node actions fail, because the Node sample is not deployed. |

## Troubleshooting

| Symptom | Cause | Fix |
| --- | --- | --- |
| No `${PROJECT}-${ENV}-liferay-sample-*` Applications | The ApplicationSet could not render | `kubectl get applicationset client-extension-applicationset --namespace argocd-system --output jsonpath='{.status.conditions}'` |
| An Application fails to sync, or a pod is in `ImagePullBackOff` | A package is still private | Make it public, as in [Step 3](#step-3-publish-the-platform-charts) |
| `UnknownVirtualInstance` | DXP has not written `liferay.com-lxc-dxp-metadata` | Check that DXP is running, and that its Application uses `liferay-aws` `4.0.1-cx.1`, which configures DXP's Kubernetes agent |
| `ExtInitMissing` | DXP has not written the extension's ext-init ConfigMap | Same as `UnknownVirtualInstance` |
| Spring Boot crash-loops on `Couldn't retrieve remote JWK set: HTTP 301`, or the batch Job fails with `Unable to get OAuth 2 access token` | DXP advertises `http` | Set `LIFERAY_WEB_PERIOD_SERVER_PERIOD_PROTOCOL` as in [Step 7](#step-7-list-the-client-extensions-in-liferayyaml) |
| The custom element is blocked by CORS | Caddy could not write `/etc/caddy.d` as a non-root user | Republish with this branch's plugin, which mounts an `emptyDir` there |
| A host name does not resolve | The local resolver cached an earlier miss | Check with `dig @1.1.1.1` |
