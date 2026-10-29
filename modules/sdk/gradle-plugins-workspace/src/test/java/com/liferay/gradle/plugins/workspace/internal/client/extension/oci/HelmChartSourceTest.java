/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.gradle.plugins.workspace.internal.client.extension.oci;

import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.OutputStream;

import java.net.InetSocketAddress;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import java.util.Map;
import java.util.zip.GZIPOutputStream;

import land.oras.LayoutRef;
import land.oras.OCILayout;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;

import org.gradle.api.GradleException;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author Gregory Amerson
 */
public class HelmChartSourceTest {

	@Test
	public void testPull() throws Exception {
		OCILayout ociLayout = _getOCILayout();

		LayoutRef layoutRef = LayoutRef.parse(ociLayout.getPath() + ":1.0.0");

		OCIPublisher<LayoutRef> ociPublisher = new OCIPublisher<>(ociLayout);

		String digest = ociPublisher.publishHelmChart(
			_createHelmChartArchive(), layoutRef);

		HelmChartSource helmChartSource = HelmChartSource.pull(
			ociLayout, layoutRef, "oci://registry.example.com/charts/able");

		_assertChartFiles(helmChartSource);

		Assert.assertEquals(
			"oci://registry.example.com/charts/able@" + digest,
			helmChartSource.getOrigin());
		Assert.assertEquals("able 1.0.0", helmChartSource.getTemplate());
	}

	@Test
	public void testPullWhenReferenceIsNotHelmChart() throws Exception {
		OCILayout ociLayout = _getOCILayout();

		LayoutRef layoutRef = LayoutRef.parse(ociLayout.getPath() + ":1.0.0");

		OCIPublisher<LayoutRef> ociPublisher = new OCIPublisher<>(ociLayout);

		ociPublisher.publishArtifact(
			layoutRef, "layer".getBytes(StandardCharsets.UTF_8));

		try {
			HelmChartSource.pull(
				ociLayout, layoutRef, "oci://registry.example.com/cx/able");

			Assert.fail();
		}
		catch (GradleException gradleException) {
			String message = gradleException.getMessage();

			Assert.assertTrue(
				message,
				message.contains(
					"\"oci://registry.example.com/cx/able\" is not a Helm " +
						"chart"));
		}
	}

	@Test
	public void testResolveWhenChartArchiveEntryPathIsInvalid()
		throws Exception {

		File file = temporaryFolder.newFile("able-1.0.0.tgz");

		try (TarArchiveOutputStream tarArchiveOutputStream =
				new TarArchiveOutputStream(
					new GZIPOutputStream(
						Files.newOutputStream(file.toPath())))) {

			byte[] data = "a: b\n".getBytes(StandardCharsets.UTF_8);

			TarArchiveEntry tarArchiveEntry = new TarArchiveEntry(
				"able/../values.yaml");

			tarArchiveEntry.setSize(data.length);

			tarArchiveOutputStream.putArchiveEntry(tarArchiveEntry);

			tarArchiveOutputStream.write(data);

			tarArchiveOutputStream.closeArchiveEntry();
		}

		_assertRefused(
			file, "\"able/../values.yaml\" is not under the chart's directory");
	}

	@Test
	public void testResolveWhenReferenceIsBlank() throws Exception {
		HelmChartSource helmChartSource = HelmChartSource.resolveReference(
			null, _rootDir);

		_assertChartFiles(helmChartSource);

		Assert.assertEquals("embedded", helmChartSource.getOrigin());
	}

	@Test
	public void testResolveWhenReferenceIsChartArchive() throws Exception {
		File file = temporaryFolder.newFile("able-1.0.0.tgz");

		Files.write(file.toPath(), _createChartArchiveData());

		HelmChartSource helmChartSource = HelmChartSource.resolveReference(
			file.getName(), temporaryFolder.getRoot());

		_assertChartFiles(helmChartSource);

		Assert.assertEquals(
			file.getAbsolutePath(), helmChartSource.getOrigin());
		Assert.assertEquals("able 1.0.0", helmChartSource.getTemplate());
	}

	@Test
	public void testResolveWhenReferenceIsURL() throws Exception {
		byte[] data = _createChartArchiveData();

		HttpServer httpServer = HttpServer.create(
			new InetSocketAddress("127.0.0.1", 0), 0);

		httpServer.createContext(
			"/able-1.0.0.tgz",
			httpExchange -> {
				httpExchange.sendResponseHeaders(200, data.length);

				try (OutputStream outputStream =
						httpExchange.getResponseBody()) {

					outputStream.write(data);
				}
			});

		httpServer.start();

		try {
			InetSocketAddress inetSocketAddress = httpServer.getAddress();

			String url =
				"http://127.0.0.1:" + inetSocketAddress.getPort() +
					"/able-1.0.0.tgz";

			HelmChartSource helmChartSource = HelmChartSource.resolveReference(
				url, _rootDir);

			_assertChartFiles(helmChartSource);

			String origin = helmChartSource.getOrigin();

			Assert.assertTrue(origin, origin.startsWith(url + " sha256:"));
		}
		finally {
			httpServer.stop(0);
		}
	}

	@Test
	public void testResolveWhenValuesContractIsMissing() throws Exception {
		_assertRefused(
			_createChartDirectory("name: old\nversion: 0.0.1\n"),
			"does not declare the \"cx.liferay.com/values-contract\" " +
				"annotation");
	}

	@Test
	public void testResolveWhenValuesContractIsUnsupported() throws Exception {
		_assertRefused(
			_createChartDirectory(
				"annotations:\n    cx.liferay.com/values-contract: \"2\"\n" +
					"name: new\nversion: 9.0.0\n"),
			"declares values contract \"2\"");
	}

	@Rule
	public final TemporaryFolder temporaryFolder = new TemporaryFolder();

	private void _assertChartFiles(HelmChartSource helmChartSource)
		throws Exception {

		HelmChartSource directoryHelmChartSource =
			HelmChartSource.resolveReference(
				"cloud/helm/client-extension", _rootDir);

		Map<String, byte[]> directoryChartFiles =
			directoryHelmChartSource.getChartFiles();

		Map<String, byte[]> chartFiles = helmChartSource.getChartFiles();

		Assert.assertEquals(directoryChartFiles.keySet(), chartFiles.keySet());
	}

	private void _assertRefused(File file, String expectedMessage)
		throws Exception {

		try {
			HelmChartSource.resolveReference(file.getAbsolutePath(), _rootDir);

			Assert.fail();
		}
		catch (GradleException gradleException) {
			String message = gradleException.getMessage();

			Assert.assertTrue(message, message.contains(expectedMessage));
		}
	}

	private byte[] _createChartArchiveData() throws Exception {
		HelmChartArchive helmChartArchive = _createHelmChartArchive();

		return helmChartArchive.getData();
	}

	private File _createChartDirectory(String chartYAML) throws Exception {
		File dir = temporaryFolder.newFolder("chart");

		Path dirPath = dir.toPath();

		Files.writeString(dirPath.resolve("Chart.yaml"), chartYAML);
		Files.writeString(dirPath.resolve("values.yaml"), "a: b\n");

		return dir;
	}

	private HelmChartArchive _createHelmChartArchive() throws Exception {
		return HelmChartArchive.create(
			Map.of(), "able",
			HelmChartSource.resolveReference(
				"cloud/helm/client-extension", _rootDir),
			Map.of(), "1.0.0");
	}

	private OCILayout _getOCILayout() throws Exception {
		File dir = temporaryFolder.newFolder();

		return OCILayout.builder(
		).defaults(
			dir.toPath()
		).build();
	}

	private static final File _rootDir = new File("../../..");

}