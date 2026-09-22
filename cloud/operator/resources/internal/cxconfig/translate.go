package cxconfig

import (
	"encoding/json"
	"fmt"
	"regexp"
	"sort"
	"strconv"
	"strings"

	yaml "sigs.k8s.io/yaml"
)

// Document is one parsed client-extension.yaml. Keys are entry IDs; the
// "assemble" key is build instruction and is ignored.
type Document map[string]map[string]any

// Options carry everything the YAML itself does not know: which project the
// entries came from, which virtual instance they target, and a stable build
// timestamp.
//
// BuildTimestamp must be derived from something that only changes when the
// payload changes. It becomes the client extension type's modifiedDate, so
// recomputing it every reconcile rewrites every configuration.
type Options struct {
	BuildTimestamp    int64
	ProjectName       string
	VirtualInstanceID string
}

// Entry is one translated configuration, kept alongside the classification so
// callers can split a payload across differently addressed ConfigMaps.
type Entry struct {
	Classification string
	Config         map[string]any
	ExtensionType  string
	PID            string
}

// Parse reads an expanded client-extension.yaml document.
func Parse(document []byte) (Document, error) {
	var raw map[string]any

	if error := yaml.Unmarshal(document, &raw); error != nil {
		return nil, fmt.Errorf("client-extension.yaml parse error: %w", error)
	}

	var parsed = Document{}

	for id, value := range raw {
		if id == "assemble" {
			continue
		}

		entry, isEntry := value.(map[string]any)

		if !isEntry {
			continue
		}

		parsed[id] = entry
	}

	return parsed, nil
}

// Translate converts a document into configuration entries keyed by
// "<pid>~<id>". Entries whose type has no registered PID (batch,
// siteInitializer) are skipped, matching the Gradle task.
func Translate(document Document, options Options) ([]Entry, error) {
	var entries []Entry

	var ids []string

	for id := range document {
		ids = append(ids, id)
	}

	sort.Strings(ids)

	for _, id := range ids {
		entry, error := translateEntry(id, document[id], options)

		if error != nil {
			return nil, error
		}

		if entry == nil {
			continue
		}

		entries = append(entries, *entry)
	}

	return entries, nil
}

// ConfigJSON renders entries as the *.client-extension-config.json payload.
func ConfigJSON(entries []Entry) ([]byte, error) {
	var payload = map[string]any{}

	for _, entry := range entries {
		payload[entry.PID] = entry.Config
	}

	return json.MarshalIndent(payload, "", "  ")
}

var nonAlphaNumericPattern = regexp.MustCompile(`[^a-zA-Z0-9]`)

// ProjectID mirrors StringUtil.toAlphaNumericLowerCase.
func ProjectID(projectName string) string {
	return strings.ToLower(nonAlphaNumericPattern.ReplaceAllString(projectName, ""))
}

