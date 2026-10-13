/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.gradle.plugins.workspace.internal.client.extension.oci;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;

import org.gradle.api.GradleException;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.representer.Representer;

/**
 * The client extension chart as a chart archive, the same layout helm package
 * produces, with the build's values and the chart's name and version baked in.
 *
 * @author Gregory Amerson
 */
public class ChartArchive {

	public static ChartArchive create(
			Map<String, byte[]> chartFiles, String chartName,
			Map<String, Object> values, String version)
		throws IOException {

		Map<String, byte[]> archiveFiles = new TreeMap<>(chartFiles);

		Map<String, Object> chartOverlay = new LinkedHashMap<>();

		chartOverlay.put("appVersion", version);
		chartOverlay.put("name", chartName);
		chartOverlay.put("version", version);

		String chartYAML = overlayYAML(
			_getText(archiveFiles, "Chart.yaml"), chartOverlay);

		archiveFiles.put(
			"Chart.yaml", chartYAML.getBytes(StandardCharsets.UTF_8));

		String valuesYAML = overlayYAML(
			_getText(archiveFiles, "values.yaml"), values);

		archiveFiles.put(
			"values.yaml", valuesYAML.getBytes(StandardCharsets.UTF_8));

		ByteArrayOutputStream byteArrayOutputStream =
			new ByteArrayOutputStream();

		try (TarArchiveOutputStream tarArchiveOutputStream =
				new TarArchiveOutputStream(
					new GZIPOutputStream(byteArrayOutputStream))) {

			tarArchiveOutputStream.setLongFileMode(
				TarArchiveOutputStream.LONGFILE_POSIX);

			List<String> names = new ArrayList<>(archiveFiles.keySet());

			names.remove("Chart.yaml");

			names.add(0, "Chart.yaml");

			for (String name : names) {
				byte[] data = archiveFiles.get(name);

				TarArchiveEntry tarArchiveEntry = new TarArchiveEntry(
					chartName + "/" + name);

				tarArchiveEntry.setGroupId(0);
				tarArchiveEntry.setGroupName("");
				tarArchiveEntry.setMode(0644);
				tarArchiveEntry.setModTime(0);
				tarArchiveEntry.setSize(data.length);
				tarArchiveEntry.setUserId(0);
				tarArchiveEntry.setUserName("");

				tarArchiveOutputStream.putArchiveEntry(tarArchiveEntry);

				tarArchiveOutputStream.write(data);

				tarArchiveOutputStream.closeArchiveEntry();
			}
		}

		Yaml yaml = _newYaml();

		Map<String, Object> chartMetadata = yaml.load(chartYAML);

		return new ChartArchive(
			byteArrayOutputStream.toByteArray(), chartMetadata);
	}

	/**
	 * Sets every leaf of the overlay in the YAML document, keeping the
	 * document's comments and key order.
	 */
	public static String overlayYAML(
		String document, Map<String, Object> overlay) {

		Yaml yaml = _newYaml();

		Node node = yaml.compose(new StringReader(document));

		if (!(node instanceof MappingNode)) {
			throw new GradleException("The YAML document is not a mapping");
		}

		_overlay((MappingNode)node, overlay, yaml);

		StringWriter stringWriter = new StringWriter();

		yaml.serialize(node, stringWriter);

		return stringWriter.toString();
	}

	public static Map<String, byte[]> readChartFiles(Path chartDirPath)
		throws IOException {

		Map<String, byte[]> chartFiles = new TreeMap<>();

		try (Stream<Path> stream = Files.walk(chartDirPath)) {
			for (Path path : (Iterable<Path>)stream::iterator) {
				if (!Files.isRegularFile(path)) {
					continue;
				}

				String name = String.valueOf(chartDirPath.relativize(path));

				chartFiles.put(
					name.replace('\\', '/'), Files.readAllBytes(path));
			}
		}

		return chartFiles;
	}

	/**
	 * Returns cloud/helm/client-extension, which the plugin build packs into
	 * the plugin so a workspace needs nothing else to package a chart.
	 */
	public static Map<String, byte[]> readEmbeddedChartFiles()
		throws IOException {

		Map<String, byte[]> chartFiles = new TreeMap<>();

		try (InputStream inputStream = ChartArchive.class.getResourceAsStream(
				"dependencies/client-extension-chart.zip")) {

			if (inputStream == null) {
				throw new GradleException(
					"The client extension chart is missing from the plugin");
			}

			try (ZipInputStream zipInputStream = new ZipInputStream(
					inputStream)) {

				ZipEntry zipEntry = null;

				while ((zipEntry = zipInputStream.getNextEntry()) != null) {
					if (!zipEntry.isDirectory()) {
						chartFiles.put(
							zipEntry.getName(), zipInputStream.readAllBytes());
					}
				}
			}
		}

		return chartFiles;
	}

	public Map<String, Object> getChartMetadata() {
		return _chartMetadata;
	}

	public byte[] getData() {
		return _data;
	}

	private static String _getText(
		Map<String, byte[]> chartFiles, String name) {

		byte[] data = chartFiles.get(name);

		if (data == null) {
			throw new GradleException("The chart has no " + name);
		}

		return new String(data, StandardCharsets.UTF_8);
	}

	private static Yaml _newYaml() {
		DumperOptions dumperOptions = new DumperOptions();

		dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
		dumperOptions.setIndent(4);
		dumperOptions.setIndicatorIndent(0);
		dumperOptions.setProcessComments(true);
		dumperOptions.setWidth(120);

		LoaderOptions loaderOptions = new LoaderOptions();

		loaderOptions.setProcessComments(true);

		return new Yaml(
			new SafeConstructor(loaderOptions), new Representer(dumperOptions),
			dumperOptions, loaderOptions);
	}

	@SuppressWarnings("unchecked")
	private static void _overlay(
		MappingNode mappingNode, Map<String, Object> overlay, Yaml yaml) {

		List<NodeTuple> nodeTuples = mappingNode.getValue();

		for (String key : new TreeMap<>(overlay).keySet()) {
			Object value = overlay.get(key);

			int index = -1;

			for (int i = 0; i < nodeTuples.size(); i++) {
				NodeTuple nodeTuple = nodeTuples.get(i);

				Node keyNode = nodeTuple.getKeyNode();

				if (!(keyNode instanceof ScalarNode)) {
					continue;
				}

				ScalarNode scalarNode = (ScalarNode)keyNode;

				if (key.equals(scalarNode.getValue())) {
					index = i;

					break;
				}
			}

			if (index < 0) {
				nodeTuples.add(
					new NodeTuple(yaml.represent(key), yaml.represent(value)));

				continue;
			}

			NodeTuple nodeTuple = nodeTuples.get(index);

			Node valueNode = nodeTuple.getValueNode();

			if ((value instanceof Map) && (valueNode instanceof MappingNode)) {
				MappingNode valueMappingNode = (MappingNode)valueNode;

				List<NodeTuple> valueNodeTuples = valueMappingNode.getValue();

				if (!valueNodeTuples.isEmpty()) {
					_overlay(
						valueMappingNode, (Map<String, Object>)value, yaml);

					continue;
				}
			}

			Node newValueNode = yaml.represent(value);

			newValueNode.setInLineComments(valueNode.getInLineComments());

			nodeTuples.set(
				index, new NodeTuple(nodeTuple.getKeyNode(), newValueNode));
		}

		mappingNode.setValue(nodeTuples);
	}

	private ChartArchive(byte[] data, Map<String, Object> chartMetadata) {
		_data = data;
		_chartMetadata = chartMetadata;
	}

	private final Map<String, Object> _chartMetadata;
	private final byte[] _data;

}