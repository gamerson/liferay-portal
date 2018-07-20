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
import com.liferay.project.templates.internal.util.Validator;

import java.io.File;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;

/**
 * @author Lawrence Lee
 * @author Gregory Amerson
 * @author Andrea Di Giorgi
 */
public class SoyPortletTest extends ProjectTemplatesTest {

	@Test
	public void testBuildTemplateSoyPortlet() throws Exception {
		File gradleProjectDir = buildTemplateWithGradle(
			"soy-portlet", "foo", "--package-name", "com.liferay.test");

		testExists(gradleProjectDir, "bnd.bnd");
		testExists(
			gradleProjectDir,
			"src/main/resources/META-INF/resources/Footer.soy");
		testExists(
			gradleProjectDir,
			"src/main/resources/META-INF/resources/Footer.es.js");
		testExists(
			gradleProjectDir,
			"src/main/resources/META-INF/resources/Header.soy");
		testExists(
			gradleProjectDir,
			"src/main/resources/META-INF/resources/Header.es.js");
		testExists(
			gradleProjectDir,
			"src/main/resources/META-INF/resources/Navigation.soy");
		testExists(
			gradleProjectDir,
			"src/main/resources/META-INF/resources/Navigation.es.js");
		testExists(
			gradleProjectDir, "src/main/resources/META-INF/resources/View.soy");
		testExists(
			gradleProjectDir,
			"src/main/resources/META-INF/resources/View.es.js");

		testNotExists(gradleProjectDir, "gulpfile.js");

		testContains(
			gradleProjectDir, "build.gradle",
			"apply plugin: \"com.liferay.plugin\"",
			"compileOnly group: \"javax.portlet\", name: \"portlet-api\", " +
				"version: \"2.0\"");
		testContains(
			gradleProjectDir,
			"src/main/java/com/liferay/test/constants/FooPortletKeys.java",
			"public static final String Foo = \"foo\"");
		testContains(
			gradleProjectDir,
			"src/main/java/com/liferay/test/portlet/FooPortlet.java",
			"public class FooPortlet extends SoyPortlet {");
		testContains(
			gradleProjectDir,
			"src/main/java/com/liferay/test/portlet/action" +
				"/FooViewMVCRenderCommand.java",
			"public class FooViewMVCRenderCommand");

		File mavenProjectDir = buildTemplateWithMaven(
			"soy-portlet", "foo", "com.test", "-DclassName=Foo",
			"-Dpackage=com.liferay.test");

		testExists(mavenProjectDir, "gulpfile.js");

		File pomXmlFile = new File(mavenProjectDir, "pom.xml");

		Path pomXmlPath = pomXmlFile.toPath();

		testPomXmlContainsDependency(
			pomXmlPath, "javax.portlet", "portlet-api", "2.0");

		File mavenPackageJsonFile = new File(mavenProjectDir, "package.json");

		Path mavenPackageJsonPath = mavenPackageJsonFile.toPath();

		String mavenPackageJSON = FileUtil.read(mavenPackageJsonPath) + "\n";

		Files.write(
			mavenPackageJsonPath,
			mavenPackageJSON.getBytes(StandardCharsets.UTF_8));

		if (Validator.isNotNull(System.getenv("JENKINS_HOME"))) {
			addNpmrc(gradleProjectDir);
			addNpmrc(mavenProjectDir);
			configureExecuteNpmTask(gradleProjectDir);
			configurePomNpmConfiguration(mavenProjectDir);
		}

		buildProjects(gradleProjectDir, mavenProjectDir);

		File gradleJarFile = new File(
			gradleProjectDir, "build/libs/com.liferay.test-1.0.0.jar");
		File mavenJarFile = new File(mavenProjectDir, "target/foo-1.0.0.jar");

		testContainsJarEntry(gradleJarFile, "package.json");
		testContainsJarEntry(mavenJarFile, "package.json");
	}

