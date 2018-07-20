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
public class ServiceTest extends ProjectTemplatesTest {

	@Test
	public void testBuildTemplateService() throws Exception {
		File gradleProjectDir = buildTemplateWithGradle(
			"service", "servicepreaction", "--class-name", "FooAction",
			"--service", "com.liferay.portal.kernel.events.LifecycleAction");

		testExists(gradleProjectDir, "bnd.bnd");

		testContains(
			gradleProjectDir, "build.gradle",
			"apply plugin: \"com.liferay.plugin\"");

		writeServiceClass(gradleProjectDir);

		File mavenProjectDir = buildTemplateWithMaven(
			"service", "servicepreaction", "com.test", "-DclassName=FooAction",
			"-Dpackage=servicepreaction",
			"-DserviceClass=com.liferay.portal.kernel.events.LifecycleAction");

		writeServiceClass(mavenProjectDir);

		buildProjects(gradleProjectDir, mavenProjectDir);
	}

	@Test
	public void testBuildTemplateService71() throws Exception {
		File gradleProjectDir = buildTemplateWithGradle(
			"service", "servicepreaction", "--class-name", "FooAction",
			"--service", "com.liferay.portal.kernel.events.LifecycleAction",
			"--liferayVersion", "7.1");

		testContains(
			gradleProjectDir, "build.gradle",
			"name: \"com.liferay.portal.kernel\", version: \"3.0.0");

		writeServiceClass(gradleProjectDir);

		File mavenProjectDir = buildTemplateWithMaven(
			"service", "servicepreaction", "com.test", "-DclassName=FooAction",
			"-Dpackage=servicepreaction",
			"-DserviceClass=com.liferay.portal.kernel.events.LifecycleAction",
			"-DliferayVersion=7.1");

		writeServiceClass(mavenProjectDir);

		buildProjects(gradleProjectDir, mavenProjectDir);
	}

	@Test
	public void testBuildTemplateServiceInWorkspace() throws Exception {
		File gradleProjectDir = buildTemplateWithGradle(
			"service", "servicepreaction", "--class-name", "FooAction",
			"--service", "com.liferay.portal.kernel.events.LifecycleAction");

		testContains(
			gradleProjectDir, "build.gradle", "buildscript {",
			"repositories {");

		writeServiceClass(gradleProjectDir);

		File workspaceDir = buildWorkspace();

		File modulesDir = new File(workspaceDir, "modules");

		File workspaceProjectDir = buildTemplateWithGradle(
			modulesDir, "service", "servicepreaction", "--class-name",
			"FooAction", "--service",
			"com.liferay.portal.kernel.events.LifecycleAction");

		testNotContains(
			workspaceProjectDir, "build.gradle", true, "^repositories \\{.*");

		writeServiceClass(workspaceProjectDir);

		executeGradle(gradleProjectDir, GRADLE_TASK_PATH_BUILD);

		testExists(gradleProjectDir, "build/libs/servicepreaction-1.0.0.jar");

		executeGradle(workspaceDir, ":modules:servicepreaction:build");

		testExists(
			workspaceProjectDir, "build/libs/servicepreaction-1.0.0.jar");
	}

	@Test
	public void testBuildTemplateServiceWithBOM() throws Exception {
		File gradleProjectDir = buildTemplateWithGradle(
			"service", "service-dependency-management", "--service",
			"com.liferay.portal.kernel.events.LifecycleAction",
			"--dependency-management-enabled");

		testNotContains(
			gradleProjectDir, "build.gradle",
			DEPENDENCY_PORTAL_KERNEL + ", version: \"2.0.0\"");

		testContains(
			gradleProjectDir, "build.gradle", DEPENDENCY_PORTAL_KERNEL);
	}

	@Test
	public void testBuildTemplateServiceWithoutBOM() throws Exception {
		File gradleProjectDir = buildTemplateWithGradle(
			"service", "service-dependency-management", "--service",
			"com.liferay.portal.kernel.events.LifecycleAction");

		testContains(
			gradleProjectDir, "build.gradle",
			DEPENDENCY_PORTAL_KERNEL + ", version: \"2.0.0\"");
	}

}