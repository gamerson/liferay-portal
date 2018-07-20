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

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Lawrence Lee
 * @author Gregory Amerson
 * @author Andrea Di Giorgi
 */
public class ServiceBuilderTest extends ProjectTemplatesTest {

	@Test
	public void testBuildTemplateServiceBuilder() throws Exception {
		String name = "guestbook";
		String packageName = "com.liferay.docs.guestbook";

		File gradleProjectDir = buildTemplateWithGradle(
			"service-builder", name, "--package-name", packageName);

		testContains(
			gradleProjectDir, name + "-api/build.gradle",
			"name: \"com.liferay.portal.kernel\", version: \"2");
		testContains(
			gradleProjectDir, name + "-service/build.gradle",
			"name: \"com.liferay.portal.kernel\", version: \"2");

		File mavenProjectDir = buildTemplateWithMaven(
			"service-builder", name, "com.test", "-Dpackage=" + packageName);

		testBuildTemplateServiceBuilder(
			gradleProjectDir, mavenProjectDir, gradleProjectDir, name,
			packageName, "");
	}

	@Test
	public void testBuildTemplateServiceBuilder71() throws Exception {
		String name = "guestbook";
		String packageName = "com.liferay.docs.guestbook";

		File gradleProjectDir = buildTemplateWithGradle(
			"service-builder", name, "--package-name", packageName,
			"--liferayVersion", "7.1");

		testContains(
			gradleProjectDir, name + "-api/build.gradle",
			"name: \"com.liferay.portal.kernel\", version: \"3.0.0");
		testContains(
			gradleProjectDir, name + "-service/build.gradle",
			"name: \"com.liferay.portal.kernel\", version: \"3.0.0");

		File mavenProjectDir = buildTemplateWithMaven(
			"service-builder", name, "com.test", "-Dpackage=" + packageName,
			"-DliferayVersion=7.1");

		testBuildTemplateServiceBuilder(
			gradleProjectDir, mavenProjectDir, gradleProjectDir, name,
			packageName, "");
	}

	@Test
	public void testBuildTemplateServiceBuilderNestedPath() throws Exception {
		File workspaceProjectDir = buildTemplateWithGradle(
			WorkspaceUtil.WORKSPACE, "ws-nested-path");

		File destinationDir = new File(
			workspaceProjectDir, "modules/nested/path");

		Assert.assertTrue(destinationDir.mkdirs());

		File gradleProjectDir = buildTemplateWithGradle(
			destinationDir, "service-builder", "sample", "--package-name",
			"com.test.sample");

		testContains(
			gradleProjectDir, "sample-service/build.gradle",
			"compileOnly project(\":modules:nested:path:sample:sample-api\")");

		File mavenProjectDir = buildTemplateWithMaven(
			"service-builder", "sample", "com.test",
			"-Dpackage=com.test.sample");

		testBuildTemplateServiceBuilder(
			gradleProjectDir, mavenProjectDir, workspaceProjectDir, "sample",
			"com.test.sample", ":modules:nested:path:sample");
	}

	@Test
	public void testBuildTemplateServiceBuilderNestedPath71() throws Exception {
		File workspaceProjectDir = buildTemplateWithGradle(
			WorkspaceUtil.WORKSPACE, "ws-nested-path");

		File destinationDir = new File(
			workspaceProjectDir, "modules/nested/path");

		Assert.assertTrue(destinationDir.mkdirs());

		File gradleProjectDir = buildTemplateWithGradle(
			destinationDir, "service-builder", "sample", "--package-name",
			"com.test.sample", "--liferayVersion", "7.1");

		testContains(
			gradleProjectDir, "sample-service/build.gradle",
			"compileOnly project(\":modules:nested:path:sample:sample-api\")");

		File mavenProjectDir = buildTemplateWithMaven(
			"service-builder", "sample", "com.test",
			"-Dpackage=com.test.sample", "-DliferayVersion=7.1");

		testBuildTemplateServiceBuilder(
			gradleProjectDir, mavenProjectDir, workspaceProjectDir, "sample",
			"com.test.sample", ":modules:nested:path:sample");
	}

	@Test
	public void testBuildTemplateServiceBuilderWithDashes() throws Exception {
		String name = "backend-integration";
		String packageName = "com.liferay.docs.guestbook";

		File gradleProjectDir = buildTemplateWithGradle(
			"service-builder", name, "--package-name", packageName);

		testContains(
			gradleProjectDir, name + "-api/build.gradle",
			"name: \"com.liferay.portal.kernel\", version: \"2");
		testContains(
			gradleProjectDir, name + "-service/build.gradle",
			"name: \"com.liferay.portal.kernel\", version: \"2");

		File mavenProjectDir = buildTemplateWithMaven(
			"service-builder", name, "com.test", "-Dpackage=" + packageName);

		testBuildTemplateServiceBuilder(
			gradleProjectDir, mavenProjectDir, gradleProjectDir, name,
			packageName, "");
	}

	@Test
	public void testBuildTemplateServiceBuilderWithDashes71() throws Exception {
		String name = "backend-integration";
		String packageName = "com.liferay.docs.guestbook";

		File gradleProjectDir = buildTemplateWithGradle(
			"service-builder", name, "--package-name", packageName,
			"--liferayVersion", "7.1");

		testContains(
			gradleProjectDir, name + "-api/build.gradle",
			"name: \"com.liferay.portal.kernel\", version: \"3");
		testContains(
			gradleProjectDir, name + "-service/build.gradle",
			"name: \"com.liferay.portal.kernel\", version: \"3");

		File mavenProjectDir = buildTemplateWithMaven(
			"service-builder", name, "com.test", "-Dpackage=" + packageName,
			"-DliferayVersion=7.1");

		testBuildTemplateServiceBuilder(
			gradleProjectDir, mavenProjectDir, gradleProjectDir, name,
			packageName, "");
	}

}