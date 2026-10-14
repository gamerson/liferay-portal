/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.gradle.plugins.workspace.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import com.liferay.gradle.plugins.workspace.WorkspacePlugin;
import com.liferay.gradle.plugins.workspace.internal.client.extension.oci.ChartArchive;
import com.liferay.gradle.plugins.workspace.internal.client.extension.oci.ClientExtensionPackager;
import com.liferay.gradle.plugins.workspace.internal.client.extension.oci.OCIPublisher;

import java.io.File;
import java.io.IOException;

import java.util.List;
import java.util.Map;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.logging.Logger;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.options.Option;

/**
 * Packages a client extension zip as an OCI artifact and a Helm chart, and
 * publishes both to a registry unless push is false. Nothing is built from the
 * zip's Dockerfile: its FROM names the stock base image the workload runs, and
 * its COPY lines say where the artifact is mounted.
 *
 * @author Gregory Amerson
 */
public class PublishClientExtensionTask extends DefaultTask {

	public PublishClientExtensionTask() {
		Project project = getProject();

		ObjectFactory objectFactory = project.getObjects();
		ProviderFactory providerFactory = project.getProviders();

		_chartDir = objectFactory.directoryProperty();

		_chartVersion = objectFactory.property(String.class);

		_chartVersion.convention(
			_getGradleProperty(providerFactory, "chart.version", "0.1.0"));

		_clusterRegistry = objectFactory.property(String.class);

		_clusterRegistry.convention(
			providerFactory.gradleProperty(
				_PROPERTY_PREFIX + "cluster.registry"));

		_containerRuntime = objectFactory.property(String.class);

		_containerRuntime.convention(
			_getGradleProperty(providerFactory, "container.runtime", "docker"));

		_outputDir = objectFactory.directoryProperty();

		ProjectLayout projectLayout = project.getLayout();

		DirectoryProperty buildDirectoryProperty =
			projectLayout.getBuildDirectory();

		_outputDir.convention(
			buildDirectoryProperty.dir("client-extension-oci"));

		_plainHTTP = objectFactory.property(Boolean.class);

		Provider<String> plainHTTPProvider = _getGradleProperty(
			providerFactory, "plain.http", "false");

		_plainHTTP.convention(plainHTTPProvider.map(Boolean::parseBoolean));

		_push = objectFactory.property(Boolean.class);

		_push.convention(true);

		_registry = objectFactory.property(String.class);

		_registry.convention(
			providerFactory.gradleProperty(_PROPERTY_PREFIX + "registry"));

		_serviceId = objectFactory.property(String.class);

		_serviceId.convention(project.getName());

		_zipFile = objectFactory.fileProperty();

		getOutputs().upToDateWhen(task -> !_push.get());
	}

	@InputDirectory
	@Optional
	public DirectoryProperty getChartDir() {
		return _chartDir;
	}

	@Input
	public Property<String> getChartVersion() {
		return _chartVersion;
	}

	@Input
	@Optional
	public Property<String> getClusterRegistry() {
		return _clusterRegistry;
	}

	@Input
	public Property<String> getContainerRuntime() {
		return _containerRuntime;
	}

	@OutputDirectory
	public DirectoryProperty getOutputDir() {
		return _outputDir;
	}

	@Input
	public Property<Boolean> getPlainHTTP() {
		return _plainHTTP;
	}

	@Input
	public Property<Boolean> getPush() {
		return _push;
	}

	@Input
	@Optional
	public Property<String> getRegistry() {
		return _registry;
	}

	@Input
	public Property<String> getServiceId() {
		return _serviceId;
	}

	@InputFile
	public RegularFileProperty getZipFile() {
		return _zipFile;
	}

