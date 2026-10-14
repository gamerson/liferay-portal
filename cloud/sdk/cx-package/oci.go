package main

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"

	digest "github.com/opencontainers/go-digest"
	specs "github.com/opencontainers/image-spec/specs-go"
	ocispec "github.com/opencontainers/image-spec/specs-go/v1"
	oras "oras.land/oras-go/v2"
	content "oras.land/oras-go/v2/content"
	oci "oras.land/oras-go/v2/content/oci"
	remote "oras.land/oras-go/v2/registry/remote"
	auth "oras.land/oras-go/v2/registry/remote/auth"
	credentials "oras.land/oras-go/v2/registry/remote/credentials"
	retry "oras.land/oras-go/v2/registry/remote/retry"
)

const (
	mediaTypeHelmChart  = "application/vnd.cncf.helm.chart.content.v1.tar+gzip"
	mediaTypeHelmConfig = "application/vnd.cncf.helm.config.v1+json"
)

func copyToRegistry(
	context context.Context,
	layoutDirectory string,
	plainHTTP bool,
	reference string,
	tag string,
) (ocispec.Descriptor, error) {
	store, error := oci.New(layoutDirectory)

	if error != nil {
		return ocispec.Descriptor{}, error
	}

	repository, error := remote.NewRepository(reference)

	if error != nil {
		return ocispec.Descriptor{}, error
	}

	credentialStore, error := credentials.NewStoreFromDocker(credentials.StoreOptions{})

	if error != nil {
		return ocispec.Descriptor{}, error
	}

	repository.Client = &auth.Client{
		Cache:      auth.NewCache(),
		Client:     retry.DefaultClient,
		Credential: credentials.Credential(credentialStore),
	}
	repository.PlainHTTP = plainHTTP

	descriptor, error := oras.Copy(context, store, tag, repository, tag, oras.DefaultCopyOptions)

	if error != nil {
		return ocispec.Descriptor{}, fmt.Errorf("push %s:%s: %w", reference, tag, error)
	}

	return descriptor, nil
}

func pushBlob(
	blob []byte,
	context context.Context,
	mediaType string,
	store *oci.Store,
) (ocispec.Descriptor, error) {
	descriptor := content.NewDescriptorFromBytes(mediaType, blob)

	exists, error := store.Exists(context, descriptor)

	if error != nil {
		return ocispec.Descriptor{}, error
	}

	if exists {
		return descriptor, nil
	}

	return descriptor, store.Push(context, descriptor, bytes.NewReader(blob))
}

func pushJSON(
	context context.Context,
	mediaType string,
	value any,
	store *oci.Store,
) (ocispec.Descriptor, error) {
	data, error := json.Marshal(value)

	if error != nil {
		return ocispec.Descriptor{}, error
	}

	return pushBlob(data, context, mediaType, store)
}

// writeArtifact stores the layer as an image index with one manifest per
// platform. The layer holds only files, so every platform shares it; an index
// keeps a node of any architecture from refusing the pull.
func writeArtifact(
	context context.Context,
	layer []byte,
	layoutDirectory string,
	tag string,
) (ocispec.Descriptor, error) {
	store, error := oci.New(layoutDirectory)

	if error != nil {
		return ocispec.Descriptor{}, error
	}

	compressedLayer, error := gzipLayer(layer)

	if error != nil {
		return ocispec.Descriptor{}, error
	}

	layerDescriptor, error := pushBlob(compressedLayer, context, ocispec.MediaTypeImageLayerGzip, store)

	if error != nil {
		return ocispec.Descriptor{}, error
	}

	var manifestDescriptors []ocispec.Descriptor

	for _, architecture := range []string{"amd64", "arm64"} {
		configJSON, error := json.Marshal(
			ocispec.Image{
				Platform: ocispec.Platform{Architecture: architecture, OS: "linux"},
				RootFS: ocispec.RootFS{
					DiffIDs: []digest.Digest{digest.FromBytes(layer)},
					Type:    "layers",
				},
			},
		)

		if error != nil {
			return ocispec.Descriptor{}, error
		}

		configDescriptor, error := pushBlob(configJSON, context, ocispec.MediaTypeImageConfig, store)

		if error != nil {
			return ocispec.Descriptor{}, error
		}

		manifestDescriptor, error := pushJSON(
			context, ocispec.MediaTypeImageManifest,
			ocispec.Manifest{
				Config:    configDescriptor,
				Layers:    []ocispec.Descriptor{layerDescriptor},
				MediaType: ocispec.MediaTypeImageManifest,
				Versioned: specs.Versioned{SchemaVersion: 2},
			},
			store,
		)

		if error != nil {
			return ocispec.Descriptor{}, error
		}

		manifestDescriptor.Platform = &ocispec.Platform{Architecture: architecture, OS: "linux"}

		manifestDescriptors = append(manifestDescriptors, manifestDescriptor)
	}

	indexDescriptor, error := pushJSON(
		context, ocispec.MediaTypeImageIndex,
		ocispec.Index{
			Manifests: manifestDescriptors,
			MediaType: ocispec.MediaTypeImageIndex,
			Versioned: specs.Versioned{SchemaVersion: 2},
		},
		store,
	)

	if error != nil {
		return ocispec.Descriptor{}, error
	}

	return indexDescriptor, store.Tag(context, indexDescriptor, tag)
}

// writeChart stores a packaged chart the way helm push does, so helm pull and
// helm install read it back from oci:// unchanged.
func writeChart(
	chartArchive []byte,
	chartMetadata map[string]any,
	context context.Context,
	layoutDirectory string,
	tag string,
) (ocispec.Descriptor, error) {
	store, error := oci.New(layoutDirectory)

	if error != nil {
		return ocispec.Descriptor{}, error
	}

	configJSON, error := json.Marshal(chartMetadata)

	if error != nil {
		return ocispec.Descriptor{}, error
	}

	configDescriptor, error := pushBlob(configJSON, context, mediaTypeHelmConfig, store)

	if error != nil {
		return ocispec.Descriptor{}, error
	}

	layerDescriptor, error := pushBlob(chartArchive, context, mediaTypeHelmChart, store)

	if error != nil {
		return ocispec.Descriptor{}, error
	}

	manifestDescriptor, error := pushJSON(
		context, ocispec.MediaTypeImageManifest,
		ocispec.Manifest{
			Config:    configDescriptor,
			Layers:    []ocispec.Descriptor{layerDescriptor},
			MediaType: ocispec.MediaTypeImageManifest,
			Versioned: specs.Versioned{SchemaVersion: 2},
		},
		store,
	)

	if error != nil {
		return ocispec.Descriptor{}, error
	}

	return manifestDescriptor, store.Tag(context, manifestDescriptor, tag)
}
