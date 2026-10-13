package main

import (
	"reflect"
	"strings"
	"testing"
)

func TestParseDockerfile(t *testing.T) {
	testCases := map[string]struct {
		text         string
		wantCopies   []dockerfileCopy
		wantEnv      map[string]string
		wantError    string
		wantImage    string
		wantRuns     []string
		wantWarnings int
	}{
		"a base image with a digest keeps it": {
			text:      "FROM liferay/caddy@sha256:abc",
			wantEnv:   map[string]string{},
			wantImage: "liferay/caddy@sha256:abc",
		},
		"a base image with no tag gets latest": {
			text:       "# comment\nFROM liferay/jar-runner\nCOPY *.jar /opt/liferay/jar-runner.jar",
			wantCopies: []dockerfileCopy{{Destination: "/opt/liferay/jar-runner.jar", Sources: []string{"*.jar"}}},
			wantEnv:    map[string]string{},
			wantImage:  "liferay/jar-runner:latest",
		},
		"a registry port is not a tag": {
			text:      "FROM registry.example.com:5000/liferay/caddy",
			wantEnv:   map[string]string{},
			wantImage: "registry.example.com:5000/liferay/caddy:latest",
		},
		"a second FROM is refused": {
			text:      "FROM liferay/node-runner AS build\nFROM liferay/caddy",
			wantError: "multistage",
		},
		"an instruction a mount cannot apply is a warning": {
			text:         "FROM liferay/caddy\nWORKDIR /srv",
			wantEnv:      map[string]string{},
			wantImage:    "liferay/caddy:latest",
			wantWarnings: 1,
		},
		"both ENV forms and the JSON COPY form": {
			text: "FROM liferay/node-runner:1\nENV A=1 B=\"two words\"\nENV C three\n" +
				"COPY [\"./\", \"/opt/liferay/\"]\nRUN npm install \\\n\t--omit=dev",
			wantCopies: []dockerfileCopy{{Destination: "/opt/liferay/", Sources: []string{"./"}}},
			wantEnv:    map[string]string{"A": "1", "B": "two words", "C": "three"},
			wantImage:  "liferay/node-runner:1",
			wantRuns:   []string{"npm install  --omit=dev"},
		},
		"no FROM is refused": {
			text:      "COPY . /srv",
			wantError: "no base image",
		},
	}

	for name, testCase := range testCases {
		t.Run(name, func(t *testing.T) {
			dockerfile, error := parseDockerfile(testCase.text)

			if testCase.wantError != "" {
				if (error == nil) || !strings.Contains(error.Error(), testCase.wantError) {
					t.Fatalf("parseDockerfile() error = %v, want %q", error, testCase.wantError)
				}

				return
			}

			if error != nil {
				t.Fatalf("parseDockerfile() error = %v, want nil", error)
			}

			if !reflect.DeepEqual(dockerfile.Copies, testCase.wantCopies) {
				t.Errorf("Copies = %+v, want %+v", dockerfile.Copies, testCase.wantCopies)
			}

			if !reflect.DeepEqual(dockerfile.Env, testCase.wantEnv) {
				t.Errorf("Env = %v, want %v", dockerfile.Env, testCase.wantEnv)
			}

			if dockerfile.Image != testCase.wantImage {
				t.Errorf("Image = %q, want %q", dockerfile.Image, testCase.wantImage)
			}

			if !reflect.DeepEqual(dockerfile.Runs, testCase.wantRuns) {
				t.Errorf("Runs = %q, want %q", dockerfile.Runs, testCase.wantRuns)
			}

			if len(dockerfile.Warnings) != testCase.wantWarnings {
				t.Errorf("Warnings = %q, want %d", dockerfile.Warnings, testCase.wantWarnings)
			}
		})
	}
}
