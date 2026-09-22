// Package cxconfig translates an expanded client-extension.yaml document into
// the OSGi configuration payload that Liferay's portal-k8s-agent consumes.
//
// The rules mirror ClientExtension.toJSONMap and CreateClientExtensionConfigTask
// in modules/sdk/gradle-plugins-workspace. Only the build-independent half is
// ported: glob expansion against built assets and frontendTokenDefinitionJSON
// file inlining stay in the Gradle build, so the document reaching this package
// must already be expanded.
package cxconfig

import (
	_ "embed"
	"strings"
)

// Classification buckets a type into how it is addressed. Frontend types are
// fetched by the browser and need a publicly routable domain; microservice
// types are called by Liferay and can use a cluster-internal address.
const (
	ClassificationBatch         = "batch"
	ClassificationConfiguration = "configuration"
	ClassificationFrontend      = "frontend"
	ClassificationMicroservice  = "microservice"
)

//go:embed client-extension.properties
var clientExtensionProperties string

// Classification returns the classification registered for a client extension
// type, and whether the type is known at all.
func Classification(extensionType string) (string, bool) {
	typeEntry, found := registry[extensionType]

	if !found {
		return "", false
	}

	return typeEntry.classification, true
}

// PID returns the configuration persistent identity registered for a client
// extension type. Types with no PID (batch, siteInitializer) produce no
// configuration entry at all and report false.
func PID(extensionType string) (string, bool) {
	typeEntry, found := registry[extensionType]

	if !found || typeEntry.pid == "" {
		return "", false
	}

	return typeEntry.pid, true
}

type typeEntry struct {
	classification string
	pid            string
}

var registry = parseRegistry(clientExtensionProperties)

func parseRegistry(properties string) map[string]typeEntry {
	var parsed = map[string]typeEntry{}

	for _, line := range strings.Split(properties, "\n") {
		line = strings.TrimSpace(line)

		if line == "" || strings.HasPrefix(line, "#") {
			continue
		}

		key, value, found := strings.Cut(line, "=")

		if !found {
			continue
		}

		extensionType, field, found := strings.Cut(key, ".")

		if !found {
			continue
		}

		entry := parsed[extensionType]

		switch field {
		case "classification":
			entry.classification = value
		case "pid":
			entry.pid = value
		}

		parsed[extensionType] = entry
	}

	return parsed
}
