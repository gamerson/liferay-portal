# Client Extension Operator: Status Report

Generated 2026-09-24T22:57:53+00:00 against k3d cluster `cx-spike`.

| Component | Value |
|---|---|
| Cluster | v1.36.4+k3s1 |
| Liferay namespace | `liferay-prod` |
| Liferay image | `liferay/dxp:latest` plus modules from this checkout |
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

**44 client extensions** — Delivered 44/44, Provisioned 44/44, Ready 44/44.

| Client Extension | Workload | Payloads | Delivered | Provisioned | Phase | Note |
|---|---|---|---|---|---|---|
| `liferay-sample-audiences-custom-attributes` | none | 1 | True | True | Ready |  |
| `liferay-sample-batch` | none | 1 | True | True | Ready |  |
| `liferay-sample-commerce-checkout-step` | none | 1 | True | True | Ready |  |
| `liferay-sample-commerce-payment-integration` | none | 1 | True | True | Ready |  |
| `liferay-sample-commerce-shipping-engine` | none | 1 | True | True | Ready |  |
| `liferay-sample-commerce-tax-engine` | none | 1 | True | True | Ready |  |
| `liferay-sample-custom-element-1` | none | 1 | True | True | Ready |  |
| `liferay-sample-custom-element-2` | none | 1 | True | True | Ready |  |
| `liferay-sample-custom-element-3` | none | 1 | True | True | Ready |  |
| `liferay-sample-custom-element-4` | none | 1 | True | True | Ready |  |
| `liferay-sample-custom-element-5` | none | 1 | True | True | Ready |  |
| `liferay-sample-custom-element-6` | none | 1 | True | True | Ready |  |
| `liferay-sample-custom-element-7` | none | 1 | True | True | Ready |  |
| `liferay-sample-custom-element-8` | none | 1 | True | True | Ready |  |
| `liferay-sample-editor-config-contributor-1` | none | 1 | True | True | Ready |  |
| `liferay-sample-editor-config-contributor-2` | none | 1 | True | True | Ready |  |
| `liferay-sample-editor-config-contributor-3` | none | 1 | True | True | Ready |  |
| `liferay-sample-editor-config-contributor-4` | none | 1 | True | True | Ready |  |
| `liferay-sample-editor-config-contributor-5` | none | 1 | True | True | Ready |  |
| `liferay-sample-editor-config-contributor-6` | none | 1 | True | True | Ready |  |
| `liferay-sample-etc-cron` | none | 1 | True | True | Ready |  |
| `liferay-sample-etc-frontend` | none | 1 | True | True | Ready |  |
| `liferay-sample-etc-node` | none | 1 | True | True | Ready |  |
| `liferay-sample-etc-spring-boot` | none | 1 | True | True | Ready |  |
| `liferay-sample-fds-cell-renderer` | none | 1 | True | True | Ready |  |
| `liferay-sample-fds-filter` | none | 1 | True | True | Ready |  |
| `liferay-sample-global-css-1` | none | 1 | True | True | Ready |  |
| `liferay-sample-global-css-2` | none | 1 | True | True | Ready |  |
| `liferay-sample-global-js-1` | none | 1 | True | True | Ready |  |
| `liferay-sample-global-js-2` | none | 1 | True | True | Ready |  |
| `liferay-sample-global-js-3` | none | 1 | True | True | Ready |  |
| `liferay-sample-iframe-1` | none | 1 | True | True | Ready |  |
| `liferay-sample-iframe-2` | none | 1 | True | True | Ready |  |
| `liferay-sample-instance-settings` | none | 1 | True | True | Ready |  |
| `liferay-sample-js-import-maps-entry` | none | 1 | True | True | Ready |  |
| `liferay-sample-site-initializer` | none | 1 | True | True | Ready |  |
| `liferay-sample-static-content` | none | 1 | True | True | Ready |  |
| `liferay-sample-theme-css-1` | none | 1 | True | True | Ready |  |
| `liferay-sample-theme-css-2` | none | 1 | True | True | Ready |  |
| `liferay-sample-theme-css-3` | none | 1 | True | True | Ready |  |
| `liferay-sample-theme-css-4` | none | 1 | True | True | Ready |  |
| `liferay-sample-theme-favicon` | none | 1 | True | True | Ready |  |
| `liferay-sample-theme-spritemap-1` | none | 1 | True | True | Ready |  |
| `liferay-sample-theme-spritemap-2` | none | 1 | True | True | Ready |  |

