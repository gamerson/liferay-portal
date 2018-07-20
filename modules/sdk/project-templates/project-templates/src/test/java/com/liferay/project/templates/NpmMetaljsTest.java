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
public class NpmMetaljsTest extends ProjectTemplatesTest {

	@Test
	public void testBuildTemplateNpmMetaljsPortlet() throws Exception {
		testBuildTemplateNpm(
			"npm-metaljs-portlet", "foo", "foo", "Foo",
			"bootstrapRequire.default('<portlet:namespace />');");
	}

	@Test
	public void testBuildTemplateNpmMetaljsPortlet71() throws Exception {
		testBuildTemplateNpm71(
			"npm-metaljs-portlet", "foo", "foo", "Foo",
			"bootstrapRequire.default('<portlet:namespace />');");
	}

	@Test
	public void testBuildTemplateNpmMetaljsPortletWithBOM() throws Exception {
		File gradleProjectDir = buildTemplateWithGradle(
			"npm-metaljs-portlet", "metaljs-dependency-management",
			"--dependency-management-enabled");

		testNotContains(
			gradleProjectDir, "build.gradle",
			DEPENDENCY_PORTAL_KERNEL + ", version: \"2.0.0\"");

		testContains(
			gradleProjectDir, "build.gradle", DEPENDENCY_PORTAL_KERNEL);
	}

	@Test
	public void testBuildTemplateNpmMetaljsPortletWithDashes()
		throws Exception {

		testBuildTemplateNpm(
			"npm-metaljs-portlet", "foo-bar", "foo.bar", "FooBar",
			"bootstrapRequire.default('<portlet:namespace />');");
	}

	@Test
	public void testBuildTemplateNpmMetaljsPortletWithDashes71()
		throws Exception {

		testBuildTemplateNpm71(
			"npm-metaljs-portlet", "foo-bar", "foo.bar", "FooBar",
			"bootstrapRequire.default('<portlet:namespace />');");
	}

	@Test
	public void testBuildTemplateNpmMetaljsPortletWithoutBOM()
		throws Exception {

		File gradleProjectDir = buildTemplateWithGradle(
			"npm-metaljs-portlet", "metaljs-dependency-management");

		testContains(
			gradleProjectDir, "build.gradle",
			DEPENDENCY_PORTAL_KERNEL + ", version: \"2.0.0\"");
	}

}