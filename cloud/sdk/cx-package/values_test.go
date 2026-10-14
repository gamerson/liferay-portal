package main

import (
	"reflect"
	"strings"
	"testing"
)

func TestBakeValues(t *testing.T) {
	testCases := map[string]struct {
		configs     map[string]any
		dockerfile  dockerfile
		lcp         map[string]any
		mounts      []mount
		wantEnv     map[string]any
		wantEnabled bool
		wantProbe   bool
		wantRef     string
	}{
		"a batch client extension names its OAuth2 application": {
			configs: map[string]any{
				configurationKeyHeadlessServer + "able-batch/liferay.com": map[string]any{},
			},
			dockerfile:  dockerfile{Image: "liferay/batch:latest"},
			lcp:         map[string]any{"kind": "Job"},
			mounts:      []mount{{MountPath: "/opt/liferay/batch", SubPath: "batch"}},
			wantEnabled: true,
			wantEnv:     map[string]any{"LIFERAY_BATCH_OAUTH_APP_ERC": "able-batch"},
			wantRef:     "registry/cx/able@sha256:1",
		},
		"a configuration only client extension runs nothing": {
			dockerfile: dockerfile{Image: "liferay/noop:latest"},
			lcp:        map[string]any{"kind": "Job"},
			wantEnv:    map[string]any{},
		},
		"LCP.json env wins and a probe yields a startup probe": {
			dockerfile: dockerfile{Env: map[string]string{"A": "dockerfile", "B": "dockerfile"}, Image: "liferay/jar-runner:latest"},
			lcp: map[string]any{
				"env":            map[string]any{"A": "lcp", "PORT": float64(58081)},
				"readinessProbe": map[string]any{"httpGet": map[string]any{"path": "/ready"}, "timeoutSeconds": float64(5)},
			},
			mounts:      []mount{{MountPath: "/opt/liferay", SubPath: ".cx-mounts/0"}},
			wantEnabled: true,
			wantEnv:     map[string]any{"A": "lcp", "B": "dockerfile", "PORT": "58081"},
			wantProbe:   true,
			wantRef:     "registry/cx/able@sha256:1",
		},
	}

	for name, testCase := range testCases {
		t.Run(name, func(t *testing.T) {
			values := bakeValues(
				"registry/cx/able@sha256:1", testCase.configs, &testCase.dockerfile, testCase.lcp, testCase.mounts, "able",
			)

			artifact := values["artifact"].(map[string]any)

			if artifact["reference"] != testCase.wantRef {
				t.Errorf("artifact.reference = %q, want %q", artifact["reference"], testCase.wantRef)
			}

			workload := values["workload"].(map[string]any)

			if workload["enabled"] != testCase.wantEnabled {
				t.Errorf("workload.enabled = %v, want %v", workload["enabled"], testCase.wantEnabled)
			}

			if !reflect.DeepEqual(workload["env"], testCase.wantEnv) {
				t.Errorf("workload.env = %v, want %v", workload["env"], testCase.wantEnv)
			}

			startupProbe, ok := workload["startupProbe"].(map[string]any)

			if ok != testCase.wantProbe {
				t.Fatalf("workload.startupProbe = %v, want one: %v", workload["startupProbe"], testCase.wantProbe)
			}

			if ok && (startupProbe["httpGet"] == nil || startupProbe["timeoutSeconds"] != nil) {
				t.Errorf("workload.startupProbe = %v, want only the readiness probe's handler", startupProbe)
			}
		})
	}
}

func TestMergeConfigsRejectsDuplicateKeys(t *testing.T) {
	files := map[string]*file{
		"a.client-extension-config.json": {Data: []byte(`{"key~a": {}}`), Name: "a.client-extension-config.json"},
		"b.client-extension-config.json": {Data: []byte(`{"key~a": {}}`), Name: "b.client-extension-config.json"},
	}

	if _, error := mergeConfigs(files); (error == nil) || !strings.Contains(error.Error(), `both declare "key~a"`) {
		t.Errorf("mergeConfigs() error = %v, want a duplicate key", error)
	}
}
