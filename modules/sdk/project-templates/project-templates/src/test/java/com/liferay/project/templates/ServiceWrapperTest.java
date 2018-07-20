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
public class ServiceWrapperTest extends ProjectTemplatesTest {

	@Test
	public void testBuildTemplateServiceWrapper() throws Exception {
		File gradleProjectDir = buildTemplateWithGradle(
			"service-wrapper", "serviceoverride", "--service",
			"com.liferay.portal.kernel.service.UserLocalServiceWrapper");

		testExists(gradleProjectDir, "bnd.bnd");

		testContains(
			gradleProjectDir, "build.gradle",
			"apply plugin: \"com.liferay.plugin\"");
		testContains(
			gradleProjectDir,
			"src/main/java/serviceoverride/Serviceoverride.java",
			"package serviceoverride;",
			"import com.liferay.portal.kernel.service.UserLocalServiceWrapper;",
			"service = ServiceWrapper.class",
			"public class Serviceoverride extends UserLocalServiceWrapper {",
			"public Serviceoverride() {");

		File mavenProjectDir = buildTemplateWithMaven(
			"service-wrapper", "serviceoverride", "com.test",
			"-DclassName=Serviceoverride", "-Dpackage=serviceoverride",
			"-DserviceWrapperClass=" +
				"com.liferay.portal.kernel.service.UserLocalServiceWrapper");

		buildProjects(gradleProjectDir, mavenProjectDir);
	}

	@Test
	public void testBuildTemplateServiceWrapper71() throws Exception {
		File gradleProjectDir = buildTemplateWithGradle(
			"service-wrapper", "serviceoverride", "--service",
			"com.liferay.portal.kernel.service.UserLocalServiceWrapper",
			"--liferayVersion", "7.1");

		testContains(
			gradleProjectDir, "build.gradle",
			"name: \"com.liferay.portal.kernel\", version: \"3.0.0");

		File mavenProjectDir = buildTemplateWithMaven(
			"service-wrapper", "serviceoverride", "com.test",
			"-DclassName=Serviceoverride", "-Dpackage=serviceoverride",
			"-DserviceWrapperClass=" +
				"com.liferay.portal.kernel.service.UserLocalServiceWrapper",
			"-DliferayVersion=7.1");

		buildProjects(gradleProjectDir, mavenProjectDir);
	}

	@Test
	public void testBuildTemplateServiceWrapperInWorkspace() throws Exception {
		File gradleProjectDir = buildTemplateWithGradle(
			"service-wrapper", "serviceoverride", "--service",
			"com.liferay.portal.kernel.service.UserLocalServiceWrapper");

		testContains(
			gradleProjectDir, "build.gradle", "buildscript {",
			"repositories {");

		File workspaceDir = buildWorkspace();

		File modulesDir = new File(workspaceDir, "modules");

		File workspaceProjectDir = buildTemplateWithGradle(
			modulesDir, "service-wrapper", "serviceoverride", "--service",
			"com.liferay.portal.kernel.service.UserLocalServiceWrapper");

		testNotContains(
			workspaceProjectDir, "build.gradle", true, "^repositories \\{.*");

		executeGradle(gradleProjectDir, GRADLE_TASK_PATH_BUILD);

		testExists(gradleProjectDir, "build/libs/serviceoverride-1.0.0.jar");

		executeGradle(workspaceDir, ":modules:serviceoverride:build");

		testExists(workspaceProjectDir, "build/libs/serviceoverride-1.0.0.jar");
	}

	@Test
	public void testBuildTemplateServiceWrapperWithBOM() throws Exception {
		File gradleProjectDir = buildTemplateWithGradle(
			"service-wrapper", "wrapper-dependency-management", "--service",
			"com.liferay.portal.kernel.service.UserLocalServiceWrapper",
			"--dependency-management-enabled");

		testNotContains(
			gradleProjectDir, "build.gradle",
			DEPENDENCY_PORTAL_KERNEL + ", version: \"2.0.0\"");

		testContains(
			gradleProjectDir, "build.gradle", DEPENDENCY_PORTAL_KERNEL);
	}

	@Test
	public void testBuildTemplateServiceWrapperWithoutBOM() throws Exception {
		File gradleProjectDir = buildTemplateWithGradle(
			"service-wrapper", "wrapper-dependency-management", "--service",
			"com.liferay.portal.kernel.service.UserLocalServiceWrapper");

		testContains(
			gradleProjectDir, "build.gradle",
			DEPENDENCY_PORTAL_KERNEL + ", version: \"2.0.0\"");
	}

}