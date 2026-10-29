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
import com.liferay.gradle.plugins.workspace.internal.util.StringUtil;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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

		String version = resolveVersion(
			clientExtensionPackage, helmChartSource);

		String tag = version.replace('+', '_');

		String helmChartRepository = registry + "/charts/" + serviceId;

		ContainerRef helmChartRef = ContainerRef.parse(
			helmChartRepository + ":" + tag);

		Logger logger = getLogger();

		// A published version names one build, so it is pushed again only
		// as a snapshot. Otherwise the chart already at the tag must hold the
		// same content, and then there is nothing to push.

		if (!version.endsWith("-SNAPSHOT")) {
			Map<String, Object> publishedChartMetadata =
				ociPublisher.getHelmChartMetadata(helmChartRef);

			if (publishedChartMetadata != null) {
				Map<?, ?> annotations = Map.of();

				if (publishedChartMetadata.get("annotations") instanceof
						Map<?, ?> map) {

					annotations = map;
				}

				if (!Objects.equals(
						annotations.get(
							HelmChartArchive.ANNOTATION_CONTENT_DIGEST),
						clientExtensionPackage.getContentDigest(
							helmChartSource, serviceId))) {

					throw new GradleException(
						StringUtil.concat(
							"Version ", version, " of \"", serviceId,
							"\" is already published to oci://",
							helmChartRepository, " with different content. ",
							"Publish the change as a new version, or use a ",
							"-SNAPSHOT version to republish it."));
				}

				logger.lifecycle(
					"Helm chart oci://{} version {} is already published " +
						"with the same content",
					helmChartRepository, version);

				Object artifactReference = annotations.get(
					HelmChartArchive.ANNOTATION_ARTIFACT);

				_writeSummary(
					(artifactReference == null) ? "" :
						String.valueOf(artifactReference),
					ociPublisher.getDigest(helmChartRef), helmChartRepository,
					version);

				return;
			}
		}

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
		}

		HelmChartArchive helmChartArchive =
			clientExtensionPackage.createHelmChartArchive(
				artifactReference, helmChartSource, serviceId, version);

		writeFile(
			helmChartArchive.getData(), serviceId + "-" + version + ".tgz");

		String digest = ociPublisher.publishHelmChart(
			helmChartArchive, helmChartRef);

		logger.lifecycle(
			"Published Helm chart oci://{} version {} from {} ({})",
			helmChartRepository, version, helmChartSource.getTemplate(),
			helmChartSource.getOrigin());

		_writeSummary(artifactReference, digest, helmChartRepository, version);
	}

	/**
	 * Writes summary.json, which tells CI what to put in an environment's
	 * liferay.yaml.
	 */
	private void _writeSummary(
			String artifactReference, String digest, String helmChartRepository,
			String version)
		throws Exception {

		Map<String, Object> summary = new LinkedHashMap<>();

		if (!artifactReference.isEmpty()) {
			summary.put("artifact", Map.of("reference", artifactReference));
		}

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