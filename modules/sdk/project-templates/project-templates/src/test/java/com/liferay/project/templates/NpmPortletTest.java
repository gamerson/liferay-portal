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
public class NpmPortletTest extends ProjectTemplatesTest {

	@Test
	public void testBuildTemplateNpmPortlet() throws Exception {
		testBuildTemplateNpm(
			"npm-portlet", "foo", "foo", "Foo",
			"bootstrapRequire.default('<portlet:namespace />');");
	}

	@Test
	public void testBuildTemplateNpmPortlet71() throws Exception {
		testBuildTemplateNpm71(
			"npm-portlet", "foo", "foo", "Foo",
			"bootstrapRequire.default('<portlet:namespace />');");
	}

	@Test
	public void testBuildTemplateNpmPortletWithBOM() throws Exception {
		File gradleProjectDir = buildTemplateWithGradle(
			"npm-portlet", "npm-portlet-dependency-management",
			"--dependency-management-enabled");

		testNotContains(
			gradleProjectDir, "build.gradle",
			DEPENDENCY_PORTAL_KERNEL + ", version: \"2.0.0\"");

		testContains(
			gradleProjectDir, "build.gradle", DEPENDENCY_PORTAL_KERNEL);
	}

	@Test
	public void testBuildTemplateNpmPortletWithDashes() throws Exception {
		testBuildTemplateNpm(
			"npm-portlet", "foo-bar", "foo.bar", "FooBar",
			"bootstrapRequire.default('<portlet:namespace />');");
	}

	@Test
	public void testBuildTemplateNpmPortletWithDashes71() throws Exception {
		testBuildTemplateNpm71(
			"npm-portlet", "foo-bar", "foo.bar", "FooBar",
			"bootstrapRequire.default('<portlet:namespace />');");
	}

	@Test
	public void testBuildTemplateNpmPortletWithoutBOM() throws Exception {
		File gradleProjectDir = buildTemplateWithGradle(
			"npm-portlet", "npm-portlet-dependency-management");

		testContains(
			gradleProjectDir, "build.gradle",
			DEPENDENCY_PORTAL_KERNEL + ", version: \"2.0.0\"");
	}

}