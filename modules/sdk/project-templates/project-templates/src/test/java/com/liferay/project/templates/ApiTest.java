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
public class ApiTest extends ProjectTemplatesTest {

	@Test
	public void testBuildTemplateApi() throws Exception {
		File gradleProjectDir = buildTemplateWithGradle("api", "foo");

		testExists(gradleProjectDir, "bnd.bnd");

		testContains(
			gradleProjectDir, "build.gradle",
			"apply plugin: \"com.liferay.plugin\"");
		testContains(
			gradleProjectDir, "src/main/java/foo/api/Foo.java",
			"public interface Foo");
		testContains(
			gradleProjectDir, "src/main/resources/foo/api/packageinfo",
			"1.0.0");

		File mavenProjectDir = buildTemplateWithMaven(
			"api", "foo", "com.test", "-DclassName=Foo", "-Dpackage=foo");

		buildProjects(gradleProjectDir, mavenProjectDir);
	}

	@Test
	public void testBuildTemplateApiContainsCorrectAuthor() throws Exception {
		String author = "Test Author";

		File gradleProjectDir = buildTemplateWithGradle(
			"api", "author-test", "--author", author);

		testContains(
			gradleProjectDir, "src/main/java/author/test/api/AuthorTest.java",
			"@author " + author);

		File mavenProjectDir = buildTemplateWithMaven(
			"api", "author-test", "com.test", "-Dauthor=" + author,
			"-DclassName=AuthorTest", "-Dpackage=author.test");

		testContains(
			mavenProjectDir, "src/main/java/author/test/api/AuthorTest.java",
			"@author " + author);
	}

	@Test
	public void testBuildTemplateApiInWorkspace() throws Exception {
		testBuildTemplateWithWorkspace(
			"api", "foo", "build/libs/foo-1.0.0.jar");
	}

	@Test
	public void testBuildTemplateApiWithBOM() throws Exception {
		File gradleProjectDir = buildTemplateWithGradle(
			"api", "api-dependency-management",
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
	public void testBuildTemplateApiWithoutBOM() throws Exception {
		File gradleProjectDir = buildTemplateWithGradle(
			"api", "api-dependency-management");

		testContains(
			gradleProjectDir, "build.gradle",
			"compileOnly group: \"org.osgi\", name: \"org.osgi.core\", " +
				"version: \"6.0.0\"");
	}

}