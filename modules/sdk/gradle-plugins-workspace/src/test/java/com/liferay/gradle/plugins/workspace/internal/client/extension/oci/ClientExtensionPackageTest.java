/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.gradle.plugins.workspace.internal.client.extension.oci;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;

import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

import org.gradle.api.GradleException;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author Gregory Amerson
 */
public class ClientExtensionPackageTest {

	@Test
	public void testCreate() throws Exception {
		ClientExtensionPackage clientExtensionPackage = _create(
			_DOCKERFILE_JAR, _DOCKERFILE_JAR, _getJarZipEntries());

		Assert.assertEquals(List.of(), clientExtensionPackage.getWarnings());

		ArtifactLayer artifactLayer = clientExtensionPackage.getArtifactLayer();

		Assert.assertEquals(
			List.of(new ArtifactMount("/opt/liferay", ".mounts/0")),
			artifactLayer.getMounts());

		HelmChartArchive helmChartArchive =
			clientExtensionPackage.createHelmChartArchive(
				"registry.example.com/cx/able@sha256:1",
				HelmChartSource.resolveReference(
					"cloud/helm/client-extension", new File("../../..")),
				"able", "1.0.0");

		Map<String, Object> chartMetadata = helmChartArchive.getChartMetadata();

		Assert.assertEquals("able", chartMetadata.get("name"));
	}

	@Test
	public void testCreateWhenDockerfileIsCustom() throws Exception {
		ClientExtensionPackage clientExtensionPackage = _create(
			_DOCKERFILE_JAR + "\nCOPY static /static\nENV DEBUG=true",
			_DOCKERFILE_JAR, _getJarZipEntries());

		List<String> warnings = clientExtensionPackage.getWarnings();

		Assert.assertEquals(warnings.toString(), 1, warnings.size());

		String warning = warnings.get(0);

		for (String expected :
				new String[] {
					"\"client-extensions/able/Dockerfile\" is not supported",
					"the default \"liferay/jar-runner:latest\" layout is used",
					"without \"COPY static /static\", \"ENV DEBUG=true\"."
				}) {

			Assert.assertTrue(warning, warning.contains(expected));
		}

		ArtifactLayer artifactLayer = clientExtensionPackage.getArtifactLayer();

		Assert.assertEquals(
			List.of(new ArtifactMount("/opt/liferay", ".mounts/0")),
			artifactLayer.getMounts());
	}

	@Test
	public void testCreateWhenDockerfileIsCustomAndDefaultLayoutDoesNotApply()
		throws Exception {

		Map<String, String> zipEntries = _getJarZipEntries();

		zipEntries.remove("app.jar");

		try {
			_create(
				"FROM liferay/jar-runner\nCOPY build/libs/app.jar /opt" +
					"/liferay/jar-runner.jar",
				_DOCKERFILE_JAR, zipEntries);

			Assert.fail();
		}
		catch (GradleException gradleException) {
			String message = gradleException.getMessage();

			Assert.assertTrue(
				message,
				message.contains(
					"is not supported for packaging, and the default " +
						"\"liferay/jar-runner:latest\" layout does not apply"));
		}
	}

	@Test
	public void testCreateWhenDockerfileRunsAreReplayed() throws Exception {
		FileSystem fileSystem = FileSystems.getDefault();

		Set<String> fileAttributeViews =
			fileSystem.supportedFileAttributeViews();

		Assume.assumeTrue(fileAttributeViews.contains("posix"));

		String dockerfile =
			"FROM liferay/node-runner\nCOPY . /opt/liferay\nRUN mkdir " +
				"node_modules && echo a > node_modules/a.js";

		Map<String, String> zipEntries = new LinkedHashMap<>();

		zipEntries.put(
			"able.client-extension-config.json", "{\"key~able\": {}}");
		zipEntries.put("package.json", "{}");

		ClientExtensionPackage clientExtensionPackage = _create(
			dockerfile, dockerfile, zipEntries);

		ArtifactLayer artifactLayer = clientExtensionPackage.getArtifactLayer();

		List<String> names = new ArrayList<>();

		try (TarArchiveInputStream tarArchiveInputStream =
				new TarArchiveInputStream(
					new ByteArrayInputStream(artifactLayer.toTar()))) {

			TarArchiveEntry tarArchiveEntry = null;

			while ((tarArchiveEntry = tarArchiveInputStream.getNextEntry()) !=
						null) {

				names.add(tarArchiveEntry.getName());
			}
		}

		Assert.assertTrue(
			names.toString(), names.contains("node_modules/a.js"));
		Assert.assertTrue(names.toString(), names.contains("package.json"));
	}

	@Rule
	public final TemporaryFolder temporaryFolder = new TemporaryFolder();

	private ClientExtensionPackage _create(
			String dockerfile, String defaultDockerfile,
			Map<String, String> zipEntries)
		throws Exception {

		File dir = temporaryFolder.newFolder();

		File defaultDockerfileFile = new File(dir, "Dockerfile.default");

		Files.writeString(defaultDockerfileFile.toPath(), defaultDockerfile);

		File zipFile = new File(dir, "able.zip");

		try (ZipOutputStream zipOutputStream = new ZipOutputStream(
				new FileOutputStream(zipFile))) {

			zipEntries.put("Dockerfile", dockerfile);

			for (Map.Entry<String, String> entry : zipEntries.entrySet()) {
				zipOutputStream.putNextEntry(new ZipEntry(entry.getKey()));

				String value = entry.getValue();

				zipOutputStream.write(value.getBytes(StandardCharsets.UTF_8));

				zipOutputStream.closeEntry();
			}
		}

		File dockerMockCommandFile = new File(dir, "docker-mock");

		Files.writeString(dockerMockCommandFile.toPath(), _DOCKER_MOCK_COMMAND);

		dockerMockCommandFile.setExecutable(true);

		return ClientExtensionPackage.create(
			dockerMockCommandFile.getAbsolutePath(), defaultDockerfileFile,
			"client-extensions/able/Dockerfile", new File(dir, "replay"),
			zipFile);
	}

	private Map<String, String> _getJarZipEntries() {
		Map<String, String> zipEntries = new LinkedHashMap<>();

		zipEntries.put("LCP.json", "{\"kind\": \"Deployment\"}");
		zipEntries.put("app.jar", "jar");
		zipEntries.put(
			"able.client-extension-config.json", "{\"key~able\": {}}");
		zipEntries.put("static/a.css", "a {}");

		return zipEntries;
	}

	private static final String _DOCKER_MOCK_COMMAND = String.join(
		"\n", "#!/bin/sh", "while [ $# -gt 0 ]; do", "\tcase \"$1\" in",
		"\t\t--volume) dir=\"${2%%:*}\"; shift 2 ;;",
		"\t\t-c) cd \"$dir\" && exec sh -c \"$2\" ;;", "\t\t*) shift ;;",
		"\tesac", "done", "exit 1");

	private static final String _DOCKERFILE_JAR =
		"FROM liferay/jar-runner:latest\n\nCOPY --chown=liferay:liferay " +
			"*.jar /opt/liferay/jar-runner.jar";

}