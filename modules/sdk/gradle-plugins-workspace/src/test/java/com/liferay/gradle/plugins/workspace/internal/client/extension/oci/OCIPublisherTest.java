/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.gradle.plugins.workspace.internal.client.extension.oci;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;

import java.nio.charset.StandardCharsets;

import java.security.MessageDigest;

import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.SSLException;

import land.oras.Config;
import land.oras.Index;
import land.oras.Layer;
import land.oras.LayoutRef;
import land.oras.Manifest;
import land.oras.ManifestDescriptor;
import land.oras.OCILayout;
import land.oras.Platform;
import land.oras.exception.OrasException;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author Gregory Amerson
 */
public class OCIPublisherTest {

	@Test
	public void testPublishArtifact() throws Exception {
		byte[] tar = _getTar();

		OCILayout ociLayout = _getOCILayout();

		LayoutRef layoutRef = _getLayoutRef(ociLayout, "1.0.0");

		OCIPublisher<LayoutRef> ociPublisher = new OCIPublisher<>(ociLayout);

		String digest = ociPublisher.publishArtifact(layoutRef, tar);

		OCILayout secondOCILayout = _getOCILayout();

		OCIPublisher<LayoutRef> secondOCIPublisher = new OCIPublisher<>(
			secondOCILayout);

		Assert.assertEquals(
			digest,
			secondOCIPublisher.publishArtifact(
				_getLayoutRef(secondOCILayout, "2.0.0"), tar));

		Index index = Index.fromJson(
			new String(
				ociLayout.getBlob(layoutRef.withDigest(digest)),
				StandardCharsets.UTF_8));

		List<String> platforms = new ArrayList<>();

		for (ManifestDescriptor manifestDescriptor : index.getManifests()) {
			Platform platform = manifestDescriptor.getPlatform();

			platforms.add(platform.os() + "/" + platform.architecture());

			Manifest manifest = Manifest.fromJson(
				new String(
					_getBlob(
						manifestDescriptor.getDigest(), layoutRef, ociLayout,
						manifestDescriptor.getSize()),
					StandardCharsets.UTF_8));

			Config config = manifest.getConfig();

			Map<String, Object> imageConfig = _objectMapper.readValue(
				_getBlob(
					config.getDigest(), layoutRef, ociLayout, config.getSize()),
				new TypeReference<Map<String, Object>>() {
				});

			Assert.assertEquals(
				platform.architecture(), imageConfig.get("architecture"));
			Assert.assertEquals(
				Map.of("diff_ids", List.of(_getDigest(tar)), "type", "layers"),
				imageConfig.get("rootfs"));

			List<Layer> layers = manifest.getLayers();

			Assert.assertEquals(layers.toString(), 1, layers.size());

			Layer layer = layers.get(0);

			try (InputStream inputStream = new GZIPInputStream(
					new ByteArrayInputStream(
						_getBlob(
							layer.getDigest(), layoutRef, ociLayout,
							layer.getSize())))) {

				Assert.assertArrayEquals(tar, inputStream.readAllBytes());
			}
		}

		Assert.assertEquals(List.of("linux/amd64", "linux/arm64"), platforms);
	}

	@Test
	public void testPublishHelmChart() throws Exception {
		HelmChartArchive helmChartArchive = HelmChartArchive.create(
			"able",
			HelmChartSource.resolveReference(
				"cloud/helm/client-extension", new File("../../..")),
			Map.of(), "1.0.0");

		OCILayout ociLayout = _getOCILayout();

		LayoutRef layoutRef = _getLayoutRef(ociLayout, "1.0.0");

		OCIPublisher<LayoutRef> ociPublisher = new OCIPublisher<>(ociLayout);

		String digest = ociPublisher.publishHelmChart(
			helmChartArchive, layoutRef);

		OCILayout secondOCILayout = _getOCILayout();

		OCIPublisher<LayoutRef> secondOCIPublisher = new OCIPublisher<>(
			secondOCILayout);

		Assert.assertEquals(
			digest,
			secondOCIPublisher.publishHelmChart(
				helmChartArchive, _getLayoutRef(secondOCILayout, "2.0.0")));

		Manifest manifest = ociLayout.getManifest(layoutRef.withDigest(digest));

		Config config = manifest.getConfig();

		Assert.assertEquals(
			helmChartArchive.getChartMetadata(),
			_objectMapper.readValue(
				_getBlob(
					config.getDigest(), layoutRef, ociLayout, config.getSize()),
				new TypeReference<Map<String, Object>>() {
				}));

		List<Layer> layers = manifest.getLayers();

		Assert.assertEquals(layers.toString(), 1, layers.size());

		Layer layer = layers.get(0);

		Assert.assertArrayEquals(
			helmChartArchive.getData(),
			_getBlob(layer.getDigest(), layoutRef, ociLayout, layer.getSize()));
	}

	@Test
	public void testToRegistryException() {
		RuntimeException runtimeException = OCIPublisher.toRegistryException(
			new OrasException(
				"Unable to create HTTP request",
				new SSLException(
					"Unrecognized SSL message, plaintext connection?")),
			"localhost:5000/example");

		String message = runtimeException.getMessage();

		Assert.assertTrue(
			message,
			message.contains(
				"Unable to reach \"localhost:5000/example\" over HTTPS"));
		Assert.assertTrue(message, message.contains("registries.conf"));
	}

	@Test
	public void testToRegistryExceptionWhenCauseIsNotSSLException() {
		OrasException orasException = new OrasException(401, "Unauthorized");

		Assert.assertSame(
			orasException,
			OCIPublisher.toRegistryException(
				orasException, "localhost:5000/example"));
	}

	@Rule
	public final TemporaryFolder temporaryFolder = new TemporaryFolder();

	private byte[] _getBlob(
		String digest, LayoutRef layoutRef, OCILayout ociLayout, long size) {

		byte[] data = ociLayout.getBlob(layoutRef.withDigest(digest));

		Assert.assertEquals(digest, size, data.length);

		return data;
	}

	private String _getDigest(byte[] data) throws Exception {
		MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");

		HexFormat hexFormat = HexFormat.of();

		return "sha256:" + hexFormat.formatHex(messageDigest.digest(data));
	}

	private LayoutRef _getLayoutRef(OCILayout ociLayout, String tag) {
		return LayoutRef.parse(ociLayout.getPath() + ":" + tag);
	}

	private OCILayout _getOCILayout() throws Exception {
		File dir = temporaryFolder.newFolder();

		return OCILayout.builder(
		).defaults(
			dir.toPath()
		).build();
	}

	private byte[] _getTar() throws Exception {
		ArtifactLayer artifactLayer = ArtifactLayer.plan(
			Map.of(
				"static/a.css",
				ArtifactFile.newFile(
					"a {}".getBytes(StandardCharsets.UTF_8), 0644,
					"static/a.css")),
			List.of(
				new ClientExtensionDockerfile.Copy(
					"/public_html/", List.of("static/"))));

		return artifactLayer.toTar();
	}

	private static final ObjectMapper _objectMapper = new ObjectMapper();

}