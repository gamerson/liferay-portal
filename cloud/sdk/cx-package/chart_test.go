package main

import (
	"bytes"
	"os"
	"strings"
	"testing"
)

func TestOverlayYAMLKeepsComments(t *testing.T) {
	document := []byte("# The workload.\nworkload:\n    # Immutable.\n    kind: Deployment\n    replicas: 1\n")

	overlaid, decoded, error := overlayYAML(
		document, map[string]any{"workload": map[string]any{"kind": "Job", "schedule": "* * * * *"}},
	)

	if error != nil {
		t.Fatal(error)
	}

	for _, want := range []string{"# The workload.", "# Immutable.", "kind: Job", "replicas: 1", "schedule: '* * * * *'"} {
		if !strings.Contains(string(overlaid), want) {
			t.Errorf("Expected %q in\n%s", want, overlaid)
		}
	}

	if decoded["workload"].(map[string]any)["kind"] != "Job" {
		t.Errorf("decoded = %v, want workload.kind Job", decoded)
	}
}

func TestPackageChartIsDeterministic(t *testing.T) {
	values := map[string]any{"clientExtension": map[string]any{"serviceId": "able"}}

	first, chartMetadata, error := packageChart(os.DirFS("../../helm/client-extension"), "able", values, "1.0.0")

	if error != nil {
		t.Fatal(error)
	}

	second, _, error := packageChart(os.DirFS("../../helm/client-extension"), "able", values, "1.0.0")

	if error != nil {
		t.Fatal(error)
	}

	if !bytes.Equal(first, second) {
		t.Error("Expected the same inputs to produce the same chart archive")
	}

	if (chartMetadata["name"] != "able") || (chartMetadata["version"] != "1.0.0") {
		t.Errorf("chartMetadata = %v, want name able and version 1.0.0", chartMetadata)
	}
}
