package cxconfig_test

import (
	"encoding/json"
	"fmt"
	"os"
	"path/filepath"
	"reflect"
	"sort"
	"testing"

	cxbuild "github.com/liferay/liferay-portal/cloud/operator/internal/cxbuild"
	cxconfig "github.com/liferay/liferay-portal/cloud/operator/internal/cxconfig"
)

const sampleWorkspace = "../../../../../workspaces/liferay-sample-workspace/client-extensions"

// TestTranslateMatchesGradleOutput runs the Go translator over every sample in
// liferay-sample-workspace and compares the result against the payload the
// Gradle task actually produced. The two build-side steps the operator cannot
// perform -- glob expansion against built assets and frontendTokenDefinitionJSON
// inlining -- are replicated in the harness so the corpus is comparable.
func TestTranslateMatchesGradleOutput(t *testing.T) {
	projects, error := os.ReadDir(sampleWorkspace)

	if error != nil {
		t.Skipf("sample workspace unavailable: %v", error)
	}

	var compared int

	for _, project := range projects {
		if !project.IsDir() {
			continue
		}

		projectName := project.Name()
		projectDir := filepath.Join(sampleWorkspace, projectName)

		documentBytes, error := os.ReadFile(filepath.Join(projectDir, "client-extension.yaml"))

		if error != nil {
			continue
		}

		want, error := loadGeneratedConfig(projectDir, projectName)

		if error != nil {
			t.Fatalf("%s: %v", projectName, error)
		}

		t.Run(projectName, func(t *testing.T) {
			document, error := cxconfig.Parse(documentBytes)

			if error != nil {
				t.Fatalf("parse: %v", error)
			}

			if error := cxbuild.Expand(document, projectDir); error != nil {
				t.Fatalf("expand: %v", error)
			}

			entries, error := cxconfig.Translate(document, cxconfig.Options{
				BuildTimestamp: buildTimestampOf(want),
				ProjectName:    projectName,
			})

			if error != nil {
				t.Fatalf("translate: %v", error)
			}

			var got = map[string]any{}

			for _, entry := range entries {
				got[entry.PID] = entry.Config
			}

			if len(got) != len(want) {
				t.Fatalf("entry count: got %d %v, want %d %v", len(got), keysOf(got), len(want), keysOf(want))
			}

			for pid, wantConfig := range want {
				gotConfig, found := got[pid]

				if !found {
					t.Fatalf("missing configuration %q, got %v", pid, keysOf(got))
				}

				compareConfig(t, pid, gotConfig.(map[string]any), wantConfig.(map[string]any))
			}
		})

		compared++
	}

	if compared == 0 {
		t.Fatal("no sample projects were compared")
	}

	t.Logf("compared %d sample projects", compared)
}

func compareConfig(t *testing.T, pid string, got map[string]any, want map[string]any) {
	t.Helper()

	for key, wantValue := range want {
		gotValue, found := got[key]

		if !found {
			t.Errorf("%s: missing key %q", pid, key)

			continue
		}

		// typeSettings and properties are encoded lists whose order is
		// unstable in the Gradle task; compare them as sets.
		if key == "properties" || key == "typeSettings" {
			if !equalStringSets(gotValue, wantValue) {
				t.Errorf("%s: %s\n got  %v\n want %v", pid, key, sortedStrings(gotValue), sortedStrings(wantValue))
			}

			continue
		}

		if !equalJSON(gotValue, wantValue) {
			t.Errorf("%s: %s\n got  %#v\n want %#v", pid, key, gotValue, wantValue)
		}
	}

	for key := range got {
		if _, found := want[key]; !found {
			t.Errorf("%s: unexpected key %q = %#v", pid, key, got[key])
		}
	}
}

func equalJSON(got any, want any) bool {
	gotBytes, _ := json.Marshal(got)
	wantBytes, _ := json.Marshal(want)

	return string(gotBytes) == string(wantBytes)
}

func equalStringSets(got any, want any) bool {
	return reflect.DeepEqual(sortedStrings(got), sortedStrings(want))
}

func sortedStrings(value any) []string {
	var strings0 []string

	switch typed := value.(type) {
	case []string:
		strings0 = append(strings0, typed...)
	case []any:
		for _, element := range typed {
			strings0 = append(strings0, fmt.Sprintf("%v", element))
		}
	}

	sort.Strings(strings0)

	return strings0
}

func keysOf(values map[string]any) []string {
	var keys []string

	for key := range values {
		keys = append(keys, key)
	}

	sort.Strings(keys)

	return keys
}

func buildTimestampOf(want map[string]any) int64 {
	for _, value := range want {
		config, isMap := value.(map[string]any)

		if !isMap {
			continue
		}

		timestamp, isNumber := config["buildTimestamp"].(float64)

		if isNumber {
			return int64(timestamp)
		}
	}

	return 0
}

func loadGeneratedConfig(projectDir string, projectName string) (map[string]any, error) {
	path := filepath.Join(
		projectDir, "build", "liferay-client-extension-build",
		projectName+".client-extension-config.json",
	)

	contents, error := os.ReadFile(path)

	if error != nil {
		if os.IsNotExist(error) {
			return map[string]any{}, nil
		}

		return nil, error
	}

	var generated map[string]any

	if error := json.Unmarshal(contents, &generated); error != nil {
		return nil, error
	}

	return generated, nil
}
