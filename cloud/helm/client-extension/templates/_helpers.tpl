{{/*
Chart name and version, for the helm.sh/chart label.
*/}}

{{- define "liferay-client-extension.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
The ConfigMap carrying the virtual instance's routes. Liferay publishes it in
its own namespace and the operator mirrors it here under the same name when the
two namespaces differ, so one name works either way.
*/}}

{{- define "liferay-client-extension.dxpMetadataName" -}}
{{- printf "%s-lxc-dxp-metadata" .Values.clientExtension.virtualInstanceId }}
{{- end }}

{{/*
The Secret the operator mirrors Liferay's OAuth2 credentials into. It does not
exist until Liferay has answered, which is deliberate: a pod that mounts it
waits in ContainerCreating until the handshake completes, rather than starting
without credentials and failing at runtime.
*/}}

{{- define "liferay-client-extension.extInitSecretName" -}}
{{- printf "%s-lxc-ext-init" (include "liferay-client-extension.serviceId" .) | trunc 253 | trimSuffix "-" }}
{{- end }}

{{/*
Fully qualified name. This is also the ClientExtension resource name and the
workload name it references, so the Service selector and the reference resolve
without either side guessing.
*/}}

{{- define "liferay-client-extension.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Common labels.
*/}}

{{- define "liferay-client-extension.labels" -}}
app.kubernetes.io/managed-by: {{ .Release.Service }}
helm.sh/chart: {{ include "liferay-client-extension.chart" . }}
{{ include "liferay-client-extension.selectorLabels" . }}
{{- with .Values.labels }}
{{ toYaml . }}
{{- end }}
{{- end }}

{{/*
Expand the name of the chart.
*/}}

{{- define "liferay-client-extension.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Whether this client extension needs the OAuth2 credentials mounted. Liferay
issues them only for a client extension that declares an oAuthApplication entry,
so mounting the Secret unconditionally would leave a frontend-only extension
waiting for a Secret that is never written. Detected from the payload, and
overridable with clientExtension.requiresOAuth when the payload is opaque.
*/}}

{{- define "liferay-client-extension.requiresOAuth" -}}
{{- if kindIs "invalid" .Values.clientExtension.requiresOAuth -}}
{{- if contains "oAuthApplication" (printf "%s%s" .Values.clientExtension.clientExtensionYaml (toString .Values.clientExtension.configs)) -}}
true
{{- end -}}
{{- else if .Values.clientExtension.requiresOAuth -}}
true
{{- end -}}
{{- end }}

{{/*
Selector labels.
*/}}

{{- define "liferay-client-extension.selectorLabels" -}}
app.kubernetes.io/instance: {{ include "liferay-client-extension.fullname" . }}
app.kubernetes.io/name: {{ include "liferay-client-extension.serviceId" . }}
{{- end }}

{{/*
The service ID. It becomes the ext.lxc.liferay.com/serviceId label, which is
what Liferay uses to name the ext-init ConfigMap it writes back.
*/}}

{{- define "liferay-client-extension.serviceId" -}}
{{- required "clientExtension.serviceId is required" .Values.clientExtension.serviceId }}
{{- end }}

{{/*
Name of the service account to use.
*/}}

{{- define "liferay-client-extension.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "liferay-client-extension.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}