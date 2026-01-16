{{/*
Expand the name of the chart.
*/}}
{{- define "openslides.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
*/}}
{{- define "openslides.fullname" -}}
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
Create chart name and version as used by the chart label.
*/}}
{{- define "openslides.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "openslides.labels" -}}
helm.sh/chart: {{ include "openslides.chart" . }}
{{ include "openslides.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "openslides.selectorLabels" -}}
app.kubernetes.io/name: {{ include "openslides.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "openslides.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "openslides.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
Return the proper image name
Uses openslidesImageRegistry which is separate from global.imageRegistry (used by Bitnami)
*/}}
{{- define "openslides.image" -}}
{{- $registryName := .global.openslidesImageRegistry | default "" -}}
{{- $repositoryName := .image.repository -}}
{{- $tag := .image.tag | default "latest" -}}
{{- if $registryName }}
{{- printf "%s/%s:%s" $registryName $repositoryName $tag -}}
{{- else }}
{{- printf "%s:%s" $repositoryName $tag -}}
{{- end }}
{{- end }}

{{/*
Component-specific labels
*/}}
{{- define "openslides.componentLabels" -}}
app.kubernetes.io/component: {{ .component }}
{{- end }}

{{/*
Full labels for a component
*/}}
{{- define "openslides.fullLabels" -}}
{{ include "openslides.labels" .context }}
{{ include "openslides.componentLabels" . }}
{{- end }}

{{/*
Selector labels for a component
*/}}
{{- define "openslides.componentSelectorLabels" -}}
{{ include "openslides.selectorLabels" .context }}
{{ include "openslides.componentLabels" . }}
{{- end }}

{{/*
PostgreSQL host
*/}}
{{- define "openslides.postgresql.host" -}}
{{- if .Values.postgresql.enabled }}
{{- printf "%s-postgresql" (include "openslides.fullname" .) }}
{{- else }}
{{- .Values.externalDatabase.host }}
{{- end }}
{{- end }}

{{/*
Redis host
*/}}
{{- define "openslides.redis.host" -}}
{{- if .Values.redis.enabled }}
{{- printf "%s-redis-master" (include "openslides.fullname" .) }}
{{- else }}
{{- .Values.externalRedis.host }}
{{- end }}
{{- end }}

{{/*
Secrets name
*/}}
{{- define "openslides.secretsName" -}}
{{- printf "%s-secrets" (include "openslides.fullname" .) }}
{{- end }}

{{/*
ConfigMap name
*/}}
{{- define "openslides.configmapName" -}}
{{- printf "%s-config" (include "openslides.fullname" .) }}
{{- end }}
