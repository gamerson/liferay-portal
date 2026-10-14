package main

import (
	"bytes"
	"context"
	"fmt"
	"os"
	"os/exec"
	"slices"
	"strings"
)

// replayRuns runs the Dockerfile's RUN steps inside the zip's own base image
// against the unpacked zip, and returns the tree they leave. An image build ran
// them, and mounting cannot: the node sample's zip carries one of its six
// dependencies, and "npm install" is where the rest come from. Running them in
// the base image installs for the exact runtime the pod uses.
func replayRuns(
	containerRuntime string,
	context context.Context,
	dockerfile *dockerfile,
	files map[string]*file,
) (map[string]*file, error) {
	root := dockerfile.Copies[0].Destination

	if root = strings.TrimSuffix(root, "/"); root == "" {
		root = "/"
	}

	if _, error := exec.LookPath(containerRuntime); error != nil {
		return nil, fmt.Errorf(
			"replay: the Dockerfile has RUN steps, which need %s to replay: %w", containerRuntime, error,
		)
	}

	treeDirectory, error := os.MkdirTemp("", "cx-package-replay-")

	if error != nil {
		return nil, error
	}

	defer os.RemoveAll(treeDirectory)

	if error := writeDirectory(files, treeDirectory); error != nil {
		return nil, error
	}

	var output bytes.Buffer

	command := exec.CommandContext(
		context, containerRuntime, "run",
		"--entrypoint", "sh",
		"--env", "HOME=/tmp",
		"--rm",
		"--user", fmt.Sprintf("%d:%d", os.Getuid(), os.Getgid()),
		"--volume", treeDirectory+":"+root,
		"--workdir", root,
		dockerfile.Image, "-c", strings.Join(dockerfile.Runs, " && "),
	)

	command.Stderr = &output
	command.Stdout = &output

	if error := command.Run(); error != nil {
		return nil, fmt.Errorf("replay: %s in %s: %w\n%s", strings.Join(dockerfile.Runs, " && "), dockerfile.Image, error, output.String())
	}

	return readDirectory(treeDirectory)
}

// replayable reports whether the RUN steps can be replayed: only when the zip
// is copied whole to one destination, so a step sees exactly the tree the
// image build would have.
func replayable(dockerfile *dockerfile) bool {
	return (len(dockerfile.Runs) > 0) && (len(dockerfile.Copies) == 1) &&
		slices.ContainsFunc(
			[]string{".", "./"},
			func(source string) bool {
				return slices.Equal(dockerfile.Copies[0].Sources, []string{source})
			},
		)
}
