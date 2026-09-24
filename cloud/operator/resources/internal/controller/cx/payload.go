package cx

import (
	"crypto/sha256"
	"encoding/hex"
	"encoding/json"
	"fmt"
	"sort"
	"strings"

	cxv1alpha1 "github.com/liferay/liferay-portal/cloud/operator/api/cx/v1alpha1"
	cxconfig "github.com/liferay/liferay-portal/cloud/operator/internal/cxconfig"
)

// BuildPayload translates the client extension document into the single
// configuration payload Liferay is given.
func BuildPayload(clientExtension *cxv1alpha1.ClientExtension) (string, error) {
	if len(clientExtension.Spec.Configs) > 0 {
		return mergeConfigs(clientExtension.Spec.Configs)
	}

	if clientExtension.Spec.ClientExtensionYAML == "" {
		return "", nil
	}

	document, error := cxconfig.Parse([]byte(clientExtension.Spec.ClientExtensionYAML))

	if error != nil {
		return "", error
	}

	entries, error := cxconfig.Translate(document, cxconfig.Options{
		BuildTimestamp:    BuildTimestamp(clientExtension),
		ProjectName:       ProjectName(clientExtension),
		VirtualInstanceID: "",
	})

	if error != nil {
		return "", error
	}

	encoded, error := cxconfig.ConfigJSON(entries)

	if error != nil {
		return "", error
	}

	return string(encoded), nil
}

// RequiresOAuth reports whether a client extension declares an OAuth2
// application. Liferay writes ext-init credentials only for those, so a client
// extension without one has nothing to wait for.
func RequiresOAuth(clientExtension *cxv1alpha1.ClientExtension) (bool, error) {
	if len(clientExtension.Spec.Configs) > 0 {
		for _, config := range clientExtension.Spec.Configs {
			if strings.Contains(config, "oAuthApplication") {
				return true, nil
			}
		}

		return false, nil
	}

	if clientExtension.Spec.ClientExtensionYAML == "" {
		return false, nil
	}

	document, error := cxconfig.Parse([]byte(clientExtension.Spec.ClientExtensionYAML))

	if error != nil {
		return false, error
	}

	for _, entry := range document {
		extensionType, _ := entry["type"].(string)

		if strings.HasPrefix(extensionType, "oAuthApplication") {
			return true, nil
		}
	}

	return false, nil
}

// BuildTimestamp is derived from the spec generation rather than the wall
// clock. It becomes the client extension type's modifiedDate, so a value that
// changed every reconcile would rewrite every configuration in Liferay.
func BuildTimestamp(clientExtension *cxv1alpha1.ClientExtension) int64 {
	return clientExtension.Generation
}

// Checksum fingerprints a payload so a ConfigMap is only rewritten when its
// content actually changed.
func Checksum(values map[string]string) string {
	var keys []string

	for key := range values {
		keys = append(keys, key)
	}

	sort.Strings(keys)

	digest := sha256.New()

	for _, key := range keys {
		digest.Write([]byte(key))
		digest.Write([]byte(values[key]))
	}

	return hex.EncodeToString(digest.Sum(nil))[:16]
}

func mergeConfigs(configs []string) (string, error) {
	var merged = map[string]any{}

	for _, config := range configs {
		var parsed map[string]any

		if error := json.Unmarshal([]byte(config), &parsed); error != nil {
			return "", fmt.Errorf("config payload parse error: %w", error)
		}

		for key, value := range parsed {
			merged[key] = value
		}
	}

	encoded, error := json.MarshalIndent(merged, "", "  ")

	if error != nil {
		return "", error
	}

	return string(encoded), nil
}
