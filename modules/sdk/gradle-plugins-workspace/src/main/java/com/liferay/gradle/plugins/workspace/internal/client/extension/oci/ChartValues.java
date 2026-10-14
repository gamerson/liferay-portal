/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.gradle.plugins.workspace.internal.client.extension.oci;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import org.gradle.api.GradleException;

/**
 * What the build decides about the chart, as an overlay on its values.yaml.
 * Nothing here is environment specific.
 *
 * @author Gregory Amerson
 */
public class ChartValues {

	public static Map<String, Object> bake(
		String artifactReference, Map<String, Object> configs,
		ClientExtensionDockerfile clientExtensionDockerfile,
		Map<String, Object> lcp, List<ArtifactMount> mounts, String serviceId) {

		String image = clientExtensionDockerfile.getImage();

		int index = image.lastIndexOf(':');

		if (image.contains("@")) {
			index = image.indexOf('@');
		}

		String repository = image.substring(0, index);
		String tag = image.substring(index + 1);

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

			if ((probe != null) && !probe.isEmpty()) {
				workload.put(probeName, probe);
			}
		}

		Map<String, Object> startupProbe = _getStartupProbe(lcp);

		if (startupProbe != null) {
			workload.put("startupProbe", startupProbe);
		}

		Map<String, Object> env = new TreeMap<>(
			clientExtensionDockerfile.getEnv());

		Map<?, ?> lcpEnv = _getMap(lcp.get("env"));

		if (lcpEnv != null) {
			for (Map.Entry<?, ?> entry : lcpEnv.entrySet()) {
				env.put(
					String.valueOf(entry.getKey()),
					String.valueOf(entry.getValue()));
			}
		}

		if (repository.endsWith("liferay/batch")) {
			for (String configurationKey : configs.keySet()) {
				index = configurationKey.indexOf(
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

		boolean workloadEnabled = !repository.endsWith("liferay/noop");

		workload.put("enabled", workloadEnabled);

		if (mounts.isEmpty()) {
			artifactReference = "";
		}

		List<Object> mountValues = new ArrayList<>();

		for (ArtifactMount mount : mounts) {
			mountValues.add(mount.toMap());
		}

		Map<String, Object> values = new LinkedHashMap<>();

		Map<String, Object> artifact = new LinkedHashMap<>();

		artifact.put("mounts", mountValues);
		artifact.put("reference", artifactReference);

		values.put("artifact", artifact);

		Map<String, Object> clientExtension = new LinkedHashMap<>();

		clientExtension.put("configs", new TreeMap<>(configs));
		clientExtension.put("serviceId", serviceId);

		values.put("clientExtension", clientExtension);

		values.put("fullnameOverride", serviceId);

		Map<String, Object> imageValues = new LinkedHashMap<>();

		imageValues.put("repository", repository);
		imageValues.put("tag", tag);

		values.put("image", imageValues);

		Map<String, Object> service = new LinkedHashMap<>();

		service.put(
			"enabled",
			workloadEnabled &&
			Objects.equals(workload.get("kind"), "Deployment"));
		service.put("targetPort", workload.get("containerPort"));

		values.put("service", service);

		values.put("workload", workload);

		return values;
	}

	/**
	 * Combines every *.client-extension-config.json in the zip into the one
	 * map spec.configs holds, refusing a configuration key two files both
	 * declare rather than silently keeping either.
	 */
	public static Map<String, Object> mergeConfigs(
		Map<String, ArtifactFile> artifactFiles) {

		Map<String, Object> configs = new TreeMap<>();
		Map<String, String> sources = new TreeMap<>();

		for (ArtifactFile artifactFile : artifactFiles.values()) {
			String name = artifactFile.getName();

			if (!name.endsWith(".client-extension-config.json") ||
				name.contains("/")) {

				continue;
			}

			Map<String, Object> fileConfigs;

			try {
				fileConfigs = _objectMapper.readValue(
					artifactFile.getData(),
					new TypeReference<LinkedHashMap<String, Object>>() {
					});
			}
			catch (IOException ioException) {
				throw new GradleException(
					"Unable to parse " + name, ioException);
			}

			for (Map.Entry<String, Object> entry : fileConfigs.entrySet()) {
				String source = sources.get(entry.getKey());

				if (source != null) {
					throw new GradleException(
						source + " and " + name + " both declare \"" +
							entry.getKey() + "\"");
				}

				configs.put(entry.getKey(), entry.getValue());
				sources.put(entry.getKey(), name);
			}
		}

		if (configs.isEmpty()) {
			throw new GradleException(
				"The zip carries no *.client-extension-config.json");
		}

		return configs;
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

	/**
	 * Derives a startup probe from LCP.json's probes. LCP.json declares them
	 * with no initial delay, relying on the platform to allow for start up,
	 * and Kubernetes does not: a liveness probe fires at once and kills a JVM
	 * before Tomcat is listening. A startup probe on the same endpoint holds
	 * both off until the process has answered once, allowing five minutes.
	 */
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

				if (value != null) {
					startupProbe.put(handler, value);
				}
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

	private static final ObjectMapper _objectMapper = new ObjectMapper();

}