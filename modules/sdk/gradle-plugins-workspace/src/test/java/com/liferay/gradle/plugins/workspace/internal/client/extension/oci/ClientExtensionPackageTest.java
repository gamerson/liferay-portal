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
			_CONTAINER_RUNTIME_MISSING, _DOCKERFILE_JAR,
			"# The plugin's template\n\n" +
				_DOCKERFILE_JAR.replace(" ", "\t  "),
			_getJarZipEntries());

		Assert.assertEquals(List.of(), clientExtensionPackage.getWarnings());
	}

	@Test
	public void testCreateWhenDockerfileIsCustom() throws Exception {
		ClientExtensionPackage clientExtensionPackage = _create(
			_CONTAINER_RUNTIME_MISSING, _DOCKERFILE_JAR,
			_DOCKERFILE_JAR + "\nCOPY static /static\nENV DEBUG=true",
			_getJarZipEntries());

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
				_CONTAINER_RUNTIME_MISSING, _DOCKERFILE_JAR,
				"FROM liferay/jar-runner\nCOPY build/libs/app.jar /opt" +
					"/liferay/jar-runner.jar",
				zipEntries);

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

		File dockerMockCommandFile = temporaryFolder.newFile("docker-mock");

		Files.writeString(dockerMockCommandFile.toPath(), _DOCKER_MOCK_COMMAND);

		dockerMockCommandFile.setExecutable(true);

		ClientExtensionPackage clientExtensionPackage = _create(
			dockerMockCommandFile.getAbsolutePath(), dockerfile, dockerfile,
			zipEntries);

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

	@Test
	public void testGetContentDigest() throws Exception {
		String contentDigest = _getContentDigest(_getJarZipEntries());

		Map<String, String> zipEntries = _getJarZipEntries();

		zipEntries.put(
			"WEB-INF/liferay-plugin-package.properties",
			"#Thu Oct 29 12:00:00 CDT 2026");
		zipEntries.put(
			"able.client-extension-config.json",
			"{\"key~able\": {\"buildTimestamp\": 1}}");

		Assert.assertEquals(contentDigest, _getContentDigest(zipEntries));

		zipEntries = _getJarZipEntries();

		zipEntries.put("static/a.css", "a {color: red}");

		Assert.assertNotEquals(contentDigest, _getContentDigest(zipEntries));

		zipEntries = _getJarZipEntries();

		zipEntries.put(
			"able.client-extension-config.json",
			"{\"key~able\": {\"name\": \"Able\"}}");

		Assert.assertNotEquals(contentDigest, _getContentDigest(zipEntries));
	}

	@Test
	public void testGetContentVersion() throws Exception {
		ClientExtensionPackage clientExtensionPackage = _create(
			_CONTAINER_RUNTIME_MISSING, _DOCKERFILE_JAR, _DOCKERFILE_JAR,
			_getJarZipEntries());

		String version = clientExtensionPackage.getContentVersion(
			_getHelmChartSource(), "able");

		Assert.assertTrue(version, version.matches("0\\.0\\.0-g[0-9a-f]{12}"));
	}

	@Rule
	public final TemporaryFolder temporaryFolder = new TemporaryFolder();

	private ClientExtensionPackage _create(
			String containerRuntime, String defaultDockerfile,
			String dockerfile, Map<String, String> zipEntries)
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

		return ClientExtensionPackage.create(
			containerRuntime, defaultDockerfileFile,
			"client-extensions/able/Dockerfile", new File(dir, "replay"),
			zipFile);
	}

	private String _getContentDigest(Map<String, String> zipEntries)
		throws Exception {

		ClientExtensionPackage clientExtensionPackage = _create(
			_CONTAINER_RUNTIME_MISSING, _DOCKERFILE_JAR, _DOCKERFILE_JAR,
			zipEntries);

		return clientExtensionPackage.getContentDigest(
			_getHelmChartSource(), "able");
	}

	private HelmChartSource _getHelmChartSource() throws Exception {
		return HelmChartSource.resolveReference(
			"cloud/helm/client-extension", new File("../../.."));
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

	private static final String _CONTAINER_RUNTIME_MISSING =
		"container-runtime-missing";

	private static final String _DOCKER_MOCK_COMMAND = String.join(
		"\n", "#!/bin/sh", "while [ $# -gt 0 ]; do", "\tcase \"$1\" in",
		"\t\t--volume) dir=\"${2%%:*}\"; shift 2 ;;",
		"\t\t-c) cd \"$dir\" && exec sh -c \"$2\" ;;", "\t\t*) shift ;;",
		"\tesac", "done", "exit 1");

	private static final String _DOCKERFILE_JAR =
		"FROM liferay/jar-runner:latest\n\nCOPY --chown=liferay:liferay " +
			"*.jar /opt/liferay/jar-runner.jar";

}