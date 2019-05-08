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

package com.liferay.gradle.plugins.workspace.configurators;

import com.liferay.gradle.plugins.LiferayFrontendPlugin;
import com.liferay.gradle.plugins.workspace.WorkspaceExtension;
import com.liferay.gradle.plugins.workspace.internal.util.GradleUtil;

import groovy.json.JsonSlurper;

import groovy.lang.Closure;

import java.io.File;
import java.io.IOException;

import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.CopySpec;
import org.gradle.api.initialization.Settings;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.tasks.Copy;

/**
 * @author Gregory Amerson
 */
public class AppsProjectConfigurator extends BaseProjectConfigurator {

	public AppsProjectConfigurator(Settings settings) {
		super(settings);
	}

	@Override
	public void apply(Project project) {
		WorkspaceExtension workspaceExtension = GradleUtil.getExtension(
			(ExtensionAware)project.getGradle(), WorkspaceExtension.class);

		GradleUtil.applyPlugin(project, LiferayFrontendPlugin.class);

		Task assembleTask = GradleUtil.getTask(
			project, BasePlugin.ASSEMBLE_TASK_NAME);

		_configureRootTaskDistBundle(assembleTask);

		addTaskDockerDeploy(
			project, _copyJarClosure(project, assembleTask),
			workspaceExtension);
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	protected Iterable<File> doGetProjectDirs(File rootDir) throws Exception {
		final Set<File> projectDirs = new HashSet<>();

		Files.walkFileTree(
			rootDir.toPath(),
			new SimpleFileVisitor<Path>() {

				@Override
				@SuppressWarnings("unchecked")
				public FileVisitResult preVisitDirectory(
						Path dirPath, BasicFileAttributes basicFileAttributes)
					throws IOException {

					Path dirNamePath = dirPath.getFileName();

					String dirName = dirNamePath.toString();

					if (dirName.equals("build") || dirName.equals("dist") ||
						dirName.equals("node_modules") ||
						dirName.equals("node_modules_cache")) {

						return FileVisitResult.SKIP_SUBTREE;
					}

					if (Files.exists(dirPath.resolve("package.json"))) {
						Map<String, Object> packageJsonMap = _getPackageJsonMap(
							dirPath.toFile());

						Map<String, Object> scripts =
							(Map<String, Object>)packageJsonMap.get("scripts");

						if ((scripts != null) &&
							(scripts.get("build") != null)) {

							projectDirs.add(dirPath.toFile());
						}

						return FileVisitResult.SKIP_SUBTREE;
					}

					return FileVisitResult.CONTINUE;
				}

			});

		return projectDirs;
	}

	protected static final String NAME = "apps";

	private void _configureRootTaskDistBundle(final Task assembleTask) {
		Project project = assembleTask.getProject();

		Copy copy = (Copy)GradleUtil.getTask(
			project.getRootProject(),
			RootProjectConfigurator.DIST_BUNDLE_TASK_NAME);

		copy.dependsOn(assembleTask);

		copy.into("osgi/modules", _copyJarClosure(project, assembleTask));
	}

	@SuppressWarnings({"rawtypes", "serial", "unused"})
	private Closure _copyJarClosure(Project project, final Task assembleTask) {
		return new Closure<Void>(project) {

			public void doCall(CopySpec copySpec) {
				Project project = assembleTask.getProject();

				File jarFile = _getJarFile(project);

				ConfigurableFileCollection configurableFileCollection =
					project.files(jarFile);

				configurableFileCollection.builtBy(assembleTask);

				copySpec.from(jarFile);
			}

		};
	}

	private File _getJarFile(Project project) {
		return project.file(
			"dist/" + GradleUtil.getArchivesBaseName(project) + "-" +
				project.getVersion() + ".jar");
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> _getPackageJsonMap(File dir) {
		File file = new File(dir, "package.json");

		if (!file.exists()) {
			return Collections.emptyMap();
		}

		JsonSlurper jsonSlurper = new JsonSlurper();

		return (Map<String, Object>)jsonSlurper.parse(file);
	}

}