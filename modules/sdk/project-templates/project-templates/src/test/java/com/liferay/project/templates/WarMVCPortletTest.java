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
public class WarMVCPortletTest extends ProjectTemplatesTest {

	@Test
	public void testBuildTemplateWarMVCPortlet() throws Exception {
		File gradleProjectDir = buildTemplateWithGradle(
			"war-mvc-portlet", "WarMVCPortlet");

		testExists(gradleProjectDir, "src/main/webapp/init.jsp");
		testExists(gradleProjectDir, "src/main/webapp/view.jsp");

		testContains(
			gradleProjectDir, "build.gradle",
			"apply plugin: \"com.liferay.css.builder\"",
			"apply plugin: \"war\"");
		testContains(
			gradleProjectDir,
			"src/main/webapp/WEB-INF/liferay-plugin-package.properties",
			"name=WarMVCPortlet");

		File mavenProjectDir = buildTemplateWithMaven(
			"war-mvc-portlet", "WarMVCPortlet", "warmvcportlet",
			"-DclassName=WarMVCPortlet", "-Dpackage=WarMVCPortlet");

		testContains(
			mavenProjectDir, "pom.xml", "maven-war-plugin",
			"com.liferay.css.builder");

		buildProjects(gradleProjectDir, mavenProjectDir);
	}

	@Test
	public void testBuildTemplateWarMVCPortlet71() throws Exception {
		File gradleProjectDir = buildTemplateWithGradle(
			"war-mvc-portlet", "WarMVCPortlet", "--liferayVersion", "7.1");

		testContains(
			gradleProjectDir, "build.gradle",
			"name: \"com.liferay.portal.kernel\", version: \"3.0.0");

		File mavenProjectDir = buildTemplateWithMaven(
			"war-mvc-portlet", "WarMVCPortlet", "warmvcportlet",
			"-DclassName=WarMVCPortlet", "-Dpackage=WarMVCPortlet",
			"-DliferayVersion=7.1");

		buildProjects(gradleProjectDir, mavenProjectDir);
	}

	@Test
	public void testBuildTemplateWarMVCPortletInWorkspace() throws Exception {
		testBuildTemplateProjectWarInWorkspace(
			"war-mvc-portlet", "WarMVCPortlet", "WarMVCPortlet");
	}

	@Test
	public void testBuildTemplateWarMVCPortletWithPackage() throws Exception {
		File gradleProjectDir = buildTemplateWithGradle(
			"war-mvc-portlet", "WarMVCPortlet", "--package-name",
			"com.liferay.test");

		testExists(gradleProjectDir, "src/main/webapp/init.jsp");
		testExists(gradleProjectDir, "src/main/webapp/view.jsp");

		testContains(
			gradleProjectDir, "build.gradle",
			"apply plugin: \"com.liferay.css.builder\"",
			"apply plugin: \"war\"");
		testContains(
			gradleProjectDir,
			"src/main/webapp/WEB-INF/liferay-plugin-package.properties",
			"name=WarMVCPortlet");

		File mavenProjectDir = buildTemplateWithMaven(
			"war-mvc-portlet", "WarMVCPortlet", "com.liferay.test",
			"-DclassName=WarMVCPortlet", "-Dpackage=com.liferay.test");

		buildProjects(gradleProjectDir, mavenProjectDir);
	}

	@Test
	public void testBuildTemplateWarMVCPortletWithPortletName()
		throws Exception {

		File gradleProjectDir = buildTemplateWithGradle(
			"war-mvc-portlet", "WarMVCPortlet");

		testExists(gradleProjectDir, "src/main/webapp/init.jsp");
		testExists(gradleProjectDir, "src/main/webapp/view.jsp");

		testContains(
			gradleProjectDir, "build.gradle",
			"apply plugin: \"com.liferay.css.builder\"",
			"apply plugin: \"war\"");
		testContains(
			gradleProjectDir,
			"src/main/webapp/WEB-INF/liferay-plugin-package.properties",
			"name=WarMVCPortlet");

		File mavenProjectDir = buildTemplateWithMaven(
			"war-mvc-portlet", "WarMVCPortlet", "warmvcportlet",
			"-DclassName=WarMVCPortlet", "-Dpackage=WarMVCPortlet");

		buildProjects(gradleProjectDir, mavenProjectDir);
	}

	@Test
	public void testBuildTemplateWarMVCPortletWithPortletSuffix()
		throws Exception {

		File gradleProjectDir = buildTemplateWithGradle(
			"war-mvc-portlet", "WarMVC-portlet");

		testExists(gradleProjectDir, "src/main/webapp/init.jsp");
		testExists(gradleProjectDir, "src/main/webapp/view.jsp");

		testContains(
			gradleProjectDir, "build.gradle",
			"apply plugin: \"com.liferay.css.builder\"",
			"apply plugin: \"war\"");
		testContains(
			gradleProjectDir,
			"src/main/webapp/WEB-INF/liferay-plugin-package.properties",
			"name=WarMVC-portlet");

		File mavenProjectDir = buildTemplateWithMaven(
			"war-mvc-portlet", "WarMVC-portlet", "warmvc.portlet",
			"-DclassName=WarMVCPortlet", "-Dpackage=WarMVC.portlet");

		buildProjects(gradleProjectDir, mavenProjectDir);
	}

	@Test
	public void testBuildTemplateWarMvcWithBOM() throws Exception {
		File gradleProjectDir = buildTemplateWithGradle(
			"war-mvc-portlet", "war-mvc-dependency-management",
			"--dependency-management-enabled");

		testNotContains(
			gradleProjectDir, "build.gradle",
			DEPENDENCY_PORTAL_KERNEL + ", version: \"2.0.0\"");

		testContains(
			gradleProjectDir, "build.gradle", DEPENDENCY_PORTAL_KERNEL);
	}

	@Test
	public void testBuildTemplateWarMvcWithoutBOM() throws Exception {
		File gradleProjectDir = buildTemplateWithGradle(
			"war-mvc-portlet", "war-mvc-dependency-management");

		testContains(
			gradleProjectDir, "build.gradle",
			DEPENDENCY_PORTAL_KERNEL + ", version: \"2.0.0\"");
	}

}