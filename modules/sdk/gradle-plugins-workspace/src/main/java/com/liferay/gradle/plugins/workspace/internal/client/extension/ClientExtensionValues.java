/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.gradle.plugins.workspace.internal.client.extension;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Derives the values that deploy a client extension with the
 * liferay-client-extension Helm chart.
 *
 * @author Gregory Amerson
 */
public class ClientExtensionValues {

	/**
	 * Returns the chart values for a client extension. The image its
	 * Dockerfile builds from decides the workload: liferay/noop has none,
	 * liferay/batch gets the OAuth2 application its Job authenticates with,
	 * and liferay/caddy gets a writable /etc/caddy.d. The image repository
	 * and tag are the ones the build pushed, and are ignored when there is
	 * no workload.
	 */
	public static Map<String, Object> derive(
		String baseImage, Map<String, Object> configs, String imageRepository,
		String imageTag, Map<String, Object> lcp, String serviceId) {

		String baseRepository = getRepository(baseImage);

		Map<String, Object> workload = new LinkedHashMap<>();

		workload.put("containerPort", 80);
		workload.put("kind", "Deployment");
		workload.put("replicas", 1);

		Map<?, ?> loadBalancer = _getMap(lcp.get("loadBalancer"));

		if (loadBalancer != null) {
			Integer targetPort = _getInteger(loadBalancer.get("targetPort"));

			if (targetPort != null) {
				workload.put("containerPort", targetPort);
			}
		}

		String kind = _getString(lcp.get("kind"));

		if (kind != null) {
			workload.put("kind", kind);
		}

		Integer scale = _getInteger(lcp.get("scale"));

		if (scale != null) {
			workload.put("replicas", scale);
		}

		String schedule = _getString(lcp.get("schedule"));

		if (schedule != null) {
			workload.put("schedule", schedule);
		}

		for (String probeName :
				new String[] {"livenessProbe", "readinessProbe"}) {

			Map<?, ?> probe = _getMap(lcp.get(probeName));

			if ((probe == null) || probe.isEmpty()) {
				continue;
			}

			workload.put(probeName, probe);
		}

		Map<String, Object> startupProbe = _getStartupProbe(lcp);

		if (startupProbe != null) {
			workload.put("startupProbe", startupProbe);
		}

		Map<String, Object> env = new TreeMap<>();

		Map<?, ?> lcpEnv = _getMap(lcp.get("env"));

		if (lcpEnv != null) {
			for (Map.Entry<?, ?> entry : lcpEnv.entrySet()) {
				env.put(
					String.valueOf(entry.getKey()),
					String.valueOf(entry.getValue()));
			}
		}

		if (baseRepository.endsWith("liferay/batch")) {
			for (String configurationKey : configs.keySet()) {
				int index = configurationKey.indexOf(
					_CONFIGURATION_KEY_HEADLESS_SERVER);

				if (index < 0) {
					continue;
				}

				String identifier = configurationKey.substring(
					index + _CONFIGURATION_KEY_HEADLESS_SERVER.length());

				index = identifier.indexOf('/');

				if (index >= 0) {
					identifier = identifier.substring(0, index);
				}

				env.put("LIFERAY_BATCH_OAUTH_APP_ERC", identifier);
			}
		}

		workload.put("env", env);

		boolean workloadEnabled = !isConfigurationOnly(baseImage);

		workload.put("enabled", workloadEnabled);

		// liferay/caddy writes the CORS rules for DXP's domains into
		// /etc/caddy.d when it starts. The chart runs every workload as a
		// non-root user, who cannot write the image's /etc/caddy.d, so mount a
		// writable empty directory there. Without the rules, browsers refuse
		// the cross-origin ES modules a custom element loads.

		if (baseRepository.endsWith("liferay/caddy")) {
			workload.put(
				"volumeMounts",
				List.of(
					new TreeMap<>(
						Map.of(
							"mountPath", "/etc/caddy.d", "name", "caddy-d"))));
			workload.put(
				"volumes",
				List.of(
					new TreeMap<>(
						Map.of("emptyDir", Map.of(), "name", "caddy-d"))));
		}

		Map<String, Object> values = new LinkedHashMap<>();

		Map<String, Object> clientExtension = new LinkedHashMap<>();

		clientExtension.put("configs", new TreeMap<>(configs));
		clientExtension.put("serviceId", serviceId);

		values.put("clientExtension", clientExtension);

		values.put("fullnameOverride", serviceId);

		if (workloadEnabled) {
			Map<String, Object> image = new LinkedHashMap<>();

			image.put("repository", imageRepository);
			image.put("tag", imageTag);

			values.put("image", image);
		}

		values.put(
			"service",
			Map.of(
				"enabled",
				workloadEnabled &&
				Objects.equals(workload.get("kind"), "Deployment")));

		values.put("workload", workload);

		return values;
	}