### Scenario B: client extensions in a separate namespace

**44 client extensions** — Delivered 44/44, Provisioned 44/44, Ready 44/44.

| Client Extension | Workload | Payloads | Delivered | Provisioned | Phase | Note |
|---|---|---|---|---|---|---|
| `liferay-sample-audiences-custom-attributes` | none | 1 | True | True | Ready |  |
| `liferay-sample-batch` | none | 1 | True | True | Ready |  |
| `liferay-sample-commerce-checkout-step` | none | 1 | True | True | Ready |  |
| `liferay-sample-commerce-payment-integration` | none | 1 | True | True | Ready |  |
| `liferay-sample-commerce-shipping-engine` | none | 1 | True | True | Ready |  |
| `liferay-sample-commerce-tax-engine` | none | 1 | True | True | Ready |  |
| `liferay-sample-custom-element-1` | none | 1 | True | True | Ready |  |
| `liferay-sample-custom-element-2` | none | 1 | True | True | Ready |  |
| `liferay-sample-custom-element-3` | none | 1 | True | True | Ready |  |
| `liferay-sample-custom-element-4` | none | 1 | True | True | Ready |  |
| `liferay-sample-custom-element-5` | none | 1 | True | True | Ready |  |
| `liferay-sample-custom-element-6` | none | 1 | True | True | Ready |  |
| `liferay-sample-custom-element-7` | none | 1 | True | True | Ready |  |
| `liferay-sample-custom-element-8` | none | 1 | True | True | Ready |  |
| `liferay-sample-editor-config-contributor-1` | none | 1 | True | True | Ready |  |
| `liferay-sample-editor-config-contributor-2` | none | 1 | True | True | Ready |  |
| `liferay-sample-editor-config-contributor-3` | none | 1 | True | True | Ready |  |
| `liferay-sample-editor-config-contributor-4` | none | 1 | True | True | Ready |  |
| `liferay-sample-editor-config-contributor-5` | none | 1 | True | True | Ready |  |
| `liferay-sample-editor-config-contributor-6` | none | 1 | True | True | Ready |  |
| `liferay-sample-etc-cron` | none | 1 | True | True | Ready |  |
| `liferay-sample-etc-frontend` | none | 1 | True | True | Ready |  |
| `liferay-sample-etc-node` | none | 1 | True | True | Ready |  |
| `liferay-sample-etc-spring-boot` | none | 1 | True | True | Ready |  |
| `liferay-sample-fds-cell-renderer` | none | 1 | True | True | Ready |  |
| `liferay-sample-fds-filter` | none | 1 | True | True | Ready |  |
| `liferay-sample-global-css-1` | none | 1 | True | True | Ready |  |
| `liferay-sample-global-css-2` | none | 1 | True | True | Ready |  |
| `liferay-sample-global-js-1` | none | 1 | True | True | Ready |  |
| `liferay-sample-global-js-2` | none | 1 | True | True | Ready |  |
| `liferay-sample-global-js-3` | none | 1 | True | True | Ready |  |
| `liferay-sample-iframe-1` | none | 1 | True | True | Ready |  |
| `liferay-sample-iframe-2` | none | 1 | True | True | Ready |  |
| `liferay-sample-instance-settings` | none | 1 | True | True | Ready |  |
| `liferay-sample-js-import-maps-entry` | none | 1 | True | True | Ready |  |
| `liferay-sample-site-initializer` | none | 1 | True | True | Ready |  |
| `liferay-sample-static-content` | none | 1 | True | True | Ready |  |
| `liferay-sample-theme-css-1` | none | 1 | True | True | Ready |  |
| `liferay-sample-theme-css-2` | none | 1 | True | True | Ready |  |
| `liferay-sample-theme-css-3` | none | 1 | True | True | Ready |  |
| `liferay-sample-theme-css-4` | none | 1 | True | True | Ready |  |
| `liferay-sample-theme-favicon` | none | 1 | True | True | Ready |  |
| `liferay-sample-theme-spritemap-1` | none | 1 | True | True | Ready |  |
| `liferay-sample-theme-spritemap-2` | none | 1 | True | True | Ready |  |

