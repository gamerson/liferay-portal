/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.gradle.plugins.workspace.task;

import com.liferay.gradle.plugins.workspace.WorkspacePlugin;
import com.liferay.gradle.plugins.workspace.internal.client.extension.oci.ClientExtensionPackage;
import com.liferay.gradle.plugins.workspace.internal.client.extension.oci.HelmChartSource;
import com.liferay.gradle.plugins.workspace.internal.client.extension.oci.OCIPublisher;

import java.io.File;
import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Path;

import land.oras.exception.OrasException;

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

/**
 * @author Gregory Amerson
 */
public abstract class BaseClientExtensionPackageTask extends DefaultTask {

	public BaseClientExtensionPackageTask(String outputDirName) {
		Project project = getProject();

		ObjectFactory objectFactory = project.getObjects();

		_chart = objectFactory.property(String.class);

		_chart.convention(getGradleProperty("chart"));

		_containerRuntime = objectFactory.property(String.class);

		Provider<String> containerRuntimeProvider = getGradleProperty(
			"container.runtime");

		_containerRuntime.convention(containerRuntimeProvider.orElse("docker"));

		_defaultDockerfile = objectFactory.fileProperty();

		_outputDir = objectFactory.directoryProperty();

		ProjectLayout projectLayout = project.getLayout();

		DirectoryProperty buildDirectoryProperty =
			projectLayout.getBuildDirectory();

		_outputDir.convention(buildDirectoryProperty.dir(outputDirName));

		_version = objectFactory.property(String.class);

		ProviderFactory providerFactory = project.getProviders();

		Provider<String> projectVersionProvider = providerFactory.provider(
			() -> {
				String version = String.valueOf(project.getVersion());

				if (version.equals(Project.DEFAULT_VERSION)) {
					return null;
				}

				return version;
			});

		_version.convention(
			projectVersionProvider.orElse(getGradleProperty("version")));

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

	/**
	 * Returns the version of the client extension's files and chart. It
	 * defaults to the project's Gradle version, then to the
	 * liferay.workspace.client.extension.version property. With neither,
	 * {@link #resolveVersion} derives one from the content.
	 */
	@Input
	@Optional
	public Property<String> getVersion() {
		return _version;
	}

	@InputFile
	public RegularFileProperty getZipFile() {
		return _zipFile;
	}

	protected ClientExtensionPackage createClientExtensionPackage()
		throws Exception {

		Project project = getProject();

		File workDir = new File(getTemporaryDir(), "replay");

		project.delete(_getFile(_outputDir), workDir);

		Path rootDirPath = project.getRootDir(
		).toPath();

		File dockerfile = project.file("Dockerfile");

		ClientExtensionPackage clientExtensionPackage =
			ClientExtensionPackage.create(
				_containerRuntime.get(), _getFile(_defaultDockerfile),
				String.valueOf(rootDirPath.relativize(dockerfile.toPath())),
				workDir, _getFile(_zipFile));

		project.delete(workDir);

		Logger logger = getLogger();

		for (String warning : clientExtensionPackage.getWarnings()) {
			logger.warn(warning);
		}

		return clientExtensionPackage;
	}

	/**
	 * Returns the liferay.workspace.client.extension.* Gradle property with
	 * the name.
	 */
	protected Provider<String> getGradleProperty(String name) {
		Project project = getProject();

		ProviderFactory providerFactory = project.getProviders();

		return providerFactory.gradleProperty(PROPERTY_PREFIX + name);
	}

	/**
	 * Returns the Helm chart to package. Tasks resolve it before reading the
	 * zip, so a bad chart reference fails before any replay runs.
	 */
	protected HelmChartSource resolveHelmChartSource() throws Exception {
		Project project = getProject();

		try {
			return HelmChartSource.resolveReference(
				_chart.getOrNull(), project.getRootDir());
		}
		catch (OrasException orasException) {
			throw OCIPublisher.toRegistryException(
				orasException, _chart.getOrNull());
		}
	}

	/**
	 * Returns the version to package or publish: the version property when it
	 * has a value, or else the version derived from the content.
	 */
	protected String resolveVersion(
			ClientExtensionPackage clientExtensionPackage,
			HelmChartSource helmChartSource)
		throws IOException {

		String version = _version.getOrNull();

		if ((version != null) && !version.isBlank()) {
			return version;
		}

		Project project = getProject();

		return clientExtensionPackage.getContentVersion(
			helmChartSource, project.getName());
	}

	/**
	 * Writes the data to a file with the name in the output directory and
	 * returns the file.
	 */
	protected File writeFile(byte[] data, String fileName) throws IOException {
		File outputDir = _getFile(_outputDir);

		outputDir.mkdirs();

		File file = new File(outputDir, fileName);

		Files.write(file.toPath(), data);

		return file;
	}

	protected static final String PROPERTY_PREFIX =
		WorkspacePlugin.PROPERTY_PREFIX + "client.extension.";

	private File _getFile(Provider<? extends FileSystemLocation> provider) {
		FileSystemLocation fileSystemLocation = provider.get();

		return fileSystemLocation.getAsFile();
	}

	private final Property<String> _chart;
	private final Property<String> _containerRuntime;
	private final RegularFileProperty _defaultDockerfile;
	private final DirectoryProperty _outputDir;
	private final Property<String> _version;
	private final RegularFileProperty _zipFile;

}