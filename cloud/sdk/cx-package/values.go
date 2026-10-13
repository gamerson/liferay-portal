package main

import (
	"encoding/json"
	"fmt"
	"maps"
	"strings"
)

const configurationKeyHeadlessServer = "com.liferay.oauth2.provider.configuration.OAuth2ProviderApplicationHeadlessServerConfiguration~"

// bakeValues returns what the build decides about the chart, as an overlay on
// its values.yaml. Nothing here is environment specific.
func bakeValues(
	artifactReference string,
	configs map[string]any,
	dockerfile *dockerfile,
	lcp map[string]any,
	mounts []mount,
	serviceID string,
) map[string]any {
	repository, tag := splitImage(dockerfile.Image)

	workload := map[string]any{
		"containerPort": 80,
		"kind":          "Deployment",
		"replicas":      1,
	}

	if loadBalancer, ok := lcp["loadBalancer"].(map[string]any); ok {
		if targetPort, ok := loadBalancer["targetPort"].(float64); ok {
			workload["containerPort"] = int(targetPort)
		}
	}

	if kind, ok := lcp["kind"].(string); ok && (kind != "") {
		workload["kind"] = kind
	}

	if scale, ok := lcp["scale"].(float64); ok {
		workload["replicas"] = int(scale)
	}

	if schedule, ok := lcp["schedule"].(string); ok && (schedule != "") {
		workload["schedule"] = schedule
	}

	for _, probeName := range []string{"livenessProbe", "readinessProbe"} {
		if probe, ok := lcp[probeName].(map[string]any); ok && (len(probe) > 0) {
			workload[probeName] = probe
		}
	}

	if startupProbe := startupProbeOf(lcp); startupProbe != nil {
		workload["startupProbe"] = startupProbe
	}

	env := map[string]any{}

	for key, value := range dockerfile.Env {
		env[key] = value
	}

	if lcpEnv, ok := lcp["env"].(map[string]any); ok {
		for key, value := range lcpEnv {
			env[key] = fmt.Sprint(value)
		}
	}

	if strings.HasSuffix(repository, "liferay/batch") {
		for configurationKey := range configs {
			if _, identifier, ok := strings.Cut(configurationKey, configurationKeyHeadlessServer); ok {
				identifier, _, _ = strings.Cut(identifier, "/")

				env["LIFERAY_BATCH_OAUTH_APP_ERC"] = identifier
			}
		}
	}

	workload["env"] = env

	workloadEnabled := !strings.HasSuffix(repository, "liferay/noop")

	workload["enabled"] = workloadEnabled

	if len(mounts) == 0 {
		artifactReference = ""
	}

	mountValues := []any{}

	for _, mount := range mounts {
		mountValue := map[string]any{"mountPath": mount.MountPath}

		if mount.SubPath != "" {
			mountValue["subPath"] = mount.SubPath
		}

		mountValues = append(mountValues, mountValue)
	}

	return map[string]any{
		"artifact": map[string]any{
			"mounts":    mountValues,
			"reference": artifactReference,
		},
		"clientExtension": map[string]any{
			"configs":   maps.Clone(configs),
			"serviceId": serviceID,
		},
		"fullnameOverride": serviceID,
		"image": map[string]any{
			"repository": repository,
			"tag":        tag,
		},
		"service": map[string]any{
			"enabled":    workloadEnabled && (workload["kind"] == "Deployment"),
			"targetPort": workload["containerPort"],
		},
		"workload": workload,
	}
}

// mergeConfigs combines every *.client-extension-config.json in the zip into
// the one map spec.configs holds, refusing a configuration key two files both
// declare rather than silently keeping either.
func mergeConfigs(files map[string]*file) (map[string]any, error) {
	configs := map[string]any{}

	sources := map[string]string{}

	for _, name := range sortedNames(files) {
		if !strings.HasSuffix(name, ".client-extension-config.json") || strings.Contains(name, "/") {
			continue
		}

		var fileConfigs map[string]any

		if error := json.Unmarshal(files[name].Data, &fileConfigs); error != nil {
			return nil, fmt.Errorf("configs: parse %s: %w", name, error)
		}

		for configurationKey, configuration := range fileConfigs {
			if source, ok := sources[configurationKey]; ok {
				return nil, fmt.Errorf("configs: %s and %s both declare %q", source, name, configurationKey)
			}

			configs[configurationKey] = configuration
			sources[configurationKey] = name
		}
	}

	if len(configs) == 0 {
		return nil, fmt.Errorf("configs: the zip carries no *.client-extension-config.json")
	}

	return configs, nil
}

func splitImage(image string) (string, string) {
	if repository, digest, ok := strings.Cut(image, "@"); ok {
		return repository, "@" + digest
	}

	index := strings.LastIndex(image, ":")

	return image[:index], image[index+1:]
}

// startupProbeOf derives a startup probe from LCP.json's probes. LCP.json
// declares them with no initial delay, relying on the platform to allow for
// start up, and Kubernetes does not: a liveness probe fires at once and kills a
// JVM before Tomcat is listening. A startup probe on the same endpoint holds
// both off until the process has answered once, allowing five minutes.
func startupProbeOf(lcp map[string]any) map[string]any {
	for _, probeName := range []string{"readinessProbe", "livenessProbe"} {
		probe, ok := lcp[probeName].(map[string]any)

		if !ok {
			continue
		}

		startupProbe := map[string]any{"failureThreshold": 60, "periodSeconds": 5}

		for _, handler := range []string{"exec", "grpc", "httpGet", "tcpSocket"} {
			if value, ok := probe[handler]; ok {
				startupProbe[handler] = value
			}
		}

		if len(startupProbe) > 2 {
			return startupProbe
		}
	}

	return nil
}
