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
public class PortletProviderTest extends ProjectTemplatesTest {

	@Test
	public void testBuildTemplatePorletProviderInWorkspace() throws Exception {
		testBuildTemplateWithWorkspace(
			"portlet-provider", "provider.test",
			"build/libs/provider.test-1.0.0.jar");
	}

	@Test
	public void testBuildTemplatePortletProvider() throws Exception {
		File gradleProjectDir = buildTemplateWithGradle(
			"portlet-provider", "provider.test");

		testExists(gradleProjectDir, "bnd.bnd");
		testExists(gradleProjectDir, "build.gradle");

		testContains(
			gradleProjectDir,
			"src/main/java/provider/test/constants" +
				"/ProviderTestPortletKeys.java",
			"package provider.test.constants;",
			"public class ProviderTestPortletKeys",
			"public static final String ProviderTest = \"providertest\";");

		File mavenProjectDir = buildTemplateWithMaven(
			"portlet-provider", "provider.test", "com.test",
			"-DclassName=ProviderTest", "-Dpackage=provider.test");

		buildProjects(gradleProjectDir, mavenProjectDir);
	}

	@Test
	public void testBuildTemplatePortletProvider71() throws Exception {
		File gradleProjectDir = buildTemplateWithGradle(
			"portlet-provider", "provider.test", "--liferayVersion", "7.1");

		testContains(
			gradleProjectDir, "build.gradle",
			"name: \"com.liferay.portal.kernel\", version: \"3.0.0");

		File mavenProjectDir = buildTemplateWithMaven(
			"portlet-provider", "provider.test", "com.test",
			"-DclassName=ProviderTest", "-Dpackage=provider.test",
			"-DliferayVersion=7.1");

		testContains(
			mavenProjectDir, "bnd.bnd", "-contract: JavaPortlet,JavaServlet");

		buildProjects(gradleProjectDir, mavenProjectDir);
	}

	@Test
	public void testBuildTemplatePortletProviderWithBOM() throws Exception {
		File gradleProjectDir = buildTemplateWithGradle(
			"portlet-provider", "provider-dependency-management",
			"--dependency-management-enabled");

		testNotContains(
			gradleProjectDir, "build.gradle",
			DEPENDENCY_PORTAL_KERNEL + ", version: \"2.0.0\"");

		testContains(
			gradleProjectDir, "build.gradle", DEPENDENCY_PORTAL_KERNEL);
	}

	@Test
	public void testBuildTemplatePortletProviderWithoutBOM() throws Exception {
		File gradleProjectDir = buildTemplateWithGradle(
			"portlet-provider", "provider-dependency-management");

		testContains(
			gradleProjectDir, "build.gradle",
			DEPENDENCY_PORTAL_KERNEL + ", version: \"2.0.0\"");
	}

}