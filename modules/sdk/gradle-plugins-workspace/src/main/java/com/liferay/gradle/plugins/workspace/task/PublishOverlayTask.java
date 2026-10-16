/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.gradle.plugins.workspace.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import com.liferay.gradle.plugins.workspace.internal.overlay.OverlayLayer;
import com.liferay.gradle.plugins.workspace.internal.overlay.OverlayPublisher;

import java.io.File;
import java.io.IOException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.logging.Logger;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.options.Option;

/**
 * Publishes what the workspace adds to Liferay -- its modules, WARs, client
 * extensions, configuration, and hotfix -- as an image that holds files alone.
 * The Liferay Helm chart mounts it as an image volume, so nothing is copied
 * into Liferay's persistent volume, and the digest the task prints is the
 * whole of what a GitOps repository has to name.
 *
 * @author Gregory Amerson
 */
public class PublishOverlayTask extends DefaultTask {

	public PublishOverlayTask() {
		Project project = getProject();

		ObjectFactory objectFactory = project.getObjects();
		ProviderFactory providerFactory = project.getProviders();

		_clientExtensionFiles = objectFactory.fileCollection();
		_configsDir = objectFactory.directoryProperty();
		_deployFiles = objectFactory.fileCollection();
		_environment = objectFactory.property(String.class);

		_image = objectFactory.property(String.class);

		_image.convention(
			providerFactory.gradleProperty(_PROPERTY_PREFIX + "image"));

		_outputDir = objectFactory.directoryProperty();

		ProjectLayout projectLayout = project.getLayout();

		DirectoryProperty buildDirectoryProperty =
			projectLayout.getBuildDirectory();

		_outputDir.convention(buildDirectoryProperty.dir("overlay"));

		_plainHTTP = objectFactory.property(Boolean.class);

		Provider<String> plainHTTPProvider = _getGradleProperty(
			providerFactory, "plain.http", "false");

		_plainHTTP.convention(plainHTTPProvider.map(Boolean::parseBoolean));

		_push = objectFactory.property(Boolean.class);

		_push.convention(true);

		_tag = objectFactory.property(String.class);

		Provider<String> tagProvider = providerFactory.gradleProperty(
			_PROPERTY_PREFIX + "tag");

		_tag.convention(tagProvider.orElse(_environment));

		getOutputs().upToDateWhen(task -> !_push.get());
	}

	@InputFiles
	@PathSensitive(PathSensitivity.NAME_ONLY)
	public ConfigurableFileCollection getClientExtensionFiles() {
		return _clientExtensionFiles;
	}

	@Internal
	public DirectoryProperty getConfigsDir() {
		return _configsDir;
	}

	@InputFiles
	@PathSensitive(PathSensitivity.RELATIVE)
	public Provider<List<File>> getConfigsDirs() {
		return _configsDir.map(
			directory -> {
				File configsDir = directory.getAsFile();

				return List.of(
					new File(configsDir, "common"),
					new File(configsDir, "docker"),
					new File(configsDir, _environment.get()));
			});
	}

	@InputFiles
	@PathSensitive(PathSensitivity.NAME_ONLY)
	public ConfigurableFileCollection getDeployFiles() {
		return _deployFiles;
	}

	@Input
	public Property<String> getEnvironment() {
		return _environment;
	}

	@Input
	@Optional
	public Property<String> getImage() {
		return _image;
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
	public Property<String> getTag() {
		return _tag;
	}

	@TaskAction
	public void publishOverlay() throws IOException {
		String image = _image.getOrNull();

		if (_push.get() && ((image == null) || image.isEmpty())) {
			throw new GradleException(
				"Set --image or " + _PROPERTY_PREFIX + "image to the " +
					"repository to publish to, such as " +
						"registry.example.com/acme/liferay-overlay, or set " +
							"--no-push to write a local OCI layout");
		}

		File configsDir = null;

		if (_configsDir.isPresent()) {
			Directory directory = _configsDir.get();

			configsDir = directory.getAsFile();
		}

		OverlayLayer overlayLayer = OverlayLayer.collect(
			_clientExtensionFiles, configsDir, _deployFiles,
			_environment.get());

		Logger logger = getLogger();

		for (String warning : overlayLayer.getWarnings()) {
			logger.warn(warning);
		}

		Directory outputDirectory = _outputDir.get();

		File outputDir = outputDirectory.getAsFile();

		outputDir.mkdirs();

		File tarFile = new File(outputDir, "layer.tar");

		overlayLayer.writeTar(tarFile.toPath());

		File layoutDir = new File(outputDir, "layout");

		OverlayPublisher overlayPublisher;

		if (_push.get()) {
			overlayPublisher = OverlayPublisher.forRegistry(_plainHTTP.get());
		}
		else {
			overlayPublisher = OverlayPublisher.forLayout(layoutDir.toPath());
		}

		String digest = overlayPublisher.publish(
			image, _tag.get(), tarFile.toPath());

		Map<String, Object> summary = new LinkedHashMap<>();

		summary.put("digest", digest);
		summary.put("environment", _environment.get());

		Map<String, File> files = overlayLayer.getFiles();

		Set<String> paths = files.keySet();

		summary.put("files", paths);

		Stream<String> stream = paths.stream();

		summary.put(
			"hotfix", stream.anyMatch(path -> path.startsWith("patching/")));

		if (_push.get()) {
			String reference = image + "@" + digest;

			summary.put("reference", reference);

			logger.lifecycle("Published {}:{}", image, _tag.get());
			logger.lifecycle(
				"Set overlay.artifacts[].reference to {}", reference);
		}
		else {
			logger.lifecycle(
				"Wrote the overlay to the OCI layout {}", layoutDir);
		}

		summary.put("warnings", overlayLayer.getWarnings());

		File summaryFile = new File(outputDir, "overlay.json");

		_objectMapper.writeValue(summaryFile, summary);

		logger.lifecycle("Wrote {}", summaryFile);
	}

	@Option(
		description = "The repository to publish the overlay to.",
		option = "image"
	)
	public void setImage(String image) {
		_image.set(image);
	}

	@Option(
		description = "Writes a local OCI layout instead of publishing.",
		option = "no-push"
	)
	public void setNoPush(boolean noPush) {
		_push.set(!noPush);
	}

	@Option(
		description = "Publishes to a registry over plain HTTP.",
		option = "plain-http"
	)
	public void setPlainHTTP(boolean plainHTTP) {
		_plainHTTP.set(plainHTTP);
	}

	@Option(description = "The tag to publish the overlay as.", option = "tag")
	public void setTag(String tag) {
		_tag.set(tag);
	}

	private Provider<String> _getGradleProperty(
		ProviderFactory providerFactory, String name, String defaultValue) {

		Provider<String> provider = providerFactory.gradleProperty(
			_PROPERTY_PREFIX + name);

		return provider.orElse(defaultValue);
	}

	private static final String _PROPERTY_PREFIX = "liferay.workspace.overlay.";

	private static final ObjectMapper _objectMapper = new ObjectMapper(
	).enable(
		SerializationFeature.INDENT_OUTPUT
	);

	private final ConfigurableFileCollection _clientExtensionFiles;
	private final DirectoryProperty _configsDir;
	private final ConfigurableFileCollection _deployFiles;
	private final Property<String> _environment;
	private final Property<String> _image;
	private final DirectoryProperty _outputDir;
	private final Property<Boolean> _plainHTTP;
	private final Property<Boolean> _push;
	private final Property<String> _tag;

}