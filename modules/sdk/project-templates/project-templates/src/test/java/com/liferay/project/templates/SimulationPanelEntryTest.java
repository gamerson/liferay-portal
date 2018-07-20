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
public class SimulationPanelEntryTest extends ProjectTemplatesTest {

	@Test
	public void testBuildTemplateSimulationPanelEntry() throws Exception {
		File gradleProjectDir = buildTemplateWithGradle(
			"simulation-panel-entry", "simulator", "--package-name",
			"test.simulator");

		testExists(gradleProjectDir, "bnd.bnd");

		testContains(
			gradleProjectDir, "build.gradle",
			"apply plugin: \"com.liferay.plugin\"");
		testContains(
			gradleProjectDir,
			"src/main/java/test/simulator/application/list" +
				"/SimulatorSimulationPanelApp.java",
			"public class SimulatorSimulationPanelApp",
			"extends BaseJSPPanelApp");

		File mavenProjectDir = buildTemplateWithMaven(
			"simulation-panel-entry", "simulator", "com.test",
			"-DclassName=Simulator", "-Dpackage=test.simulator");

		buildProjects(gradleProjectDir, mavenProjectDir);
	}

	@Test
	public void testBuildTemplateSimulationPanelEntry71() throws Exception {
		File gradleProjectDir = buildTemplateWithGradle(
			"simulation-panel-entry", "simulator", "--package-name",
			"test.simulator", "--liferayVersion", "7.1");

		testContains(
			gradleProjectDir, "build.gradle",
			"name: \"com.liferay.portal.kernel\", version: \"3.0.0");

		File mavenProjectDir = buildTemplateWithMaven(
			"simulation-panel-entry", "simulator", "com.test",
			"-DclassName=Simulator", "-Dpackage=test.simulator",
			"-DliferayVersion=7.1");

		testContains(
			mavenProjectDir, "bnd.bnd", "-contract: JavaPortlet,JavaServlet");

		buildProjects(gradleProjectDir, mavenProjectDir);
	}

	@Test
	public void testBuildTemplateSimulationPanelEntryInWorkspace()
		throws Exception {

		testBuildTemplateWithWorkspace(
			"simulation-panel-entry", "test.simulator",
			"build/libs/test.simulator-1.0.0.jar");
	}

	@Test
	public void testBuildTemplateSimulationPanelEntryWithBOM()
		throws Exception {

		File gradleProjectDir = buildTemplateWithGradle(
			"simulation-panel-entry", "simulator-dependency-management",
			"--dependency-management-enabled");

		testNotContains(
			gradleProjectDir, "build.gradle",
			DEPENDENCY_PORTAL_KERNEL + ", version: \"2.3.0\"");

		testContains(
			gradleProjectDir, "build.gradle", DEPENDENCY_PORTAL_KERNEL);
	}

	@Test
	public void testBuildTemplateSimulationPanelEntryWithoutBOM()
		throws Exception {

		File gradleProjectDir = buildTemplateWithGradle(
			"simulation-panel-entry", "simulator-dependency-management");

		testContains(
			gradleProjectDir, "build.gradle",
			DEPENDENCY_PORTAL_KERNEL + ", version: \"2.3.0\"");
	}

}