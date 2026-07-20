# Regenerating Generated Code

The operator uses [`controller-gen`](https://github.com/kubernetes-sigs/controller-tools)
to derive Go boilerplate and Kubernetes manifests from the API types and their
markers. Anything `controller-gen` produces is regenerated on demand and must
never be hand-edited — regenerate instead.

## When To Regenerate

Regenerate whenever you touch either of the following:

- **API types** under `resources/api/<group>/<version>` (currently
  `resources/api/licensing/v1alpha1`) — any change to a struct field, its JSON
  tag, or a `// +kubebuilder:` marker. This affects the deepcopy functions and
  the CRD schema.
- **RBAC markers** — any `// +kubebuilder:rbac:...` comment on a reconciler under
  `resources/internal/controller/<group>` (currently
  `resources/internal/controller/licensing`). This affects the generated
  `ClusterRole`.
- **Webhook markers** — a package-level `// +kubebuilder:webhook:...` marker under
  `resources/internal/webhook/<group>` (currently
  `resources/internal/webhook/licensing`). This affects the generated
  `ValidatingWebhookConfiguration`.

The operator uses a multi-group layout: each API group lives under its own
`api/<group>/<version>` package with a matching `internal/controller/<group>`.
The `controller-gen` commands below recurse with `paths=./...`, so adding a new
group (for example `upgrades.liferay.com`) needs no change to the commands.

## Generated Artifacts

| Artifact | Path | Driven By |
|---|---|---|
| Deepcopy methods | `resources/api/<group>/<version>/zz_generated.deepcopy.go` | `// +kubebuilder:object` markers on the API types |
| CRD | `resources/config/crd/bases/licensing.liferay.com_liferayenvironments.yaml` | field types, validation, and printcolumn markers |
| `ClusterRole` | `resources/config/rbac/role.yaml` | `// +kubebuilder:rbac` markers on the reconciler |
| `ValidatingWebhookConfiguration` | `resources/config/webhook/manifests.yaml` | the package-level `// +kubebuilder:webhook` marker |

## The Toolchain

`controller-gen` is pinned in `resources/go.mod` as a Go
[`tool` directive](https://go.dev/doc/modules/managing-dependencies#tools), so
there is nothing to install — it is invoked through `go tool` and the version is
locked with the rest of the module dependencies. Confirm it resolves:

```bash
cd cloud/operator/resources
go tool controller-gen --version
```

To change the pinned version, run
`go get -tool sigs.k8s.io/controller-tools/cmd/controller-gen@<version>`.

## Regenerate

Run all three commands from `cloud/operator/resources`. The first regenerates
the deepcopy methods; the second regenerates the CRD and the `ClusterRole`; the
third regenerates the `ValidatingWebhookConfiguration`.

```bash
cd cloud/operator/resources

go tool controller-gen object paths=./api/...

go tool controller-gen \
	crd \
	rbac:roleName=cne-licensing-agent-role \
	paths=./... \
	output:crd:artifacts:config=config/crd/bases \
	output:rbac:artifacts:config=config/rbac

go tool controller-gen \
	webhook \
	paths=./... \
	output:webhook:artifacts:config=config/webhook
```

Keep `roleName=cne-licensing-agent-role` stable — it is the `metadata.name` of
the generated `ClusterRole`, and changing it silently orphans any binding that
references the old name.

The generated `ValidatingWebhookConfiguration` carries placeholder
`clientConfig.service` values (`webhook-service` in namespace `system`) and no
`caBundle`. These are filled in at deploy time — point the service at the agent
and inject the CA bundle (for example with cert-manager's
`cert-manager.io/inject-ca-from` annotation). The webhook serves on the
manager's webhook port, reading its serving certificate from the standard
controller-runtime location (`/tmp/k8s-webhook-server/serving-certs`).

## Verify

After regenerating, confirm the module still builds and the working tree only
changed where you expected:

```bash
cd cloud/operator/resources
go build ./...
go vet ./...
git status --short config api internal/webhook
```

Commit the regenerated files in the same change as the markers that drove them,
so the generated output never drifts from its source.

## Sync The Helm Chart

The chart at `cloud/helm/operator` ships copies of two generated artifacts, so
regenerating is not enough — mirror the results into the chart:

- **CRD** — copy
  `resources/config/crd/bases/licensing.liferay.com_liferayenvironments.yaml`
  into `cloud/helm/operator/crds/`.
- **RBAC** — the `ClusterRole` rules in `cloud/helm/operator/templates/rbac.yaml`
  are a hand-mirrored copy of `resources/config/rbac/role.yaml`. Update them to
  match whenever the RBAC markers change.

The `ValidatingWebhookConfiguration` is authored directly in the chart's
`templates/webhook.yaml` (not copied from the generated manifest), so its rules
must be kept in step with the `+kubebuilder:webhook` marker by hand.
