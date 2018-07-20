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
public class ControlMenuEntryTest extends ProjectTemplatesTest {

	@Test
	public void testBuildTemplateControlMenuEntry() throws Exception {
		File gradleProjectDir = buildTemplateWithGradle(
			"control-menu-entry", "foo-bar");

		testExists(gradleProjectDir, "bnd.bnd");

		testContains(
			gradleProjectDir,
			"src/main/java/foo/bar/control/menu" +
				"/FooBarProductNavigationControlMenuEntry.java",
			"public class FooBarProductNavigationControlMenuEntry",
			"extends BaseProductNavigationControlMenuEntry",
			"implements ProductNavigationControlMenuEntry");

		File mavenProjectDir = buildTemplateWithMaven(
			"control-menu-entry", "foo-bar", "com.test", "-DclassName=FooBar",
			"-Dpackage=foo.bar");

		buildProjects(gradleProjectDir, mavenProjectDir);
	}

	@Test
	public void testBuildTemplateControlMenuEntry71() throws Exception {
		File gradleProjectDir = buildTemplateWithGradle(
			"control-menu-entry", "foo-bar", "--liferayVersion", "7.1");

		testContains(
			gradleProjectDir, "build.gradle",
			"name: \"com.liferay.portal.kernel\", version: \"3.0.0");

		File mavenProjectDir = buildTemplateWithMaven(
			"control-menu-entry", "foo-bar", "com.test", "-DclassName=FooBar",
			"-Dpackage=foo.bar", "-DliferayVersion=7.1");

		testContains(
			mavenProjectDir, "bnd.bnd", "-contract: JavaPortlet,JavaServlet");

		buildProjects(gradleProjectDir, mavenProjectDir);
	}

	@Test
	public void testBuildTemplateControlMenuEntryInWorkspace()
		throws Exception {

		testBuildTemplateWithWorkspace(
			"control-menu-entry", "foo-bar", "build/libs/foo.bar-1.0.0.jar");
	}

	@Test
	public void testBuildTemplateControlMenuEntryWithBOM() throws Exception {
		File gradleProjectDir = buildTemplateWithGradle(
			"control-menu-entry", "entry-dependency-management",
			"--dependency-management-enabled");

		testNotContains(
			gradleProjectDir, "build.gradle",
			DEPENDENCY_PORTAL_KERNEL + ", version: \"2.0.0\"");

		testContains(
			gradleProjectDir, "build.gradle", DEPENDENCY_PORTAL_KERNEL);
	}

	@Test
	public void testBuildTemplateControlMenuEntryWithoutBOM() throws Exception {
		File gradleProjectDir = buildTemplateWithGradle(
			"control-menu-entry", "entry-dependency-management");

		testContains(
			gradleProjectDir, "build.gradle",
			DEPENDENCY_PORTAL_KERNEL + ", version: \"2.0.0\"");
	}

}