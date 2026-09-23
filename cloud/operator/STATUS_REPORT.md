# Client Extension Operator: Status Report

Generated 2026-09-23T00:21:41+00:00 against k3d cluster `cx-spike`.

| Component | Value |
|---|---|
| Cluster | v1.31.4+k3s1 |
| Liferay namespace | `liferay-prod` (simulated agent) |
| Operator | `liferay/liferay-dxp-operator:spike` in `liferay-system` |
| Split namespace | `team-a` |
| Unlisted namespace | `team-b` |

## Configuration Translation Conformance

The Go translator is compared against the payload the Gradle task produced for every sample.

```
=== RUN   TestTranslateMatchesGradleOutput/liferay-sample-theme-spritemap-1
=== RUN   TestTranslateMatchesGradleOutput/liferay-sample-theme-spritemap-2
--- PASS: TestTranslateMatchesGradleOutput (0.01s)
PASS
ok  	github.com/liferay/liferay-portal/cloud/operator/internal/cxconfig	(cached)
```

Sample projects translated byte-equivalent to the Gradle output: **44 of 44**.

## Scenarios

### Scenario A: Liferay and client extensions in one namespace

**45 client extensions** — Delivered 45/45, Provisioned 44/45, Ready 37/45.

| Client Extension | Workload | Payloads | Delivered | Provisioned | Phase | Note |
|---|---|---|---|---|---|---|
| `broken-payload-demo` | none | 1 | True | False | Degraded | AwaitingProvisioning |
| `liferay-sample-audiences-custom-attributes` | Deployment | 1 | True | True | Ready | |
| `liferay-sample-batch` | Job | 1 | True | True | Pending | WorkloadNotAvailable |
| `liferay-sample-commerce-checkout-step` | Deployment | 2 | True | True | Pending | WorkloadNotAvailable |
| `liferay-sample-commerce-payment-integration` | Deployment | 1 | True | True | Pending | WorkloadNotAvailable |
| `liferay-sample-commerce-shipping-engine` | Deployment | 1 | True | True | Pending | WorkloadNotAvailable |
| `liferay-sample-commerce-tax-engine` | Deployment | 1 | True | True | Pending | WorkloadNotAvailable |
| `liferay-sample-custom-element-1` | Deployment | 1 | True | True | Ready | |
| `liferay-sample-custom-element-2` | Deployment | 1 | True | True | Ready | |
| `liferay-sample-custom-element-3` | Deployment | 1 | True | True | Ready | |
| `liferay-sample-custom-element-4` | Deployment | 1 | True | True | Ready | |
| `liferay-sample-custom-element-5` | Deployment | 1 | True | True | Ready | |
| `liferay-sample-custom-element-6` | Deployment | 1 | True | True | Ready | |
| `liferay-sample-custom-element-7` | Deployment | 1 | True | True | Ready | |
| `liferay-sample-custom-element-8` | Deployment | 1 | True | True | Ready | |
| `liferay-sample-editor-config-contributor-1` | Deployment | 1 | True | True | Ready | |
| `liferay-sample-editor-config-contributor-2` | Deployment | 1 | True | True | Ready | |
| `liferay-sample-editor-config-contributor-3` | Deployment | 1 | True | True | Ready | |
| `liferay-sample-editor-config-contributor-4` | Deployment | 1 | True | True | Ready | |
| `liferay-sample-editor-config-contributor-5` | Deployment | 1 | True | True | Ready | |
| `liferay-sample-editor-config-contributor-6` | Deployment | 1 | True | True | Ready | |
| `liferay-sample-etc-cron` | CronJob | 1 | True | True | Ready | |
| `liferay-sample-etc-frontend` | Deployment | 1 | True | True | Ready | |
| `liferay-sample-etc-node` | Deployment | 1 | True | True | Ready | |
| `liferay-sample-etc-spring-boot` | Deployment | 1 | True | True | Pending | WorkloadNotAvailable |
| `liferay-sample-fds-cell-renderer` | Deployment | 1 | True | True | Ready | |
| `liferay-sample-fds-filter` | Deployment | 1 | True | True | Ready | |
| `liferay-sample-global-css-1` | Deployment | 1 | True | True | Ready | |
| `liferay-sample-global-css-2` | Deployment | 1 | True | True | Ready | |
| `liferay-sample-global-js-1` | Deployment | 1 | True | True | Ready | |
| `liferay-sample-global-js-2` | Deployment | 1 | True | True | Ready | |
| `liferay-sample-global-js-3` | Deployment | 1 | True | True | Ready | |
| `liferay-sample-iframe-1` | Job | 1 | True | True | Ready | |
| `liferay-sample-iframe-2` | Job | 1 | True | True | Ready | |
| `liferay-sample-instance-settings` | Job | 1 | True | True | Ready | |
| `liferay-sample-js-import-maps-entry` | Deployment | 1 | True | True | Ready | |
| `liferay-sample-site-initializer` | Job | 1 | True | True | Pending | WorkloadNotAvailable |
| `liferay-sample-static-content` | Deployment | 1 | True | True | Ready | |
| `liferay-sample-theme-css-1` | Deployment | 1 | True | True | Ready | |
| `liferay-sample-theme-css-2` | Deployment | 1 | True | True | Ready | |
| `liferay-sample-theme-css-3` | Deployment | 1 | True | True | Ready | |
| `liferay-sample-theme-css-4` | Deployment | 1 | True | True | Ready | |
| `liferay-sample-theme-favicon` | Deployment | 1 | True | True | Ready | |
| `liferay-sample-theme-spritemap-1` | Deployment | 1 | True | True | Ready | |
| `liferay-sample-theme-spritemap-2` | Deployment | 1 | True | True | Ready | |

