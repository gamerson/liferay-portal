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
public class TemplateContextContributorTest extends ProjectTemplatesTest {

	@Test
	public void testBuildTemplateTemplateContextContributor() throws Exception {
		File gradleProjectDir = buildTemplateWithGradle(
			"template-context-contributor", "blade-test");

		testExists(gradleProjectDir, "bnd.bnd");

		testContains(
			gradleProjectDir, "build.gradle",
			"apply plugin: \"com.liferay.plugin\"");

		testContains(
			gradleProjectDir,
			"src/main/java/blade/test/context/contributor" +
				"/BladeTestTemplateContextContributor.java",
			"public class BladeTestTemplateContextContributor",
			"implements TemplateContextContributor");

		File mavenProjectDir = buildTemplateWithMaven(
			"template-context-contributor", "blade-test", "com.test",
			"-DclassName=BladeTest", "-Dpackage=blade.test");

		buildProjects(gradleProjectDir, mavenProjectDir);
	}

	@Test
	public void testBuildTemplateTemplateContextContributor71()
		throws Exception {

		File gradleProjectDir = buildTemplateWithGradle(
			"template-context-contributor", "blade-test", "--liferayVersion",
			"7.1");

		testExists(gradleProjectDir, "bnd.bnd");

		testContains(
			gradleProjectDir, "build.gradle",
			"apply plugin: \"com.liferay.plugin\"",
			"name: \"com.liferay.portal.kernel\", version: \"3.0.0");

		File mavenProjectDir = buildTemplateWithMaven(
			"template-context-contributor", "blade-test", "com.test",
			"-DclassName=BladeTest", "-Dpackage=blade.test",
			"-DliferayVersion=7.1");

		buildProjects(gradleProjectDir, mavenProjectDir);
	}

	@Test
	public void testBuildTemplateTemplateContextContributorInWorkspace()
		throws Exception {

		testBuildTemplateWithWorkspace(
			"template-context-contributor", "blade-test",
			"build/libs/blade.test-1.0.0.jar");
	}

	@Test
	public void testBuildTemplateTemplateContextContributorWithBOM()
		throws Exception {

		File gradleProjectDir = buildTemplateWithGradle(
			"template-context-contributor",
			"context-contributor-dependency-management",
			"--dependency-management-enabled");

		testNotContains(
			gradleProjectDir, "build.gradle",
			DEPENDENCY_PORTAL_KERNEL + ", version: \"2.0.0\"");

		testContains(
			gradleProjectDir, "build.gradle", DEPENDENCY_PORTAL_KERNEL);
	}

	@Test
	public void testBuildTemplateTemplateContextContributorWithoutBOM()
		throws Exception {

		File gradleProjectDir = buildTemplateWithGradle(
			"template-context-contributor",
			"context-contributor-dependency-management");

		testContains(
			gradleProjectDir, "build.gradle",
			DEPENDENCY_PORTAL_KERNEL + ", version: \"2.0.0\"");
	}

}