package main

import (
	"archive/tar"
	"bytes"
	"compress/gzip"
	"fmt"
	"path"
	"slices"
	"strings"
	"time"
)

const mountsDirectory = ".cx-mounts"

// buildLayer writes every file at its own path, plus the hard links that give
// each file destination a directory of its own to mount. Timestamps and owners
// are fixed, so the same files always produce the same bytes.
func buildLayer(files map[string]*file, hardLinks []hardLink) ([]byte, error) {
	directories := map[string]bool{}

	for name, file := range files {
		if file.Directory {
			directories[name] = true
		}

		for _, parentDirectory := range parentDirectories(name) {
			directories[parentDirectory] = true
		}
	}

	for _, hardLink := range hardLinks {
		for _, parentDirectory := range parentDirectories(hardLink.Name) {
			directories[parentDirectory] = true
		}
	}

	var buffer bytes.Buffer

	tarWriter := tar.NewWriter(&buffer)

	for _, directory := range sortedKeys(directories) {
		if error := tarWriter.WriteHeader(
			newHeader(0o755, directory+"/", tar.TypeDir),
		); error != nil {
			return nil, error
		}
	}

	for _, name := range sortedNames(files) {
		file := files[name]

		if file.Directory {
			continue
		}

		if file.LinkTarget != "" {
			header := newHeader(0o777, name, tar.TypeSymlink)

			header.Linkname = file.LinkTarget

			if error := tarWriter.WriteHeader(header); error != nil {
				return nil, error
			}

			continue
		}

		header := newHeader(file.Mode, name, tar.TypeReg)

		header.Size = int64(len(file.Data))

		if error := tarWriter.WriteHeader(header); error != nil {
			return nil, error
		}

		if _, error := tarWriter.Write(file.Data); error != nil {
			return nil, error
		}
	}

	for _, hardLink := range hardLinks {
		header := newHeader(0o644, hardLink.Name, tar.TypeLink)

		header.Linkname = hardLink.Target

		if error := tarWriter.WriteHeader(header); error != nil {
			return nil, error
		}
	}

	if error := tarWriter.Close(); error != nil {
		return nil, error
	}

	return buffer.Bytes(), nil
}

func cleanMountPath(destination string) string {
	if destination = strings.TrimSuffix(destination, "/"); destination == "" {
		return "/"
	}

	return destination
}

func gzipLayer(layer []byte) ([]byte, error) {
	var buffer bytes.Buffer

	gzipWriter := gzip.NewWriter(&buffer)

	if _, error := gzipWriter.Write(layer); error != nil {
		return nil, error
	}

	if error := gzipWriter.Close(); error != nil {
		return nil, error
	}

	return buffer.Bytes(), nil
}

func newHeader(mode int64, name string, typeflag byte) *tar.Header {
	return &tar.Header{
		Format:   tar.FormatPAX,
		Mode:     mode,
		ModTime:  time.Unix(0, 0),
		Name:     name,
		Typeflag: typeflag,
	}
}

// planMounts turns the Dockerfile's COPY lines into image volume mounts. A
// directory source is mounted as is. An image volume cannot mount a single
// file, so every file destination gets a directory of its own under
// .cx-mounts, holding the file under its destination name as a hard link, and
// that directory is mounted over the destination's parent.
func planMounts(copies []dockerfileCopy, files map[string]*file) ([]hardLink, []mount, error) {
	var mounts []mount

	fileDestinations := map[string][]hardLink{}

	for _, dockerfileCopy := range copies {
		for _, source := range dockerfileCopy.Sources {
			source = strings.Trim(source, "/")

			if (source == ".") || (source == "./") {
				source = ""
			}

			if isDirectoryIn(files, source) {
				mounts = append(
					mounts,
					mount{
						MountPath: cleanMountPath(dockerfileCopy.Destination),
						SubPath:   source,
					},
				)

				continue
			}

			var matches []string

			for _, name := range sortedNames(files) {
				if matched, _ := path.Match(source, name); matched && !files[name].Directory {
					matches = append(matches, name)
				}
			}

			if len(matches) == 0 {
				return nil, nil, fmt.Errorf("dockerfile: COPY %s matches nothing in the zip", source)
			}

			for _, match := range matches {
				target := dockerfileCopy.Destination

				if strings.HasSuffix(target, "/") {
					target = path.Join(target, path.Base(match))
				}

				fileDestinations[path.Dir(target)] = append(
					fileDestinations[path.Dir(target)],
					hardLink{Name: path.Base(target), Target: match},
				)
			}
		}
	}

	var hardLinks []hardLink

	for index, directory := range sortedKeys(fileDestinations) {
		mountDirectory := fmt.Sprintf("%s/%d", mountsDirectory, index)

		for _, fileDestination := range fileDestinations[directory] {
			hardLinks = append(
				hardLinks,
				hardLink{Name: mountDirectory + "/" + fileDestination.Name, Target: fileDestination.Target},
			)
		}

		mounts = append(mounts, mount{MountPath: directory, SubPath: mountDirectory})
	}

	return hardLinks, mounts, nil
}

func sortedKeys[V any](values map[string]V) []string {
	keys := make([]string, 0, len(values))

	for key := range values {
		keys = append(keys, key)
	}

	slices.Sort(keys)

	return keys
}

func sortedNames(files map[string]*file) []string {
	return sortedKeys(files)
}

type hardLink struct {
	Name   string
	Target string
}

type mount struct {
	MountPath string `json:"mountPath"`
	SubPath   string `json:"subPath,omitempty"`
}
