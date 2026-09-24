// Package cxbuild performs the two client-extension.yaml steps that depend on
// built artifacts: expanding globs in URL valued keys against the built static
// directory, and inlining frontendTokenDefinitionJSON from a file.
//
// These belong to the build, not the operator: the assets live inside the
// container image, where a controller cannot see them. The package exists so
// the same rules back both the conformance test and the cxexpand command that
// prepares a document for a ClientExtension resource.
package cxbuild

import (
	"encoding/json"
	"fmt"
	"os"
	"path/filepath"
	"regexp"
	"sort"
	"strings"

	cxconfig "github.com/liferay/liferay-portal/cloud/operator/internal/cxconfig"
)

// Expand resolves globs and inlines file references in place.
func Expand(document cxconfig.Document, projectDir string) error {
	staticDir := filepath.Join(projectDir, "build", "liferay-client-extension-build", "static")

	for _, entry := range document {
		for key, value := range entry {
			if key == "frontendTokenDefinitionJSON" {
				inlined, error := inlineJSON(projectDir, value)

				if error != nil {
					return error
				}

				if inlined != "" {
					entry[key] = inlined
				}

				continue
			}

			if !strings.Contains(strings.ToLower(key), "url") {
				continue
			}

			expanded, error := expandValue(staticDir, value)

			if error != nil {
				return error
			}

			if expanded != nil {
				entry[key] = expanded
			}
		}
	}

	return nil
}

// IsWildcard mirrors CreateClientExtensionConfigTask._isWildcardValue: only a
// star triggers expansion, and a URL is left alone.
func IsWildcard(value string) bool {
	return strings.Contains(value, "*") && !strings.Contains(value, "://")
}

func expandValue(staticDir string, value any) (any, error) {
	switch typed := value.(type) {
	case string:
		if !IsWildcard(typed) {
			return nil, nil
		}

		matches, error := MatchingPaths(staticDir, typed)

		if error != nil {
			return nil, error
		}

		return matches[0], nil
	case []any:
		var expanded []any

		for _, element := range typed {
			elementString, isString := element.(string)

			if !isString || !IsWildcard(elementString) {
				expanded = append(expanded, element)

				continue
			}

			matches, error := MatchingPaths(staticDir, elementString)

			if error != nil {
				return nil, error
			}

			for _, match := range matches {
				expanded = append(expanded, match)
			}
		}

		return expanded, nil
	}

	return nil, nil
}

func inlineJSON(projectDir string, value any) (string, error) {
	path, isString := value.(string)

	if !isString || strings.HasPrefix(strings.TrimSpace(path), "{") {
		return "", nil
	}

	contents, error := os.ReadFile(filepath.Join(projectDir, path))

	if error != nil {
		return "", error
	}

	var parsed any

	if error := json.Unmarshal(contents, &parsed); error != nil {
		return "", error
	}

	encoded, error := json.Marshal(parsed)

	if error != nil {
		return "", error
	}

	return string(encoded), nil
}

// MatchingPaths returns every path under baseDir matching a glob, sorted, as
// Files.walk plus a glob PathMatcher does in the Gradle task.
func MatchingPaths(baseDir string, glob string) ([]string, error) {
	pattern, compileError := regexp.Compile(globToRegexp(glob))

	if compileError != nil {
		return nil, compileError
	}

	var matches []string

	walk := func(path string, info os.FileInfo, walkError error) error {
		if walkError != nil {
			return walkError
		}

		relative, relativeError := filepath.Rel(baseDir, path)

		if relativeError != nil {
			return relativeError
		}

		if pattern.MatchString(relative) {
			matches = append(matches, relative)
		}

		return nil
	}

	if walkError := filepath.Walk(baseDir, walk); walkError != nil {
		return nil, walkError
	}

	if len(matches) == 0 {
		return nil, fmt.Errorf("no paths matched %q under %s", glob, baseDir)
	}

	sort.Strings(matches)

	return matches, nil
}

func globToRegexp(glob string) string {
	var builder strings.Builder

	builder.WriteString("^")

	for index := 0; index < len(glob); index++ {
		switch {
		case strings.HasPrefix(glob[index:], "**/"):
			builder.WriteString("(?:.*/)?")

			index += 2
		case glob[index] == '*':
			builder.WriteString("[^/]*")
		default:
			builder.WriteString(regexp.QuoteMeta(string(glob[index])))
		}
	}

	builder.WriteString("$")

	return builder.String()
}