### Scenario B: client extensions in a separate namespace

**44 client extensions** — Delivered 44/44, Provisioned 44/44, Ready 37/44.

| Client Extension | Workload | Payloads | Delivered | Provisioned | Phase | Note |
|---|---|---|---|---|---|---|
| `liferay-sample-audiences-custom-attributes` | Deployment | 1 | True | True | Ready | |
| `liferay-sample-batch` | Job | 1 | True | True | Pending | WorkloadNotAvailable |
| `liferay-sample-commerce-checkout-step` | Deployment | 2 | True | True | Pending | WorkloadNotAvailable |
| `liferay-sample-commerce-payment-integration` | Deployment | 1 | True | True | Pending | WorkloadNotAvailable |
| `liferay-sample-commerce-shipping-engine` | Deployment | 1 | True | True | Pending | WorkloadNotAvailable |
| `liferay-sample-commerce-tax-engine` | Deployment | 1 | True | True | Pending | WorkloadNotAvailable |
| `liferay-sample-custom-element-1` | Deployment | 1 | True | True | Ready | |
| `liferay-sample-custom-element-2` | Deployment | 1 | True | True | Ready | |
| `liferay-sample-custom-element-3` | Deployment | 1 | True | True | Ready | |
| `liferay-sample-custom-element-4` | Deployment | 1 | True | True | Ready | |
| `liferay-sample-custom-element-5` | Deployment | 1 | True | True | Ready | |
| `liferay-sample-custom-element-6` | Deployment | 1 | True | True | Ready | |
| `liferay-sample-custom-element-7` | Deployment | 1 | True | True | Ready | |
| `liferay-sample-custom-element-8` | Deployment | 1 | True | True | Ready | |
| `liferay-sample-editor-config-contributor-1` | Deployment | 1 | True | True | Ready | |
| `liferay-sample-editor-config-contributor-2` | Deployment | 1 | True | True | Ready | |
| `liferay-sample-editor-config-contributor-3` | Deployment | 1 | True | True | Ready | |
| `liferay-sample-editor-config-contributor-4` | Deployment | 1 | True | True | Ready | |
| `liferay-sample-editor-config-contributor-5` | Deployment | 1 | True | True | Ready | |
| `liferay-sample-editor-config-contributor-6` | Deployment | 1 | True | True | Ready | |
| `liferay-sample-etc-cron` | CronJob | 1 | True | True | Ready | |
| `liferay-sample-etc-frontend` | Deployment | 1 | True | True | Ready | |
| `liferay-sample-etc-node` | Deployment | 1 | True | True | Ready | |
| `liferay-sample-etc-spring-boot` | Deployment | 1 | True | True | Pending | WorkloadNotAvailable |
| `liferay-sample-fds-cell-renderer` | Deployment | 1 | True | True | Ready | |
| `liferay-sample-fds-filter` | Deployment | 1 | True | True | Ready | |
| `liferay-sample-global-css-1` | Deployment | 1 | True | True | Ready | |
| `liferay-sample-global-css-2` | Deployment | 1 | True | True | Ready | |
| `liferay-sample-global-js-1` | Deployment | 1 | True | True | Ready | |
| `liferay-sample-global-js-2` | Deployment | 1 | True | True | Ready | |
| `liferay-sample-global-js-3` | Deployment | 1 | True | True | Ready | |
| `liferay-sample-iframe-1` | Job | 1 | True | True | Ready | |
| `liferay-sample-iframe-2` | Job | 1 | True | True | Ready | |
| `liferay-sample-instance-settings` | Job | 1 | True | True | Ready | |
| `liferay-sample-js-import-maps-entry` | Deployment | 1 | True | True | Ready | |
| `liferay-sample-site-initializer` | Job | 1 | True | True | Pending | WorkloadNotAvailable |
| `liferay-sample-static-content` | Deployment | 1 | True | True | Ready | |
| `liferay-sample-theme-css-1` | Deployment | 1 | True | True | Ready | |
| `liferay-sample-theme-css-2` | Deployment | 1 | True | True | Ready | |
| `liferay-sample-theme-css-3` | Deployment | 1 | True | True | Ready | |
| `liferay-sample-theme-css-4` | Deployment | 1 | True | True | Ready | |
| `liferay-sample-theme-favicon` | Deployment | 1 | True | True | Ready | |
| `liferay-sample-theme-spritemap-1` | Deployment | 1 | True | True | Ready | |
| `liferay-sample-theme-spritemap-2` | Deployment | 1 | True | True | Ready | |

