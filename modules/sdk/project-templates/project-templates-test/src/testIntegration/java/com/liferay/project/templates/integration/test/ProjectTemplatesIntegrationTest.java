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

package com.liferay.project.templates.integration.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.portal.kernel.util.PropsKeys;
import com.liferay.portal.kernel.util.PropsUtil;

import java.io.File;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

/**
 * @author Lawrence Lee
 */
@RunWith(Arquillian.class)
public class ProjectTemplatesIntegrationTest {

	@Test
	public void testProjectInstall() throws Exception {
		File projectBuildOutputDir = new File(
			PropsUtil.get(PropsKeys.LIFERAY_HOME),
			"/project-templates-tests");

		Stream<File> projectTemplateBuildFiles = Stream.of(projectBuildOutputDir.listFiles());

		projectTemplateBuildFiles.flatMap( file -> {
			try {
				Bundle bundle = FrameworkUtil.getBundle(
					ProjectTemplatesIntegrationTest.class);

				BundleContext bundleContext = bundle.getBundleContext();

				Assert.assertTrue(file.exists());

				Bundle testBundle = bundleContext.installBundle(
					file.toURI().toASCIIString());

				testBundle.start();

				Assert.assertEquals(
					_toPrintStatus(Bundle.ACTIVE),
					_toPrintStatus(testBundle.getState()));

				testBundle.uninstall();

				Assert.assertEquals(
					_toPrintStatus(Bundle.UNINSTALLED),
					_toPrintStatus(testBundle.getState()));

				return null;
			} catch (Throwable t) {
	            return Stream.of(t);
	        }
		}).reduce((t1, t2) -> {
            t1.addSuppressed(t2);
            return t1;
        }).ifPresent(ex -> {
            throw new RuntimeException(ex);
        });
	}

	private static String _toPrintStatus(int status) {
		switch (status) {
			case Bundle.ACTIVE:
				return "ACTIVE";
			case Bundle.UNINSTALLED:
				return "UNINSTALLED";
		}

		return null;
	}

}