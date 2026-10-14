// Command liferay-cx-package turns a client extension zip from a Liferay
// Workspace build into what a cluster runs, without building an image.
//
// The zip carries a Dockerfile that is never more than a FROM, a COPY and at
// most a RUN: a stock Liferay base image plus the extension's files copied to
// where that image expects them. Rather than run that build, this reads it.
// The files are published as a single layer OCI image that the chart mounts
// into the unmodified base image as an image volume, pinned by digest, and the
// chart is published with the configuration payload, the base image, the
// mounts and the workload shape from LCP.json baked into its defaults.
//
//	liferay-cx-package --registry <host>/<path> --zip <zip> [--version <version>]
//
// publishes <registry>/cx/<serviceId>, the files, and
// <registry>/charts/<serviceId>, the chart, and prints a JSON summary.
package main

import (
	"context"
	"encoding/json"
	"errors"
	"flag"
	"fmt"
	"io/fs"
	"os"
	"path/filepath"
	"regexp"
	"strings"
)

func main() {
	if error := run(context.Background(), os.Args[1:]); error != nil {
		fmt.Fprintf(os.Stderr, "liferay-cx-package: %v\n", error)

		os.Exit(1)
	}
}

func packageClientExtension(
	chartFS fs.FS,
	clusterRegistry string,
	containerRuntime string,
	context context.Context,
	outputDirectory string,
	plainHTTP bool,
	push bool,
	registry string,
	serviceID string,
	version string,
	zipPath string,
) (*summary, error) {
	if error := os.MkdirAll(outputDirectory, 0o755); error != nil {
		return nil, error
	}

	files, error := readZip(zipPath)

	if error != nil {
		return nil, error
	}

	dockerfileFile, ok := files["Dockerfile"]

	if !ok {
		return nil, fmt.Errorf("%s carries no Dockerfile", zipPath)
	}

	dockerfile, error := parseDockerfile(string(dockerfileFile.Data))

	if error != nil {
		return nil, error
	}

	configs, error := mergeConfigs(files)

	if error != nil {
		return nil, error
	}

	var lcp map[string]any

	if lcpFile, ok := files["LCP.json"]; ok {
		if error := json.Unmarshal(lcpFile.Data, &lcp); error != nil {
			return nil, fmt.Errorf("LCP.json: %w", error)
		}
	}

	warnings := dockerfile.Warnings

	if replayable(dockerfile) {
		files, error = replayRuns(containerRuntime, context, dockerfile, files)

		if error != nil {
			return nil, error
		}
	} else {
		for _, runCommand := range dockerfile.Runs {
			warnings = append(
				warnings,
				fmt.Sprintf(
					"RUN %s is not applied because the zip is not copied whole to one destination.", runCommand,
				),
			)
		}
	}

	hardLinks, mounts, error := planMounts(dockerfile.Copies, files)

	if error != nil {
		return nil, error
	}

	tag := strings.ReplaceAll(version, "+", "_")

	summary := summary{Warnings: warnings}

	artifactLayout := filepath.Join(outputDirectory, "artifact")
	artifactRepository := registry + "/cx/" + serviceID

	if len(mounts) > 0 {
		layer, error := buildLayer(files, hardLinks)

		if error != nil {
			return nil, error
		}

		artifactDescriptor, error := writeArtifact(context, layer, artifactLayout, tag)

		if error != nil {
			return nil, error
		}

		summary.Artifact = &artifactSummary{
			Digest:    artifactDescriptor.Digest.String(),
			Layout:    artifactLayout,
			Mounts:    mounts,
			Reference: clusterRegistry + "/cx/" + serviceID + "@" + artifactDescriptor.Digest.String(),
		}
	}

	artifactReference := ""

	if summary.Artifact != nil {
		artifactReference = summary.Artifact.Reference
	}

	chartArchive, chartMetadata, error := packageChart(
		chartFS, serviceID, bakeValues(artifactReference, configs, dockerfile, lcp, mounts, serviceID), version,
	)

	if error != nil {
		return nil, error
	}

	chartArchivePath := filepath.Join(outputDirectory, fmt.Sprintf("%s-%s.tgz", serviceID, version))

	if error := os.WriteFile(chartArchivePath, chartArchive, 0o644); error != nil {
		return nil, error
	}

	chartLayout := filepath.Join(outputDirectory, "chart")
	chartRepository := registry + "/charts/" + serviceID

	chartDescriptor, error := writeChart(chartArchive, chartMetadata, context, chartLayout, tag)

	if error != nil {
		return nil, error
	}

	summary.Chart = chartSummary{
		Archive:   chartArchivePath,
		Digest:    chartDescriptor.Digest.String(),
		Layout:    chartLayout,
		Reference: "oci://" + chartRepository,
		Version:   version,
	}

	if !push {
		return &summary, nil
	}

	if summary.Artifact != nil {
		if _, error := copyToRegistry(context, artifactLayout, plainHTTP, artifactRepository, tag); error != nil {
			return nil, error
		}
	}

	if _, error := copyToRegistry(context, chartLayout, plainHTTP, chartRepository, tag); error != nil {
		return nil, error
	}

	summary.Pushed = true

	return &summary, nil
}

