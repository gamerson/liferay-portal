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

import com.liferay.gradle.plugins.defaults.LiferaySettingsPlugin;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Scanner;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;
import org.gradle.api.tasks.TaskAction;

/**
 * @author Gregory Amerson
 * @author Christopher Bryan Boyd
 */
public class SetBuildProfileTask extends BaseBuildProfileTask {

	public static final String SET_BUILD_PROFILE_TASK_NAME = "setBuildProfile";

	@TaskAction
	public void setBuildProfile() throws Exception {
		Project project = getProject();

		String profileName = System.getProperty(
			BUILD_PROFILE_NAME_PROPERTY_NAME, "");

		if (profileName.isEmpty()) {
			profileName = getDefaultProfileName(project);
		}
		else if (profileName.contains(",") || profileName.contains(":")) {
			StringBuilder sb = new StringBuilder();

			sb.append(BUILD_PROFILE_NAME_PROPERTY_NAME);
			sb.append("'" + profileName + "' is invalid. ");
			sb.append(System.lineSeparator());
			sb.append("Profile name cannot contain ',' or ':'.");
			sb.append(System.lineSeparator());

			throw new IllegalArgumentException(sb.toString());
		}

		Logger logger = getLogger();

		logger.lifecycle("Setting build profile name: {}", profileName);

		Collection<Project> projectDependencies = getDependencyProjects(
			project);

		for (Project projectDependency : projectDependencies) {
			File buildProfilesFile = new File(
				projectDependency.getProjectDir(), BUILD_PROFILES_FILENAME);

			try {
				_processBuildFile(profileName, logger, buildProfilesFile);
			}
			catch (IOException ioe) {
				throw new GradleException(
					"Unable to create build profiles marker:" +
						buildProfilesFile.getAbsolutePath(),
					ioe);
			}
		}

		StringBuilder sb = new StringBuilder();

		sb.append("Build profile " + profileName + " created successfully.");
		sb.append(System.lineSeparator());
		sb.append("To import or use this profile, ");
		sb.append("please use the following argument: ");
		sb.append(System.lineSeparator());
		sb.append("-D");
		sb.append(LiferaySettingsPlugin.BUILD_PROFILE_PROPERTY_NAME);
		sb.append("=" + profileName);

		if (profileName.contains(":")) {
			String[] path = profileName.split(":");

			String simpleProfileName = path[path.length - 1];

			sb.append(System.lineSeparator());
			sb.append("The value can also be specified as: ");
			sb.append(System.lineSeparator());
			sb.append("-D");
			sb.append(LiferaySettingsPlugin.BUILD_PROFILE_PROPERTY_NAME);
			sb.append("=" + simpleProfileName);
		}

		sb.append(System.lineSeparator());

		sb.append(
			"The value may be comma separated to specify multiple profiles.");

		String message = sb.toString();

		logger.lifecycle(message);
	}

	private static void _processBuildFile(
			String profileName, Logger logger, File buildProfilesFile)
		throws FileNotFoundException, IOException {

		ArrayList<String> list = new ArrayList<>();

		boolean missingProfile = false;

		if (buildProfilesFile.exists()) {
			logger.lifecycle("Reading {}", buildProfilesFile);

			try (Scanner s = new Scanner(buildProfilesFile)) {
				while (s.hasNext()) {
					String foundProfileName = s.next();

					if (!foundProfileName.isEmpty()) {
						if (!list.contains(foundProfileName)) {
							list.add(foundProfileName);
						}
					}
				}
			}

			missingProfile = !list.contains(profileName);

			if (missingProfile) {
				buildProfilesFile.delete();
			}
		}
		else {
			missingProfile = true;
		}

		if (missingProfile) {
			list.add(profileName);
		}

		if (missingProfile) {
			boolean buildFileExists = buildProfilesFile.exists();

			try (FileWriter fileWriter = new FileWriter(
					buildProfilesFile, buildFileExists)) {

				if (buildFileExists) {
					fileWriter.write(System.lineSeparator());
				}

				for (String foundProfileName : list) {
					fileWriter.write(foundProfileName + System.lineSeparator());
				}
			}
		}
	}

}