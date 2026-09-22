{{/*
Chart name and version, for the helm.sh/chart label.
*/}}
{{- define "liferay-client-extension.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Fully qualified name. This is also the ClientExtension resource name and, in
turn, the name of the workload the operator creates, so the Service selector
and the workload resolve without either side guessing.
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
The service ID. It becomes the ext.lxc.liferay.com/serviceId label, which is
what Liferay uses to name the ext-init ConfigMap it writes back.
*/}}
{{- define "liferay-client-extension.serviceId" -}}
{{- required "clientExtension.serviceId is required" .Values.clientExtension.serviceId }}
{{- end }}

{{/*
Selector labels. The operator is additive on pod labels and never rewrites
these, so the Service selector stays under the chart's control.
*/}}
{{- define "liferay-client-extension.selectorLabels" -}}
app.kubernetes.io/instance: {{ include "liferay-client-extension.fullname" . }}
app.kubernetes.io/name: {{ include "liferay-client-extension.serviceId" . }}
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