func run(context context.Context, arguments []string) error {
	flagSet := flag.NewFlagSet("liferay-cx-package", flag.ContinueOnError)

	chartDirectory := flagSet.String(
		"chart", "", "a client extension chart directory to package instead of the one built into this binary",
	)
	clusterRegistry := flagSet.String(
		"cluster-registry", "",
		"the registry the cluster pulls the artifact from, when it reaches --registry under another name",
	)
	containerRuntime := flagSet.String("container-runtime", "docker", "the container runtime that replays RUN steps")
	outputDirectory := flagSet.String("output-dir", "", "where to write the OCI layouts and the chart archive")
	plainHTTP := flagSet.Bool("plain-http", false, "push over HTTP instead of HTTPS")
	push := flagSet.Bool("push", true, "push the artifact and the chart; false only writes them to --output-dir")
	registry := flagSet.String("registry", "", "the registry and path to publish to, such as ghcr.io/example")
	serviceID := flagSet.String("service-id", "", "the client extension's serviceId; defaults to the zip's name")
	version := flagSet.String("version", "0.1.0", "the chart version and the artifact tag")
	zipPath := flagSet.String("zip", "", "the client extension zip a Liferay Workspace build produced")

	if error := flagSet.Parse(arguments); error != nil {
		return error
	}

	if *zipPath == "" {
		return errors.New("--zip is required")
	}

	if *registry == "" {
		return errors.New("--registry is required")
	}

	if !semanticVersionPattern.MatchString(*version) {
		return fmt.Errorf("--version %q is not a semantic version", *version)
	}

	if *clusterRegistry == "" {
		*clusterRegistry = *registry
	}

	if *serviceID == "" {
		*serviceID = strings.TrimSuffix(filepath.Base(*zipPath), ".zip")
	}

	if *outputDirectory == "" {
		temporaryDirectory, error := os.MkdirTemp("", "cx-package-")

		if error != nil {
			return error
		}

		*outputDirectory = temporaryDirectory
	}

	chartFS, error := fs.Sub(embeddedChart, "chart")

	if error != nil {
		return error
	}

	if *chartDirectory != "" {
		chartFS = os.DirFS(*chartDirectory)
	}

	summary, error := packageClientExtension(
		chartFS, *clusterRegistry, *containerRuntime, context, *outputDirectory, *plainHTTP, *push, *registry, *serviceID,
		*version, *zipPath,
	)

	if error != nil {
		return error
	}

	encoder := json.NewEncoder(os.Stdout)

	encoder.SetIndent("", "\t")

	return encoder.Encode(summary)
}

type artifactSummary struct {
	Digest    string  `json:"digest"`
	Layout    string  `json:"layout"`
	Mounts    []mount `json:"mounts"`
	Reference string  `json:"reference"`
}

type chartSummary struct {
	Archive   string `json:"archive"`
	Digest    string `json:"digest"`
	Layout    string `json:"layout"`
	Reference string `json:"reference"`
	Version   string `json:"version"`
}

type summary struct {
	Artifact *artifactSummary `json:"artifact,omitempty"`
	Chart    chartSummary     `json:"chart"`
	Pushed   bool             `json:"pushed"`
	Warnings []string         `json:"warnings"`
}

var semanticVersionPattern = regexp.MustCompile(
	`^(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)(-[0-9A-Za-z.-]+)?(\+[0-9A-Za-z.-]+)?$`,
)
