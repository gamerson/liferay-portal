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
public class SocialBookmarkTest extends ProjectTemplatesTest {

	@Test
	public void testBuildTemplateSocialBookmark() throws Exception {
		File gradleProjectDir = buildTemplateWithGradle(
			"social-bookmark", "foo", "--package-name", "com.liferay.test",
			"--liferayVersion", "7.1");

		testExists(gradleProjectDir, "bnd.bnd");

		testExists(gradleProjectDir, "build.gradle");

		testContains(
			gradleProjectDir,
			"src/main/java/com/liferay/test/social/bookmark" +
				"/FooSocialBookmark.java",
			"public class FooSocialBookmark implements SocialBookmark");
		testContains(
			gradleProjectDir, "src/main/resources/META-INF/resources/page.jsp",
			"<clay:link");
		testContains(
			gradleProjectDir, "src/main/resources/content/Language.properties",
			"foo=Foo");

		File mavenProjectDir = buildTemplateWithMaven(
			"social-bookmark", "foo", "com.test", "-DclassName=Foo",
			"-Dpackage=com.liferay.test", "-DliferayVersion=7.1");

		buildProjects(gradleProjectDir, mavenProjectDir);
	}

}