### Scenario C: a namespace Liferay has not consented to

**1 client extensions** — Delivered 0/1, Provisioned 0/1, Ready 0/1.

| Client Extension | Workload | Payloads | Delivered | Provisioned | Phase | Note |
|---|---|---|---|---|---|---|
| `liferay-sample-iframe-2` | Job | 0 | False | - | Degraded | EnvironmentUnusable |

## Handshake Artifacts

Objects produced by one client extension, end to end.

```
# 1. The chart renders a ClientExtension
NAME                             SERVICE-ID                       VI            INTERNAL   PUBLIC
liferay-sample-etc-spring-boot   liferay-sample-etc-spring-boot   liferay.com   auto       liferay-sample-etc-spring-boot.localtest.me

# 2. The operator publishes ext-provision ConfigMaps, split by addressing bucket
NAME                                                                             MAIN-DOMAIN
liferay-sample-etc-spring-boot-liferay.com-internal-lxc-ext-provision-metadata   liferay-sample-etc-spring-boot.liferay-prod.svc.cluster.local:58081

# 3. Liferay writes back ext-init with the OAuth2 credentials
NAME                                                               KEYS
liferay-sample-etc-spring-boot-liferay.com-lxc-ext-init-metadata   map[liferay-sample-etc-spring-boot-oaua.oau

# 4. The operator mirrors it into a Secret, never a ConfigMap
NAME                                          TYPE     KEYS
liferay-sample-etc-spring-boot-lxc-ext-init   Opaque   map[liferay-sample-etc-spring-boot-oaua.oauth2.authoriz

# 5. The virtual instance reports back on the payload it was given
NAME                                                                              ACCEPTED   ERRORS
broken-payload-demo-liferay.com-lxc-ext-status-metadata                           false      1
liferay-sample-audiences-custom-attributes-liferay.com-lxc-ext-status-metadata    true       0
liferay-sample-batch-liferay.com-lxc-ext-status-metadata                          true       0

# 6. A rejected payload names the stage that failed
The virtual instance refused 1 configuration entries. First failure during Parse of "broken-payload-demo.client-extension-config.json": json: cannot unmarshal string into Go value of type map[string]interface {}

# 7. The workload mounts both, and the operator injected them
lxc-dxp-metadata -> liferay.com-lxc-dxp-metadata
lxc-ext-init-metadata -> liferay-sample-etc-spring-boot-lxc-ext-init

# 8. A shared virtual instance mirror, owned by every client extension using it
liferay.com-lxc-dxp-metadata owners=liferay-sample-custom-element-1,liferay-sample-static-content,liferay-sample-etc-node,liferay-sample-audiences-custom-attributes,liferay-sample-batch,liferay-sample

```

## What Is Real And What Is Simulated

Real: the CRD, the operator and both of its controllers, the configuration translator, the Helm chart, the ext-provision and ext-init ConfigMaps, the Secret mirroring, the shared virtual instance mirror, the cross-namespace consent check, and the workloads themselves.

Simulated: Liferay. `dxpsim` reproduces the observable contract of `portal-k8s-agent` -- it watches ext-provision ConfigMaps, treats labels as configuration properties, and writes back ext-init credentials -- but it serves no HTTP and has no database.

That boundary explains every workload that is not Ready:

- The `liferay/jar-runner` microservices boot, read the OAuth2 credentials from the mounted Secret, then try to fetch Liferay's JWKS endpoint over HTTP and exit when nothing answers. Reaching that failure proves the credentials arrived.
- The `liferay/batch` importers call Liferay's headless batch API, which the simulator does not serve.
- The CronJob sample is created on its declared schedule and does not run inside the test window.

The operator-level contract -- Delivered and Provisioned -- is what this spike validates, and it does not depend on the simulator's limits.