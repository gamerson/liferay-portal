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
public class MVCPortletTest extends ProjectTemplatesTest {

	@Test
	public void testBuildTemplateMVCPortlet() throws Exception {
		testBuildTemplatePortlet(
			"mvc-portlet", "MVCPortlet", "META-INF/resources/init.jsp",
			"META-INF/resources/view.jsp");
	}

	@Test
	public void testBuildTemplateMVCPortlet71() throws Exception {
		testBuildTemplatePortlet71(
			"mvc-portlet", "MVCPortlet", "META-INF/resources/init.jsp",
			"META-INF/resources/view.jsp");
	}

	@Test
	public void testBuildTemplateMVCPortletInWorkspace() throws Exception {
		testBuildTemplateWithWorkspace(
			"mvc-portlet", "foo", "build/libs/foo-1.0.0.jar");
	}

	@Test
	public void testBuildTemplateMVCPortletWithBOM() throws Exception {
		File gradleProjectDir = buildTemplateWithGradle(
			"mvc-portlet", "mvc-dependency-management",
			"--dependency-management-enabled");

		testNotContains(
			gradleProjectDir, "build.gradle",
			DEPENDENCY_PORTAL_KERNEL + ", version: \"2.0.0\"");

		testContains(
			gradleProjectDir, "build.gradle", DEPENDENCY_PORTAL_KERNEL);
	}

	@Test
	public void testBuildTemplateMVCPortletWithoutBOM() throws Exception {
		File gradleProjectDir = buildTemplateWithGradle(
			"mvc-portlet", "mvc-dependency-management");

		testContains(
			gradleProjectDir, "build.gradle",
			DEPENDENCY_PORTAL_KERNEL + ", version: \"2.0.0\"");
	}

	@Test
	public void testBuildTemplateMVCPortletWithPackage() throws Exception {
		testBuildTemplatePortletWithPackage(
			"mvc-portlet", "MVCPortlet", "META-INF/resources/init.jsp",
			"META-INF/resources/view.jsp");
	}

	@Test
	public void testBuildTemplateMVCPortletWithPortletName() throws Exception {
		testBuildTemplatePortletWithPortletName(
			"mvc-portlet", "MVCPortlet", "META-INF/resources/init.jsp",
			"META-INF/resources/view.jsp");
	}

	@Test
	public void testBuildTemplateMVCPortletWithPortletSuffix()
		throws Exception {

		testBuildTemplatePortletWithPortletSuffix(
			"mvc-portlet", "MVCPortlet", "META-INF/resources/init.jsp",
			"META-INF/resources/view.jsp");
	}

}