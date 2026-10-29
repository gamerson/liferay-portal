/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.gradle.plugins.workspace.internal.client.extension.oci;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.nio.file.Files;
import java.nio.file.Path;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import land.oras.Config;
import land.oras.ContainerRef;
import land.oras.Descriptor;
import land.oras.Layer;
import land.oras.Manifest;
import land.oras.OCI;
import land.oras.Ref;
import land.oras.Registry;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

import org.gradle.api.GradleException;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

/**
 * @author Gregory Amerson
 */
public class HelmChartSource {

	public static final String ANNOTATION_VALUES_CONTRACT =
		"cx.liferay.com/values-contract";

	public static File getLocalFile(String reference, File rootDir) {
		if ((reference == null) || reference.isBlank() ||
			reference.startsWith("http://") ||
			reference.startsWith("https://") ||
			reference.startsWith("oci://")) {

			return null;
		}

		File file = new File(reference);

		if (file.isAbsolute()) {
			return file;
		}

		return new File(rootDir, reference);
	}

	public static <T extends Ref<T>> HelmChartSource pull(
			OCI<T> oci, T ref, String repository)
		throws IOException {

		Descriptor descriptor = oci.getDescriptor(ref);

		if (!OCIPublisher.MEDIA_TYPE_IMAGE_MANIFEST.equals(
				descriptor.getMediaType())) {

			throw new GradleException(
				"\"" + repository + "\" is not a Helm chart");
		}

		Manifest manifest = oci.getManifest(ref);

		Config config = manifest.getConfig();

		if (!OCIPublisher.MEDIA_TYPE_HELM_CONFIG.equals(
				config.getMediaType())) {

			throw new GradleException(
				"\"" + repository + "\" is not a Helm chart");
		}

		for (Layer layer : manifest.getLayers()) {
			if (!OCIPublisher.MEDIA_TYPE_HELM_CHART.equals(
					layer.getMediaType())) {

				continue;
			}

			return new HelmChartSource(
				_readTGZ(oci.getBlob(ref.withDigest(layer.getDigest()))),
				repository + "@" + descriptor.getDigest());
		}

		throw new GradleException(
			"\"" + repository + "\" has no Helm chart archive layer");
	}

	public static HelmChartSource resolveReference(
			String reference, File rootDir)
		throws InterruptedException, IOException {

		HelmChartSource helmChartSource = null;

		if ((reference == null) || reference.isBlank()) {
			helmChartSource = new HelmChartSource(_readEmbedded(), "embedded");
		}
		else if (reference.startsWith("oci://")) {
			ContainerRef containerRef = ContainerRef.parse(
				reference.substring("oci://".length()));

			helmChartSource = pull(
				Registry.builder(
				).defaults(
				).build(),
				containerRef,
				"oci://" + containerRef.getRegistry() + "/" +
					containerRef.getFullRepository());
		}
		else if (reference.startsWith("http://") ||
				 reference.startsWith("https://")) {

			helmChartSource = _readURL(reference);
		}
		else {
			helmChartSource = _readLocal(getLocalFile(reference, rootDir));
		}

		helmChartSource._validate(reference);

		return helmChartSource;
	}

	public Map<String, byte[]> getChartFiles() {
		return _chartFiles;
	}

	public String getOrigin() {
		return _origin;
	}

	public String getTemplate() {
		return _chartMetadata.get("name") + " " + _chartMetadata.get("version");
	}

	/**
	 * Returns whether the chart was read from a directory or an archive on
	 * this machine, rather than embedded in the plugin or downloaded.
	 */
	public boolean isLocal() {
		if (_origin.equals("embedded") || _origin.contains("://")) {
			return false;
		}

		return true;
	}

	private static String _getSHA256(byte[] data) {
		try {
			MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");

			HexFormat hexFormat = HexFormat.of();

			return "sha256:" + hexFormat.formatHex(messageDigest.digest(data));
		}
		catch (NoSuchAlgorithmException noSuchAlgorithmException) {
			throw new IllegalStateException(noSuchAlgorithmException);
		}
	}

	private static Map<String, byte[]> _readEmbedded() throws IOException {
		Map<String, byte[]> chartFiles = new TreeMap<>();

		try (InputStream inputStream =
				HelmChartSource.class.getResourceAsStream(
					"dependencies/client-extension-helm-chart.zip")) {

			if (inputStream == null) {
				throw new GradleException(
					"The plugin is missing the client extension Helm chart");
			}

			try (ZipInputStream zipInputStream = new ZipInputStream(
					inputStream)) {

				ZipEntry zipEntry = null;

				while ((zipEntry = zipInputStream.getNextEntry()) != null) {
					if (zipEntry.isDirectory()) {
						continue;
					}

					chartFiles.put(
						zipEntry.getName(), zipInputStream.readAllBytes());
				}
			}
		}

		return chartFiles;
	}

