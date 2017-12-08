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

package com.liferay.project.templates.test;

import aQute.bnd.osgi.Jar;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.portal.kernel.util.PropsKeys;
import com.liferay.portal.kernel.util.PropsUtil;

import java.io.File;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
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
			PropsUtil.get(PropsKeys.LIFERAY_HOME), "project-templates-test");

		Assert.assertTrue(
			"Project Build Output Dir: " +
				projectBuildOutputDir.getAbsolutePath(),
			projectBuildOutputDir.exists());

		Assert.assertTrue(
			"Project Build Output Dir contains " +
				projectBuildOutputDir.listFiles().length,
			projectBuildOutputDir.listFiles().length > 0);

		Map<File, Bundle> installedBundleMap = _installBundles(
			projectBuildOutputDir);

		Stream<Entry<File, Bundle>> installedBundleStream =
			installedBundleMap.entrySet().stream();

		Stream<Entry<File, Bundle>> sortedInstalledBundleStream =
			installedBundleStream.sorted(
				Map.Entry.<File, Bundle>comparingByKey());

		Map<File, Bundle> sortedInstalledBundleMap =
			sortedInstalledBundleStream.collect(
				Collectors.toMap(
					Map.Entry::getKey, Map.Entry::getValue,
					(e1, e2) -> e1, LinkedHashMap::new));

		_execute("start", sortedInstalledBundleMap);

		_execute("uninstall", sortedInstalledBundleMap);
	}

	private static String _toPrintStatus(int status) {
		switch (status) {
			case Bundle.ACTIVE:
				return "ACTIVE";
			case Bundle.INSTALLED:
				return "INSTALLED";
			case Bundle.UNINSTALLED:
				return "UNINSTALLED";
		}

		return null;
	}

	private void _execute(String command, Map<File, Bundle> map) {
		Set<Entry<File, Bundle>> entrySet = map.entrySet();

		Stream<Entry<File, Bundle>> stream = entrySet.stream();

		stream.map(
			entry -> {
				File file = entry.getKey();
				Bundle bundle = entry.getValue();
				Throwable throwable = null;

				try {
					if (command.contentEquals("start")) {
						if (!_isFragment(file) &&
							!file.getName().contains("simple.ct")) {

							bundle.start();

							Assert.assertEquals(
								_toPrintStatus(Bundle.ACTIVE),
								_toPrintStatus(bundle.getState()));
						}
					}
					else {
						bundle.uninstall();

						Assert.assertEquals(
							_toPrintStatus(Bundle.UNINSTALLED),
							_toPrintStatus(bundle.getState()));
					}

					return null;
				}
				catch (Throwable t) {
					throwable = t;
				}

				return throwable;
			}
		).filter(
			x -> {
				return x != null;
			}
		).reduce(
			(t1, t2) -> {
				t1.addSuppressed(t2);

				return t1;
			}
		).ifPresent(
			ex -> {
				throw new RuntimeException(ex);
			}
		);
	}

	private Map<File, Bundle> _installBundles(File fileDir) {
		Stream<File> fileListStream = Stream.of(fileDir.listFiles());

		Map<File, Bundle> installedBundleList = new HashMap<>();

		fileListStream.flatMap(
			file -> {
				Bundle bundleFile;
				BundleContext bundleContext;
				Bundle testBundle;

				try {
					bundleFile = FrameworkUtil.getBundle(
						ProjectTemplatesIntegrationTest.class);

					bundleContext = bundleFile.getBundleContext();

					Assert.assertTrue(file.exists());

					if (file.getName().endsWith(".war")) {
						testBundle = bundleContext.installBundle(
							"webbundle:" + file.toURI().toASCIIString() +
								"?Web-ContextPath=/" + file.getName());
					}
					else {
						testBundle = bundleContext.installBundle(
							file.toURI().toASCIIString());
					}

					Assert.assertEquals(
						_toPrintStatus(Bundle.INSTALLED),
						_toPrintStatus(testBundle.getState()));

					installedBundleList.put(file, testBundle);

					return null;
				}
				catch (Throwable t) {
					return Stream.of(t);
				}
			}
		).reduce(
			(t1, t2) -> {
				t1.addSuppressed(t2);

				return t1;
			}
		).ifPresent(
			ex -> {
				throw new RuntimeException(ex);
			}
		);

		return installedBundleList;
	}

	private boolean _isFragment(File file) throws Exception {
		try (Jar jar = new Jar(file)) {
			Manifest manifest = jar.getManifest();

			Attributes mainAttributes = manifest.getMainAttributes();

			if (mainAttributes.getValue("Fragment-Host") == null) {
				return false;
			}
			else {
				return true;
			}
		}
	}

}