	@Test
	public void testBuildTemplateSoyPortlet71() throws Exception {
		File gradleProjectDir = buildTemplateWithGradle(
			"soy-portlet", "foo", "--package-name", "com.liferay.test",
			"--liferayVersion", "7.1");

		testContains(
			gradleProjectDir, "build.gradle",
			"name: \"com.liferay.portal.kernel\", version: \"3.0.0",
			"compileOnly group: \"javax.portlet\", name: \"portlet-api\", " +
				"version: \"3.0.0\"");

		File mavenProjectDir = buildTemplateWithMaven(
			"soy-portlet", "foo", "com.test", "-DclassName=Foo",
			"-Dpackage=com.liferay.test", "-DliferayVersion=7.1");

		testContains(
			mavenProjectDir, "bnd.bnd", "-contract: JavaPortlet,JavaServlet");

		File pomXmlFile = new File(mavenProjectDir, "pom.xml");

		Path pomXmlPath = pomXmlFile.toPath();

		testPomXmlContainsDependency(
			pomXmlPath, "com.liferay.portal", "com.liferay.portal.kernel",
			"3.0.0");

		testPomXmlContainsDependency(
			pomXmlPath, "javax.portlet", "portlet-api", "3.0.0");

		File mavenPackageJsonFile = new File(mavenProjectDir, "package.json");

		Path mavenPackageJsonPath = mavenPackageJsonFile.toPath();

		String mavenPackageJSON = FileUtil.read(mavenPackageJsonPath) + "\n";

		Files.write(
			mavenPackageJsonPath,
			mavenPackageJSON.getBytes(StandardCharsets.UTF_8));

		if (Validator.isNotNull(System.getenv("JENKINS_HOME"))) {
			addNpmrc(gradleProjectDir);
			addNpmrc(mavenProjectDir);
			configureExecuteNpmTask(gradleProjectDir);
			configurePomNpmConfiguration(mavenProjectDir);
		}

		buildProjects(gradleProjectDir, mavenProjectDir);

		File gradleJarFile = new File(
			gradleProjectDir, "build/libs/com.liferay.test-1.0.0.jar");
		File mavenJarFile = new File(mavenProjectDir, "target/foo-1.0.0.jar");

		testContainsJarEntry(gradleJarFile, "package.json");
		testContainsJarEntry(mavenJarFile, "package.json");
	}

	@Test
	public void testBuildTemplateSoyPortletCustomClass() throws Exception {
		File gradleProjectDir = buildTemplateWithGradle(
			"soy-portlet", "foo", "--class-name", "MySoyPortlet");

		testContains(
			gradleProjectDir,
			"src/main/java/foo/portlet/MySoyPortletPortlet.java",
			"public class MySoyPortletPortlet extends SoyPortlet {");
	}

	@Test
	public void testBuildTemplateSoyPortletCustomClass71() throws Exception {
		File gradleProjectDir = buildTemplateWithGradle(
			"soy-portlet", "foo", "--class-name", "MySPR", "--liferayVersion",
			"7.1");

		testContains(
			gradleProjectDir,
			"src/main/java/foo/portlet/MySPRSoyPortletRegister.java",
			"public class MySPRSoyPortletRegister implements " +
				"SoyPortletRegister {");
	}

	@Test
	public void testBuildTemplateSoyPortletWithBOM() throws Exception {
		File gradleProjectDir = buildTemplateWithGradle(
			"soy-portlet", "soy-dependency-management",
			"--dependency-management-enabled");

		testNotContains(
			gradleProjectDir, "build.gradle",
			DEPENDENCY_PORTAL_KERNEL + ", version: \"2.0.0\"");

		testContains(
			gradleProjectDir, "build.gradle", DEPENDENCY_PORTAL_KERNEL);
	}

	@Test
	public void testBuildTemplateSoyPortletWithoutBOM() throws Exception {
		File gradleProjectDir = buildTemplateWithGradle(
			"soy-portlet", "soy-dependency-management");

		testContains(
			gradleProjectDir, "build.gradle",
			DEPENDENCY_PORTAL_KERNEL + ", version: \"2.0.0\"");
	}

}