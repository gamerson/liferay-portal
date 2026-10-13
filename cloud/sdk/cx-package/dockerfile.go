package main

import (
	"encoding/json"
	"fmt"
	"strings"
)

func joinContinuations(text string) []string {
	var lines []string

	var pending strings.Builder

	for _, line := range strings.Split(text, "\n") {
		line = strings.TrimSpace(line)

		if (pending.Len() == 0) && ((line == "") || strings.HasPrefix(line, "#")) {
			continue
		}

		if strings.HasSuffix(line, "\\") {
			pending.WriteString(strings.TrimSuffix(line, "\\"))
			pending.WriteString(" ")

			continue
		}

		pending.WriteString(line)

		lines = append(lines, strings.TrimSpace(pending.String()))

		pending.Reset()
	}

	if pending.Len() > 0 {
		lines = append(lines, strings.TrimSpace(pending.String()))
	}

	return lines
}

func normalizeImage(image string) string {
	if strings.Contains(image, "@") {
		return image
	}

	if index := strings.LastIndex(image, "/"); strings.Contains(image[index+1:], ":") {
		return image
	}

	return image + ":latest"
}

func parseDockerfile(text string) (*dockerfile, error) {
	var dockerfile dockerfile

	dockerfile.Env = map[string]string{}

	for _, line := range joinContinuations(text) {
		instruction, rest, _ := strings.Cut(line, " ")

		instruction = strings.ToUpper(instruction)
		rest = strings.TrimSpace(rest)

		if instruction == "COPY" {
			arguments, error := splitArguments(rest)

			if error != nil {
				return nil, error
			}

			if len(arguments) < 2 {
				return nil, fmt.Errorf("dockerfile: %q names no destination", line)
			}

			dockerfile.Copies = append(
				dockerfile.Copies,
				dockerfileCopy{
					Destination: arguments[len(arguments)-1],
					Sources:     arguments[:len(arguments)-1],
				},
			)
		} else if instruction == "ENV" {
			parseEnv(dockerfile.Env, rest)
		} else if instruction == "FROM" {
			if dockerfile.Image != "" {
				return nil, fmt.Errorf("dockerfile: multistage builds are not supported")
			}

			arguments, error := splitArguments(rest)

			if error != nil {
				return nil, error
			}

			if len(arguments) == 0 {
				return nil, fmt.Errorf("dockerfile: %q names no image", line)
			}

			dockerfile.Image = normalizeImage(arguments[0])
		} else if instruction == "RUN" {
			dockerfile.Runs = append(dockerfile.Runs, rest)
		} else {
			dockerfile.Warnings = append(
				dockerfile.Warnings,
				fmt.Sprintf("%s is not applied because the artifact is mounted, not built.", line),
			)
		}
	}

	if dockerfile.Image == "" {
		return nil, fmt.Errorf("dockerfile: no base image")
	}

	return &dockerfile, nil
}

func parseEnv(env map[string]string, rest string) {
	words := splitWords(rest)

	if (len(words) > 0) && !strings.Contains(words[0], "=") {
		env[words[0]] = strings.Join(words[1:], " ")

		return
	}

	for _, word := range words {
		key, value, _ := strings.Cut(word, "=")

		env[key] = value
	}
}

func splitArguments(rest string) ([]string, error) {
	if strings.HasPrefix(rest, "[") {
		var arguments []string

		if error := json.Unmarshal([]byte(rest), &arguments); error != nil {
			return nil, fmt.Errorf("dockerfile: parse %q: %w", rest, error)
		}

		return arguments, nil
	}

	var arguments []string

	for _, word := range splitWords(rest) {
		if !strings.HasPrefix(word, "--") {
			arguments = append(arguments, word)
		}
	}

	return arguments, nil
}

// splitWords splits on whitespace the way a shell does, keeping quoted text
// together and dropping the quotes.
func splitWords(text string) []string {
	var words []string

	var word strings.Builder

	inWord := false
	quote := rune(0)
	escaped := false

	for _, character := range text {
		if escaped {
			word.WriteRune(character)

			escaped = false

			continue
		}

		if (character == '\\') && (quote != '\'') {
			escaped = true
			inWord = true

			continue
		}

		if quote != 0 {
			if character == quote {
				quote = 0
			} else {
				word.WriteRune(character)
			}

			continue
		}

		if (character == '"') || (character == '\'') {
			quote = character
			inWord = true

			continue
		}

		if (character == ' ') || (character == '\t') {
			if inWord {
				words = append(words, word.String())

				word.Reset()

				inWord = false
			}

			continue
		}

		word.WriteRune(character)

		inWord = true
	}

	if inWord {
		words = append(words, word.String())
	}

	return words
}

type dockerfile struct {
	Copies   []dockerfileCopy
	Env      map[string]string
	Image    string
	Runs     []string
	Warnings []string
}

type dockerfileCopy struct {
	Destination string
	Sources     []string
}
