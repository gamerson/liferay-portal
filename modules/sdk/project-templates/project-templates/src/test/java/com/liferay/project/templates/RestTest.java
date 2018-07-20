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
public class RestTest extends ProjectTemplatesTest {

	@Test
	public void testBuildTemplateRest() throws Exception {
		File gradleProjectDir = buildTemplateWithGradle("rest", "my-rest");

		testExists(gradleProjectDir, "bnd.bnd");

		testContains(
			gradleProjectDir,
			"src/main/java/my/rest/application/MyRestApplication.java",
			"public class MyRestApplication extends Application");
		testContains(
			gradleProjectDir,
			"src/main/resources/configuration" +
				"/com.liferay.portal.remote.cxf.common.configuration." +
					"CXFEndpointPublisherConfiguration-cxf.properties",
			"contextPath=/my-rest");
		testContains(
			gradleProjectDir,
			"src/main/resources/configuration/com.liferay.portal.remote.rest." +
				"extender.configuration.RestExtenderConfiguration-rest." +
					"properties",
			"contextPaths=/my-rest",
			"jaxRsApplicationFilterStrings=(component.name=" +
				"my.rest.application.MyRestApplication)");

		File mavenProjectDir = buildTemplateWithMaven(
			"rest", "my-rest", "com.test", "-DclassName=MyRest",
			"-Dpackage=my.rest");

		testContains(
			mavenProjectDir,
			"src/main/java/my/rest/application/MyRestApplication.java",
			"public class MyRestApplication extends Application");
		testContains(
			mavenProjectDir,
			"src/main/resources/configuration" +
				"/com.liferay.portal.remote.cxf.common.configuration." +
					"CXFEndpointPublisherConfiguration-cxf.properties",
			"contextPath=/my-rest");

		buildProjects(gradleProjectDir, mavenProjectDir);
	}

	@Test
	public void testBuildTemplateRestInWorkspace() throws Exception {
		testBuildTemplateWithWorkspace(
			"rest", "my-rest", "build/libs/my.rest-1.0.0.jar");
	}

	@Test
	public void testBuildTemplateRestWithBOM() throws Exception {
		File gradleProjectDir = buildTemplateWithGradle(
			"rest", "rest-dependency-management",
			"--dependency-management-enabled");

		testNotContains(
			gradleProjectDir, "build.gradle",
			"compileOnly group: \"javax.ws.rs\", name: \"javax.ws.rs-api\", " +
				"version: \"2.0.1\"");

		testContains(
			gradleProjectDir, "build.gradle",
			"compileOnly group: \"javax.ws.rs\", name: \"javax.ws.rs-api\"");
	}

	@Test
	public void testBuildTemplateRestWithoutBOM() throws Exception {
		File gradleProjectDir = buildTemplateWithGradle(
			"rest", "rest-dependency-management");

		testContains(
			gradleProjectDir, "build.gradle",
			"compileOnly group: \"javax.ws.rs\", name: \"javax.ws.rs-api\", " +
				"version: \"2.0.1\"");
	}

}