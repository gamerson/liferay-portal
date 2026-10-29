/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.gradle.plugins.workspace.internal.client.extension.oci;

import java.nio.charset.StandardCharsets;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.gradle.api.GradleException;

/**
 * Applies a chart's .helmignore the way "helm package" does. A pattern
 * without a slash matches a file or directory name at any depth, a pattern
 * with a slash matches the path from the chart's root, a leading slash
 * anchors it there, a trailing slash matches only directories, and a leading
 * "!" negates it. Ignoring a directory ignores everything in it. Like Helm,
 * it always ignores dotfiles in templates/ and rejects "**".
 *
 * @author Gregory Amerson
 */
public class HelmIgnore {

	/**
	 * Returns the chart files without those the chart's .helmignore ignores.
	 */
	public static Map<String, byte[]> filter(Map<String, byte[]> chartFiles) {
		byte[] data = chartFiles.get(".helmignore");

		HelmIgnore helmIgnore = new HelmIgnore(
			(data == null) ? "" : new String(data, StandardCharsets.UTF_8));

		Map<String, byte[]> filteredChartFiles = new TreeMap<>();

		for (Map.Entry<String, byte[]> entry : chartFiles.entrySet()) {
			if (helmIgnore.isIgnored(entry.getKey())) {
				continue;
			}

			filteredChartFiles.put(entry.getKey(), entry.getValue());
		}

		return filteredChartFiles;
	}

	public HelmIgnore(String text) {
		_addRule("templates/.?*");

		for (String line : text.split("\n")) {
			_addRule(line.trim());
		}
	}

	/**
	 * Returns whether the file at the path from the chart's root is ignored,
	 * itself or through one of its directories.
	 */
	public boolean isIgnored(String path) {
		String[] names = path.split("/");

		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < names.length; i++) {
			if (i > 0) {
				sb.append('/');
			}

			sb.append(names[i]);

			if (_isIgnored(sb.toString(), i < (names.length - 1))) {
				return true;
			}
		}

		return false;
	}

	private void _addRule(String rule) {
		if (rule.isEmpty() || rule.startsWith("#")) {
			return;
		}

		if (rule.contains("**")) {
			throw new GradleException(
				"The .helmignore pattern \"" + rule +
					"\" uses \"**\", which Helm does not support");
		}

		boolean negated = false;

		if (rule.startsWith("!")) {
			negated = true;

			rule = rule.substring(1);
		}

		boolean directoryOnly = false;

		if (rule.endsWith("/")) {
			directoryOnly = true;

			rule = rule.substring(0, rule.length() - 1);
		}

		boolean wholePath = rule.contains("/");

		if (rule.startsWith("/")) {
			rule = rule.substring(1);
		}

		_rules.add(
			new Rule(directoryOnly, negated, _toPattern(rule), wholePath));
	}

	/**
	 * Returns whether the path itself is ignored. As in Helm, the rules are
	 * read in order, a match stops the reading, and a negated rule ignores
	 * every path it does not match.
	 */
	private boolean _isIgnored(String path, boolean directory) {
		for (Rule rule : _rules) {
			if (rule._negated) {
				if ((rule._directoryOnly && !directory) ||
					!rule.matches(path)) {

					return true;
				}

				continue;
			}

			if (rule._directoryOnly && !directory) {
				continue;
			}

			if (rule.matches(path)) {
				return true;
			}
		}

		return false;
	}

	private Pattern _toPattern(String glob) {
		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < glob.length(); i++) {
			char c = glob.charAt(i);

			if (c == '*') {
				sb.append("[^/]*");
			}
			else if (c == '?') {
				sb.append("[^/]");
			}
			else if (c == '[') {
				int end = glob.indexOf(']', i + 2);

				if (end < 0) {
					throw new GradleException(
						"The .helmignore pattern \"" + glob + "\" has an " +
							"unclosed character class");
				}

				String characterClass = glob.substring(i + 1, end);

				if (characterClass.startsWith("^")) {
					sb.append("[^");

					characterClass = characterClass.substring(1);
				}
				else {
					sb.append('[');
				}

				for (char classChar : characterClass.toCharArray()) {
					if (classChar == '-') {
						sb.append('-');
					}
					else {
						sb.append("\\x{");
						sb.append(Integer.toHexString(classChar));
						sb.append('}');
					}
				}

				sb.append(']');

				i = end;
			}
			else {
				if ((c == '\\') && ((i + 1) < glob.length())) {
					c = glob.charAt(++i);
				}

				sb.append(Pattern.quote(String.valueOf(c)));
			}
		}

		return Pattern.compile(sb.toString());
	}

	private final List<Rule> _rules = new ArrayList<>();

	private static class Rule {

		public boolean matches(String path) {
			if (!_wholePath) {
				int index = path.lastIndexOf('/');

				path = path.substring(index + 1);
			}

			Matcher matcher = _pattern.matcher(path);

			return matcher.matches();
		}

		private Rule(
			boolean directoryOnly, boolean negated, Pattern pattern,
			boolean wholePath) {

			_directoryOnly = directoryOnly;
			_negated = negated;
			_pattern = pattern;
			_wholePath = wholePath;
		}

		private final boolean _directoryOnly;
		private final boolean _negated;
		private final Pattern _pattern;
		private final boolean _wholePath;

	}

}