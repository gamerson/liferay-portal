/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.gradle.plugins.workspace.internal.client.extension.oci;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import com.liferay.gradle.plugins.workspace.internal.util.StringUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import java.nio.charset.StandardCharsets;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import javax.net.ssl.SSLException;

import land.oras.Index;
import land.oras.Manifest;
import land.oras.ManifestDescriptor;
import land.oras.OCI;
import land.oras.Ref;
import land.oras.exception.OrasException;

import org.gradle.api.GradleException;

/**
 * @author Gregory Amerson
 */
public class OCIPublisher<T extends Ref<T>> {

	public static final String MEDIA_TYPE_HELM_CHART =
		"application/vnd.cncf.helm.chart.content.v1.tar+gzip";

	public static final String MEDIA_TYPE_HELM_CONFIG =
		"application/vnd.cncf.helm.config.v1+json";

	public static final String MEDIA_TYPE_IMAGE_MANIFEST =
		"application/vnd.oci.image.manifest.v1+json";

	public static RuntimeException toRegistryException(
		OrasException orasException, String reference) {

		Throwable throwable = orasException;

		while (throwable != null) {
			if (throwable instanceof SSLException) {
				return new GradleException(
					StringUtil.concat(
						"Unable to reach \"", reference, "\" over HTTPS. If ",
						"its registry only accepts HTTP, such as a local ",
						"registry, mark the registry insecure in ",
						"~/.config/containers/registries.conf."),
					orasException);
			}

			throwable = throwable.getCause();
		}

		return orasException;
	}

	public OCIPublisher(OCI<T> oci) {
		_oci = oci;
	}

	public String publishArtifact(T ref, byte[] tar) throws IOException {
		Map<String, Object> layer = _pushBlob(
			_gzip(tar), _MEDIA_TYPE_IMAGE_LAYER, ref);

		List<Map<String, Object>> manifests = new ArrayList<>();

		for (String architecture : new String[] {"amd64", "arm64"}) {
			Map<String, Object> platform = new LinkedHashMap<>();

			platform.put("architecture", architecture);
			platform.put("os", "linux");

			Map<String, Object> imageConfig = new LinkedHashMap<>(platform);

			imageConfig.put("config", Map.of());
			imageConfig.put(
				"rootfs",
				Map.of("diff_ids", List.of(_getDigest(tar)), "type", "layers"));

			byte[] data = _toManifestJSON(
				_pushBlob(
					_objectMapper.writeValueAsBytes(imageConfig),
					_MEDIA_TYPE_IMAGE_CONFIG, ref),
				layer);

			_oci.pushManifest(
				ref.withDigest(_getDigest(data)),
				Manifest.fromJson(new String(data, StandardCharsets.UTF_8)));

			Map<String, Object> manifest = _getDescriptor(
				data, MEDIA_TYPE_IMAGE_MANIFEST);

			manifest.put("platform", platform);

			manifests.add(manifest);
		}

		Map<String, Object> index = new LinkedHashMap<>();

		index.put("manifests", manifests);
		index.put("mediaType", _MEDIA_TYPE_IMAGE_INDEX);
		index.put("schemaVersion", 2);

		Index pushedIndex = _oci.pushIndex(
			ref,
			Index.fromJson(
				new String(
					_objectMapper.writeValueAsBytes(index),
					StandardCharsets.UTF_8)));

		ManifestDescriptor manifestDescriptor = pushedIndex.getDescriptor();

		return manifestDescriptor.getDigest();
	}

	public String publishHelmChart(HelmChartArchive helmChartArchive, T ref)
		throws IOException {

		byte[] data = _toManifestJSON(
			_pushBlob(
				_objectMapper.writeValueAsBytes(
					helmChartArchive.getChartMetadata()),
				MEDIA_TYPE_HELM_CONFIG, ref),
			_pushBlob(helmChartArchive.getData(), MEDIA_TYPE_HELM_CHART, ref));

		Manifest manifest = _oci.pushManifest(
			ref, Manifest.fromJson(new String(data, StandardCharsets.UTF_8)));

		ManifestDescriptor manifestDescriptor = manifest.getDescriptor();

		return manifestDescriptor.getDigest();
	}

	private Map<String, Object> _getDescriptor(byte[] data, String mediaType) {
		Map<String, Object> descriptor = new LinkedHashMap<>();

		descriptor.put("digest", _getDigest(data));
		descriptor.put("mediaType", mediaType);
		descriptor.put("size", data.length);

		return descriptor;
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

	private byte[] _gzip(byte[] data) throws IOException {
		ByteArrayOutputStream byteArrayOutputStream =
			new ByteArrayOutputStream();

		try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(
				byteArrayOutputStream)) {

			gzipOutputStream.write(data);
		}

		return byteArrayOutputStream.toByteArray();
	}

	private Map<String, Object> _pushBlob(
		byte[] data, String mediaType, T ref) {

		Map<String, Object> descriptor = _getDescriptor(data, mediaType);

		_oci.pushBlob(ref.withDigest((String)descriptor.get("digest")), data);

		return descriptor;
	}

	private byte[] _toManifestJSON(
			Map<String, Object> config, Map<String, Object> layer)
		throws IOException {

		Map<String, Object> manifest = new LinkedHashMap<>();

		manifest.put("config", config);
		manifest.put("layers", List.of(layer));
		manifest.put("mediaType", MEDIA_TYPE_IMAGE_MANIFEST);
		manifest.put("schemaVersion", 2);

		return _objectMapper.writeValueAsBytes(manifest);
	}

	private static final String _MEDIA_TYPE_IMAGE_CONFIG =
		"application/vnd.oci.image.config.v1+json";

	private static final String _MEDIA_TYPE_IMAGE_INDEX =
		"application/vnd.oci.image.index.v1+json";

	private static final String _MEDIA_TYPE_IMAGE_LAYER =
		"application/vnd.oci.image.layer.v1.tar+gzip";

	private static final ObjectMapper _objectMapper = new ObjectMapper(
	).configure(
		SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true
	);

	private final OCI<T> _oci;

}