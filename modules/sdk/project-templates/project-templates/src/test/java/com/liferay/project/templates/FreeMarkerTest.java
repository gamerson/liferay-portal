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
public class FreeMarkerTest extends ProjectTemplatesTest {

	@Test
	public void testBuildTemplateFreeMarker71() throws Exception {
		testBuildTemplatePortlet71(
			"freemarker-portlet", "FreeMarkerPortlet", "templates/init.ftl",
			"templates/view.ftl");
	}

	@Test
	public void testBuildTemplateFreeMarkerPortlet() throws Exception {
		File gradleProjectDir = testBuildTemplatePortlet(
			"freemarker-portlet", "FreeMarkerPortlet", "templates/init.ftl",
			"templates/view.ftl");

		testStartsWith(
			gradleProjectDir, "src/main/resources/templates/view.ftl",
			FREEMARKER_PORTLET_VIEW_FTL_PREFIX);
	}

	@Test
	public void testBuildTemplateFreeMarkerPortletInWorkspace()
		throws Exception {

		testBuildTemplateWithWorkspace(
			"freemarker-portlet", "foo", "build/libs/foo-1.0.0.jar");
	}

	@Test
	public void testBuildTemplateFreeMarkerPortletWithBOM() throws Exception {
		File gradleProjectDir = buildTemplateWithGradle(
			"freemarker-portlet", "freemarker-dependency-management",
			"--dependency-management-enabled");

		testNotContains(
			gradleProjectDir, "build.gradle",
			DEPENDENCY_PORTAL_KERNEL + ", version: \"2.0.0\"");

		testContains(
			gradleProjectDir, "build.gradle", DEPENDENCY_PORTAL_KERNEL);
	}

	@Test
	public void testBuildTemplateFreeMarkerPortletWithoutBOM()
		throws Exception {

		File gradleProjectDir = buildTemplateWithGradle(
			"freemarker-portlet", "freemarker-dependency-management");

		testContains(
			gradleProjectDir, "build.gradle",
			DEPENDENCY_PORTAL_KERNEL + ", version: \"2.0.0\"");
	}

	@Test
	public void testBuildTemplateFreeMarkerPortletWithPackage()
		throws Exception {

		File gradleProjectDir = testBuildTemplatePortletWithPackage(
			"freemarker-portlet", "FreeMarkerPortlet", "templates/init.ftl",
			"templates/view.ftl");

		testStartsWith(
			gradleProjectDir, "src/main/resources/templates/view.ftl",
			FREEMARKER_PORTLET_VIEW_FTL_PREFIX);
	}

	@Test
	public void testBuildTemplateFreeMarkerPortletWithPortletName()
		throws Exception {

		File gradleProjectDir = testBuildTemplatePortletWithPortletName(
			"freemarker-portlet", "FreeMarkerPortlet", "templates/init.ftl",
			"templates/view.ftl");

		testStartsWith(
			gradleProjectDir, "src/main/resources/templates/view.ftl",
			FREEMARKER_PORTLET_VIEW_FTL_PREFIX);
	}

	@Test
	public void testBuildTemplateFreeMarkerPortletWithPortletSuffix()
		throws Exception {

		File gradleProjectDir = testBuildTemplatePortletWithPortletSuffix(
			"freemarker-portlet", "FreeMarkerPortlet", "templates/init.ftl",
			"templates/view.ftl");

		testStartsWith(
			gradleProjectDir, "src/main/resources/templates/view.ftl",
			FREEMARKER_PORTLET_VIEW_FTL_PREFIX);
	}

}