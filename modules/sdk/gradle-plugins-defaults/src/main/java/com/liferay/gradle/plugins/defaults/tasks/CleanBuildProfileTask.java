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

import com.liferay.gradle.plugins.defaults.internal.util.GradlePluginsDefaultsUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;

import java.nio.file.Path;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.gradle.api.DefaultTask;
import org.gradle.api.DomainObjectSet;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.DependencySet;
import org.gradle.api.artifacts.ProjectDependency;
import org.gradle.api.artifacts.ResolvedConfiguration;
import org.gradle.api.logging.Logger;
import org.gradle.api.tasks.TaskAction;

/**
 * @author Christopher Bryan Boyd
 * @author Gregory Amerson
 */
public class CleanBuildProfileTask extends DefaultTask {

	public static final String CLEAN_BUILD_PROFILE_TASK_NAME =
		"cleanBuildProfile";

	@TaskAction
	public void cleanBuildProfile() throws Exception {
		Project project = getProject();

		Project sourceProject = _getSourceProject();

		String profileName = System.getProperty(
			SetBuildProfileTask.BUILD_PROFILE_NAME_PROP_KEY);

		if ((profileName == null) || profileName.isEmpty()) {
			profileName = sourceProject.getPath();

			System.setProperty(
				SetBuildProfileTask.BUILD_PROFILE_NAME_PROP_KEY, profileName);
		}
		else if (profileName.contains(",") || profileName.contains(":")) {
			StringBuilder sb = new StringBuilder();

			sb.append(SetBuildProfileTask.BUILD_PROFILE_NAME_PROP_KEY);
			sb.append("'" + profileName + "' is invalid. ");
			sb.append(System.lineSeparator());
			sb.append("Profile name cannot contain ',' or ':'.");
			sb.append(System.lineSeparator());

			throw new IllegalArgumentException(sb.toString());
		}

		Logger logger = getLogger();

		logger.lifecycle(
			"Cleaning " + SetBuildProfileTask.BUILD_PROFILE_NAME_PROP_KEY +
				": {}",
			profileName);

		Set<Project> projectDependencies = _getProjectDependencies(project);

		for (Project projectDependency : projectDependencies) {
			File lfrBuildFile = new File(
				projectDependency.getProjectDir(),
				GradlePluginsDefaultsUtil.BUILD_PROFILE_FILE_NAME);

			try {
				_processBuildFile(profileName, logger, lfrBuildFile);
			}
			catch (IOException ioe) {
				StringBuilder sb = new StringBuilder();

				sb.append("Error cleaning ");
				sb.append(SetBuildProfileTask.BUILD_PROFILE_NAME_PROP_KEY);
				sb.append(" specified as ");
				sb.append(profileName);
				sb.append(" in ");
				sb.append(lfrBuildFile.getAbsolutePath());

				String message = sb.toString();

				throw new GradleException(message, ioe);
			}
		}
	}

	private Set<Project> _getProjectDependencies(Project project) {
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
			c -> {
				DependencySet dependencySet = c.getDependencies();

				DomainObjectSet<ProjectDependency> projectDependencies =
					dependencySet.withType(ProjectDependency.class);

				if (!projectDependencies.isEmpty()) {
					ResolvedConfiguration rc = c.getResolvedConfiguration();

					rc.getFirstLevelModuleDependencies();

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

	private Project _getSourceProject() {
		String sourceProjectName = System.getProperty(
			SetBuildProfileTask.SOURCE_PROJECT_PATH_KEY);

		Project currentProject = getProject();
		Project foundProject = null;

		if (sourceProjectName == null) {
			Project rootProject = currentProject.getRootProject();

			Collection<Project> allProjects = rootProject.getAllprojects();

			for (Project subproject : allProjects) {
				File subprojectDir = subproject.getProjectDir();

				Path subprojectPath = subprojectDir.toPath();

				if (subprojectPath.equals(
						SetBuildProfileTask.CURRENT_WORKING_PATH)) {

					foundProject = subproject;

					break;
				}
			}

			if (foundProject != null) {
				System.setProperty(
					SetBuildProfileTask.SOURCE_PROJECT_PATH_KEY,
					foundProject.getPath());
			}
		}
		else {
			foundProject = currentProject.findProject(sourceProjectName);
		}

		return foundProject;
	}

	private void _processBuildFile(
			String profileName, Logger logger, File lfrBuildFile)
		throws FileNotFoundException, IOException {

		ArrayList<String> list = new ArrayList<>();

		if (lfrBuildFile.exists()) {
			try (Scanner s = new Scanner(lfrBuildFile)) {
				while (s.hasNext()) {
					String foundProfileName = s.next();

					if (!foundProfileName.isEmpty() &&
						!list.contains(foundProfileName)) {

						list.add(foundProfileName);
					}
				}
			}
		}

		if (list.contains(profileName)) {
			logger.lifecycle(
				"Removing " + profileName + " from {}", lfrBuildFile);
			list.remove(profileName);
			lfrBuildFile.delete();

			try (FileWriter fw = new FileWriter(lfrBuildFile)) {
				for (String foundProfileName : list) {
					fw.write(foundProfileName + System.lineSeparator());
				}
			}
		}
		else if (list.isEmpty()) {
			logger.lifecycle(
				"Deleting empty marker file {}",
				lfrBuildFile.getAbsolutePath());

			lfrBuildFile.delete();
		}
	}

}