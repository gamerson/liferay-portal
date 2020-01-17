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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.DependencySet;
import org.gradle.api.logging.Logger;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.options.Option;
import org.gradle.api.tasks.options.OptionValues;

import groovy.util.XmlSlurper;
import groovy.util.slurpersupport.GPathResult;

/**
 * @author Simon Jiang
 */
public class DependencyManagementTask extends DefaultTask {

	private String _outputFile = "";
	private OutputType _outputType = OutputType.text;
	 
	public DependencyManagementTask() {
		_project = getProject();

		_logger = _project.getLogger();
	}

	@Option(option = "output-file", description = "Set target file of saving target platform dependencyManagement.")
    public void setOutputFile(String outputFile) {
		_outputFile = outputFile;
    }

	@Option(option = "output-type", description = "Configures the output type.")
    public void setOutputType(OutputType outputType) {
		System.out.print("*********************************** " + _outputType);
		_outputType = outputType;
    }

	private static enum OutputType {
		json, xml, text
    }	
	
	@OptionValues("output-type")
    public List<OutputType> getAvailableOutputTypes() {
        return new ArrayList<OutputType>(Arrays.asList(OutputType.values()));
    }	
	
	public Configuration getBomsConfiguration() {
		return GradleUtil.getConfiguration(_project, "targetPlatformIDEBoms");
	}

	@TaskAction
	public void report() {
		_startProject(_project);

		Configuration bomsConfiguration = getBomsConfiguration();

		Map<String, String> managedVersions = _getTargetPlatformDependencies(
			_project, bomsConfiguration);

		_renderConfigurationManagedVersions(managedVersions, bomsConfiguration);
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

	private void _renderConfigurationManagedVersions(
		Map<String, String> managedVersions,
		final Configuration configuration) {

		_logger.lifecycle(
			configuration.getName() +
			" Dependency management for the " + configuration.getName() +
				" configuration");
		
		_logger.lifecycle("");
		
		if ((managedVersions != null) && !managedVersions.isEmpty()) {
			_renderManagedVersions(managedVersions);
		}
		else {
			_logger.lifecycle("No dependency management");
			_logger.lifecycle("");
		}

		_logger.lifecycle("");
	}

	private void _renderManagedVersions(Map<String, String> managedVersions) {
		Map<String, String> sortedVersions = new TreeMap<String, String>(
			new Comparator<String>() {

				@Override
				public int compare(String one, String two) {
					String[] oneComponents = one.split(":");
					String[] twoComponents = two.split(":");

					int result = oneComponents[0].compareTo(twoComponents[0]);

					if (result == 0) {
						result = oneComponents[1].compareTo(twoComponents[1]);
					}

					return result;
				}

			});

		sortedVersions.putAll(managedVersions);

		for (Map.Entry<String, String> entry : sortedVersions.entrySet()) {
			_logger.lifecycle("    " + entry.getKey() + " " + entry.getValue());
		}

		_logger.lifecycle("");
	}

	private void _startProject(final Project project) {
		_logger.lifecycle("");
		_logger.lifecycle(
			"------------------------------------------------------------");
		String heading;

		if (project.equals(project.getRootProject())) {
			heading = "Root project";
		}
		else {
			heading = "Project " + project.getPath();
		}

		if (project.getDescription() != null) {
			heading += " - " + project.getDescription();
		}

		_logger.lifecycle(heading);
		_logger.lifecycle(
			"------------------------------------------------------------");

		_logger.lifecycle("");
	}

	private final Logger _logger;
	private final Project _project;

}