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

package com.liferay.gradle.plugins.target.platform.tasks;

import com.google.common.collect.Maps;

import com.liferay.gradle.plugins.target.platform.internal.util.GradleUtil;

import groovy.util.XmlSlurper;
import groovy.util.slurpersupport.GPathResult;

import java.io.File;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.DependencySet;
import org.gradle.api.logging.Logger;
import org.gradle.api.tasks.TaskAction;

/**
 * @author Simon Jiang
 */
public class TargetPlatformManagementTask extends DefaultTask {

	public TargetPlatformManagementTask() {
	}

	public Configuration getBomsConfiguration() {
		return GradleUtil.getConfiguration(
			getProject(), "targetPlatformIDEBoms");
	}

	@TaskAction
	public void report() {
		this._renderer.startProject(getProject());

		Configuration bomsConfiguration = getBomsConfiguration();

		Map<String, String> managedVersions = _getTargetPlatformDependencies(
			getProject(), bomsConfiguration);

		this._renderer.renderConfigurationManagedVersions(
			managedVersions, bomsConfiguration);
	}

	private Map<String, String> _getTargetPlatformDependencies(
		Project project, Configuration ideBomsConfiguration) {

		Map<String, String> managedVersions = Maps.newHashMap();
		DependencySet allDependencies = ideBomsConfiguration.getDependencies();

		allDependencies.all(
			new Action<Dependency>() {

				@Override
				public void execute(Dependency dependency) {
					if (ideBomsConfiguration.isCanBeResolved()) {
						Set<File> files = ideBomsConfiguration.files(
							dependency);

						for (File file : files) {
							try {
								XmlSlurper xmlSlurper = new XmlSlurper();

								GPathResult gPathResult = xmlSlurper.parse(
									file);

								gPathResult =
									(GPathResult)gPathResult.getProperty(
										"dependencyManagement");

								gPathResult =
									(GPathResult)gPathResult.getProperty(
										"dependencies");

								gPathResult =
									(GPathResult)gPathResult.getProperty(
										"dependency");

								Iterator<?> iterator = gPathResult.iterator();

								while (iterator.hasNext()) {
									gPathResult = (GPathResult)iterator.next();

									String groupId = String.valueOf(
										gPathResult.getProperty("groupId"));
									String artifactId = String.valueOf(
										gPathResult.getProperty("artifactId"));
									String version = String.valueOf(
										gPathResult.getProperty("version"));

									managedVersions.put(
										groupId + ":" + artifactId, version);
								}
							}
							catch (Exception e) {
								Logger logger = project.getLogger();

								if (logger.isWarnEnabled()) {
									logger.warn(
										"Unable to parse BOM from {}", file);
								}
							}
						}
					}
				}

			});

		return managedVersions;
	}

	private DependencyManagementReportRenderer _renderer =
		new DependencyManagementReportRenderer();

}