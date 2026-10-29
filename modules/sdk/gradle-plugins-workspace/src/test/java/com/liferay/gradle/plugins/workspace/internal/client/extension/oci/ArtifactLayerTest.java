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
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

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
	public void testPlanWhenSourceIsDirectory() {
		Assert.assertEquals(
			List.of(new ArtifactMount("/public_html", "static")),
			_getMounts("/public_html/", "static"));
	}

	@Test
	public void testPlanWhenSourceIsFile() {
		Assert.assertEquals(
			List.of(new ArtifactMount("/opt/liferay", ".mounts/0")),
			_getMounts("/opt/liferay/jar-runner.jar", "*.jar"));
	}

	@Test
	public void testPlanWhenSourceIsWholeZip() {
		Assert.assertEquals(
			List.of(new ArtifactMount("/opt/liferay", "")),
			_getMounts("/opt/liferay/", "."));
	}

	@Test
	public void testPlanWhenSourceMatchesManyFiles() {
		_assertPlanFails("/opt/liferay/app", "**", "matches 3 files");
	}

	@Test
	public void testPlanWhenSourceMatchesNothing() {
		_assertPlanFails("/opt/", "*.war", "matches nothing");
	}

	@Test
	public void testToTar() throws IOException {
		Map<String, ArtifactFile> artifactFiles = _getArtifactFiles();

		String longName = "static/" + "a".repeat(100) + ".css";

		for (ArtifactFile artifactFile :
				List.of(
					ArtifactFile.newFile(
						"#!/bin/sh".getBytes(StandardCharsets.UTF_8), 0755,
						"bin/run.sh"),
					ArtifactFile.newFile(
						"a".getBytes(StandardCharsets.UTF_8), 0644,
						longName))) {

			artifactFiles.put(artifactFile.getName(), artifactFile);
		}

		ArtifactLayer artifactLayer = ArtifactLayer.plan(
			artifactFiles,
			List.of(
				new ClientExtensionDockerfile.Copy(
					"/opt/liferay/jar-runner.jar", List.of("*.jar")),
				new ClientExtensionDockerfile.Copy("/opt/app/", List.of("."))));

		Map<String, TarArchiveEntry> tarArchiveEntries = new TreeMap<>();

		try (TarArchiveInputStream tarArchiveInputStream =
				new TarArchiveInputStream(
					new ByteArrayInputStream(artifactLayer.toTar()))) {

			TarArchiveEntry tarArchiveEntry = null;

			while ((tarArchiveEntry = tarArchiveInputStream.getNextEntry()) !=
						null) {

				Assert.assertEquals(
					0,
					tarArchiveEntry.getLastModifiedDate(
					).getTime());

				tarArchiveEntries.put(
					tarArchiveEntry.getName(
					).replaceAll(
						"/$", ""
					),
					tarArchiveEntry);
			}
		}

		TarArchiveEntry tarArchiveEntry = tarArchiveEntries.get(".mounts/0");

		Assert.assertTrue(tarArchiveEntry.isDirectory());

		tarArchiveEntry = tarArchiveEntries.get(".mounts/0/jar-runner.jar");

		Assert.assertTrue(tarArchiveEntry.isLink());
		Assert.assertEquals("app.jar", tarArchiveEntry.getLinkName());

		tarArchiveEntry = tarArchiveEntries.get("bin/run.sh");

		Assert.assertEquals(0755, tarArchiveEntry.getMode() & 0777);

		tarArchiveEntry = tarArchiveEntries.get("node_modules/.bin/semver");

		Assert.assertTrue(tarArchiveEntry.isSymbolicLink());
		Assert.assertEquals(
			"../semver/bin/semver.js", tarArchiveEntry.getLinkName());

		Assert.assertTrue(tarArchiveEntries.containsKey(longName));
	}

	@Test
	public void testToTarWhenCopiesLeaveOutFiles() throws IOException {
		Assert.assertEquals(
			Set.of("static/", "static/a.css"),
			_getTarEntryNames("/public_html/", "static"));
		Assert.assertEquals(
			Set.of(
				".mounts/", ".mounts/0/", ".mounts/0/jar-runner.jar",
				"app.jar"),
			_getTarEntryNames("/opt/liferay/jar-runner.jar", "*.jar"));
	}

	private void _assertPlanFails(
		String destination, String source, String expectedMessage) {

		try {
			_getMounts(destination, source);

			Assert.fail();
		}
		catch (GradleException gradleException) {
			String message = gradleException.getMessage();

			Assert.assertTrue(message, message.contains(expectedMessage));
		}
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

	private Set<String> _getTarEntryNames(String destination, String source)
		throws IOException {

		ArtifactLayer artifactLayer = ArtifactLayer.plan(
			_getArtifactFiles(),
			List.of(
				new ClientExtensionDockerfile.Copy(
					destination, List.of(source))));

		Set<String> names = new TreeSet<>();

		try (TarArchiveInputStream tarArchiveInputStream =
				new TarArchiveInputStream(
					new ByteArrayInputStream(artifactLayer.toTar()))) {

			TarArchiveEntry tarArchiveEntry = null;

			while ((tarArchiveEntry = tarArchiveInputStream.getNextEntry()) !=
						null) {

				names.add(tarArchiveEntry.getName());
			}
		}

		return names;
	}

}