	/**
	 * Returns the image of the Dockerfile's first FROM instruction, or an
	 * empty string when it has none.
	 */
	public static String getBaseImage(String dockerfileText) {
		for (String line : dockerfileText.split("\n")) {
			String[] tokens = line.trim(
			).split(
				"\\s+"
			);

			if (!tokens[0].equalsIgnoreCase("FROM")) {
				continue;
			}

			for (int i = 1; i < tokens.length; i++) {
				if (!tokens[i].startsWith("--")) {
					return tokens[i];
				}
			}
		}

		return "";
	}

	/**
	 * Returns the image without its tag or digest.
	 */
	public static String getRepository(String image) {
		int index = image.indexOf('@');

		if (index >= 0) {
			image = image.substring(0, index);
		}

		index = image.lastIndexOf(':');

		if (index > image.lastIndexOf('/')) {
			image = image.substring(0, index);
		}

		return image;
	}

	/**
	 * Returns whether a client extension built from the image has only
	 * configuration, and no workload to run.
	 */
	public static boolean isConfigurationOnly(String baseImage) {
		String baseRepository = getRepository(baseImage);

		return baseRepository.endsWith("liferay/noop");
	}

	/**
	 * Returns the base values with the overlay on top, the way Helm combines
	 * values files: maps merge key by key, and any other overlay value,
	 * including a list, replaces the base value.
	 */
	public static Map<String, Object> merge(
		Map<String, Object> base, Map<String, Object> overlay) {

		Map<String, Object> values = new LinkedHashMap<>(base);

		for (Map.Entry<String, Object> entry : overlay.entrySet()) {
			Object baseValue = values.get(entry.getKey());
			Object value = entry.getValue();

			if ((baseValue instanceof Map<?, ?> baseMap) &&
				(value instanceof Map<?, ?> map)) {

				value = merge(
					(Map<String, Object>)baseMap, (Map<String, Object>)map);
			}

			values.put(entry.getKey(), value);
		}

		return values;
	}

	private static Integer _getInteger(Object value) {
		if (value instanceof Number number) {
			return number.intValue();
		}

		return null;
	}

	private static Map<?, ?> _getMap(Object value) {
		if (value instanceof Map<?, ?> map) {
			return map;
		}

		return null;
	}

	private static Map<String, Object> _getStartupProbe(
		Map<String, Object> lcp) {

		for (String probeName :
				new String[] {"readinessProbe", "livenessProbe"}) {

			Map<?, ?> probe = _getMap(lcp.get(probeName));

			if (probe == null) {
				continue;
			}

			Map<String, Object> startupProbe = new LinkedHashMap<>();

			startupProbe.put("failureThreshold", 60);
			startupProbe.put("periodSeconds", 5);

			for (String handler :
					new String[] {"exec", "grpc", "httpGet", "tcpSocket"}) {

				Object value = probe.get(handler);

				if (value == null) {
					continue;
				}

				startupProbe.put(handler, value);
			}

			if (startupProbe.size() > 2) {
				return startupProbe;
			}
		}

		return null;
	}

	private static String _getString(Object value) {
		if ((value instanceof String string) && !string.isEmpty()) {
			return string;
		}

		return null;
	}

	private static final String _CONFIGURATION_KEY_HEADLESS_SERVER =
		"com.liferay.oauth2.provider.configuration." +
			"OAuth2ProviderApplicationHeadlessServerConfiguration~";

}