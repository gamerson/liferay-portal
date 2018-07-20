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

import org.apache.commons.compress.archivers.zip.ZipFile;

import org.junit.Test;

/**
 * @author Lawrence Lee
 * @author Gregory Amerson
 * @author Andrea Di Giorgi
 */
public class SpringMVCTest extends ProjectTemplatesTest {

	@Test
	public void testBuildTemplateSpringMVCPortlet() throws Exception {
		File gradleProjectDir = buildTemplateWithGradle(
			"spring-mvc-portlet", "foo");

		testExists(gradleProjectDir, "src/main/webapp/WEB-INF/jsp/init.jsp");
		testExists(gradleProjectDir, "src/main/webapp/WEB-INF/jsp/view.jsp");

		testContains(
			gradleProjectDir,
			"src/main/java/foo/portlet/FooPortletViewController.java",
			"public class FooPortletViewController {");

		File mavenProjectDir = buildTemplateWithMaven(
			"spring-mvc-portlet", "foo", "com.test", "-DclassName=Foo",
			"-Dpackage=foo");

		buildProjects(gradleProjectDir, mavenProjectDir);

		ZipFile zipFile = null;

		File gradleWarFile = new File(gradleProjectDir, "build/libs/foo.war");

		try {
			zipFile = new ZipFile(gradleWarFile);

			testExists(zipFile, "css/main.css");
			testExists(zipFile, "css/main_rtl.css");

			testExists(zipFile, "WEB-INF/lib/aopalliance-1.0.jar");
			testExists(zipFile, "WEB-INF/lib/commons-logging-1.2.jar");

			for (String jarName : SPRING_MVC_PORTLET_JAR_NAMES) {
				testExists(
					zipFile,
					"WEB-INF/lib/spring-" + jarName + "-" +
						SPRING_MVC_PORTLET_VERSION + ".jar");
			}
		}
		finally {
			ZipFile.closeQuietly(zipFile);
		}
	}

	@Test
	public void testBuildTemplateSpringMVCPortlet71() throws Exception {
		File gradleProjectDir = buildTemplateWithGradle(
			"spring-mvc-portlet", "foo", "--liferayVersion", "7.1");

		testContains(
			gradleProjectDir, "build.gradle",
			"name: \"com.liferay.portal.kernel\", version: \"3.0.0");

		File mavenProjectDir = buildTemplateWithMaven(
			"spring-mvc-portlet", "foo", "com.test", "-DclassName=Foo",
			"-Dpackage=foo", "-DliferayVersion=7.1");

		buildProjects(gradleProjectDir, mavenProjectDir);
	}

	@Test
	public void testBuildTemplateSpringMVCPortletInWorkspace()
		throws Exception {

		testBuildTemplateProjectWarInWorkspace(
			"spring-mvc-portlet", "foo", "foo");
	}

	@Test
	public void testBuildTemplateSpringMvcPortletWithBOM() throws Exception {
		File gradleProjectDir = buildTemplateWithGradle(
			"spring-mvc-portlet", "spring-mvc-dependency-management",
			"--dependency-management-enabled");

		testNotContains(
			gradleProjectDir, "build.gradle",
			DEPENDENCY_PORTAL_KERNEL + ", version: \"2.6.0\"");

		testContains(
			gradleProjectDir, "build.gradle", DEPENDENCY_PORTAL_KERNEL);
	}

	@Test
	public void testBuildTemplateSpringMvcPortletWithoutBOM() throws Exception {
		File gradleProjectDir = buildTemplateWithGradle(
			"spring-mvc-portlet", "spring-mvc-dependency-management");

		testContains(
			gradleProjectDir, "build.gradle",
			DEPENDENCY_PORTAL_KERNEL + ", version: \"2.6.0\"");
	}

	@Test
	public void testBuildTemplateSpringMVCPortletWithPackage()
		throws Exception {

		File gradleProjectDir = buildTemplateWithGradle(
			"spring-mvc-portlet", "foo", "--package-name", "com.liferay.test");

		testExists(gradleProjectDir, "src/main/webapp/WEB-INF/jsp/init.jsp");
		testExists(gradleProjectDir, "src/main/webapp/WEB-INF/jsp/view.jsp");

		testContains(
			gradleProjectDir,
			"src/main/java/com/liferay/test/portlet" +
				"/FooPortletViewController.java",
			"public class FooPortletViewController {");

		File mavenProjectDir = buildTemplateWithMaven(
			"spring-mvc-portlet", "foo", "com.test", "-DclassName=Foo",
			"-Dpackage=com.liferay.test");

		buildProjects(gradleProjectDir, mavenProjectDir);
	}

	@Test
	public void testBuildTemplateSpringMVCPortletWithPortletName()
		throws Exception {

		File gradleProjectDir = buildTemplateWithGradle(
			"spring-mvc-portlet", "portlet");

		testExists(gradleProjectDir, "src/main/webapp/WEB-INF/jsp/init.jsp");
		testExists(gradleProjectDir, "src/main/webapp/WEB-INF/jsp/view.jsp");

		testContains(
			gradleProjectDir,
			"src/main/java/portlet/portlet/PortletPortletViewController.java",
			"public class PortletPortletViewController {");

		File mavenProjectDir = buildTemplateWithMaven(
			"spring-mvc-portlet", "portlet", "com.test", "-DclassName=Portlet",
			"-Dpackage=portlet");

		buildProjects(gradleProjectDir, mavenProjectDir);
	}

	@Test
	public void testBuildTemplateSpringMVCPortletWithPortletSuffix()
		throws Exception {

		File gradleProjectDir = buildTemplateWithGradle(
			"spring-mvc-portlet", "portlet-portlet");

		testExists(gradleProjectDir, "src/main/webapp/WEB-INF/jsp/init.jsp");
		testExists(gradleProjectDir, "src/main/webapp/WEB-INF/jsp/view.jsp");

		testContains(
			gradleProjectDir,
			"src/main/java/portlet/portlet/portlet" +
				"/PortletPortletViewController.java",
			"public class PortletPortletViewController {");

		File mavenProjectDir = buildTemplateWithMaven(
			"spring-mvc-portlet", "portlet-portlet", "com.test",
			"-DclassName=Portlet", "-Dpackage=portlet.portlet");

		buildProjects(gradleProjectDir, mavenProjectDir);
	}

}