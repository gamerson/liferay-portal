/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.gradle.plugins.workspace.internal.overlay;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import java.security.DigestOutputStream;
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
 * Publishes an overlay layer as an image that holds files alone, either to a
 * registry or to a local OCI layout. Kubernetes mounts it into the Liferay pod
 * as an image volume.
 *
 * @author Gregory Amerson
 */
public class OverlayPublisher {

	public static OverlayPublisher forLayout(Path layoutDirPath) {
		return new OverlayPublisher(layoutDirPath, false);
	}

	public static OverlayPublisher forRegistry(boolean plainHTTP) {
		return new OverlayPublisher(null, plainHTTP);
	}

	/**
	 * Publishes the tar as the single layer of an image index with one
	 * manifest per platform, and returns the index digest. The layer holds
	 * only files, so every platform shares it, and the index keeps a node of
	 * any architecture from refusing the pull.
	 */
	public String publish(String repository, String tag, Path tarPath)
		throws IOException {

		Path layerPath = tarPath.resolveSibling(tarPath.getFileName() + ".gz");

		String layerDigest = _gzip(tarPath, layerPath);

		String diffId = _getDigest(tarPath);

		if (_layoutDirPath != null) {
			Files.createDirectories(_layoutDirPath);

			OCILayout ociLayout = OCILayout.builder(
			).defaults(
				_layoutDirPath
			).build();

			return _publish(
				diffId, layerDigest, layerPath, ociLayout,
				LayoutRef.parse(ociLayout.getPath() + ":" + tag));
		}

		Registry registry = Registry.builder(
		).defaults(
		).withInsecure(
			_plainHTTP
		).build();

		return _publish(
			diffId, layerDigest, layerPath, registry,
			ContainerRef.parse(repository + ":" + tag));
	}

	private OverlayPublisher(Path layoutDirPath, boolean plainHTTP) {
		_layoutDirPath = layoutDirPath;
		_plainHTTP = plainHTTP;
	}

	private String _getDigest(byte[] data) {
		MessageDigest messageDigest = _getMessageDigest();

		HexFormat hexFormat = HexFormat.of();

		return "sha256:" + hexFormat.formatHex(messageDigest.digest(data));
	}

	private String _getDigest(Path path) throws IOException {
		MessageDigest messageDigest = _getMessageDigest();

		try (InputStream inputStream = Files.newInputStream(path);
			OutputStream outputStream = new DigestOutputStream(
				OutputStream.nullOutputStream(), messageDigest)) {

			inputStream.transferTo(outputStream);
		}

		HexFormat hexFormat = HexFormat.of();

		return "sha256:" + hexFormat.formatHex(messageDigest.digest());
	}

	private MessageDigest _getMessageDigest() {
		try {
			return MessageDigest.getInstance("SHA-256");
		}
		catch (NoSuchAlgorithmException noSuchAlgorithmException) {
			throw new IllegalStateException(noSuchAlgorithmException);
		}
	}

	private String _gzip(Path sourcePath, Path targetPath) throws IOException {
		try (InputStream inputStream = Files.newInputStream(sourcePath);
			OutputStream outputStream = new GZIPOutputStream(
				Files.newOutputStream(targetPath))) {

			inputStream.transferTo(outputStream);
		}

		return _getDigest(targetPath);
	}

	private <T extends Ref<T>> String _publish(
			String diffId, String layerDigest, Path layerPath, OCI<T> oci,
			T ref)
		throws IOException {

		Layer layer = oci.pushBlob(ref.withDigest(layerDigest), layerPath);

		layer = Layer.fromDigest(layer.getDigest(), Files.size(layerPath));

		layer = layer.withMediaType(
			"application/vnd.oci.image.layer.v1.tar+gzip");

		List<ManifestDescriptor> manifestDescriptors = new ArrayList<>();

		for (String architecture : new String[] {"amd64", "arm64"}) {
			String configJSON =
				"{\"architecture\":\"" + architecture +
					"\",\"config\":{},\"os\":\"linux\",\"rootfs\":" +
						"{\"diff_ids\":[\"" + diffId +
							"\"],\"type\":\"layers\"}}";

			byte[] configData = configJSON.getBytes(StandardCharsets.UTF_8);

			Layer configLayer = oci.pushBlob(
				ref.withDigest(_getDigest(configData)), configData);

			Manifest manifest = Manifest.empty(
			).withAnnotations(
				_annotations
			).withConfig(
				Config.fromBlob(
					"application/vnd.oci.image.config.v1+json",
					Layer.fromDigest(
						configLayer.getDigest(), configData.length))
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

	/**
	 * oras-java stamps the current time on every manifest and index it pushes
	 * unless one is given, which would change every digest on every run.
	 */
	private static final Map<String, String> _annotations = Map.of(
		"org.opencontainers.image.created", "1970-01-01T00:00:00Z");

	private final Path _layoutDirPath;
	private final boolean _plainHTTP;

}