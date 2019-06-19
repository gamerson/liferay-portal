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

package com.liferay.gradle.plugins.defaults.internal;

import com.liferay.gradle.plugins.BaseDefaultsPlugin;
import com.liferay.gradle.plugins.LiferayBasePlugin;
import com.liferay.gradle.plugins.defaults.internal.util.GradleUtil;
import com.liferay.gradle.plugins.defaults.tasks.CleanBuildProfileTask;
import com.liferay.gradle.plugins.defaults.tasks.SetBuildProfileTask;
import com.liferay.gradle.plugins.extensions.LiferayExtension;

import java.io.File;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.UnknownTaskException;
import org.gradle.api.tasks.TaskContainer;

/**
 * @author Andrea Di Giorgi
 * @author Gregory Amerson
 * @author Christopher Bryan Boyd
 */
public class LiferayBaseDefaultsPlugin
	extends BaseDefaultsPlugin<LiferayBasePlugin> {

	public static final Plugin<Project> INSTANCE =
		new LiferayBaseDefaultsPlugin();

	@Override
	public void apply(Project project) {
		super.apply(project);

		Project parentProject = project.getParent();

		TaskContainer taskContainer = parentProject.getTasks();

		if (taskContainer.findByPath(
				parentProject.getPath() + ":" +
					CleanBuildProfileTask.CLEAN_BUILD_PROFILE_TASK_NAME) ==
						null) {

			taskContainer.create(
				CleanBuildProfileTask.CLEAN_BUILD_PROFILE_TASK_NAME,
				CleanBuildProfileTask.class);
		}

		if (taskContainer.findByPath(
				parentProject.getPath() + ":" +
					SetBuildProfileTask.SET_BUILD_PROFILE_TASK_NAME) == null) {

			taskContainer.create(
				SetBuildProfileTask.SET_BUILD_PROFILE_TASK_NAME,
				SetBuildProfileTask.class);
		}

		GradleUtil.addTask(
			project, CleanBuildProfileTask.CLEAN_BUILD_PROFILE_TASK_NAME,
			CleanBuildProfileTask.class);

		GradleUtil.addTask(
			project, SetBuildProfileTask.SET_BUILD_PROFILE_TASK_NAME,
			SetBuildProfileTask.class);
	}

	@Override
	protected void configureDefaults(
		Project project, LiferayBasePlugin liferayBasePlugin) {

		project.afterEvaluate(
			new Action<Project>() {

				@Override
				public void execute(Project project) {
					_configureLiferayDeployDir(project);
					_configureBuildProfileTasks(project);
				}

			});
	}

	@Override
	protected Class<LiferayBasePlugin> getPluginClass() {
		return LiferayBasePlugin.class;
	}

	private LiferayBaseDefaultsPlugin() {
	}

	private void _configureBuildProfileTasks(Project project) {
		try {
			TaskContainer taskContainer = project.getTasks();

			Task cleanBuildProfile = taskContainer.getByPath(
				project.getPath() + ":" +
					CleanBuildProfileTask.CLEAN_BUILD_PROFILE_TASK_NAME);

			Task setBuildProfile = taskContainer.getByPath(
				project.getPath() + ":" +
					SetBuildProfileTask.SET_BUILD_PROFILE_TASK_NAME);

			setBuildProfile.dependsOn(cleanBuildProfile);
		}
		catch (UnknownTaskException ute) {
		}
	}

	private void _configureLiferayDeployDir(Project project) {
		File forcedDeployDir = GradleUtil.getProperty(
			project, "forced.deploy.dir", (File)null);

		if (forcedDeployDir != null) {
			LiferayExtension liferayExtension = GradleUtil.getExtension(
				project, LiferayExtension.class);

			liferayExtension.setDeployDir(forcedDeployDir);
		}
	}

}