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
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Scanner;
import java.util.Set;
import org.gradle.api.DefaultTask;
import org.gradle.api.DomainObjectSet;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
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
public class SetBuildProfileTask extends DefaultTask {

	public static final String LFRBUILD_PROFILES_FILENAME = ".lfrbuild-profiles";

	public static final String BUILD_PROFILE_KEY = "build.profile";

	public static final String _SOURCE_PROJECT_PATH_KEY = "source.project.path";

	public static final Path _CURRENT_WORKING_PATH = Paths.get(System.getProperty("user.dir"));

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
	public void setBuildProfile() throws Exception {
		
		Project project = getProject();
		
		Project sourceProject = getSourceProject();
		

		String profileName = System.getProperty(BUILD_PROFILE_KEY);
		
		if (profileName == null) {
			profileName = sourceProject.getPath();
			
			System.setProperty(BUILD_PROFILE_KEY, profileName);
		}

		Logger logger = getLogger();

		logger.lifecycle("Setting build profile name: {}", profileName);
		
		Collection<Project> projectDependencies = _getProjectDependencies(project);

		for (Project projectDependency : projectDependencies) {
			File lfrBuildFile = new File(projectDependency.getProjectDir(), LFRBUILD_PROFILES_FILENAME);
			
			try {

				_processBuildFile(profileName, logger, lfrBuildFile);
			 
			} catch (IOException ioe) {
				throw new GradleException(
					"Unable to create build profile custom marker:" +
						lfrBuildFile.getAbsolutePath(),
					ioe);
			}
		}
		
		logger.lifecycle("Build profile " + profileName + " created successfully.");
		logger.lifecycle("To import or use this profile, please use the following argument: ");
		logger.lifecycle("-Dbuild.profile="+profileName);
	}

	private static void _processBuildFile(String profileName, Logger logger, File lfrBuildFile)
			throws FileNotFoundException, IOException {
		
		ArrayList<String> list = new ArrayList<String>();
		
		list.add(profileName);
		
		if (lfrBuildFile.exists()) {

			logger.lifecycle("Reading {}", lfrBuildFile);
			try (Scanner s = new Scanner(lfrBuildFile)) {
				while (s.hasNext()){
					String foundProfileName = s.next();
					if (!list.contains(foundProfileName)) {

					    list.add(foundProfileName);
					}
				}
			}
			lfrBuildFile.delete();
		}
		
		
		try (FileWriter fw = new FileWriter(lfrBuildFile)) {
		 
			for (String foundProfileName : list) {
				fw.write(foundProfileName + System.lineSeparator());
			}
		}
	}

	private Collection<Project> _getProjectDependencies(Project project) {
		Collection<Project> projects = new LinkedHashSet<>();

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
				_processConfiguration(projects, c);
			}
		);

		return projects;
	}

	private void _processConfiguration(Collection<Project> projects, Configuration c) {
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
					Collection<Project> pds = _getProjectDependencies(pd);

					return pds.stream();
				}
			).forEach(
				projects::add
			);
		}
	}

}