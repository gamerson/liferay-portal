# Client Extension Operator: Status Report

Generated 2026-09-23T21:31:06+00:00 against k3d cluster `cx-spike`.

| Component | Value |
|---|---|
| Cluster | v1.31.4+k3s1 |
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

**5 client extensions** — Delivered 5/5, Provisioned 5/5, Ready 5/5.

| Client Extension | Workload | Payloads | Delivered | Provisioned | Phase | Note |
|---|---|---|---|---|---|---|
| `liferay-sample-batch` | Job | 1 | True | True | Ready |  |
| `liferay-sample-custom-element-2` | Deployment | 1 | True | True | Ready |  |
| `liferay-sample-etc-node` | Deployment | 1 | True | True | Ready |  |
| `liferay-sample-etc-spring-boot` | Deployment | 1 | True | True | Ready |  |
| `liferay-sample-global-js-1` | Deployment | 1 | True | True | Ready |  |

### Scenario B: client extensions in a separate namespace

_No client extensions in `team-a`._

### Scenario C: a namespace Liferay has not consented to

_No client extensions in `team-b`._

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
lxc-dxp-metadata -> liferay.localtest.me-lxc-dxp-metadata
lxc-ext-init-metadata -> liferay-sample-etc-spring-boot-lxc-ext-init

# 8. A shared virtual instance mirror, owned by every client extension using it

```

## What Is Under Test

Everything in this report runs against a published Liferay DXP image carrying the portal modules built from this checkout. Nothing is simulated: the ext-provision payloads are read by `portal-k8s-agent`, the OAuth2 applications are registered by Liferay, and the credentials in every ext-init Secret were issued by it.

A workload that is not Ready is therefore a real failure rather than a limit of the harness. The CronJob sample is the exception -- it is created on its declared schedule and does not run inside the test window.

