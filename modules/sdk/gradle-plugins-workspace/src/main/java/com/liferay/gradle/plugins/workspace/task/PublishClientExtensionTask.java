/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.gradle.plugins.workspace.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import com.liferay.gradle.plugins.workspace.internal.client.extension.oci.ArtifactLayer;
import com.liferay.gradle.plugins.workspace.internal.client.extension.oci.ArtifactMount;
import com.liferay.gradle.plugins.workspace.internal.client.extension.oci.ClientExtensionPackage;
import com.liferay.gradle.plugins.workspace.internal.client.extension.oci.HelmChartArchive;
import com.liferay.gradle.plugins.workspace.internal.client.extension.oci.HelmChartSource;
import com.liferay.gradle.plugins.workspace.internal.client.extension.oci.OCIPublisher;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import land.oras.ContainerRef;
import land.oras.Registry;
import land.oras.exception.OrasException;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskOutputs;

/**
 * @author Gregory Amerson
 */
public class PublishClientExtensionTask extends BaseClientExtensionPackageTask {

	public PublishClientExtensionTask() {
		super("client-extension-publish");

		Project project = getProject();

		ObjectFactory objectFactory = project.getObjects();

		_clusterRegistry = objectFactory.property(String.class);

		_clusterRegistry.convention(getGradleProperty("cluster.registry"));

		_registry = objectFactory.property(String.class);

		_registry.convention(getGradleProperty("registry"));

		TaskOutputs taskOutputs = getOutputs();

		taskOutputs.upToDateWhen(task -> false);
	}

	@Input
	@Optional
	public Property<String> getClusterRegistry() {
		return _clusterRegistry;
	}

	@Input
	@Optional
	public Property<String> getRegistry() {
		return _registry;
	}

	@TaskAction
	public void publishClientExtension() throws Exception {
		String registry = _registry.getOrNull();

		if ((registry == null) || registry.isBlank()) {
			throw new GradleException(
				"Set " + PROPERTY_PREFIX + "registry to the registry and " +
					"path to publish to, such as ghcr.io/example");
		}

		try {
			_publishClientExtension(registry);
		}
		catch (OrasException orasException) {
			throw OCIPublisher.toRegistryException(orasException, registry);
		}
	}

	private void _publishClientExtension(String registry) throws Exception {
		HelmChartSource helmChartSource = resolveHelmChartSource();

		ClientExtensionPackage clientExtensionPackage =
			createClientExtensionPackage();

		OCIPublisher<ContainerRef> ociPublisher = new OCIPublisher<>(
			Registry.builder(
			).defaults(
			).build());

		Project project = getProject();

		String serviceId = project.getName();

		Property<String> chartVersionProperty = getChartVersion();

		String version = chartVersionProperty.get();

		String tag = version.replace('+', '_');

		Logger logger = getLogger();

		Map<String, Object> summary = new LinkedHashMap<>();

		String artifactReference = "";

		ArtifactLayer artifactLayer = clientExtensionPackage.getArtifactLayer();

		List<ArtifactMount> artifactMounts = artifactLayer.getMounts();

		if (!artifactMounts.isEmpty()) {
			String digest = ociPublisher.publishArtifact(
				ContainerRef.parse(registry + "/cx/" + serviceId + ":" + tag),
				artifactLayer.toTar());

			artifactReference =
				_clusterRegistry.getOrElse(registry) + "/cx/" + serviceId +
					"@" + digest;

			logger.lifecycle("Published artifact {}", artifactReference);

			summary.put("artifact", Map.of("reference", artifactReference));
		}

		HelmChartArchive helmChartArchive =
			clientExtensionPackage.createHelmChartArchive(
				artifactReference, helmChartSource, serviceId, version);

		writeFile(
			helmChartArchive.getData(), serviceId + "-" + version + ".tgz");

		String helmChartRepository = registry + "/charts/" + serviceId;

		String digest = ociPublisher.publishHelmChart(
			helmChartArchive,
			ContainerRef.parse(helmChartRepository + ":" + tag));

		logger.lifecycle(
			"Published Helm chart oci://{} version {} from {} ({})",
			helmChartRepository, version, helmChartSource.getTemplate(),
			helmChartSource.getOrigin());

		Map<String, Object> helmChart = new LinkedHashMap<>();

		helmChart.put("digest", digest);
		helmChart.put("reference", "oci://" + helmChartRepository);
		helmChart.put("version", version);

		summary.put("chart", helmChart);

		writeFile(_objectMapper.writeValueAsBytes(summary), "summary.json");
	}

	private static final ObjectMapper _objectMapper = new ObjectMapper(
	).enable(
		SerializationFeature.INDENT_OUTPUT
	);

	private final Property<String> _clusterRegistry;
	private final Property<String> _registry;

}