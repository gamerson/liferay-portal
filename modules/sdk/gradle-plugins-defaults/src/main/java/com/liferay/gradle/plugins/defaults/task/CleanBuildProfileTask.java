/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.gradle.plugins.defaults.tasks;

import java.io.File;
import java.io.IOException;

import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import java.util.Map;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;
import org.gradle.api.tasks.TaskAction;

/**
 * @author Gregory Amerson
 */
public class CleanBuildProfileTask extends DefaultTask {

	@TaskAction
	public void cleanBuildProfile() throws Exception {
		Project project = getProject();

		String profileName = project.getName();

		Map<String, ?> properties = project.getProperties();

		Object value = properties.get("profileName");

		if (value instanceof String) {
			profileName = (String)value;
		}

		Logger logger = getLogger();

		logger.lifecycle("Cleaning build profile name: {}", profileName);

		_cleanBuildProfile(profileName);
	}

	private void _cleanBuildProfile(String profileName) {
		Logger logger = getLogger();

		Project project = getProject();

		Project rootProject = project.getRootProject();

		File rootProjectDir = rootProject.getProjectDir();

		try {
			Files.walkFileTree(
				rootProjectDir.toPath(),
				new SimpleFileVisitor<Path>() {

					@Override
					public FileVisitResult preVisitDirectory(
							Path path, BasicFileAttributes baseFileAttributes)
						throws IOException {

						Path buildProfilePath = path.resolve(
							".lfrbuild-" + profileName);

						if (Files.exists(buildProfilePath)) {
							Files.delete(buildProfilePath);

							logger.lifecycle("Cleaned {}", buildProfilePath);

							return FileVisitResult.SKIP_SUBTREE;
						}

						if (Files.exists(path.resolve("bnd.bnd")) ||
							Files.exists(path.resolve("build")) ||
							Files.exists(path.resolve("build.xml")) ||
							Files.exists(path.resolve("gulpfile.js")) ||
							Files.exists(path.resolve("node_modules")) ||
							Files.exists(path.resolve("node_modules_cache"))) {

							return FileVisitResult.SKIP_SUBTREE;
						}

						return FileVisitResult.CONTINUE;
					}

				});
		}
		catch (IOException ioe) {
			throw new GradleException("Error deleting build profile", ioe);
		}
	}

}