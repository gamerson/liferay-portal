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
public class WarHookTest extends ProjectTemplatesTest {

	@Test
	public void testBuildTemplateWarHook() throws Exception {
		File gradleProjectDir = buildTemplateWithGradle("war-hook", "WarHook");

		testExists(gradleProjectDir, "src/main/resources/portal.properties");
		testExists(
			gradleProjectDir, "src/main/webapp/WEB-INF/liferay-hook.xml");
		testExists(gradleProjectDir, "build.gradle");

		testContains(
			gradleProjectDir,
			"src/main/java/warhook/WarHookLoginPostAction.java",
			"public class WarHookLoginPostAction extends Action");
		testContains(
			gradleProjectDir, "src/main/java/warhook/WarHookStartupAction.java",
			"public class WarHookStartupAction extends SimpleAction");
		testContains(
			gradleProjectDir,
			"src/main/webapp/WEB-INF/liferay-plugin-package.properties",
			"name=WarHook");

		File mavenProjectDir = buildTemplateWithMaven(
			"war-hook", "WarHook", "warhook", "-DclassName=WarHook",
			"-Dpackage=warhook");

		testContains(mavenProjectDir, "pom.xml");

		buildProjects(gradleProjectDir, mavenProjectDir);
	}

	@Test
	public void testBuildTemplateWarHook71() throws Exception {
		File gradleProjectDir = buildTemplateWithGradle(
			"war-hook", "WarHook", "--liferayVersion", "7.1");

		testContains(
			gradleProjectDir, "build.gradle",
			"name: \"com.liferay.portal.kernel\", version: \"3.0.0");

		File mavenProjectDir = buildTemplateWithMaven(
			"war-hook", "WarHook", "warhook", "-DclassName=WarHook",
			"-Dpackage=warhook", "-DliferayVersion=7.1");

		buildProjects(gradleProjectDir, mavenProjectDir);
	}

	@Test
	public void testBuildTemplateWarHookWithBOM() throws Exception {
		File gradleProjectDir = buildTemplateWithGradle(
			"war-hook", "war-hook-dependency-management",
			"--dependency-management-enabled");

		testNotContains(
			gradleProjectDir, "build.gradle",
			DEPENDENCY_PORTAL_KERNEL + ", version: \"2.0.0\"");

		testContains(
			gradleProjectDir, "build.gradle", DEPENDENCY_PORTAL_KERNEL);
	}

	@Test
	public void testBuildTemplateWarHookWithoutBOM() throws Exception {
		File gradleProjectDir = buildTemplateWithGradle(
			"war-hook", "war-hook-dependency-management");

		testContains(
			gradleProjectDir, "build.gradle",
			DEPENDENCY_PORTAL_KERNEL + ", version: \"2.0.0\"");
	}

}