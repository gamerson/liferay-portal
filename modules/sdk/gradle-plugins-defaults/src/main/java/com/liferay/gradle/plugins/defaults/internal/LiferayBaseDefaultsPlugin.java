/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
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

/**
 * @author Andrea Di Giorgi
 * @author Gregory Amerson
 */
public class LiferayBaseDefaultsPlugin
	extends BaseDefaultsPlugin<LiferayBasePlugin> {

	public static final Plugin<Project> INSTANCE =
		new LiferayBaseDefaultsPlugin();

	@Override
	protected void applyPluginDefaults(
		Project project, LiferayBasePlugin liferayBasePlugin) {

		GradleUtil.addTask(
			project, "cleanBuildProfile", CleanBuildProfileTask.class);
		GradleUtil.addTask(
			project, "setBuildProfile", SetBuildProfileTask.class);

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
		Task cleanBuildProfile = GradleUtil.getTask(
			project, "cleanBuildProfile");
		Task setBuildProfile = GradleUtil.getTask(project, "setBuildProfile");

		setBuildProfile.dependsOn(cleanBuildProfile);
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
