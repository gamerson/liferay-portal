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

import org.junit.Test;

/**
 * @author Lawrence Lee
 * @author Gregory Amerson
 * @author Andrea Di Giorgi
 */
public class ThemeTest extends ProjectTemplatesTest {

	@Test
	public void testBuildTemplateTheme() throws Exception {
		File gradleProjectDir = buildTemplateWithGradle("theme", "theme-test");

		testContains(
			gradleProjectDir, "build.gradle",
			"name: \"com.liferay.gradle.plugins.theme.builder\"",
			"apply plugin: \"com.liferay.portal.tools.theme.builder\"");
		testContains(
			gradleProjectDir,
			"src/main/webapp/WEB-INF/liferay-plugin-package.properties",
			"name=theme-test");

		File mavenProjectDir = buildTemplateWithMaven(
			"theme", "theme-test", "com.test");

		testContains(
			mavenProjectDir, "pom.xml",
			"com.liferay.portal.tools.theme.builder");

		buildProjects(gradleProjectDir, mavenProjectDir);
	}

	@Test
	public void testBuildTemplateThemeContributorCustom() throws Exception {
		File gradleProjectDir = buildTemplateWithGradle(
			"theme-contributor", "my-contributor-custom", "--contributor-type",
			"foo-bar");

		testContains(
			gradleProjectDir, "bnd.bnd",
			"Liferay-Theme-Contributor-Type: foo-bar",
			"Web-ContextPath: /foo-bar-theme-contributor");
		testNotContains(
			gradleProjectDir, "bnd.bnd",
			"-plugin.sass: com.liferay.ant.bnd.sass.SassAnalyzerPlugin");

		testExists(
			gradleProjectDir,
			"src/main/resources/META-INF/resources/css/foo-bar.scss");
		testExists(
			gradleProjectDir,
			"src/main/resources/META-INF/resources/js/foo-bar.js");

		File mavenProjectDir = buildTemplateWithMaven(
			"theme-contributor", "my-contributor-custom", "com.test",
			"-DcontributorType=foo-bar", "-Dpackage=my.contributor.custom");

		testContains(
			mavenProjectDir, "bnd.bnd",
			"-plugin.sass: com.liferay.ant.bnd.sass.SassAnalyzerPlugin");

		buildProjects(gradleProjectDir, mavenProjectDir);
	}

	@Test
	public void testBuildTemplateThemeContributorDefaults() throws Exception {
		File gradleProjectDir = buildTemplateWithGradle(
			"theme-contributor", "my-contributor-default");

		testContains(
			gradleProjectDir, "bnd.bnd",
			"Liferay-Theme-Contributor-Type: my-contributor-default",
			"Web-ContextPath: /my-contributor-default-theme-contributor");
	}

	@Test
	public void testBuildTemplateThemeContributorinWorkspace()
		throws Exception {

		testBuildTemplateWithWorkspace(
			"theme-contributor", "my-contributor",
			"build/libs/my.contributor-1.0.0.jar");
	}

	@Test
	public void testBuildTemplateThemeInWorkspace() throws Exception {
		File gradleProjectDir = buildTemplateWithGradle("theme", "theme-test");

		testContains(
			gradleProjectDir, "build.gradle", "buildscript {",
			"apply plugin: \"com.liferay.portal.tools.theme.builder\"",
			"repositories {");

		File workspaceDir = buildWorkspace();

		File warsDir = new File(workspaceDir, "wars");

		File workspaceProjectDir = buildTemplateWithGradle(
			warsDir, "theme", "theme-test");

		testNotContains(
			workspaceProjectDir, "build.gradle", true, "^repositories \\{.*");

		executeGradle(gradleProjectDir, GRADLE_TASK_PATH_BUILD);

		File gradleWarFile = testExists(
			gradleProjectDir, "build/libs/theme-test.war");

		executeGradle(workspaceDir, ":wars:theme-test:build");

		File workspaceWarFile = testExists(
			workspaceProjectDir, "build/libs/theme-test.war");

		testWarsDiff(gradleWarFile, workspaceWarFile);
	}

}