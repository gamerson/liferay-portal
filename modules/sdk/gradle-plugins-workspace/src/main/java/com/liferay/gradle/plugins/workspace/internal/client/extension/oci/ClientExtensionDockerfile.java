/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.gradle.plugins.workspace.internal.client.extension.oci;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.gradle.api.GradleException;

/**
 * @author Gregory Amerson
 */
public class ClientExtensionDockerfile {

	public static List<String> getInstructions(String text) {
		List<String> instructions = new ArrayList<>();

		for (String line : text.split("\n")) {
			line = line.trim();

			if (line.isEmpty() || line.startsWith("#")) {
				continue;
			}

			instructions.add(line.replaceAll("\\s+", " "));
		}

		return instructions;
	}

	public static ClientExtensionDockerfile parse(String text) {
		List<Copy> copies = new ArrayList<>();
		String image = null;
		List<String> runs = new ArrayList<>();

		for (String instruction : getInstructions(text)) {
			List<String> words = new ArrayList<>();

			for (String word : instruction.split(" ")) {
				if (!word.startsWith("--")) {
					words.add(word);
				}
			}

			String keyword = words.remove(0);

			keyword = keyword.toUpperCase(Locale.ROOT);

			if (keyword.equals("COPY") && (words.size() >= 2)) {
				copies.add(
					new Copy(
						words.get(words.size() - 1),
						words.subList(0, words.size() - 1)));
			}
			else if (keyword.equals("FROM") && (words.size() == 1) &&
					 (image == null)) {

				image = _normalizeImage(words.get(0));
			}
			else if (keyword.equals("RUN") && !words.isEmpty()) {
				runs.add(String.join(" ", words));
			}
			else {
				throw new GradleException(
					"Dockerfile instruction \"" + instruction +
						"\" is not supported");
			}
		}

		if (image == null) {
			throw new GradleException("Dockerfile names no base image");
		}

		String repository = _getRepository(image);

		if (!_repositories.contains(repository)) {
			throw new GradleException(
				"Base image " + image + " is not supported. Use one of " +
					_repositories + ".");
		}

		if (!runs.isEmpty() && !repository.equals("liferay/node-runner")) {
			throw new GradleException(
				"Dockerfile RUN instructions are supported only on " +
					"liferay/node-runner");
		}

		return new ClientExtensionDockerfile(copies, image, runs);
	}

	public List<Copy> getCopies() {
		return Collections.unmodifiableList(_copies);
	}

	public String getImage() {
		return _image;
	}

	public List<String> getRuns() {
		return Collections.unmodifiableList(_runs);
	}

	public boolean isReplayable() {
		if (_runs.isEmpty() || (_copies.size() != 1)) {
			return false;
		}

		Copy copy = _copies.get(0);

		List<String> sources = copy.getSources();

		if (sources.equals(List.of(".")) || sources.equals(List.of("./"))) {
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

	private static String _getRepository(String image) {
		int index = image.indexOf('@');

		if (index < 0) {
			index = image.lastIndexOf(':');
		}

		return image.substring(0, index);
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

	private ClientExtensionDockerfile(
		List<Copy> copies, String image, List<String> runs) {

		_copies = copies;
		_image = image;
		_runs = runs;
	}

	private static final List<String> _repositories = List.of(
		"liferay/batch", "liferay/caddy", "liferay/jar-runner",
		"liferay/node-runner", "liferay/noop");

	private final List<Copy> _copies;
	private final String _image;
	private final List<String> _runs;

}