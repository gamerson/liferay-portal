package cx

import (
	"crypto/sha256"
	"encoding/hex"
	"encoding/json"
	"fmt"
	"sort"

	cxv1alpha1 "github.com/liferay/liferay-portal/cloud/operator/api/cx/v1alpha1"
	cxconfig "github.com/liferay/liferay-portal/cloud/operator/internal/cxconfig"
)

// Payload is the configuration split across addressing buckets. Frontend
// entries are fetched by a browser and carry the public domain; microservice
// and configuration entries are called by Liferay and carry the internal
// address. They must travel in separate ConfigMaps because the mainDomain
// annotation that drives baseURL and .serviceAddress applies to the whole
// ConfigMap.
type Payload struct {
	Buckets map[string]string
}

// BuildPayload translates the client extension document into one JSON document
// per addressing bucket.
func BuildPayload(clientExtension *cxv1alpha1.ClientExtension) (*Payload, error) {
	var payload = &Payload{Buckets: map[string]string{}}

	if len(clientExtension.Spec.Configs) > 0 {
		merged, error := mergeConfigs(clientExtension.Spec.Configs)

		if error != nil {
			return nil, error
		}

		payload.Buckets[BucketPublic] = merged

		return payload, nil
	}

	if clientExtension.Spec.ClientExtensionYAML == "" {
		return payload, nil
	}

	document, error := cxconfig.Parse([]byte(clientExtension.Spec.ClientExtensionYAML))

	if error != nil {
		return nil, error
	}

	entries, error := cxconfig.Translate(document, cxconfig.Options{
		BuildTimestamp:    BuildTimestamp(clientExtension),
		ProjectName:       ProjectName(clientExtension),
		VirtualInstanceID: "",
	})

	if error != nil {
		return nil, error
	}

	var bucketed = map[string][]cxconfig.Entry{}

	for _, entry := range entries {
		bucketed[bucketOf(entry, clientExtension)] = append(
			bucketed[bucketOf(entry, clientExtension)], entry,
		)
	}

	for bucket, bucketEntries := range bucketed {
		encoded, error := cxconfig.ConfigJSON(bucketEntries)

		if error != nil {
			return nil, error
		}

		payload.Buckets[bucket] = string(encoded)
	}

	return payload, nil
}

// BuildTimestamp is derived from the spec generation rather than the wall
// clock. It becomes the client extension type's modifiedDate, so a value that
// changed every reconcile would rewrite every configuration in Liferay.
func BuildTimestamp(clientExtension *cxv1alpha1.ClientExtension) int64 {
	return clientExtension.Generation
}

// bucketOf routes an entry to an addressing bucket. Without an internal
// address configured everything travels in the public bucket, which is the
// behavior of the existing Helm chart.
func bucketOf(entry cxconfig.Entry, clientExtension *cxv1alpha1.ClientExtension) string {
	if clientExtension.Spec.Domains.Internal == "" {
		return BucketPublic
	}

	if entry.Classification == cxconfig.ClassificationFrontend {
		return BucketPublic
	}

	return BucketInternal
}

// DomainFor returns the mainDomain annotation value for a bucket.
func DomainFor(clientExtension *cxv1alpha1.ClientExtension, bucket string) string {
	if bucket == BucketInternal {
		return clientExtension.Spec.Domains.Internal
	}

	return clientExtension.Spec.Domains.Public
}

// InternalAddress resolves the cluster-local address for a client extension,
// expanding the "auto" shorthand from the referenced Service.
func InternalAddress(clientExtension *cxv1alpha1.ClientExtension) string {
	internal := clientExtension.Spec.Domains.Internal

	if internal != "auto" {
		return internal
	}

	serviceRef := clientExtension.Spec.ServiceRef

	if serviceRef == nil {
		return ""
	}

	port := serviceRef.Port

	if port == 0 {
		port = 80
	}

	return fmt.Sprintf(
		"%s.%s.svc.cluster.local:%d", serviceRef.Name, clientExtension.Namespace, port,
	)
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
