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
public class ContentTargetingReportTest extends ProjectTemplatesTest {

	@Test
	public void testBuildTemplateContentTargetingReport() throws Exception {
		File gradleProjectDir = buildTemplateWithGradle(
			"content-targeting-report", "foo-bar");

		testExists(gradleProjectDir, "bnd.bnd");

		testContains(
			gradleProjectDir,
			"src/main/java/foo/bar/content/targeting/report/FooBarReport.java",
			"public class FooBarReport extends BaseJSPReport");

		File mavenProjectDir = buildTemplateWithMaven(
			"content-targeting-report", "foo-bar", "com.test",
			"-DclassName=FooBar", "-Dpackage=foo.bar");

		buildProjects(gradleProjectDir, mavenProjectDir);
	}

	@Test
	public void testBuildTemplateContentTargetingReport71() throws Exception {
		File gradleProjectDir = buildTemplateWithGradle(
			"content-targeting-report", "foo-bar", "--liferayVersion", "7.1");

		testExists(gradleProjectDir, "bnd.bnd");

		testContains(
			gradleProjectDir, "build.gradle",
			"name: \"com.liferay.portal.kernel\", version: \"3.0.0");

		File mavenProjectDir = buildTemplateWithMaven(
			"content-targeting-report", "foo-bar", "com.test",
			"-DclassName=FooBar", "-Dpackage=foo.bar", "-DliferayVersion=7.1");

		testContains(
			mavenProjectDir, "bnd.bnd", "-contract: JavaPortlet,JavaServlet");

		buildProjects(gradleProjectDir, mavenProjectDir);
	}

	@Test
	public void testBuildTemplateContentTargetingReportInWorkspace()
		throws Exception {

		testBuildTemplateWithWorkspace(
			"content-targeting-report", "foo-bar",
			"build/libs/foo.bar-1.0.0.jar");
	}

	@Test
	public void testBuildTemplateContentTargetingReportWithBOM()
		throws Exception {

		File gradleProjectDir = buildTemplateWithGradle(
			"content-targeting-report", "report-dependency-management",
			"--dependency-management-enabled");

		testNotContains(
			gradleProjectDir, "build.gradle",
			DEPENDENCY_PORTAL_KERNEL + ", version: \"2.3.0\"");

		testContains(
			gradleProjectDir, "build.gradle", DEPENDENCY_PORTAL_KERNEL);
	}

	@Test
	public void testBuildTemplateContentTargetingReportWithoutBOM()
		throws Exception {

		File gradleProjectDir = buildTemplateWithGradle(
			"content-targeting-report", "report-dependency-management");

		testContains(
			gradleProjectDir, "build.gradle",
			DEPENDENCY_PORTAL_KERNEL + ", version: \"2.3.0\"");
	}

}