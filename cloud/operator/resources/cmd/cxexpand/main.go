// Command cxexpand prepares a client-extension.yaml for a ClientExtension
// resource by resolving the parts that need built artifacts. It stands in for
// the Gradle build in the demo scripts.
//
//	cxexpand <project-dir> [> expanded.yaml]
package main

import (
	"fmt"
	"os"

	cxbuild "github.com/liferay/liferay-portal/cloud/operator/internal/cxbuild"
	cxconfig "github.com/liferay/liferay-portal/cloud/operator/internal/cxconfig"
	yaml "sigs.k8s.io/yaml"
)

func main() {
	if len(os.Args) != 2 {
		fmt.Fprintln(os.Stderr, "usage: cxexpand <project-dir>")
		os.Exit(2)
	}

	projectDir := os.Args[1]

	contents, error := os.ReadFile(projectDir + "/client-extension.yaml")

	if error != nil {
		fmt.Fprintf(os.Stderr, "cxexpand: %v\n", error)
		os.Exit(1)
	}

	document, error := cxconfig.Parse(contents)

	if error != nil {
		fmt.Fprintf(os.Stderr, "cxexpand: %v\n", error)
		os.Exit(1)
	}

	if error := cxbuild.Expand(document, projectDir); error != nil {
		fmt.Fprintf(os.Stderr, "cxexpand: %v\n", error)
		os.Exit(1)
	}

	encoded, error := yaml.Marshal(document)

	if error != nil {
		fmt.Fprintf(os.Stderr, "cxexpand: %v\n", error)
		os.Exit(1)
	}

	os.Stdout.Write(encoded)
}
