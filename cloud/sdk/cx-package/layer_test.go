package main

import (
	"archive/tar"
	"bytes"
	"reflect"
	"strings"
	"testing"
)

func TestBuildLayerIsDeterministic(t *testing.T) {
	files := map[string]*file{
		"app.jar":         {Data: []byte("jar"), Mode: 0o644, Name: "app.jar"},
		"static/a.css":    {Data: []byte("a"), Mode: 0o644, Name: "static/a.css"},
		"node_modules/.b": {LinkTarget: "../x", Name: "node_modules/.b"},
	}

	hardLinks := []hardLink{{Name: ".cx-mounts/0/jar-runner.jar", Target: "app.jar"}}

	first, error := buildLayer(files, hardLinks)

	if error != nil {
		t.Fatal(error)
	}

	second, error := buildLayer(files, hardLinks)

	if error != nil {
		t.Fatal(error)
	}

	if !bytes.Equal(first, second) {
		t.Fatal("Expected the same files to produce the same layer")
	}

	entries := map[string]byte{}

	tarReader := tar.NewReader(bytes.NewReader(first))

	for {
		header, error := tarReader.Next()

		if error != nil {
			break
		}

		entries[strings.TrimSuffix(header.Name, "/")] = header.Typeflag
	}

	wantEntries := map[string]byte{
		".cx-mounts":                  tar.TypeDir,
		".cx-mounts/0":                tar.TypeDir,
		".cx-mounts/0/jar-runner.jar": tar.TypeLink,
		"app.jar":                     tar.TypeReg,
		"node_modules":                tar.TypeDir,
		"node_modules/.b":             tar.TypeSymlink,
		"static":                      tar.TypeDir,
		"static/a.css":                tar.TypeReg,
	}

	if !reflect.DeepEqual(entries, wantEntries) {
		t.Errorf("entries = %v, want %v", entries, wantEntries)
	}
}

func TestPlanMounts(t *testing.T) {
	files := map[string]*file{
		"app.jar":      {Name: "app.jar"},
		"batch":        {Directory: true, Name: "batch"},
		"batch/a.json": {Name: "batch/a.json"},
		"static/a.css": {Name: "static/a.css"},
	}

	testCases := map[string]struct {
		copies        []dockerfileCopy
		wantError     string
		wantHardLinks []hardLink
		wantMounts    []mount
	}{
		"a directory is mounted as is": {
			copies:     []dockerfileCopy{{Destination: "/public_html/", Sources: []string{"static"}}},
			wantMounts: []mount{{MountPath: "/public_html", SubPath: "static"}},
		},
		"a file gets a directory of its own": {
			copies:        []dockerfileCopy{{Destination: "/opt/liferay/jar-runner.jar", Sources: []string{"*.jar"}}},
			wantHardLinks: []hardLink{{Name: ".cx-mounts/0/jar-runner.jar", Target: "app.jar"}},
			wantMounts:    []mount{{MountPath: "/opt/liferay", SubPath: ".cx-mounts/0"}},
		},
		"a source that matches nothing is refused": {
			copies:    []dockerfileCopy{{Destination: "/opt/", Sources: []string{"*.war"}}},
			wantError: "matches nothing",
		},
		"the whole zip is mounted at its destination": {
			copies:     []dockerfileCopy{{Destination: "/opt/liferay/", Sources: []string{"."}}},
			wantMounts: []mount{{MountPath: "/opt/liferay"}},
		},
	}

	for name, testCase := range testCases {
		t.Run(name, func(t *testing.T) {
			hardLinks, mounts, error := planMounts(testCase.copies, files)

			if testCase.wantError != "" {
				if (error == nil) || !strings.Contains(error.Error(), testCase.wantError) {
					t.Fatalf("planMounts() error = %v, want %q", error, testCase.wantError)
				}

				return
			}

			if error != nil {
				t.Fatalf("planMounts() error = %v, want nil", error)
			}

			if !reflect.DeepEqual(hardLinks, testCase.wantHardLinks) {
				t.Errorf("hardLinks = %+v, want %+v", hardLinks, testCase.wantHardLinks)
			}

			if !reflect.DeepEqual(mounts, testCase.wantMounts) {
				t.Errorf("mounts = %+v, want %+v", mounts, testCase.wantMounts)
			}
		})
	}
}
