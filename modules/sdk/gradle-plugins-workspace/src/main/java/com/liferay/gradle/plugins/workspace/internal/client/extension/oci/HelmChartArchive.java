/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.gradle.plugins.workspace.internal.client.extension.oci;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import java.nio.charset.StandardCharsets;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.GZIPOutputStream;

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
 * @author Gregory Amerson
 */
public class HelmChartArchive {

	/**
	 * Names the artifact the chart mounts, as a reference pinned by digest.
	 */
	public static final String ANNOTATION_ARTIFACT = "cx.liferay.com/artifact";

	/**
	 * Holds the client extension's content digest, which tells whether two
	 * charts of the same version deploy the same thing.
	 */
	public static final String ANNOTATION_CONTENT_DIGEST =
		"cx.liferay.com/content-digest";

	/**
	 * Returns the chart for the client extension. The annotations are added to
	 * the chart's own, beside the template it was made from. The chart's
	 * origin is recorded only when it is not a local file, so a published
	 * chart never carries a path from the machine that built it.
	 */
	public static HelmChartArchive create(
			Map<String, String> annotations, String chartName,
			HelmChartSource helmChartSource, Map<String, Object> values,
			String version)
		throws IOException {

		Map<String, byte[]> archiveFiles = new TreeMap<>(
			helmChartSource.getChartFiles());

		Map<String, Object> chartOverlay = new LinkedHashMap<>();

		Map<String, Object> chartAnnotations = new TreeMap<>(annotations);

		if (!helmChartSource.isLocal()) {
			chartAnnotations.put(
				_ANNOTATION_CHART_SOURCE, helmChartSource.getOrigin());
		}

		chartAnnotations.put(
			_ANNOTATION_CHART_TEMPLATE, helmChartSource.getTemplate());

		chartOverlay.put("annotations", chartAnnotations);
		chartOverlay.put("appVersion", version);
		chartOverlay.put("name", chartName);
		chartOverlay.put("version", version);

		String chartYAML = overlayYAML(
			true, _getText(archiveFiles, "Chart.yaml"), chartOverlay);

		archiveFiles.put(
			"Chart.yaml", chartYAML.getBytes(StandardCharsets.UTF_8));

		String valuesYAML = overlayYAML(
			false, _getText(archiveFiles, "values.yaml"), values);

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

		return new HelmChartArchive(
			yaml.load(chartYAML), byteArrayOutputStream.toByteArray());
	}

	public static String overlayYAML(
		boolean allowNewKeys, String document, Map<String, Object> overlay) {

		Yaml yaml = _newYaml();

		Node node = yaml.compose(new StringReader(document));

		if (!(node instanceof MappingNode)) {
			throw new GradleException("The YAML document is not a mapping");
		}

		_overlay(allowNewKeys, (MappingNode)node, overlay, "", yaml);

		StringWriter stringWriter = new StringWriter();

		yaml.serialize(node, stringWriter);

		return stringWriter.toString();
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
			throw new GradleException("The chart has no \"" + name + "\"");
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
		boolean allowNewKeys, MappingNode mappingNode,
		Map<String, Object> overlay, String path, Yaml yaml) {

		List<NodeTuple> nodeTuples = mappingNode.getValue();

		for (String key : new TreeMap<>(overlay).keySet()) {
			Object value = overlay.get(key);

			String keyPath = path + key;

			int index = -1;

			for (int i = 0; i < nodeTuples.size(); i++) {
				NodeTuple nodeTuple = nodeTuples.get(i);

				if ((nodeTuple.getKeyNode() instanceof ScalarNode scalarNode) &&
					key.equals(scalarNode.getValue())) {

					index = i;

					break;
				}
			}

			if (index < 0) {
				if (!allowNewKeys) {
					throw new GradleException(
						"The chart's \"values.yaml\" has no \"" + keyPath +
							"\", so the chart does not accept the values " +
								"this plugin writes into it");
				}

				nodeTuples.add(
					new NodeTuple(yaml.represent(key), yaml.represent(value)));

				continue;
			}

			NodeTuple nodeTuple = nodeTuples.get(index);

			Node valueNode = nodeTuple.getValueNode();

			if (path.isEmpty() && (value instanceof Map) &&
				(valueNode instanceof MappingNode valueMappingNode) &&
				!valueMappingNode.getValue(
				).isEmpty()) {

				_overlay(
					allowNewKeys, valueMappingNode, (Map<String, Object>)value,
					keyPath + ".", yaml);

				continue;
			}

			Node newValueNode = yaml.represent(value);

			newValueNode.setInLineComments(valueNode.getInLineComments());

			nodeTuples.set(
				index, new NodeTuple(nodeTuple.getKeyNode(), newValueNode));
		}
	}

	private HelmChartArchive(Map<String, Object> chartMetadata, byte[] data) {
		_chartMetadata = chartMetadata;
		_data = data;
	}

	private static final String _ANNOTATION_CHART_SOURCE =
		"cx.liferay.com/chart-source";

	private static final String _ANNOTATION_CHART_TEMPLATE =
		"cx.liferay.com/chart-template";

	private final Map<String, Object> _chartMetadata;
	private final byte[] _data;

}