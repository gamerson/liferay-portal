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

import static com.liferay.gradle.plugins.defaults.tasks.SetBuildProfileTask.*;

/**
 * @author Christopher Bryan Boyd
 * @author Gregory Amerson
 */
public class CleanBuildProfileTask extends DefaultTask {

	private Project getSourceProject() {
		String sourceProjectName = System.getProperty(_SOURCE_PROJECT_PATH_KEY);
		
		Project currentProject = getProject();
		Project foundProject = null;
		if (sourceProjectName == null) {
			
			Collection<Project> allProjects = currentProject.getRootProject().getAllprojects();
			
			
			for (Project subProject : allProjects) {
				
				Path subProjectPath = subProject.getProjectDir().toPath();
				if (subProjectPath.equals(_CURRENT_WORKING_PATH)) {
					foundProject = subProject;
					break;
				}
			}
			if (foundProject != null) {
				System.setProperty(_SOURCE_PROJECT_PATH_KEY, foundProject.getPath());
			}
		}
		else {
			foundProject = currentProject.findProject(sourceProjectName);
		}
		return foundProject;
	}
	
	@TaskAction
	public void cleanBuildProfile() throws Exception {
		Project project = getProject();
		
		Project sourceProject = getSourceProject();
		
		String profileName 
		= sourceProject.getPath();

		Map<String, ?> properties = sourceProject.getProperties();

		Object value = properties.get("profileName");

		if (value instanceof String) {
			profileName = (String)value;
		}

		Logger logger = getLogger();

		logger.lifecycle("Cleaning build profile name: {}", profileName);

		Set<Project> projectDependencies = _getProjectDependencies(project);

		for (Project projectDependency : projectDependencies) {
			File lfrBuildFile = new File(projectDependency.getProjectDir(), ".lfrbuild-profiles");
			
			try {

				_processBuildFile(profileName, logger, lfrBuildFile);
			} 
			catch (IOException ioe) {
				throw new GradleException(
					"Error cleaning build profile " + profileName + " in " +
						lfrBuildFile.getAbsolutePath(),
					ioe);
			}
		}
	}

	private void _processBuildFile(String profileName, Logger logger, File lfrBuildFile)
			throws FileNotFoundException, IOException {
		ArrayList<String> list = new ArrayList<String>();
		if (lfrBuildFile.exists()) {

			logger.lifecycle("Removing " + profileName + " from {}", lfrBuildFile);
			try (Scanner s = new Scanner(lfrBuildFile)) {
				while (s.hasNext()){
					String foundProfileName = s.next();

					list.add(foundProfileName);
				}
			}
		}
		
		if (list.contains(profileName)) {
			list.remove(profileName);
			lfrBuildFile.delete();
			
			try (FileWriter fw = new FileWriter(lfrBuildFile)) {
			
				for (String foundProfileName : list) {
					fw.write(foundProfileName + System.lineSeparator());
				}
			}
		}
		else if (list.isEmpty()) {

			logger.lifecycle("Deleting {}", lfrBuildFile.getAbsolutePath());

			lfrBuildFile.delete();
		}
	}

	private Set<Project> _getProjectDependencies(Project project) {
		Set<Project> projects = new LinkedHashSet<>();

		projects.add(project);
		
		Set<ConfigurationContainer> configurationContainers = new HashSet<>();

		for (Project childProject: project.getChildProjects().values()) {
			projects.add(childProject);
			
			configurationContainers.add(childProject.getConfigurations());
		}

		ConfigurationContainer configurationContainer =
			project.getConfigurations();
		

		configurationContainers.add(configurationContainer);
		
		configurationContainers.stream()
		.flatMap(
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


}