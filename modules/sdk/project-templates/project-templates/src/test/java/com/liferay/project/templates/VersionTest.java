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

package com.liferay.project.templates;

import java.io.File;

import java.util.Optional;
import java.util.regex.Matcher;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Lawrence Lee
 * @author Gregory Amerson
 * @author Andrea Di Giorgi
 */
public class VersionTest extends ProjectTemplatesTest {

	@Test(expected = IllegalArgumentException.class)
	public void testBuildTemplateLiferayVersionInvalid62() throws Exception {
		buildTemplateWithGradle(
			"mvc-portlet", "test", "--liferayVersion", "6.2");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testBuildTemplateLiferayVersionInvalid70test()
		throws Exception {

		buildTemplateWithGradle(
			"mvc-portlet", "test", "--liferayVersion", "7.0test");
	}

	@Test
	public void testBuildTemplateLiferayVersionValid70() throws Exception {
		buildTemplateWithGradle(
			"mvc-portlet", "test", "--liferayVersion", "7.0");
	}

	@Test
	public void testBuildTemplateLiferayVersionValid712() throws Exception {
		buildTemplateWithGradle(
			"mvc-portlet", "test", "--liferayVersion", "7.1.2");
	}

	@Test
	public void testCompareGradlePluginVersions() throws Exception {
		String template = "mvc-portlet";
		String name = "foo";

		File gradleProjectDir = buildTemplateWithGradle(template, name);

		File workspaceDir = buildWorkspace();

		File modulesDir = new File(workspaceDir, "modules");

		buildTemplateWithGradle(modulesDir, template, name);

		Optional<String> result = executeGradle(
			gradleProjectDir, true, GRADLE_TASK_PATH_BUILD);

		Matcher matcher = gradlePluginVersionPattern.matcher(result.get());

		String standaloneGradlePluginVersion = null;

		if (matcher.matches()) {
			standaloneGradlePluginVersion = matcher.group(1);
		}

		result = executeGradle(
			workspaceDir, true, ":modules:" + name + ":clean");

		matcher = gradlePluginVersionPattern.matcher(result.get());

		String workspaceGradlePluginVersion = null;

		if (matcher.matches()) {
			workspaceGradlePluginVersion = matcher.group(1);
		}

		Assert.assertEquals(
			"com.liferay.plugin versions do not match",
			standaloneGradlePluginVersion, workspaceGradlePluginVersion);
	}

	@Test
	public void testCompareServiceBuilderPluginVersions() throws Exception {
		String name = "sample";
		String packageName = "com.test.sample";
		String serviceProjectName = name + "-service";

		File gradleProjectDir = buildTemplateWithGradle(
			"service-builder", name, "--package-name", packageName);

		Optional<String> gradleResult = executeGradle(
			gradleProjectDir, true, ":" + serviceProjectName + ":dependencies");

		String gradleServiceBuilderVersion = null;

		Matcher matcher = serviceBuilderVersionPattern.matcher(
			gradleResult.get());

		if (matcher.matches()) {
			gradleServiceBuilderVersion = matcher.group(1);
		}

		File mavenProjectDir = buildTemplateWithMaven(
			"service-builder", name, "com.test", "-Dpackage=" + packageName);

		String mavenResult = executeMaven(
			new File(mavenProjectDir, serviceProjectName),
			MAVEN_GOAL_BUILD_SERVICE);

		matcher = serviceBuilderVersionPattern.matcher(mavenResult);

		String mavenServiceBuilderVersion = null;

		if (matcher.matches()) {
			mavenServiceBuilderVersion = matcher.group(1);
		}

		Assert.assertEquals(
			"com.liferay.portal.tools.service.builder versions do not match",
			gradleServiceBuilderVersion, mavenServiceBuilderVersion);
	}

}