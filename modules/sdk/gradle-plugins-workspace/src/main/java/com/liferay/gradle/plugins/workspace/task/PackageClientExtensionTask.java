/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.gradle.plugins.workspace.task;

import com.liferay.gradle.plugins.workspace.WorkspacePlugin;
import com.liferay.gradle.plugins.workspace.internal.client.extension.oci.ArtifactLayer;
import com.liferay.gradle.plugins.workspace.internal.client.extension.oci.ArtifactMount;
import com.liferay.gradle.plugins.workspace.internal.client.extension.oci.ClientExtensionPackage;
import com.liferay.gradle.plugins.workspace.internal.client.extension.oci.HelmChartArchive;
import com.liferay.gradle.plugins.workspace.internal.client.extension.oci.HelmChartSource;

import java.io.File;

import java.nio.file.Files;
import java.nio.file.Path;

import java.util.List;

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileSystemLocation;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.logging.Logger;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

/**
 * @author Gregory Amerson
 */
public class PackageClientExtensionTask extends DefaultTask {

	public PackageClientExtensionTask() {
		Project project = getProject();

		ObjectFactory objectFactory = project.getObjects();
		ProviderFactory providerFactory = project.getProviders();

		_chart = objectFactory.property(String.class);

		_chart.convention(
			providerFactory.gradleProperty(_PROPERTY_PREFIX + "chart"));

		_chartVersion = objectFactory.property(String.class);

		_chartVersion.convention(
			_getGradleProperty(providerFactory, "chart.version", "0.1.0"));

		_containerRuntime = objectFactory.property(String.class);

		_containerRuntime.convention(
			_getGradleProperty(providerFactory, "container.runtime", "docker"));

		_defaultDockerfile = objectFactory.fileProperty();

		_outputDir = objectFactory.directoryProperty();

		ProjectLayout projectLayout = project.getLayout();

		DirectoryProperty buildDirectoryProperty =
			projectLayout.getBuildDirectory();

		_outputDir.convention(
			buildDirectoryProperty.dir("client-extension-package"));

		_zipFile = objectFactory.fileProperty();
	}

	@Input
	@Optional
	public Property<String> getChart() {
		return _chart;
	}

	@InputFiles
	public FileCollection getChartFiles() {
		Project project = getProject();

		File file = HelmChartSource.getLocalFile(
			_chart.getOrNull(), project.getRootDir());

		if (file == null) {
			return project.files();
		}

		return project.files(file);
	}

	@Input
	public Property<String> getChartVersion() {
		return _chartVersion;
	}

	@Internal
	public Property<String> getContainerRuntime() {
		return _containerRuntime;
	}

	@InputFile
	public RegularFileProperty getDefaultDockerfile() {
		return _defaultDockerfile;
	}

	@OutputDirectory
	public DirectoryProperty getOutputDir() {
		return _outputDir;
	}

	@InputFile
	public RegularFileProperty getZipFile() {
		return _zipFile;
	}

	@TaskAction
	public void packageClientExtension() throws Exception {
		Project project = getProject();

		File rootDir = project.getRootDir();

		HelmChartSource helmChartSource = HelmChartSource.resolveReference(
			_chart.getOrNull(), rootDir);

		File outputDir = _getFile(_outputDir);

		File workDir = new File(getTemporaryDir(), "replay");

		project.delete(outputDir, workDir);

		outputDir.mkdirs();

		Path rootDirPath = rootDir.toPath();

		File dockerfile = project.file("Dockerfile");

		ClientExtensionPackage clientExtensionPackage =
			ClientExtensionPackage.create(
				_containerRuntime.get(), _getFile(_defaultDockerfile),
				String.valueOf(rootDirPath.relativize(dockerfile.toPath())),
				workDir, _getFile(_zipFile));

		Logger logger = getLogger();

		for (String warning : clientExtensionPackage.getWarnings()) {
			logger.warn(warning);
		}

		String serviceId = project.getName();
		String version = _chartVersion.get();

		ArtifactLayer artifactLayer = clientExtensionPackage.getArtifactLayer();

		List<ArtifactMount> artifactMounts = artifactLayer.getMounts();

		if (!artifactMounts.isEmpty()) {
			File layerFile = new File(
				outputDir, serviceId + "-" + version + "-layer.tar");

			Files.write(layerFile.toPath(), artifactLayer.toTar());

			logger.lifecycle("Wrote artifact layer {}", layerFile);
		}

		HelmChartArchive helmChartArchive =
			clientExtensionPackage.createHelmChartArchive(
				"", helmChartSource, serviceId, version);

		File helmChartFile = new File(
			outputDir, serviceId + "-" + version + ".tgz");

		Files.write(helmChartFile.toPath(), helmChartArchive.getData());

		logger.lifecycle(
			"Wrote Helm chart {} from {} ({})", helmChartFile,
			helmChartSource.getTemplate(), helmChartSource.getOrigin());

		project.delete(workDir);
	}

	private File _getFile(Provider<? extends FileSystemLocation> provider) {
		FileSystemLocation fileSystemLocation = provider.get();

		return fileSystemLocation.getAsFile();
	}

	private Provider<String> _getGradleProperty(
		ProviderFactory providerFactory, String name, String defaultValue) {

		Provider<String> provider = providerFactory.gradleProperty(
			_PROPERTY_PREFIX + name);

		return provider.orElse(defaultValue);
	}

	private static final String _PROPERTY_PREFIX =
		WorkspacePlugin.PROPERTY_PREFIX + "client.extension.";

	private final Property<String> _chart;
	private final Property<String> _chartVersion;
	private final Property<String> _containerRuntime;
	private final RegularFileProperty _defaultDockerfile;
	private final DirectoryProperty _outputDir;
	private final RegularFileProperty _zipFile;

}