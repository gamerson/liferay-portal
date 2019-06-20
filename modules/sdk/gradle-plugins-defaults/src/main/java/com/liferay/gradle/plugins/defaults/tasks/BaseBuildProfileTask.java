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

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.gradle.StartParameter;
import org.gradle.api.DefaultTask;
import org.gradle.api.DomainObjectSet;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.DependencySet;
import org.gradle.api.artifacts.ProjectDependency;
import org.gradle.api.artifacts.ResolvedConfiguration;
import org.gradle.api.invocation.Gradle;

/**
 * @author Gregory Amerson
 */
public abstract class BaseBuildProfileTask extends DefaultTask {

	public static final String BUILD_PROFILE_NAME_PROPERTY_NAME =
		"build.profile.name";

	public static final String BUILD_PROFILES_FILENAME = ".lfrbuild-profiles";

	protected String getDefaultProfileName(Project project) {
		Gradle gradle = project.getGradle();

		StartParameter startParameter = gradle.getStartParameter();

		File currentDir = startParameter.getCurrentDir();

		return project.getRootProject(
		).getAllprojects(
		).stream(
		).filter(
			p -> Objects.equals(p.getProjectDir(), currentDir)
		).findFirst(
		).map(
			Project::getPath
		).get();
	}

	protected Collection<Project> getDependencyProjects(Project project) {
		Set<Project> projects = new LinkedHashSet<>();

		projects.add(project);

		Set<ConfigurationContainer> configurationContainers = new HashSet<>();

		Map<String, Project> childProjects = project.getChildProjects();

		for (Project childProject : childProjects.values()) {
			projects.add(childProject);

			configurationContainers.add(childProject.getConfigurations());
		}

		ConfigurationContainer configurationContainer =
			project.getConfigurations();

		configurationContainers.add(configurationContainer);

		configurationContainers.stream(
		).flatMap(
			Set::stream
		).forEach(
			c -> _collectDependencyProjects(projects, c)
		);

		return projects;
	}

	protected static final String SOURCE_PROJECT_PATH_KEY =
		"source.project.path";

	private void _collectDependencyProjects(
		Collection<Project> dependencyProjects, Configuration configuration) {

		DependencySet dependencySet = configuration.getDependencies();

		DomainObjectSet<ProjectDependency> projectDependencies =
			dependencySet.withType(ProjectDependency.class);

		if (!projectDependencies.isEmpty()) {
			ResolvedConfiguration resolvedConfiguration =
				configuration.getResolvedConfiguration();

			resolvedConfiguration.getFirstLevelModuleDependencies();

			projectDependencies.stream(
			).map(
				ProjectDependency::getDependencyProject
			).map(
				this::getDependencyProjects
			).flatMap(
				Collection::stream
			).forEach(
				dependencyProjects::add
			);
		}
	}

}