func translateEntry(id string, entry map[string]any, options Options) (*Entry, error) {
	extensionType, _ := entry["type"].(string)

	if extensionType == "" {
		return nil, fmt.Errorf("client extension %q has no type", id)
	}

	var typeSettings = map[string]any{}

	for key, value := range entry {
		switch key {
		case "description", "name", "properties", "sourceCodeURL", "type":
		default:
			typeSettings[key] = value
		}
	}

	pid, hasPID := PID(extensionType)

	if extensionType == "instanceSettings" {
		scopedPID, _ := typeSettings["pid"].(string)

		if scopedPID == "" {
			return nil, fmt.Errorf("client extension %q of type instanceSettings has no pid", id)
		}

		delete(typeSettings, "pid")

		pid = scopedPID + ".scoped"
		hasPID = true
	}

	if !hasPID {
		return nil, nil
	}

	if extensionType == "globalJS" {
		if error := mapScriptElementAttributes(typeSettings); error != nil {
			return nil, fmt.Errorf("client extension %q: %w", id, error)
		}
	}

	virtualInstanceID := options.VirtualInstanceID

	if virtualInstanceID == "" {
		virtualInstanceID = "default"
	}

	webContextPath, _ := typeSettings["webContextPath"].(string)

	if webContextPath == "" {
		webContextPath = "/" + suffixIfNotBlank(options.ProjectName, "_", options.VirtualInstanceID)
	}

	baseURL, _ := typeSettings["baseURL"].(string)

	if baseURL == "" {
		baseURL = "${portalURL}/o/" + strings.TrimPrefix(webContextPath, "/")
	}

	var config = map[string]any{
		":configurator:policy":                  "force",
		"baseURL":                               baseURL,
		"buildTimestamp":                        options.BuildTimestamp,
		"description":                           stringOrEmpty(entry["description"]),
		"dxp.lxc.liferay.com.virtualInstanceId": virtualInstanceID,
		"name":                                  stringOrEmpty(entry["name"]),
		"projectId":                             ProjectID(options.ProjectName),
		"projectName":                           options.ProjectName,
		"properties":                            encode(asMap(entry["properties"])),
		"sourceCodeURL":                         stringOrEmpty(entry["sourceCodeURL"]),
		"type":                                  extensionType,
		"webContextPath":                        webContextPath,
	}

	// Non-CET configurations also carry their type settings at the top level,
	// with JSON types preserved. CET configurations only ever get the encoded
	// copy and parse the strings back themselves.
	if !strings.Contains(pid, "CETConfiguration") {
		for key, value := range typeSettings {
			config[key] = value
		}
	}

	if extensionType == "oAuthApplicationHeadlessServer" || extensionType == "oAuthApplicationUserAgent" {
		homePageURL, _ := typeSettings["homePageURL"].(string)

		if homePageURL == "" {
			homePageURL = "$[conf:.serviceScheme]://$[conf:.serviceAddress]"
		}

		config["homePageURL"] = homePageURL
	}

	config["typeSettings"] = encode(typeSettings)

	classification, _ := Classification(extensionType)

	return &Entry{
		Classification: classification,
		Config:         config,
		ExtensionType:  extensionType,
		PID:            pid + "~" + suffixIfNotBlank(id, "/", options.VirtualInstanceID),
	}, nil
}

func mapScriptElementAttributes(typeSettings map[string]any) error {
	attributes, found := typeSettings["scriptElementAttributes"]

	if !found {
		return nil
	}

	delete(typeSettings, "scriptElementAttributes")

	encoded, error := json.Marshal(attributes)

	if error != nil {
		return fmt.Errorf("unable to encode scriptElementAttributes: %w", error)
	}

	typeSettings["scriptElementAttributesJSON"] = string(encoded)

	return nil
}

// encode mirrors ClientExtension._encode: a sorted list of "key=value", with
// list values newline joined. Sorting is a deliberate departure from the Gradle
// task, whose HashMap iteration order is unstable and would otherwise churn the
// ConfigMap on every reconcile.
func encode(values map[string]any) []string {
	var keys []string

	for key := range values {
		keys = append(keys, key)
	}

	sort.Strings(keys)

	var encoded = []string{}

	for _, key := range keys {
		encoded = append(encoded, key+"="+javaString(values[key]))
	}

	return encoded
}

// javaString reproduces String.valueOf for the value shapes Jackson produces
// from YAML, because the encoded type settings are compared against payloads
// generated by the Gradle task.
func javaString(value any) string {
	switch typed := value.(type) {
	case nil:
		return "null"
	case string:
		return typed
	case bool:
		return strconv.FormatBool(typed)
	case float64:
		if typed == float64(int64(typed)) {
			return strconv.FormatInt(int64(typed), 10)
		}

		return strconv.FormatFloat(typed, 'f', -1, 64)
	case []any:
		var joined []string

		for _, element := range typed {
			joined = append(joined, javaString(element))
		}

		return strings.Join(joined, "\n")
	case map[string]any:
		var keys []string

		for key := range typed {
			keys = append(keys, key)
		}

		sort.Strings(keys)

		var pairs []string

		for _, key := range keys {
			pairs = append(pairs, key+"="+javaString(typed[key]))
		}

		return "{" + strings.Join(pairs, ", ") + "}"
	}

	return fmt.Sprintf("%v", value)
}

func asMap(value any) map[string]any {
	typed, isMap := value.(map[string]any)

	if !isMap {
		return map[string]any{}
	}

	return typed
}

func stringOrEmpty(value any) string {
	typed, isString := value.(string)

	if !isString {
		return ""
	}

	return typed
}

func suffixIfNotBlank(value string, separator string, suffix string) string {
	if strings.TrimSpace(suffix) == "" {
		return value
	}

	return value + separator + suffix
}
