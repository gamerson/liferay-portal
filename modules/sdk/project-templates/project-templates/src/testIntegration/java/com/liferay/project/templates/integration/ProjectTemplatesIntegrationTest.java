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

package com.liferay.project.templates.integration;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;

/**
 * @author Lawrence Lee
 */

@RunWith(Arquillian.class)
public class ProjectTemplatesIntegrationTest {

	@Test
	public void testProjectInstall() throws Exception {
		File projectTemplateBuildDir = new File(System.getProperty("projectTemplateBuildDir"));

		ArrayList<File> projectTemplateBuildFiles = new ArrayList<File>(Arrays.asList(projectTemplateBuildDir.listFiles()));

		for (File file : projectTemplateBuildFiles) {
			Bundle bundle = FrameworkUtil.getBundle(ProjectTemplatesIntegrationTest.class);

			BundleContext bundleContext = bundle.getBundleContext();

			Assert.assertTrue(file.exists());

			Bundle testBundle = bundleContext.installBundle(file.getAbsolutePath());

			testBundle.start();

			Assert.assertEquals(Bundle.ACTIVE, testBundle.getState());
		}
	}

}