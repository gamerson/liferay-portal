package main

import (
	"archive/tar"
	"bytes"
	"compress/gzip"
	"embed"
	"fmt"
	"io/fs"
	"path"
	"slices"

	yaml "gopkg.in/yaml.v3"
)

// chartEntryRank puts Chart.yaml first, as helm package does, and keeps every
// other entry in name order.
func chartEntryRank(name string) int {
	if name == "Chart.yaml" {
		return -1
	}

	return 0
}

func overlayNode(mapping *yaml.Node, overlay map[string]any) error {
	if mapping.Kind != yaml.MappingNode {
		return fmt.Errorf("expected a mapping at line %d", mapping.Line)
	}

	for _, key := range sortedKeys(overlay) {
		value := overlay[key]

		var valueNode *yaml.Node

		for index := 0; index < len(mapping.Content); index += 2 {
			if mapping.Content[index].Value == key {
				valueNode = mapping.Content[index+1]

				break
			}
		}

		if nested, ok := value.(map[string]any); ok && (valueNode != nil) && (valueNode.Kind == yaml.MappingNode) &&
			(len(valueNode.Content) > 0) {

			if error := overlayNode(valueNode, nested); error != nil {
				return error
			}

			continue
		}

		var encodedNode yaml.Node

		if error := encodedNode.Encode(value); error != nil {
			return error
		}

		if valueNode == nil {
			mapping.Content = append(
				mapping.Content,
				&yaml.Node{Kind: yaml.ScalarNode, Tag: "!!str", Value: key},
				&encodedNode,
			)

			continue
		}

		encodedNode.FootComment = valueNode.FootComment
		encodedNode.HeadComment = valueNode.HeadComment
		encodedNode.LineComment = valueNode.LineComment

		*valueNode = encodedNode
	}

	return nil
}

// overlayYAML sets every leaf of the overlay in the document, keeping the
// document's comments and key order, and returns the result both as YAML and
// decoded.
func overlayYAML(document []byte, overlay map[string]any) ([]byte, map[string]any, error) {
	var root yaml.Node

	if error := yaml.Unmarshal(document, &root); error != nil {
		return nil, nil, error
	}

	if (root.Kind != yaml.DocumentNode) || (len(root.Content) == 0) {
		return nil, nil, fmt.Errorf("not a YAML document")
	}

	if error := overlayNode(root.Content[0], overlay); error != nil {
		return nil, nil, error
	}

	var buffer bytes.Buffer

	encoder := yaml.NewEncoder(&buffer)

	encoder.SetIndent(4)

	if error := encoder.Encode(&root); error != nil {
		return nil, nil, error
	}

	if error := encoder.Close(); error != nil {
		return nil, nil, error
	}

	var decoded map[string]any

	if error := yaml.Unmarshal(buffer.Bytes(), &decoded); error != nil {
		return nil, nil, error
	}

	return buffer.Bytes(), decoded, nil
}

// packageChart writes the chart as a chart archive, the same layout helm
// package produces, with the build's values and the chart's name and version
// baked in. Entries are sorted and timestamps fixed, so the same inputs always
// produce the same archive.
func packageChart(
	chartFS fs.FS,
	chartName string,
	values map[string]any,
	version string,
) ([]byte, map[string]any, error) {
	chartFiles := map[string][]byte{}

	if error := fs.WalkDir(
		chartFS, ".",
		func(name string, directoryEntry fs.DirEntry, walkError error) error {
			if (walkError != nil) || directoryEntry.IsDir() {
				return walkError
			}

			data, error := fs.ReadFile(chartFS, name)

			if error != nil {
				return error
			}

			chartFiles[name] = data

			return nil
		},
	); error != nil {
		return nil, nil, error
	}

	chartYAML, chartMetadata, error := overlayYAML(
		chartFiles["Chart.yaml"],
		map[string]any{"appVersion": version, "name": chartName, "version": version},
	)

	if error != nil {
		return nil, nil, fmt.Errorf("chart: Chart.yaml: %w", error)
	}

	chartFiles["Chart.yaml"] = chartYAML

	valuesYAML, _, error := overlayYAML(chartFiles["values.yaml"], values)

	if error != nil {
		return nil, nil, fmt.Errorf("chart: values.yaml: %w", error)
	}

	chartFiles["values.yaml"] = valuesYAML

	var buffer bytes.Buffer

	gzipWriter := gzip.NewWriter(&buffer)

	tarWriter := tar.NewWriter(gzipWriter)

	names := sortedKeys(chartFiles)

	slices.SortFunc(
		names,
		func(left string, right string) int {
			return chartEntryRank(left) - chartEntryRank(right)
		},
	)

	for _, name := range names {
		header := newHeader(0o644, path.Join(chartName, name), tar.TypeReg)

		header.Size = int64(len(chartFiles[name]))

		if error := tarWriter.WriteHeader(header); error != nil {
			return nil, nil, error
		}

		if _, error := tarWriter.Write(chartFiles[name]); error != nil {
			return nil, nil, error
		}
	}

	if error := tarWriter.Close(); error != nil {
		return nil, nil, error
	}

	if error := gzipWriter.Close(); error != nil {
		return nil, nil, error
	}

	return buffer.Bytes(), chartMetadata, nil
}

// embeddedChart is cloud/helm/client-extension, copied in by go generate so the
// binary carries the chart it was built with.
//
//go:embed all:chart
var embeddedChart embed.FS
