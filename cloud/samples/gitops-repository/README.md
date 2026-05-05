# Sample GitOps Repository

A minimal Liferay Cloud Native GitOps repository layout for GCP that demonstrates the **shared workspace overlay bucket** feature: one `${deployment_name}-liferay-overlay` GCS bucket per GitOps repo, shared across every environment, with the bucket name injected by Terraform — so developers never write `bucketName` anywhere in this directory.

## Layout

```
liferay/
├── projects/
│   └── sample/
│       ├── base/                       # values applied to every env in this project
│       │   ├── infrastructure.yaml     # liferay-gcp-infrastructure overrides
│       │   └── liferay.yaml            # liferay-gcp / liferay-default overrides
│       └── environments/
│           ├── dev/                    # per-env overrides for dev
│           │   ├── infrastructure.yaml
│           │   └── liferay.yaml
│           └── prd/                    # per-env overrides for prd
│               ├── infrastructure.yaml
│               └── liferay.yaml
└── system/
    ├── infrastructure-provider.yaml    # liferay-gcp-infrastructure-provider overrides
    └── resources.yaml                  # liferay-gcp-resources overrides (NEW)
```

The path glob comes from the Terraform stack at `cloud/terraform/gcp/gitops/resources/`:

| ArgoCD Application                  | Path                                                         |
| ----------------------------------- | ------------------------------------------------------------ |
| `liferay-infrastructure-applicationset` | `liferay/projects/*/environments/*/infrastructure.yaml`  |
| `liferay-applicationset`            | `liferay/projects/*/environments/*/liferay.yaml`             |
| `liferay-infrastructure-provider`   | `liferay/system/infrastructure-provider.yaml`                |
| `liferay-resources`                 | `liferay/system/resources.yaml`                              |

`{{path[2]}}` resolves to the project name (`sample`); `{{path[4]}}` resolves to the environment name (`dev`, `prd`).

## What developers configure here

| File                                  | Owns                                                           |
| ------------------------------------- | -------------------------------------------------------------- |
| `system/resources.yaml`               | Whether the workspace overlay bucket should be provisioned.    |
| `system/infrastructure-provider.yaml` | Cluster-wide provider toggles.                                 |
| `projects/sample/base/infrastructure.yaml` | DB / backup / overlay-enabled / storage shape for every env. |
| `projects/sample/base/liferay.yaml`   | Liferay image tag and overlay copy mappings.                   |
| `projects/sample/environments/<env>/infrastructure.yaml` | Env-level toggles.                              |
| `projects/sample/environments/<env>/liferay.yaml`        | Per-env hostnames, replica counts, env-specific overrides. |

## What developers do NOT configure here

These flow in from Terraform as ArgoCD `Application` Helm parameters and override anything you'd set in YAML:

| Application                          | Helm parameter                                       | Source                                                |
| ------------------------------------ | ---------------------------------------------------- | ----------------------------------------------------- |
| `liferay-resources`                  | `deploymentName`                                     | `var.deployment_name`                                 |
| `liferay-resources`                  | `region`                                             | `var.region`                                          |
| `liferay-infrastructure-applicationset` | `overlay.bucketName`                              | `local.overlay_bucket_name` = `${var.deployment_name}-liferay-overlay` |
| `liferay-infrastructure-applicationset` | `environmentId`, `gateway.*`, `projectId`, `region`, `secretStoreName` | various                                  |
| `liferay-applicationset`             | `liferay-default.overlay.bucketName`                 | `local.overlay_bucket_name`                           |
| `liferay-applicationset`             | `liferay-default.environmentId`, `liferay-default.network.gatewayName`, `global.deploymentName`, `global.projectId`, `global.liferayServiceAccount.*` | various |
| `liferay-infrastructure-provider`    | `crossplaneGsaEmail`, `crossplaneNamespace`, `deploymentName`, `gateway.*`, `global.gcp.*` | various |

In particular, **the overlay bucket name is never written by the developer**. To verify that, this sample contains zero occurrences of `bucketName`:

```sh
grep -rn bucketName liferay/
# (no output)
```

## Where overlay artifacts come from

The Liferay pod's overlay-sync init container reads from `gs://${deployment_name}-liferay-overlay/` (the bucket created by `liferay-resources` and granted read access by the per-env `BucketIAMMember` from the `gcp-infrastructure-provider` composition). The customer's workspace build CI uploads to that bucket; CI auth is out of scope for this sample.

The `overlay.copy` block in `projects/sample/base/liferay.yaml` configures which paths inside the bucket are mounted to which paths inside the Liferay container. In this sample:

```yaml
overlay:
    copy:
        -   from: "overlay-build-1/osgi/*"
            into: "osgi/"
```

…which maps `gs://${deployment_name}-liferay-overlay/overlay-build-1/osgi/*` to `/opt/liferay/osgi/`.

## Using this sample

1. Copy this directory into a private git repository:
   ```sh
   cp -r cloud/samples/gitops-repository/* /path/to/your-gitops-repo/
   cd /path/to/your-gitops-repo && git init && git add . && git commit -m "Initial GitOps layout"
   git remote add origin <your-private-repo-url> && git push -u origin main
   ```

2. Point the Terraform stack at it. In your `terraform.tfvars` (or wherever you set the variables for `cloud/terraform/gcp/gitops/resources/`):
   ```hcl
   liferay_git_repo_url = "https://github.com/your-org/your-gitops-repo.git"
   # (infrastructure_git_repo_config.url defaults to liferay_git_repo_url)
   ```

3. `terraform apply`. ArgoCD picks up the four Applications and starts reconciling. The bucket appears as `${deployment_name}-liferay-overlay`.

4. Upload your workspace build outputs to the bucket — for example:
   ```sh
   gsutil cp -r build/overlay/osgi/ gs://${deployment_name}-liferay-overlay/overlay-build-1/osgi/
   ```

5. Both `sample-dev` and `sample-prd` Liferay pods pick up the same bucket on next reconcile.
