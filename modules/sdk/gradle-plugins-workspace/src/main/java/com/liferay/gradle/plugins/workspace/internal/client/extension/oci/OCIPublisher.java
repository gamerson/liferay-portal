/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.gradle.plugins.workspace.internal.client.extension.oci;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import land.oras.Config;
import land.oras.ContainerRef;
import land.oras.Index;
import land.oras.Layer;
import land.oras.LayoutRef;
import land.oras.Manifest;
import land.oras.ManifestDescriptor;
import land.oras.OCI;
import land.oras.OCILayout;
import land.oras.Platform;
import land.oras.Ref;
import land.oras.Registry;

/**
 * Publishes the artifact and the chart with oras-java, either to a registry or
 * to a local OCI layout.
 *
 * @author Gregory Amerson
 */
public class OCIPublisher {

	public static OCIPublisher forLayout(Path layoutDirPath) {
		return new OCIPublisher(layoutDirPath, false);
	}

	public static OCIPublisher forRegistry(boolean plainHTTP) {
		return new OCIPublisher(null, plainHTTP);
	}

	/**
	 * Publishes the layer as an image index with one manifest per platform.
	 * The layer holds only files, so every platform shares it, and an index
	 * keeps a node of any architecture from refusing the pull.
	 */
	public String publishArtifact(String repository, byte[] tar, String tag)
		throws IOException {

		byte[] layerData = _gzip(tar);

		if (_layoutDirPath != null) {
			OCILayout ociLayout = _getOCILayout(
				_layoutDirPath.resolve("artifact"));

			return _publishArtifact(
				layerData, ociLayout,
				LayoutRef.parse(ociLayout.getPath() + ":" + tag), tar);
		}

		return _publishArtifact(
			layerData, _getRegistry(),
			ContainerRef.parse(repository + ":" + tag), tar);
	}

	/**
	 * Publishes a packaged chart the way helm push does, so helm pull and helm
	 * install read it back from oci:// unchanged.
	 */
	public String publishChart(
			byte[] chartArchive, Map<String, Object> chartMetadata,
			String repository, String tag)
		throws IOException {

		byte[] configData = _objectMapper.writeValueAsBytes(chartMetadata);

		if (_layoutDirPath != null) {
			OCILayout ociLayout = _getOCILayout(
				_layoutDirPath.resolve("chart"));

			return _publishChart(
				chartArchive, configData, ociLayout,
				LayoutRef.parse(ociLayout.getPath() + ":" + tag));
		}

		return _publishChart(
			chartArchive, configData, _getRegistry(),
			ContainerRef.parse(repository + ":" + tag));
	}

	private OCIPublisher(Path layoutDirPath, boolean plainHTTP) {
		_layoutDirPath = layoutDirPath;
		_plainHTTP = plainHTTP;
	}

	private String _getDigest(byte[] data) {
		try {
			MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");

			HexFormat hexFormat = HexFormat.of();

			return "sha256:" + hexFormat.formatHex(messageDigest.digest(data));
		}
		catch (NoSuchAlgorithmException noSuchAlgorithmException) {
			throw new IllegalStateException(noSuchAlgorithmException);
		}
	}

	private OCILayout _getOCILayout(Path path) throws IOException {
		Files.createDirectories(path);

		return OCILayout.builder(
		).defaults(
			path
		).build();
	}

	private Registry _getRegistry() {
		return Registry.builder(
		).defaults(
		).withInsecure(
			_plainHTTP
		).build();
	}

	private byte[] _gzip(byte[] data) throws IOException {
		ByteArrayOutputStream byteArrayOutputStream =
			new ByteArrayOutputStream();

		try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(
				byteArrayOutputStream)) {

			gzipOutputStream.write(data);
		}

		return byteArrayOutputStream.toByteArray();
	}

	private <T extends Ref<T>> String _publishArtifact(
		byte[] layerData, OCI<T> oci, T ref, byte[] tar) {

		Layer layer = _pushBlob(
			layerData, "application/vnd.oci.image.layer.v1.tar+gzip", oci, ref);

		List<ManifestDescriptor> manifestDescriptors = new ArrayList<>();

		for (String architecture : new String[] {"amd64", "arm64"}) {
			String configJSON =
				"{\"architecture\":\"" + architecture +
					"\",\"config\":{},\"os\":\"linux\",\"rootfs\":" +
						"{\"diff_ids\":[\"" + _getDigest(tar) +
							"\"],\"type\":\"layers\"}}";

			Layer configLayer = _pushBlob(
				configJSON.getBytes(StandardCharsets.UTF_8),
				"application/vnd.oci.image.config.v1+json", oci, ref);

			Manifest manifest = Manifest.empty(
			).withAnnotations(
				_annotations
			).withConfig(
				Config.fromBlob(
					"application/vnd.oci.image.config.v1+json", configLayer)
			).withLayers(
				List.of(layer)
			);

			manifest = oci.pushManifest(ref, manifest);

			ManifestDescriptor manifestDescriptor = manifest.getDescriptor();

			manifestDescriptors.add(
				manifestDescriptor.withPlatform(
					Platform.of("linux", architecture)));
		}

		Index index = Index.fromManifests(manifestDescriptors);

		index = oci.pushIndex(ref, index.withAnnotations(_annotations));

		ManifestDescriptor manifestDescriptor = index.getDescriptor();

		return manifestDescriptor.getDigest();
	}

	private <T extends Ref<T>> String _publishChart(
		byte[] chartArchive, byte[] configData, OCI<T> oci, T ref) {

		Layer configLayer = _pushBlob(
			configData, _MEDIA_TYPE_HELM_CONFIG, oci, ref);

		Manifest manifest = Manifest.empty(
		).withAnnotations(
			_annotations
		).withConfig(
			Config.fromBlob(_MEDIA_TYPE_HELM_CONFIG, configLayer)
		).withLayers(
			List.of(_pushBlob(chartArchive, _MEDIA_TYPE_HELM_CHART, oci, ref))
		);

		manifest = oci.pushManifest(ref, manifest);

		ManifestDescriptor manifestDescriptor = manifest.getDescriptor();

		return manifestDescriptor.getDigest();
	}

	/**
	 * Pushes a blob and returns a descriptor that refers to it by digest
	 * alone. The layer oras-java returns from a push carries the blob's own
	 * bytes, which a manifest would otherwise embed. A local layout needs the
	 * blob's digest in the reference, and a registry accepts it.
	 */
	private <T extends Ref<T>> Layer _pushBlob(
		byte[] data, String mediaType, OCI<T> oci, T ref) {

		Layer layer = oci.pushBlob(ref.withDigest(_getDigest(data)), data);

		Layer digestLayer = Layer.fromDigest(
			layer.getDigest(), layer.getSize());

		return digestLayer.withMediaType(mediaType);
	}

	private static final String _MEDIA_TYPE_HELM_CHART =
		"application/vnd.cncf.helm.chart.content.v1.tar+gzip";

	private static final String _MEDIA_TYPE_HELM_CONFIG =
		"application/vnd.cncf.helm.config.v1+json";

	/**
	 * oras-java stamps the current time on every manifest and index it pushes
	 * unless one is given, which would change every digest on every run.
	 */
	private static final Map<String, String> _annotations = Map.of(
		"org.opencontainers.image.created", "1970-01-01T00:00:00Z");

	private static final ObjectMapper _objectMapper = new ObjectMapper(
	).configure(
		SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true
	);

	private final Path _layoutDirPath;
	private final boolean _plainHTTP;

}