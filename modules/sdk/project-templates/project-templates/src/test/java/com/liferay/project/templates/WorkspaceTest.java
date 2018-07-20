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
import java.io.FileOutputStream;

import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Lawrence Lee
 * @author Gregory Amerson
 * @author Andrea Di Giorgi
 */
public class WorkspaceTest extends ProjectTemplatesTest {

	@Test
	public void testBuildTemplateWorkspace() throws Exception {
		File workspaceProjectDir = buildTemplateWithGradle(
			WorkspaceUtil.WORKSPACE, "foows");

		testExists(workspaceProjectDir, "configs/dev/portal-ext.properties");
		testExists(workspaceProjectDir, "gradle.properties");
		testExists(workspaceProjectDir, "modules");
		testExists(workspaceProjectDir, "themes");
		testExists(workspaceProjectDir, "wars");

		testNotExists(workspaceProjectDir, "modules/pom.xml");
		testNotExists(workspaceProjectDir, "themes/pom.xml");
		testNotExists(workspaceProjectDir, "wars/pom.xml");

		String gradlePluginsWorkspaceVersion = System.getProperty(
			"com.liferay.gradle.plugins.workspace.version");

		Assert.assertNotNull(gradlePluginsWorkspaceVersion);

		testContains(
			workspaceProjectDir, "settings.gradle",
			"version: \"" + gradlePluginsWorkspaceVersion + "\"");

		File moduleProjectDir = buildTemplateWithGradle(
			new File(workspaceProjectDir, "modules"), "", "foo-portlet");

		testNotContains(
			moduleProjectDir, "build.gradle", "buildscript", "repositories");

		executeGradle(
			workspaceProjectDir,
			":modules:foo-portlet" + GRADLE_TASK_PATH_BUILD);

		testExists(moduleProjectDir, "build/libs/foo.portlet-1.0.0.jar");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testBuildTemplateWorkspaceExistingFile() throws Exception {
		File destinationDir = temporaryFolder.newFolder("existing-file");

		createNewFiles("foo", destinationDir);

		buildTemplateWithGradle(destinationDir, WorkspaceUtil.WORKSPACE, "foo");
	}

	@Test
	public void testBuildTemplateWorkspaceForce() throws Exception {
		File destinationDir = temporaryFolder.newFolder("existing-file");

		createNewFiles("foo", destinationDir);

		buildTemplateWithGradle(
			destinationDir, WorkspaceUtil.WORKSPACE, "forced", "--force");
	}

	@Test
	public void testBuildTemplateWorkspaceLocalProperties() throws Exception {
		File workspaceProjectDir = buildTemplateWithGradle(
			WorkspaceUtil.WORKSPACE, "foo");

		testExists(workspaceProjectDir, "gradle-local.properties");

		Properties gradleLocalProperties = new Properties();

		String homeDirName = "foo/bar/baz";
		String modulesDirName = "qux/quux";

		gradleLocalProperties.put("liferay.workspace.home.dir", homeDirName);
		gradleLocalProperties.put(
			"liferay.workspace.modules.dir", modulesDirName);

		File gradleLocalPropertiesFile = new File(
			workspaceProjectDir, "gradle-local.properties");

		try (FileOutputStream fileOutputStream = new FileOutputStream(
				gradleLocalPropertiesFile)) {

			gradleLocalProperties.store(fileOutputStream, null);
		}

		buildTemplateWithGradle(
			new File(workspaceProjectDir, modulesDirName), "", "foo-portlet");

		executeGradle(
			workspaceProjectDir,
			":" + modulesDirName.replace('/', ':') + ":foo-portlet" +
				GRADLE_TASK_PATH_DEPLOY);

		testExists(
			workspaceProjectDir, homeDirName + "/osgi/modules/foo.portlet.jar");
	}

	@Test
	public void testBuildTemplateWorkspaceWithPortlet() throws Exception {
		File gradleWorkspaceProjectDir = buildTemplateWithGradle(
			WorkspaceUtil.WORKSPACE, "withportlet");

		File gradleModulesDir = new File(gradleWorkspaceProjectDir, "modules");

		buildTemplateWithGradle(gradleModulesDir, "mvc-portlet", "foo-portlet");

		File mavenWorkspaceProjectDir = buildTemplateWithMaven(
			WorkspaceUtil.WORKSPACE, "withportlet", "com.test");

		File mavenModulesDir = new File(mavenWorkspaceProjectDir, "modules");

		buildTemplateWithMaven(
			mavenWorkspaceProjectDir.getParentFile(), mavenModulesDir,
			"mvc-portlet", "foo-portlet", "com.test", "-DclassName=Foo",
			"-Dpackage=foo.portlet", "-DprojectType=workspace");

		executeGradle(
			gradleWorkspaceProjectDir,
			":modules:foo-portlet" + GRADLE_TASK_PATH_BUILD);

		testExists(
			gradleModulesDir, "foo-portlet/build/libs/foo.portlet-1.0.0.jar");

		executeMaven(mavenModulesDir, MAVEN_GOAL_PACKAGE);

		testExists(mavenModulesDir, "foo-portlet/target/foo-portlet-1.0.0.jar");
	}

	@Test
	public void testBuildTemplateWorkspaceWithVersion() throws Exception {
		File workspaceProjectDir = buildTemplateWithMaven(
			WorkspaceUtil.WORKSPACE, "withportlet", "com.test",
			"-DliferayVersion=7.1");

		testContains(
			workspaceProjectDir, "pom.xml", "<liferay.workspace.bundle.url>",
			"liferay.com/portal/7.1.0-");

		workspaceProjectDir = buildTemplateWithGradle(
			WorkspaceUtil.WORKSPACE, "withportlet", "--liferayVersion", "7.1");

		testContains(
			workspaceProjectDir, "gradle.properties", true,
			".*liferay.workspace.bundle.url=.*liferay.com/portal/7.1.0-.*");
	}

}