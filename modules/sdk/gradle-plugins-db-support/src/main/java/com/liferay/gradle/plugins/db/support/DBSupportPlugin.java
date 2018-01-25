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

package com.liferay.gradle.plugins.db.support;

import com.liferay.gradle.plugins.db.support.internal.util.GradleUtil;
import com.liferay.gradle.plugins.db.support.tasks.BaseDBSupportTask;
import com.liferay.gradle.plugins.db.support.tasks.CleanServiceBuilderTask;

import java.io.File;

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Callable;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.DependencySet;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.internal.plugins.osgi.OsgiHelper;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.plugins.PluginContainer;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskContainer;

/**
 * @author Andrea Di Giorgi
 */
public class DBSupportPlugin implements Plugin<Project> {

	public static final String CLEAN_SERVICE_BUILDER_TASK_NAME =
		"cleanServiceBuilder";

	public static final String CONFIGURATION_NAME = "dbSupport";

	public static final String TOOL_CONFIGURATION_NAME = "dbSupportTool";

	@Override
	public void apply(Project project) {
		Configuration dbSupportConfiguration = _addConfigurationDBSupport(
			project);
		Configuration dbSupportToolConfiguration =
			_addConfigurationDBSupportTool(project);

		_addTaskCleanServiceBuilder(project);

		_configureTasksBaseDBSupport(
			project,
			project.files(dbSupportConfiguration, dbSupportToolConfiguration));
	}

	private Configuration _addConfigurationDBSupport(Project project) {
		Configuration configuration = GradleUtil.addConfiguration(
			project, CONFIGURATION_NAME);

		configuration.setDescription(
			"Configures the additional classpath of the DB Support tasks.");
		configuration.setVisible(false);

		return configuration;
	}

	private Configuration _addConfigurationDBSupportTool(
		final Project project) {

		Configuration configuration = GradleUtil.addConfiguration(
			project, TOOL_CONFIGURATION_NAME);

		configuration.defaultDependencies(
			new Action<DependencySet>() {

				@Override
				public void execute(DependencySet dependencySet) {
					_addDependenciesDBSupportTool(project);
				}

			});

		configuration.setDescription(
			"Configures Liferay DB Support for this project.");
		configuration.setVisible(false);

		return configuration;
	}

	private void _addDependenciesDBSupportTool(Project project) {
		GradleUtil.addDependency(
			project, TOOL_CONFIGURATION_NAME, "com.liferay",
			"com.liferay.portal.tools.db.support", "latest.release");
	}

	private CleanServiceBuilderTask _addTaskCleanServiceBuilder(
		final Project project) {

		final CleanServiceBuilderTask cleanServiceBuilderTask =
			GradleUtil.addTask(
				project, CLEAN_SERVICE_BUILDER_TASK_NAME,
				CleanServiceBuilderTask.class);

		cleanServiceBuilderTask.setDescription(
			"Cleans the Liferay database from the Service Builder tables and " +
				"rows of a module.");
		cleanServiceBuilderTask.setGroup(BasePlugin.BUILD_GROUP);

		cleanServiceBuilderTask.setServletContextName(
			new Callable<String>() {

				@Override
				public String call() throws Exception {
					PluginContainer pluginContainer = project.getPlugins();

					if (pluginContainer.hasPlugin(BasePlugin.class)) {
						return _osgiHelper.getBundleSymbolicName(project);
					}

					return null;
				}

			});

		cleanServiceBuilderTask.setServiceXmlFile(
			new Callable<File>() {

				@Override
				public File call() throws Exception {
					File resourcesDir = _getResourcesDir(
						cleanServiceBuilderTask.getProject());

					File defaultServiceXml = new File(
						resourcesDir, "META-INF/service.xml");

					if (defaultServiceXml.exists()) {
						return defaultServiceXml;
					}

					return new File("service.xml");
				}

			});

		return cleanServiceBuilderTask;
	}

	private void _configureTasksBaseDBSupport(
		Project project, final FileCollection classpath) {

		TaskContainer taskContainer = project.getTasks();

		taskContainer.withType(
			BaseDBSupportTask.class,
			new Action<BaseDBSupportTask>() {

				@Override
				public void execute(BaseDBSupportTask baseDBSupportTask) {
					baseDBSupportTask.setClasspath(classpath);
				}

			});
	}

	private File _getResourcesDir(Project project) {
		SourceSet sourceSet = GradleUtil.getSourceSet(
			project, SourceSet.MAIN_SOURCE_SET_NAME);

		return _getSrcDir(sourceSet.getResources());
	}

	private File _getSrcDir(SourceDirectorySet sourceDirectorySet) {
		Set<File> srcDirs = sourceDirectorySet.getSrcDirs();

		Iterator<File> iterator = srcDirs.iterator();

		return iterator.next();
	}

	private static final OsgiHelper _osgiHelper = new OsgiHelper();

}