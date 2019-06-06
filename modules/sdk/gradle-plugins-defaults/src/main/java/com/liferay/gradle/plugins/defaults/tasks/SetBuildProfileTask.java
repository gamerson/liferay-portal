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

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.gradle.api.DefaultTask;
import org.gradle.api.DomainObjectSet;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.DependencySet;
import org.gradle.api.artifacts.ProjectDependency;
import org.gradle.api.artifacts.ResolvedConfiguration;
import org.gradle.api.artifacts.ResolvedDependency;
import org.gradle.api.logging.Logger;
import org.gradle.api.tasks.TaskAction;

/**
 * @author Gregory Amerson
 */
public class SetBuildProfileTask extends DefaultTask {

	@TaskAction
	public void setBuildProfile() throws Exception {
		Project project = getProject();

		String profileName = project.getName();

		Map<String, ?> properties = project.getProperties();

		Object value = properties.get("profileName");

		if (value instanceof String) {
			profileName = (String)value;
		}

		Logger logger = getLogger();

		logger.lifecycle("Setting build profile name: {}", profileName);

		_setBuildProfile(profileName);
	}

	private void _createNewFile(File file) {
		try {
			file.createNewFile();

			Logger logger = getLogger();

			logger.lifecycle("Created {}", file);
		}
		catch (IOException ioe) {
			throw new GradleException(
				"Unable to create build profile custom marker:" +
					file.getAbsolutePath(),
				ioe);
		}
	}

	private Set<Project> _getProjectDependencies(Project project) {
		Set<Project> projects = new LinkedHashSet<>();

		projects.add(project);

		ConfigurationContainer configurationContainer =
			project.getConfigurations();

		configurationContainer.stream(
		).forEach(
			c -> {
				DependencySet dependencySet = c.getDependencies();

				DomainObjectSet<ProjectDependency> projectDependencies =
					dependencySet.withType(ProjectDependency.class);

				if (!projectDependencies.isEmpty()) {
					ResolvedConfiguration rc = c.getResolvedConfiguration();

					Set<ResolvedDependency> resolvedDeps =
						rc.getFirstLevelModuleDependencies();

					System.out.println(resolvedDeps.size());

					projectDependencies.stream(
					).map(
						ProjectDependency::getDependencyProject
					).flatMap(
						pd -> {
							Set<Project> pds = _getProjectDependencies(pd);

							return pds.stream();
						}
					).forEach(
						projects::add
					);
				}
			}
		);

		return projects;
	}

	private void _setBuildProfile(String profileName) {
		Project project = getProject();

		Set<Project> allProjects = new LinkedHashSet<>(
			project.getAllprojects());

		allProjects.stream(
		).map(
			this::_getProjectDependencies
		).flatMap(
			Set::stream
		).map(
			Project::getProjectDir
		).filter(
			File::exists
		).map(
			file -> new File(file, "/.lfrbuild-" + profileName)
		).forEach(
			this::_createNewFile
		);
	}

}