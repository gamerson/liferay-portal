/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.gradle.plugins.workspace.task;

import com.bmuschko.gradle.docker.tasks.image.DockerPushImage;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.InspectImageCmd;
import com.github.dockerjava.api.command.InspectImageResponse;
import com.github.dockerjava.api.command.PushImageCmd;
import com.github.dockerjava.api.command.TagImageCmd;
import com.github.dockerjava.api.model.AuthConfig;
import com.github.dockerjava.api.model.PushResponseItem;
import com.github.dockerjava.api.model.ResponseItem;

import com.liferay.gradle.plugins.workspace.internal.client.extension.ClientExtensionDescriptor;

import java.io.File;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.logging.Logger;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskOutputs;

/**
 * @author Gregory Amerson
 */
public class PushClientExtensionImageTask extends DockerPushImage {

	public PushClientExtensionImageTask() {
		Project project = getProject();

		ObjectFactory objectFactory = project.getObjects();

		_imageDigestFile = objectFactory.fileProperty();

		ProjectLayout projectLayout = project.getLayout();

		DirectoryProperty buildDirectoryProperty =
			projectLayout.getBuildDirectory();

		_imageDigestFile.convention(
			buildDirectoryProperty.file("client-extension-image/digest"));

		TaskOutputs taskOutputs = getOutputs();

		taskOutputs.upToDateWhen(task -> false);
	}

	@OutputFile
	public RegularFileProperty getImageDigestFile() {
		return _imageDigestFile;
	}

	@Override
	public void runRemoteCommand() throws Exception {
		SetProperty<String> imagesSetProperty = getImages();

		Set<String> images = imagesSetProperty.get();

		if (images.isEmpty()) {
			throw new GradleException(
				"Set the liferay.workspace.client.extension.registry " +
					"property and a version to publish \"" +
						getProject().getName() + "\"");
		}

		super.runRemoteCommand();

		Iterator<String> iterator = images.iterator();

		String image = iterator.next();

		String repository = ClientExtensionDescriptor.getRepository(image);

		String digest = _getDigest(image, repository);

		RegularFile regularFile = _imageDigestFile.get();

		File file = regularFile.getAsFile();

		File parentFile = file.getParentFile();

		parentFile.mkdirs();

		Files.write(file.toPath(), digest.getBytes(StandardCharsets.UTF_8));

		// The version tag can be pushed again with a new build, so the same
		// image is also pushed under a tag that ends in its digest, which a
		// liferay.yaml entry can name to keep exactly this build.

		String digestTag =
			image.substring(repository.length() + 1) + "." +
				digest.substring(digest.indexOf(':') + 1).substring(0, 12);

		DockerClient dockerClient = getDockerClient();

		TagImageCmd tagImageCmd = dockerClient.tagImageCmd(
			image, repository, digestTag);

		tagImageCmd.exec();

		String digestImage = repository + ":" + digestTag;

		_pushImage(digestImage);

		Logger logger = getLogger();

		logger.lifecycle("Pushed {}@{}", image, digest);
		logger.lifecycle("Pushed {}@{}", digestImage, digest);
	}

	private String _getDigest(String image, String imageRepository) {
		DockerClient dockerClient = getDockerClient();

		InspectImageCmd inspectImageCmd = dockerClient.inspectImageCmd(image);

		InspectImageResponse inspectImageResponse = inspectImageCmd.exec();

		List<String> repoDigests = inspectImageResponse.getRepoDigests();

		if (repoDigests != null) {
			for (String repoDigest : repoDigests) {
				if (repoDigest.startsWith(imageRepository + "@")) {
					return repoDigest.substring(imageRepository.length() + 1);
				}
			}
		}

		throw new GradleException(
			"Unable to find the digest of \"" + image + "\" after pushing it");
	}

	private void _pushImage(String image) throws Exception {
		AuthConfig authConfig = getRegistryAuthLocator().lookupAuthConfig(
			image, getRegistryCredentials());

		DockerClient dockerClient = getDockerClient();

		PushImageCmd pushImageCmd = dockerClient.pushImageCmd(image);

		pushImageCmd.withAuthConfig(authConfig);

		pushImageCmd.exec(
			new ResultCallback.Adapter<PushResponseItem>() {

				@Override
				public void onNext(PushResponseItem pushResponseItem) {
					if (pushResponseItem.isErrorIndicated()) {
						ResponseItem.ErrorDetail errorDetail =
							pushResponseItem.getErrorDetail();

						String message = pushResponseItem.getError();

						if (errorDetail != null) {
							message = errorDetail.getMessage();
						}

						throw new GradleException(
							"Unable to push \"" + image + "\": " + message);
					}
				}

			}
		).awaitCompletion();
	}

	private final RegularFileProperty _imageDigestFile;

}