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
public class PortletTest extends ProjectTemplatesTest {

	@Test
	public void testBuildTemplatePortlet() throws Exception {
		File gradleProjectDir = buildTemplateWithGradle(
			"portlet", "foo.test", "--class-name", "Foo");

		testContains(
			gradleProjectDir, "bnd.bnd", "Export-Package: foo.test.constants");
		testContains(
			gradleProjectDir, "build.gradle",
			"apply plugin: \"com.liferay.plugin\"");
		testContains(
			gradleProjectDir,
			"src/main/java/foo/test/constants/FooPortletKeys.java",
			"public class FooPortletKeys");
		testContains(
			gradleProjectDir, "src/main/java/foo/test/portlet/FooPortlet.java",
			"package foo.test.portlet;", "javax.portlet.display-name=Foo",
			"javax.portlet.name=\" + FooPortletKeys.Foo",
			"public class FooPortlet extends GenericPortlet {",
			"printWriter.print(\"Hello from Foo");

		File mavenProjectDir = buildTemplateWithMaven(
			"portlet", "foo.test", "com.test", "-DclassName=Foo",
			"-Dpackage=foo.test");

		buildProjects(gradleProjectDir, mavenProjectDir);
	}

	@Test
	public void testBuildTemplatePortlet71() throws Exception {
		File gradleProjectDir = buildTemplateWithGradle(
			"portlet", "foo.test", "--class-name", "Foo", "--liferayVersion",
			"7.1");

		testContains(
			gradleProjectDir, "build.gradle",
			"name: \"com.liferay.portal.kernel\", version: \"3.0.0");

		File mavenProjectDir = buildTemplateWithMaven(
			"portlet", "foo.test", "com.test", "-DclassName=Foo",
			"-Dpackage=foo.test", "-DliferayVersion=7.1");

		testContains(
			mavenProjectDir, "bnd.bnd", "-contract: JavaPortlet,JavaServlet");

		buildProjects(gradleProjectDir, mavenProjectDir);
	}

	@Test
	public void testBuildTemplatePortletInWorkspace() throws Exception {
		testBuildTemplateWithWorkspace(
			"portlet", "foo.test", "build/libs/foo.test-1.0.0.jar");
	}

	@Test
	public void testBuildTemplatePortletWithBOM() throws Exception {
		File gradleProjectDir = buildTemplateWithGradle(
			"portlet", "portlet-dependency-management",
			"--dependency-management-enabled");

		testNotContains(
			gradleProjectDir, "build.gradle",
			DEPENDENCY_PORTAL_KERNEL + ", version: \"2.0.0\"");

		testContains(
			gradleProjectDir, "build.gradle", DEPENDENCY_PORTAL_KERNEL);
	}

	@Test
	public void testBuildTemplatePortletWithoutBOM() throws Exception {
		File gradleProjectDir = buildTemplateWithGradle(
			"portlet", "portlet-dependency-management");

		testContains(
			gradleProjectDir, "build.gradle",
			DEPENDENCY_PORTAL_KERNEL + ", version: \"2.0.0\"");
	}

	@Test
	public void testBuildTemplatePortletWithPortletName() throws Exception {
		File gradleProjectDir = buildTemplateWithGradle("portlet", "portlet");

		testExists(gradleProjectDir, "bnd.bnd");

		testContains(
			gradleProjectDir, "build.gradle",
			"apply plugin: \"com.liferay.plugin\"");
		testContains(
			gradleProjectDir,
			"src/main/java/portlet/portlet/PortletPortlet.java",
			"package portlet.portlet;", "javax.portlet.display-name=Portlet",
			"public class PortletPortlet extends GenericPortlet {",
			"printWriter.print(\"Hello from Portlet");

		File mavenProjectDir = buildTemplateWithMaven(
			"portlet", "portlet", "com.test", "-DclassName=Portlet",
			"-Dpackage=portlet");

		buildProjects(gradleProjectDir, mavenProjectDir);
	}

}