### Scenario C: a namespace Liferay has not consented to

**1 client extensions** — Delivered 0/1, Provisioned 0/1, Ready 0/1.

| Client Extension | Workload | Payloads | Delivered | Provisioned | Phase | Note |
|---|---|---|---|---|---|---|
| `liferay-sample-iframe-2` | none | 0 | False | - | Degraded | EnvironmentUnusable |

## Handshake Artifacts

Objects produced by one client extension, end to end.

```
# 1. The chart renders a ClientExtension
NAME                             SERVICE-ID                       VI                     INTERNAL   PUBLIC
liferay-sample-etc-spring-boot   liferay-sample-etc-spring-boot   liferay.localtest.me   <none>     <none>

# 2. The operator publishes ext-provision ConfigMaps, split by addressing bucket
NAME                                                                              MAIN-DOMAIN
liferay-sample-etc-spring-boot-liferay.localtest.me-lxc-ext-provision-metadata    liferay-sample-etc-spring-boot.localtest.me
liferay-sample-etc-spring-boot-virtual1.localtest.me-lxc-ext-provision-metadata   liferay-sample-etc-spring-boot.vi2.localtest.me

# 3. Liferay writes back ext-init with the OAuth2 credentials
NAME                                                                        KEYS
liferay-sample-etc-spring-boot-liferay.localtest.me-lxc-ext-init-metadata   map[liferay-sample-etc-spring-boot
Liferay.Headless.Admin.Workflow.everything]
liferay-sample-etc-spring-boot-virtual1.localtest.me-lxc-ext-init-metadata   map[liferay-sample-etc-spring-boo
Liferay.Headless.Admin.Workflow.everything]

# 4. The operator mirrors it into a Secret, never a ConfigMap
NAME                                          TYPE     KEYS
liferay-sample-etc-spring-boot-lxc-ext-init   Opaque   map[liferay-sample-etc-spring-boot-oaua.oauth2.authoriz

# 5. The virtual instance reports back on the payload it was given
NAME   ACCEPTED   ERRORS

# 6. A rejected payload names the stage that failed

# 7. The workload mounts both, and the operator injected them
artifact -> 
lxc-dxp-metadata -> liferay.localtest.me-lxc-dxp-metadata
lxc-ext-init-metadata -> liferay-sample-etc-spring-boot-lxc-ext-init

# 8. A shared virtual instance mirror, owned by every client extension using it
liferay.localtest.me-lxc-dxp-metadata owners=liferay-sample-audiences-custom-attributes,liferay-sample-batch,liferay-sample-commerce-checkout-step,liferay-sample-commerce-payment-integration,liferay-s

```

## What Is Under Test

Everything in this report runs against a published Liferay DXP image carrying the portal modules built from this checkout. Nothing is simulated: the ext-provision payloads are read by `portal-k8s-agent`, the OAuth2 applications are registered by Liferay, and the credentials in every ext-init Secret were issued by it.

A workload that is not Ready is therefore a real failure rather than a limit of the harness. The CronJob sample is the exception -- it is created on its declared schedule and does not run inside the test window.

