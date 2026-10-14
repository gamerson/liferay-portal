/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.gradle.plugins.workspace.internal.client.extension.oci;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.gradle.api.GradleException;

/**
 * Reads the Dockerfile a client extension build writes instead of running it.
 * It is never more than a FROM, a COPY and at most a RUN: a stock Liferay base
 * image plus the client extension's files copied to where that image expects
 * them.
 *
 * @author Gregory Amerson
 */
public class ClientExtensionDockerfile {

	public static ClientExtensionDockerfile parse(String text) {
		ClientExtensionDockerfile clientExtensionDockerfile =
			new ClientExtensionDockerfile();

		for (String line : _joinContinuations(text)) {
			int index = line.indexOf(' ');

			String instruction = line;
			String rest = "";

			if (index > 0) {
				instruction = line.substring(0, index);
				rest = line.substring(index + 1);
			}

			instruction = instruction.toUpperCase(Locale.ROOT);
			rest = rest.trim();

			if (instruction.equals("COPY")) {
				List<String> arguments = _splitArguments(rest);

				if (arguments.size() < 2) {
					throw new GradleException(
						"Dockerfile instruction \"" + line +
							"\" names no destination");
				}

				clientExtensionDockerfile._copies.add(
					new Copy(
						arguments.get(arguments.size() - 1),
						arguments.subList(0, arguments.size() - 1)));
			}
			else if (instruction.equals("ENV")) {
				_parseEnv(clientExtensionDockerfile._env, rest);
			}
			else if (instruction.equals("FROM")) {
				if (clientExtensionDockerfile._image != null) {
					throw new GradleException(
						"Multistage Dockerfiles are not supported");
				}

				List<String> arguments = _splitArguments(rest);

				if (arguments.isEmpty()) {
					throw new GradleException(
						"Dockerfile instruction \"" + line +
							"\" names no image");
				}

				clientExtensionDockerfile._image = _normalizeImage(
					arguments.get(0));
			}
			else if (instruction.equals("RUN")) {
				clientExtensionDockerfile._runs.add(rest);
			}
			else {
				clientExtensionDockerfile._warnings.add(
					line + " is not applied because the artifact is mounted, " +
						"not built.");
			}
		}

		if (clientExtensionDockerfile._image == null) {
			throw new GradleException("Dockerfile names no base image");
		}

		return clientExtensionDockerfile;
	}

	public List<Copy> getCopies() {
		return Collections.unmodifiableList(_copies);
	}

	public Map<String, String> getEnv() {
		return Collections.unmodifiableMap(_env);
	}

	public String getImage() {
		return _image;
	}

	public List<String> getRuns() {
		return Collections.unmodifiableList(_runs);
	}

	public List<String> getWarnings() {
		return Collections.unmodifiableList(_warnings);
	}

	/**
	 * Returns whether the RUN steps can be replayed: only when the zip is
	 * copied whole to one destination, so a step sees exactly the tree the
	 * image build would have.
	 */
	public boolean isReplayable() {
		if (_runs.isEmpty() || (_copies.size() != 1)) {
			return false;
		}

		Copy copy = _copies.get(0);

		List<String> sources = copy.getSources();

		if (sources.size() != 1) {
			return false;
		}

		String source = sources.get(0);

		if (source.equals(".") || source.equals("./")) {
			return true;
		}

		return false;
	}

	public static class Copy {

		public Copy(String destination, List<String> sources) {
			_destination = destination;
			_sources = new ArrayList<>(sources);
		}

		public String getDestination() {
			return _destination;
		}

		public List<String> getSources() {
			return Collections.unmodifiableList(_sources);
		}

		private final String _destination;
		private final List<String> _sources;

	}

	private static List<String> _joinContinuations(String text) {
		List<String> lines = new ArrayList<>();

		StringBuilder sb = new StringBuilder();

		for (String line : text.split("\n")) {
			line = line.trim();

			if ((sb.length() == 0) &&
				(line.isEmpty() || line.startsWith("#"))) {

				continue;
			}

			if (line.endsWith("\\")) {
				sb.append(line.substring(0, line.length() - 1));
				sb.append(' ');

				continue;
			}

			sb.append(line);

			String joinedLine = sb.toString();

			lines.add(joinedLine.trim());

			sb.setLength(0);
		}

		if (sb.length() > 0) {
			String joinedLine = sb.toString();

			lines.add(joinedLine.trim());
		}

		return lines;
	}

	private static String _normalizeImage(String image) {
		if (image.contains("@")) {
			return image;
		}

		int index = image.lastIndexOf('/');

		String name = image.substring(index + 1);

		if (name.contains(":")) {
			return image;
		}

		return image + ":latest";
	}

	private static void _parseEnv(Map<String, String> env, String rest) {
		List<String> words = _splitWords(rest);

		if (words.isEmpty()) {
			return;
		}

		String firstWord = words.get(0);

		if (!firstWord.contains("=")) {
			env.put(
				firstWord, String.join(" ", words.subList(1, words.size())));

			return;
		}

		for (String word : words) {
			int index = word.indexOf('=');

			if (index < 0) {
				env.put(word, "");
			}
			else {
				env.put(word.substring(0, index), word.substring(index + 1));
			}
		}
	}

	private static List<String> _splitArguments(String rest) {
		if (rest.startsWith("[")) {
			try {
				return _objectMapper.readValue(
					rest,
					new TypeReference<List<String>>() {
					});
			}
			catch (IOException ioException) {
				throw new GradleException(
					"Unable to parse Dockerfile arguments " + rest,
					ioException);
			}
		}

		List<String> arguments = new ArrayList<>();

		for (String word : _splitWords(rest)) {
			if (!word.startsWith("--")) {
				arguments.add(word);
			}
		}

		return arguments;
	}

	/**
	 * Splits on whitespace the way a shell does, keeping quoted text together
	 * and dropping the quotes.
	 */
	private static List<String> _splitWords(String text) {
		List<String> words = new ArrayList<>();

		boolean escaped = false;
		boolean inWord = false;
		char quote = 0;
		StringBuilder sb = new StringBuilder();

		for (char character : text.toCharArray()) {
			if (escaped) {
				sb.append(character);

				escaped = false;

				continue;
			}

			if ((character == '\\') && (quote != '\'')) {
				escaped = true;
				inWord = true;

				continue;
			}

			if (quote != 0) {
				if (character == quote) {
					quote = 0;
				}
				else {
					sb.append(character);
				}

				continue;
			}

			if ((character == '"') || (character == '\'')) {
				inWord = true;
				quote = character;

				continue;
			}

			if ((character == ' ') || (character == '\t')) {
				if (inWord) {
					words.add(sb.toString());

					sb.setLength(0);

					inWord = false;
				}

				continue;
			}

			sb.append(character);

			inWord = true;
		}

		if (inWord) {
			words.add(sb.toString());
		}

		return words;
	}

	private static final ObjectMapper _objectMapper = new ObjectMapper();

	private final List<Copy> _copies = new ArrayList<>();
	private final Map<String, String> _env = new LinkedHashMap<>();
	private String _image;
	private final List<String> _runs = new ArrayList<>();
	private final List<String> _warnings = new ArrayList<>();

}