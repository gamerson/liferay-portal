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

import com.liferay.project.templates.internal.util.FileUtil;
import com.liferay.project.templates.util.FileTestUtil;

import java.io.File;

import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Lawrence Lee
 * @author Gregory Amerson
 * @author Andrea Di Giorgi
 */
public class MiscTest extends ProjectTemplatesTest {

	@Test
	public void testBuildTemplate() throws Exception {
		File gradleProjectDir = buildTemplateWithGradle(
			null, "hello-world-portlet");

		testExists(gradleProjectDir, "bnd.bnd");
		testExists(
			gradleProjectDir, "src/main/resources/META-INF/resources/init.jsp");
		testExists(
			gradleProjectDir, "src/main/resources/META-INF/resources/view.jsp");

		testContains(
			gradleProjectDir, "build.gradle",
			"apply plugin: \"com.liferay.plugin\"");
		testContains(
			gradleProjectDir,
			"src/main/java/hello/world/portlet/portlet/HelloWorldPortlet.java",
			"public class HelloWorldPortlet extends MVCPortlet {");

		File mavenProjectDir = buildTemplateWithMaven(
			"mvc-portlet", "hello-world-portlet", "com.test",
			"-DclassName=HelloWorld", "-Dpackage=hello.world.portlet");

		buildProjects(gradleProjectDir, mavenProjectDir);
	}

	@Test
	public void testBuildTemplateFragment() throws Exception {
		File gradleProjectDir = buildTemplateWithGradle(
			"fragment", "loginhook", "--host-bundle-symbolic-name",
			"com.liferay.login.web", "--host-bundle-version", "1.0.0");

		testContains(
			gradleProjectDir, "bnd.bnd", "Bundle-SymbolicName: loginhook",
			"Fragment-Host: com.liferay.login.web;bundle-version=\"1.0.0\"");
		testContains(
			gradleProjectDir, "build.gradle",
			"apply plugin: \"com.liferay.plugin\"");

		File mavenProjectDir = buildTemplateWithMaven(
			"fragment", "loginhook", "com.test",
			"-DhostBundleSymbolicName=com.liferay.login.web",
			"-DhostBundleVersion=1.0.0", "-Dpackage=loginhook");

		buildProjects(gradleProjectDir, mavenProjectDir);
	}

	@Test
	public void testBuildTemplateInWorkspace() throws Exception {
		testBuildTemplateWithWorkspace(
			null, "hello-world-portlet",
			"build/libs/hello.world.portlet-1.0.0.jar");
	}

	@Test
	public void testBuildTemplateLayoutTemplate() throws Exception {
		File gradleProjectDir = buildTemplateWithGradle(
			"layout-template", "foo");

		testExists(gradleProjectDir, "src/main/webapp/foo.png");

		testContains(
			gradleProjectDir, "src/main/webapp/foo.ftl", "class=\"foo\"");
		testContains(
			gradleProjectDir,
			"src/main/webapp/WEB-INF/liferay-layout-templates.xml",
			"<layout-template id=\"foo\" name=\"foo\">",
			"<template-path>/foo.ftl</template-path>",
			"<thumbnail-path>/foo.png</thumbnail-path>");
		testContains(
			gradleProjectDir,
			"src/main/webapp/WEB-INF/liferay-plugin-package.properties",
			"name=foo");
		testEquals(gradleProjectDir, "build.gradle", "apply plugin: \"war\"");

		File mavenProjectDir = buildTemplateWithMaven(
			"layout-template", "foo", "com.test");

		createNewFiles(
			"src/main/resources/.gitkeep", gradleProjectDir, mavenProjectDir);

		buildProjects(gradleProjectDir, mavenProjectDir);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testBuildTemplateOnExistingDirectory() throws Exception {
		File destinationDir = temporaryFolder.newFolder("gradle");

		buildTemplateWithGradle(destinationDir, "activator", "dup-activator");
		buildTemplateWithGradle(destinationDir, "activator", "dup-activator");
	}

	@Test
	public void testBuildTemplateWithGradle() throws Exception {
		buildTemplateWithGradle(
			temporaryFolder.newFolder(), null, "foo-portlet", false, false);
		buildTemplateWithGradle(
			temporaryFolder.newFolder(), null, "foo-portlet", false, true);
		buildTemplateWithGradle(
			temporaryFolder.newFolder(), null, "foo-portlet", true, false);
		buildTemplateWithGradle(
			temporaryFolder.newFolder(), null, "foo-portlet", true, true);
	}

	@Test
	public void testBuildTemplateWithPackageName() throws Exception {
		File gradleProjectDir = buildTemplateWithGradle(
			"", "barfoo", "--package-name", "foo.bar");

		testExists(
			gradleProjectDir, "src/main/resources/META-INF/resources/init.jsp");
		testExists(
			gradleProjectDir, "src/main/resources/META-INF/resources/view.jsp");

		testContains(
			gradleProjectDir, "bnd.bnd", "Bundle-SymbolicName: foo.bar");
		testContains(
			gradleProjectDir, "build.gradle",
			"apply plugin: \"com.liferay.plugin\"");

		File mavenProjectDir = buildTemplateWithMaven(
			"mvc-portlet", "barfoo", "com.test", "-DclassName=Barfoo",
			"-Dpackage=foo.bar");

		buildProjects(gradleProjectDir, mavenProjectDir);
	}

	@Test
	public void testListTemplates() throws Exception {
		final Map<String, String> expectedTemplates = new TreeMap<>();

		try (DirectoryStream<Path> directoryStream =
				FileTestUtil.getProjectTemplatesDirectoryStream()) {

			for (Path path : directoryStream) {
				String fileName = String.valueOf(path.getFileName());

				String template = fileName.substring(
					FileTestUtil.PROJECT_TEMPLATE_DIR_PREFIX.length());

				if (!template.equals(WorkspaceUtil.WORKSPACE)) {
					Properties properties = FileUtil.readProperties(
						path.resolve("bnd.bnd"));

					String bundleDescription = properties.getProperty(
						"Bundle-Description");

					expectedTemplates.put(template, bundleDescription);
				}
			}
		}

		Assert.assertEquals(expectedTemplates, ProjectTemplates.getTemplates());
	}

	@Test
	public void testListTemplatesWithCustomArchetypesDir() throws Exception {
		File archetypesDir = FileUtil.getJarFile(ProjectTemplatesTest.class);

		Path templateFilePath = FileTestUtil.getFile(
			archetypesDir.toPath(), "*.jar");

		Assert.assertNotNull(templateFilePath);

		File customArchetypesDir = temporaryFolder.newFolder();

		Path customArchetypesDirPath = customArchetypesDir.toPath();

		String fileName = String.valueOf(templateFilePath.getFileName());

		String suffix = fileName.substring(fileName.indexOf('-'));

		Files.copy(
			templateFilePath,
			customArchetypesDirPath.resolve(
				"custom.name.project.templates.foo.bar-" + suffix));

		List<File> customArchetypesDirs = new ArrayList<>();

		customArchetypesDirs.add(customArchetypesDir);

		Map<String, String> customTemplatesMap = ProjectTemplates.getTemplates(
			customArchetypesDirs);

		Map<String, String> templatesMap = ProjectTemplates.getTemplates();

		Assert.assertEquals(customTemplatesMap.size(), templatesMap.size() + 1);
	}

}