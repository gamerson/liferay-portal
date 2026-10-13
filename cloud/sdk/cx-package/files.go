package main

import (
	"archive/zip"
	"fmt"
	"io"
	"io/fs"
	"os"
	"path"
	"path/filepath"
	"strings"
)

func isDirectoryIn(files map[string]*file, name string) bool {
	if name == "" {
		return true
	}

	if file, ok := files[name]; ok {
		return file.Directory
	}

	for fileName := range files {
		if strings.HasPrefix(fileName, name+"/") {
			return true
		}
	}

	return false
}

func parentDirectories(name string) []string {
	var parentDirectories []string

	for parent := path.Dir(name); parent != "."; parent = path.Dir(parent) {
		parentDirectories = append(parentDirectories, parent)
	}

	return parentDirectories
}

func readDirectory(root string) (map[string]*file, error) {
	files := map[string]*file{}

	error := filepath.WalkDir(
		root,
		func(filePath string, directoryEntry fs.DirEntry, walkError error) error {
			if walkError != nil {
				return walkError
			}

			relativePath, error := filepath.Rel(root, filePath)

			if error != nil {
				return error
			}

			if relativePath == "." {
				return nil
			}

			name := filepath.ToSlash(relativePath)

			fileInfo, error := directoryEntry.Info()

			if error != nil {
				return error
			}

			if directoryEntry.IsDir() {
				files[name] = &file{Directory: true, Mode: int64(fileInfo.Mode().Perm()), Name: name}

				return nil
			}

			if fileInfo.Mode()&fs.ModeSymlink != 0 {
				linkTarget, error := os.Readlink(filePath)

				if error != nil {
					return error
				}

				files[name] = &file{LinkTarget: linkTarget, Mode: 0o777, Name: name}

				return nil
			}

			data, error := os.ReadFile(filePath)

			if error != nil {
				return error
			}

			files[name] = &file{Data: data, Mode: int64(fileInfo.Mode().Perm()), Name: name}

			return nil
		},
	)

	return files, error
}

func readZip(zipPath string) (map[string]*file, error) {
	zipReader, error := zip.OpenReader(zipPath)

	if error != nil {
		return nil, error
	}

	defer zipReader.Close()

	files := map[string]*file{}

	for _, zipFile := range zipReader.File {
		name := strings.TrimSuffix(zipFile.Name, "/")

		if !fs.ValidPath(name) {
			return nil, fmt.Errorf("zip: %q is not a valid path", zipFile.Name)
		}

		fileInfo := zipFile.FileInfo()

		if fileInfo.IsDir() {
			files[name] = &file{Directory: true, Mode: 0o755, Name: name}

			continue
		}

		reader, error := zipFile.Open()

		if error != nil {
			return nil, error
		}

		data, error := io.ReadAll(reader)

		reader.Close()

		if error != nil {
			return nil, error
		}

		if fileInfo.Mode()&fs.ModeSymlink != 0 {
			files[name] = &file{LinkTarget: string(data), Mode: 0o777, Name: name}

			continue
		}

		mode := int64(fileInfo.Mode().Perm())

		if mode == 0 {
			mode = 0o644
		}

		files[name] = &file{Data: data, Mode: mode, Name: name}
	}

	return files, nil
}

func writeDirectory(files map[string]*file, root string) error {
	for _, name := range sortedNames(files) {
		file := files[name]

		filePath := filepath.Join(root, filepath.FromSlash(name))

		if error := os.MkdirAll(filepath.Dir(filePath), 0o755); error != nil {
			return error
		}

		if file.Directory {
			if error := os.MkdirAll(filePath, 0o755); error != nil {
				return error
			}

			continue
		}

		if file.LinkTarget != "" {
			if error := os.Symlink(file.LinkTarget, filePath); error != nil {
				return error
			}

			continue
		}

		if error := os.WriteFile(filePath, file.Data, fs.FileMode(file.Mode)|0o200); error != nil {
			return error
		}
	}

	return nil
}

type file struct {
	Data       []byte
	Directory  bool
	LinkTarget string
	Mode       int64
	Name       string
}
