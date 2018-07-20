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
public class PanelAppTest extends ProjectTemplatesTest {

	@Test
	public void testBuildTemplatePanelApp() throws Exception {
		File gradleProjectDir = buildTemplateWithGradle(
			"panel-app", "gradle.test", "--class-name", "Foo");

		testExists(gradleProjectDir, "build.gradle");

		testContains(
			gradleProjectDir, "bnd.bnd",
			"Export-Package: gradle.test.constants");
		testContains(
			gradleProjectDir,
			"src/main/java/gradle/test/application/list/FooPanelApp.java",
			"public class FooPanelApp extends BasePanelApp");
		testContains(
			gradleProjectDir,
			"src/main/java/gradle/test/constants/FooPortletKeys.java",
			"public class FooPortletKeys");
		testContains(
			gradleProjectDir,
			"src/main/java/gradle/test/portlet/FooPortlet.java",
			"javax.portlet.name=\" + FooPortletKeys.Foo",
			"public class FooPortlet extends MVCPortlet");

		File mavenProjectDir = buildTemplateWithMaven(
			"panel-app", "gradle.test", "com.test", "-DclassName=Foo",
			"-Dpackage=gradle.test");

		buildProjects(gradleProjectDir, mavenProjectDir);
	}

	@Test
	public void testBuildTemplatePanelApp71() throws Exception {
		File gradleProjectDir = buildTemplateWithGradle(
			"panel-app", "gradle.test", "--class-name", "Foo",
			"--liferayVersion", "7.1");

		testContains(
			gradleProjectDir, "build.gradle",
			"name: \"com.liferay.portal.kernel\", version: \"3.0.0");

		File mavenProjectDir = buildTemplateWithMaven(
			"panel-app", "gradle.test", "com.test", "-DclassName=Foo",
			"-Dpackage=gradle.test", "-DliferayVersion=7.1");

		testContains(
			mavenProjectDir, "bnd.bnd", "-contract: JavaPortlet,JavaServlet");

		buildProjects(gradleProjectDir, mavenProjectDir);
	}

	@Test
	public void testBuildTemplatePanelAppInWorkspace() throws Exception {
		testBuildTemplateWithWorkspace(
			"panel-app", "gradle.test", "build/libs/gradle.test-1.0.0.jar");
	}

	@Test
	public void testBuildTemplatePanelAppWithBOM() throws Exception {
		File gradleProjectDir = buildTemplateWithGradle(
			"panel-app", "panel-dependency-management",
			"--dependency-management-enabled");

		testNotContains(
			gradleProjectDir, "build.gradle",
			DEPENDENCY_PORTAL_KERNEL + ", version: \"2.0.0\"");

		testContains(
			gradleProjectDir, "build.gradle", DEPENDENCY_PORTAL_KERNEL);
	}

	@Test
	public void testBuildTemplatePanelAppWithoutBOM() throws Exception {
		File gradleProjectDir = buildTemplateWithGradle(
			"panel-app", "panel-dependency-management");

		testContains(
			gradleProjectDir, "build.gradle",
			DEPENDENCY_PORTAL_KERNEL + ", version: \"2.0.0\"");
	}

}