	@TaskAction
	public void publishClientExtension()
		throws InterruptedException, IOException {

		String registry = _registry.getOrNull();

		if ((registry == null) || registry.isEmpty()) {
			throw new GradleException(
				"Set --registry or " + _PROPERTY_PREFIX +
					"registry to the registry and path to publish to, such " +
						"as ghcr.io/example");
		}

		File outputDir = _getFile(_outputDir);

		Map<String, byte[]> chartFiles;

		if (_chartDir.isPresent()) {
			File chartDir = _getFile(_chartDir);

			chartFiles = ChartArchive.readChartFiles(chartDir.toPath());
		}
		else {
			chartFiles = ChartArchive.readEmbeddedChartFiles();
		}

		OCIPublisher ociPublisher;

		if (_push.get()) {
			ociPublisher = OCIPublisher.forRegistry(_plainHTTP.get());
		}
		else {
			ociPublisher = OCIPublisher.forLayout(outputDir.toPath());
		}

		ClientExtensionPackager clientExtensionPackager =
			new ClientExtensionPackager(
				chartFiles, _clusterRegistry.getOrElse(registry),
				_containerRuntime.get(), ociPublisher, outputDir, registry,
				_serviceId.get(), _chartVersion.get(), _getFile(_zipFile));

		Map<String, Object> summary =
			clientExtensionPackager.packageClientExtension();

		summary.put("pushed", _push.get());

		File summaryFile = new File(outputDir, "summary.json");

		_objectMapper.writeValue(summaryFile, summary);

		Logger logger = getLogger();

		@SuppressWarnings("unchecked")
		List<String> warnings = (List<String>)summary.get("warnings");

		for (String warning : warnings) {
			logger.warn(warning);
		}

		@SuppressWarnings("unchecked")
		Map<String, Object> chart = (Map<String, Object>)summary.get("chart");

		if (_push.get()) {
			logger.lifecycle(
				"Published chart {} version {}", chart.get("reference"),
				chart.get("version"));
		}
		else {
			logger.lifecycle("Packaged chart {}", chart.get("archive"));
		}

		logger.lifecycle("Wrote {}", summaryFile);
	}

	@Option(
		description = "The chart version and the artifact tag.",
		option = "chart-version"
	)
	public void setChartVersion(String chartVersion) {
		_chartVersion.set(chartVersion);
	}

	@Option(
		description = "The registry the cluster pulls the artifact from, when it reaches the registry under another name.",
		option = "cluster-registry"
	)
	public void setClusterRegistry(String clusterRegistry) {
		_clusterRegistry.set(clusterRegistry);
	}

	@Option(
		description = "Push the artifact and the chart over HTTP instead of HTTPS.",
		option = "plain-http"
	)
	public void setPlainHTTP(boolean plainHTTP) {
		_plainHTTP.set(plainHTTP);
	}

	@Option(
		description = "The registry and path to publish to, such as ghcr.io/example.",
		option = "registry"
	)
	public void setRegistry(String registry) {
		_registry.set(registry);
	}

	private File _getFile(DirectoryProperty directoryProperty) {
		return directoryProperty.get(
		).getAsFile();
	}

	private File _getFile(RegularFileProperty regularFileProperty) {
		return regularFileProperty.get(
		).getAsFile();
	}

	private Provider<String> _getGradleProperty(
		ProviderFactory providerFactory, String name, String defaultValue) {

		Provider<String> provider = providerFactory.gradleProperty(
			_PROPERTY_PREFIX + name);

		return provider.orElse(defaultValue);
	}

	private static final String _PROPERTY_PREFIX =
		WorkspacePlugin.PROPERTY_PREFIX + "client.extension.";

	private static final ObjectMapper _objectMapper = new ObjectMapper(
	).enable(
		SerializationFeature.INDENT_OUTPUT
	);

	private final DirectoryProperty _chartDir;
	private final Property<String> _chartVersion;
	private final Property<String> _clusterRegistry;
	private final Property<String> _containerRuntime;
	private final DirectoryProperty _outputDir;
	private final Property<Boolean> _plainHTTP;
	private final Property<Boolean> _push;
	private final Property<String> _registry;
	private final Property<String> _serviceId;
	private final RegularFileProperty _zipFile;

}