	private static HelmChartSource _readLocal(File file) throws IOException {
		if (file.isDirectory()) {
			Map<String, byte[]> chartFiles = new TreeMap<>();

			Path dirPath = file.toPath();

			try (Stream<Path> stream = Files.walk(dirPath)) {
				for (Path path : (Iterable<Path>)stream::iterator) {
					if (!Files.isRegularFile(path)) {
						continue;
					}

					String name = String.valueOf(dirPath.relativize(path));

					chartFiles.put(
						name.replace('\\', '/'), Files.readAllBytes(path));
				}
			}

			return new HelmChartSource(chartFiles, file.getAbsolutePath());
		}

		if (file.isFile()) {
			return new HelmChartSource(
				_readTGZ(Files.readAllBytes(file.toPath())),
				file.getAbsolutePath());
		}

		throw new GradleException(
			"Chart \"" + file + "\" is neither a chart directory nor a chart " +
				"archive");
	}

	private static Map<String, byte[]> _readTGZ(byte[] data)
		throws IOException {

		Map<String, byte[]> chartFiles = new TreeMap<>();

		try (TarArchiveInputStream tarArchiveInputStream =
				new TarArchiveInputStream(
					new GZIPInputStream(new ByteArrayInputStream(data)))) {

			TarArchiveEntry tarArchiveEntry = null;

			while ((tarArchiveEntry = tarArchiveInputStream.getNextEntry()) !=
						null) {

				if (!tarArchiveEntry.isFile()) {
					continue;
				}

				String name = tarArchiveEntry.getName();

				int index = name.indexOf('/');

				List<String> segments = List.of(name.split("/"));

				if ((index <= 0) || segments.contains("..")) {
					throw new GradleException(
						"Chart archive entry \"" + name + "\" is not under " +
							"the chart's directory");
				}

				chartFiles.put(
					name.substring(index + 1),
					tarArchiveInputStream.readAllBytes());
			}
		}

		return chartFiles;
	}

	private static HelmChartSource _readURL(String reference)
		throws InterruptedException, IOException {

		HttpClient httpClient = HttpClient.newBuilder(
		).followRedirects(
			HttpClient.Redirect.NORMAL
		).build();

		HttpResponse<byte[]> httpResponse = httpClient.send(
			HttpRequest.newBuilder(
				URI.create(reference)
			).GET(
			).build(),
			HttpResponse.BodyHandlers.ofByteArray());

		if (httpResponse.statusCode() != 200) {
			throw new GradleException(
				"Unable to download chart \"" + reference + "\": HTTP " +
					httpResponse.statusCode());
		}

		byte[] data = httpResponse.body();

		return new HelmChartSource(
			_readTGZ(data), reference + " " + _getSHA256(data));
	}

	private HelmChartSource(Map<String, byte[]> chartFiles, String origin) {
		_chartFiles = chartFiles;
		_origin = origin;

		_chartMetadata = _readChartMetadata();
	}

	private Map<String, Object> _readChartMetadata() {
		byte[] data = _chartFiles.get("Chart.yaml");

		if (data == null) {
			throw new GradleException("The chart has no \"Chart.yaml\"");
		}

		Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));

		Map<String, Object> chartMetadata = yaml.load(
			new ByteArrayInputStream(data));

		if (chartMetadata == null) {
			throw new GradleException("The chart's \"Chart.yaml\" is empty");
		}

		return chartMetadata;
	}

	private void _validate(String reference) {
		Object annotations = _chartMetadata.get("annotations");

		Object valuesContract = null;

		if (annotations instanceof Map<?, ?> annotationsMap) {
			valuesContract = annotationsMap.get(ANNOTATION_VALUES_CONTRACT);
		}

		if (_valuesContracts.contains(String.valueOf(valuesContract))) {
			return;
		}

		String chart = "The embedded chart";

		if ((reference != null) && !reference.isBlank()) {
			chart = "Chart \"" + reference + "\"";
		}

		if (valuesContract == null) {
			throw new GradleException(
				chart + " does not declare the \"" +
					ANNOTATION_VALUES_CONTRACT + "\" annotation, so it may " +
						"not accept the values this plugin writes into it");
		}

		throw new GradleException(
			chart + " declares values contract \"" + valuesContract +
				"\", but this plugin supports only " + _valuesContracts);
	}

	private static final Set<String> _valuesContracts = Set.of("1");

	private final Map<String, byte[]> _chartFiles;
	private final Map<String, Object> _chartMetadata;
	private final String _origin;

}