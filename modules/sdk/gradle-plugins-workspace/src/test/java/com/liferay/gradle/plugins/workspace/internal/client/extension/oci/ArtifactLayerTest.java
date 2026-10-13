/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.gradle.plugins.workspace.internal.client.extension.oci;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import java.nio.charset.StandardCharsets;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

import org.gradle.api.GradleException;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Gregory Amerson
 */
public class ArtifactLayerTest {

	@Test
	public void testPlan() {

		// A directory is mounted as is

		Assert.assertEquals(
			List.of(new ArtifactMount("/public_html", "static")),
			_getMounts("/public_html/", "static"));

		// The whole zip is mounted at its destination

		Assert.assertEquals(
			List.of(new ArtifactMount("/opt/liferay", "")),
			_getMounts("/opt/liferay/", "."));

		// A file gets a directory of its own

		Assert.assertEquals(
			List.of(new ArtifactMount("/opt/liferay", ".cx-mounts/0")),
			_getMounts("/opt/liferay/jar-runner.jar", "*.jar"));

		// A source that matches nothing is refused

		try {
			_getMounts("/opt/", "*.war");

			Assert.fail();
		}
		catch (GradleException gradleException) {
			String message = gradleException.getMessage();

			Assert.assertTrue(message, message.contains("matches nothing"));
		}
	}

	@Test
	public void testToTar() throws IOException {
		ArtifactLayer artifactLayer = ArtifactLayer.plan(
			_getArtifactFiles(),
			List.of(
				new ClientExtensionDockerfile.Copy(
					"/opt/liferay/jar-runner.jar", List.of("*.jar"))));

		byte[] tar = artifactLayer.toTar();

		Assert.assertArrayEquals(tar, artifactLayer.toTar());

		Map<String, Byte> entries = new TreeMap<>();

		try (TarArchiveInputStream tarArchiveInputStream =
				new TarArchiveInputStream(new ByteArrayInputStream(tar))) {

			TarArchiveEntry tarArchiveEntry = null;

			while ((tarArchiveEntry =
						tarArchiveInputStream.getNextTarEntry()) != null) {

				String name = tarArchiveEntry.getName();

				if (tarArchiveEntry.isDirectory()) {
					name = name.substring(0, name.length() - 1);
				}

				entries.put(name, _getType(tarArchiveEntry));

				Assert.assertEquals(
					0,
					tarArchiveEntry.getLastModifiedDate(
					).getTime());
			}
		}

		Map<String, Byte> expectedEntries = new TreeMap<>();

		expectedEntries.put(".cx-mounts", _DIRECTORY);
		expectedEntries.put(".cx-mounts/0", _DIRECTORY);
		expectedEntries.put(".cx-mounts/0/jar-runner.jar", _HARD_LINK);
		expectedEntries.put("app.jar", _FILE);
		expectedEntries.put("node_modules", _DIRECTORY);
		expectedEntries.put("node_modules/.bin", _DIRECTORY);
		expectedEntries.put("node_modules/.bin/semver", _SYMBOLIC_LINK);
		expectedEntries.put("static", _DIRECTORY);
		expectedEntries.put("static/a.css", _FILE);

		Assert.assertEquals(expectedEntries, entries);
	}

	private Map<String, ArtifactFile> _getArtifactFiles() {
		Map<String, ArtifactFile> artifactFiles = new TreeMap<>();

		for (ArtifactFile artifactFile :
				List.of(
					ArtifactFile.newFile(
						"jar".getBytes(StandardCharsets.UTF_8), 0644,
						"app.jar"),
					ArtifactFile.newSymbolicLink(
						"../semver/bin/semver.js", "node_modules/.bin/semver"),
					ArtifactFile.newFile(
						"a".getBytes(StandardCharsets.UTF_8), 0644,
						"static/a.css"))) {

			artifactFiles.put(artifactFile.getName(), artifactFile);
		}

		return artifactFiles;
	}

	private List<ArtifactMount> _getMounts(String destination, String source) {
		ArtifactLayer artifactLayer = ArtifactLayer.plan(
			_getArtifactFiles(),
			List.of(
				new ClientExtensionDockerfile.Copy(
					destination, List.of(source))));

		return artifactLayer.getMounts();
	}

	private Byte _getType(TarArchiveEntry tarArchiveEntry) {
		if (tarArchiveEntry.isDirectory()) {
			return _DIRECTORY;
		}

		if (tarArchiveEntry.isLink()) {
			return _HARD_LINK;
		}

		if (tarArchiveEntry.isSymbolicLink()) {
			return _SYMBOLIC_LINK;
		}

		return _FILE;
	}

	private static final Byte _DIRECTORY = 0;

	private static final Byte _FILE = 1;

	private static final Byte _HARD_LINK = 2;

	private static final Byte _SYMBOLIC_LINK = 3;

}