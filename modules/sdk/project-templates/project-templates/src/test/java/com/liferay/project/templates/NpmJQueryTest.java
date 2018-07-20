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
public class NpmJQueryTest extends ProjectTemplatesTest {

	@Test
	public void testBuildTemplateNpmJQueryPortlet() throws Exception {
		testBuildTemplateNpm(
			"npm-jquery-portlet", "foo", "foo", "Foo",
			"bootstrapRequire.default('<portlet:namespace />');");
	}

	@Test
	public void testBuildTemplateNpmJQueryPortlet71() throws Exception {
		testBuildTemplateNpm71(
			"npm-jquery-portlet", "foo", "foo", "Foo",
			"bootstrapRequire.default('<portlet:namespace />');");
	}

	@Test
	public void testBuildTemplateNpmJQueryPortletWithBOM() throws Exception {
		File gradleProjectDir = buildTemplateWithGradle(
			"npm-jquery-portlet", "jquery-dependency-management",
			"--dependency-management-enabled");

		testNotContains(
			gradleProjectDir, "build.gradle",
			DEPENDENCY_PORTAL_KERNEL + ", version: \"2.0.0\"");

		testContains(
			gradleProjectDir, "build.gradle", DEPENDENCY_PORTAL_KERNEL);
	}

	@Test
	public void testBuildTemplateNpmJQueryPortletWithDashes() throws Exception {
		testBuildTemplateNpm(
			"npm-jquery-portlet", "foo-bar", "foo.bar", "FooBar",
			"bootstrapRequire.default('<portlet:namespace />');");
	}

	@Test
	public void testBuildTemplateNpmJQueryPortletWithDashes71()
		throws Exception {

		testBuildTemplateNpm71(
			"npm-jquery-portlet", "foo-bar", "foo.bar", "FooBar",
			"bootstrapRequire.default('<portlet:namespace />');");
	}

	@Test
	public void testBuildTemplateNpmJQueryPortletWithoutBOM() throws Exception {
		File gradleProjectDir = buildTemplateWithGradle(
			"npm-jquery-portlet", "jquery-dependency-management");

		testContains(
			gradleProjectDir, "build.gradle",
			DEPENDENCY_PORTAL_KERNEL + ", version: \"2.0.0\"");
	}

}