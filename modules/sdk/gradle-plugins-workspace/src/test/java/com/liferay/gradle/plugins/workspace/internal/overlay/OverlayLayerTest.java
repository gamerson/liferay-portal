/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.gradle.plugins.workspace.internal.overlay;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author Gregory Amerson
 */
public class OverlayLayerTest {

	@Test
	public void testCollect() throws IOException {
		File configsDir = temporaryFolder.newFolder("configs");

		_write(configsDir, "common/osgi/configs/com.acme.Common.config", "a");
		_write(configsDir, "common/tomcat/conf/catalina.properties", "common");
		_write(configsDir, "docker/tomcat/conf/catalina.properties", "docker");
		_write(configsDir, "prod/patching/liferay-dxp-hotfix-1.zip", "hotfix");
		_write(configsDir, "prod/scripts/setup.sh", "echo");
		_write(configsDir, "prod/tomcat/conf/catalina.properties", "prod");
		_write(configsDir, "uat/osgi/configs/com.acme.Uat.config", "uat");

		File jarFile = _write(temporaryFolder.getRoot(), "com.acme.jar", "jar");
		File lpkgFile = _write(
			temporaryFolder.getRoot(), "com.acme.lpkg", "lpkg");
		File warFile = _write(temporaryFolder.getRoot(), "acme-theme.war", "w");
		File zipFile = _write(temporaryFolder.getRoot(), "acme-cx.zip", "zip");

		OverlayLayer overlayLayer = OverlayLayer.collect(
			List.of(zipFile), configsDir, List.of(jarFile, lpkgFile, warFile),
			"prod");

		Map<String, File> files = overlayLayer.getFiles();

		Assert.assertEquals(
			List.of(
				"osgi/client-extensions/acme-cx.zip",
				"osgi/configs/com.acme.Common.config",
				"osgi/modules/com.acme.jar", "osgi/war/acme-theme.war",
				"patching/liferay-dxp-hotfix-1.zip",
				"tomcat/conf/catalina.properties"),
			List.copyOf(files.keySet()));

		// The environment's configuration replaces the common one

		File catalinaPropertiesFile = files.get(
			"tomcat/conf/catalina.properties");

		Assert.assertEquals(
			"prod", Files.readString(catalinaPropertiesFile.toPath()));

		List<String> warnings = overlayLayer.getWarnings();

		Assert.assertEquals(warnings.toString(), 2, warnings.size());

		String warning = warnings.get(0);

		Assert.assertTrue(warning, warning.contains("lpkg"));

		warning = warnings.get(1);

		Assert.assertTrue(warning, warning.contains("setup.sh"));
	}

	@Test
	public void testGetModifiedTime() throws IOException {
		File file1 = _write(temporaryFolder.getRoot(), "1/com.acme.jar", "a");
		File file2 = _write(temporaryFolder.getRoot(), "2/com.acme.jar", "a");
		File file3 = _write(temporaryFolder.getRoot(), "3/com.acme.jar", "b");

		Assert.assertTrue(file2.setLastModified(0));

		Assert.assertEquals(
			OverlayLayer.getModifiedTime(file1),
			OverlayLayer.getModifiedTime(file2));
		Assert.assertNotEquals(
			OverlayLayer.getModifiedTime(file1),
			OverlayLayer.getModifiedTime(file3));
	}

	@Test
	public void testWriteTar() throws IOException {
		File jarFile = _write(temporaryFolder.getRoot(), "com.acme.jar", "jar");

		OverlayLayer overlayLayer = OverlayLayer.collect(
			List.of(), null, List.of(jarFile), "prod");

		File tarFile1 = temporaryFolder.newFile("1.tar");

		String digest1 = overlayLayer.writeTar(tarFile1.toPath());

		Assert.assertTrue(jarFile.setLastModified(0));

		File tarFile2 = temporaryFolder.newFile("2.tar");

		// The same files produce the same layer, whenever they were built

		Assert.assertEquals(digest1, overlayLayer.writeTar(tarFile2.toPath()));

		Map<String, TarArchiveEntry> tarArchiveEntries = new HashMap<>();

		try (InputStream inputStream = Files.newInputStream(tarFile1.toPath());

			TarArchiveInputStream tarArchiveInputStream =
				new TarArchiveInputStream(inputStream)) {

			TarArchiveEntry tarArchiveEntry;

			while ((tarArchiveEntry =
						tarArchiveInputStream.getNextTarEntry()) != null) {

				tarArchiveEntries.put(
					tarArchiveEntry.getName(), tarArchiveEntry);
			}
		}

		Assert.assertTrue(tarArchiveEntries.containsKey("osgi/"));
		Assert.assertTrue(tarArchiveEntries.containsKey("osgi/modules/"));

		TarArchiveEntry tarArchiveEntry = tarArchiveEntries.get(
			"osgi/modules/com.acme.jar");

		Date modTime = tarArchiveEntry.getModTime();

		Assert.assertEquals(
			OverlayLayer.getModifiedTime(jarFile), modTime.getTime() / 1000);
	}

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	private File _write(File dir, String path, String content)
		throws IOException {

		File file = new File(dir, path);

		File parentFile = file.getParentFile();

		parentFile.mkdirs();

		Files.writeString(file.toPath(), content, StandardCharsets.UTF_8);

		return file;
	}

}