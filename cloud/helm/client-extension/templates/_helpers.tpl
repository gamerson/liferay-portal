{{/*
Chart name and version, for the helm.sh/chart label.
*/}}

{{- define "liferay-client-extension.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
The ConfigMap carrying the virtual instance's routes. DXP writes it in its own
namespace, and the DXP operator mirrors it into this one under the same name
when the two differ, so one name works either way.
*/}}

{{- define "liferay-client-extension.dxpMetadataName" -}}
{{- printf "%s-lxc-dxp-metadata" .Values.clientExtension.virtualInstanceId }}
{{- end }}

{{/*
The ConfigMap carrying the OAuth2 credentials DXP issues. Like the dxp metadata,
it has the same name in DXP's namespace and in the DXP operator's mirror.
*/}}

{{- define "liferay-client-extension.extInitName" -}}
{{- printf "%s-%s-lxc-ext-init-metadata" (include "liferay-client-extension.serviceId" .) .Values.clientExtension.virtualInstanceId }}
{{- end }}

{{/*
Fully qualified name. It is also the ClientExtension's name and the name of the
workload it references.
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
Whether the workload mounts the ext-init ConfigMap. DXP writes it only for a
client extension whose configs declare an OAuth2 application, the same test the
DXP operator applies, so a pod mounting it unconditionally would wait for a
ConfigMap that never comes.
*/}}

{{- define "liferay-client-extension.requiresExtInit" -}}
{{- range $configurationKey, $_ := .Values.clientExtension.configs }}
{{- if or (hasPrefix "com.liferay.oauth2.provider.configuration.OAuth2ProviderApplicationHeadlessServerConfiguration" $configurationKey) (hasPrefix "com.liferay.oauth2.provider.configuration.OAuth2ProviderApplicationUserAgentConfiguration" $configurationKey) -}}
true
{{- end }}
{{- end }}
{{- end }}

{{/*
Selector labels.
*/}}

{{- define "liferay-client-extension.selectorLabels" -}}
app.kubernetes.io/instance: {{ include "liferay-client-extension.fullname" . }}
app.kubernetes.io/name: {{ include "liferay-client-extension.serviceId" . }}
{{- end }}

{{/*
The service ID, which DXP uses to name the ext-init ConfigMap it writes.
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
