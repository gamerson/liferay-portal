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
public class FormFieldTest extends ProjectTemplatesTest {

	@Test
	public void testBuildTemplateFormField() throws Exception {
		File gradleProjectDir = buildTemplateWithGradle("form-field", "foobar");

		testExists(gradleProjectDir, "bnd.bnd");

		testContains(
			gradleProjectDir, "build.gradle",
			"apply plugin: \"com.liferay.plugin\"");
		testContains(
			gradleProjectDir,
			"src/main/java/foobar/form/field/FoobarDDMFormFieldRenderer.java",
			"public class FoobarDDMFormFieldRenderer extends " +
				"BaseDDMFormFieldRenderer {");
		testContains(
			gradleProjectDir,
			"src/main/java/foobar/form/field/FoobarDDMFormFieldType.java",
			"class FoobarDDMFormFieldType extends BaseDDMFormFieldType");
		testContains(
			gradleProjectDir, "src/main/resources/META-INF/resources/config.js",
			"'foobar-form-field': {");
		testContains(
			gradleProjectDir,
			"src/main/resources/META-INF/resources/foobar.soy",
			"{template .Foobar autoescape");
		testContains(
			gradleProjectDir,
			"src/main/resources/META-INF/resources/foobar_field.js",
			"var FoobarField");

		File mavenProjectDir = buildTemplateWithMaven(
			"form-field", "foobar", "com.test", "-DclassName=Foobar",
			"-Dpackage=foobar");

		buildProjects(gradleProjectDir, mavenProjectDir);
	}

	@Test
	public void testBuildTemplateFormField71() throws Exception {
		File gradleProjectDir = buildTemplateWithGradle(
			"form-field", "foobar", "--liferayVersion", "7.1");

		testContains(
			gradleProjectDir, "build.gradle",
			"name: \"com.liferay.portal.kernel\", version: \"3.0.0",
			"apply plugin: \"com.liferay.plugin\"");

		File mavenProjectDir = buildTemplateWithMaven(
			"form-field", "foobar", "com.test", "-DclassName=Foobar",
			"-Dpackage=foobar", "-DliferayVersion=7.1");

		testContains(
			mavenProjectDir, "bnd.bnd", "-contract: JavaPortlet,JavaServlet");

		buildProjects(gradleProjectDir, mavenProjectDir);
	}

	@Test
	public void testBuildTemplateFormFieldInWorkspace() throws Exception {
		testBuildTemplateWithWorkspace(
			"form-field", "foobar", "build/libs/foobar-1.0.0.jar");
	}

	@Test
	public void testBuildTemplateFormFieldWithBOM() throws Exception {
		File gradleProjectDir = buildTemplateWithGradle(
			"form-field", "field-dependency-management",
			"--dependency-management-enabled");

		testNotContains(
			gradleProjectDir, "build.gradle",
			DEPENDENCY_PORTAL_KERNEL + ", version: \"2.0.0\"");

		testContains(
			gradleProjectDir, "build.gradle", DEPENDENCY_PORTAL_KERNEL);
	}

	@Test
	public void testBuildTemplateFormFieldWithoutBOM() throws Exception {
		File gradleProjectDir = buildTemplateWithGradle(
			"form-field", "field-dependency-management");

		testContains(
			gradleProjectDir, "build.gradle",
			DEPENDENCY_PORTAL_KERNEL + ", version: \"2.0.0\"");
	}

}