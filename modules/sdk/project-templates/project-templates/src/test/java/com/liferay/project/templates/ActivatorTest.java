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
public class ActivatorTest extends ProjectTemplatesTest {

	@Test
	public void testBuildTemplateActivator() throws Exception {
		File gradleProjectDir = buildTemplateWithGradle(
			"activator", "bar-activator");

		testExists(gradleProjectDir, "bnd.bnd");

		testContains(
			gradleProjectDir, "build.gradle",
			"apply plugin: \"com.liferay.plugin\"");
		testContains(
			gradleProjectDir, "src/main/java/bar/activator/BarActivator.java",
			"public class BarActivator implements BundleActivator {");

		File mavenProjectDir = buildTemplateWithMaven(
			"activator", "bar-activator", "com.test",
			"-DclassName=BarActivator", "-Dpackage=bar.activator");

		buildProjects(gradleProjectDir, mavenProjectDir);
	}

	@Test
	public void testBuildTemplateActivatorInWorkspace() throws Exception {
		testBuildTemplateWithWorkspace(
			"activator", "bar-activator", "build/libs/bar.activator-1.0.0.jar");
	}

	@Test
	public void testBuildTemplateActivatorWithBOM() throws Exception {
		File gradleProjectDir = buildTemplateWithGradle(
			"activator", "activator-dependency-management",
			"--dependency-management-enabled");

		testNotContains(
			gradleProjectDir, "build.gradle",
			"compileOnly group: \"org.osgi\", name: \"org.osgi.core\", " +
				"version: \"6.0.0\"");

		testContains(
			gradleProjectDir, "build.gradle",
			"compileOnly group: \"org.osgi\", name: \"org.osgi.core\"");
	}

	@Test
	public void testBuildTemplateActivatorWithoutBOM() throws Exception {
		File gradleProjectDir = buildTemplateWithGradle(
			"activator", "activator-dependency-management");

		testContains(
			gradleProjectDir, "build.gradle",
			"compileOnly group: \"org.osgi\", name: \"org.osgi.core\", " +
				"version: \"6.0.0\"");
	}

}