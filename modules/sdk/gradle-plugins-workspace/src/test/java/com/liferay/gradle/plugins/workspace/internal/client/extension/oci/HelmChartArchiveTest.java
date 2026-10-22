/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.gradle.plugins.workspace.internal.client.extension.oci;

import java.io.ByteArrayInputStream;
import java.io.File;

import java.nio.file.Files;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.gradle.api.GradleException;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

/**
 * @author Gregory Amerson
 */
public class HelmChartArchiveTest {

	@Test
	public void testCreate() throws Exception {
		File rootDir = new File("../../..");

		HelmChartSource helmChartSource = HelmChartSource.resolveReference(
			"cloud/helm/client-extension", rootDir);

		Map<String, Object> lcp = new LinkedHashMap<>();

		lcp.put("kind", "CronJob");
		lcp.put("livenessProbe", Map.of("httpGet", Map.of("path", "/live")));
		lcp.put("readinessProbe", Map.of("httpGet", Map.of("path", "/ready")));
		lcp.put("schedule", "* * * * *");

		Map<String, Object> values = HelmChartValues.derive(
			"registry/cx/able@sha256:1", Map.of("key~a", Map.of("b", "c")),
			ClientExtensionDockerfile.parse("FROM liferay/jar-runner"), lcp,
			List.of(new ArtifactMount("/opt/liferay", ".mounts/0")), "able");

		HelmChartArchive helmChartArchive = HelmChartArchive.create(
			"able", helmChartSource, values, "1.0.0");

		Assert.assertArrayEquals(
			helmChartArchive.getData(),
			HelmChartArchive.create(
				"able", helmChartSource, values, "1.0.0"
			).getData());

		Map<String, Object> chartMetadata = helmChartArchive.getChartMetadata();

		Assert.assertEquals("1.0.0", chartMetadata.get("appVersion"));
		Assert.assertEquals("able", chartMetadata.get("name"));
		Assert.assertEquals("1.0.0", chartMetadata.get("version"));

		Map<String, Object> annotations =
			(Map<String, Object>)chartMetadata.get("annotations");

		Assert.assertEquals(
			helmChartSource.getOrigin(),
			annotations.get("cx.liferay.com/chart-source"));
		Assert.assertEquals(
			helmChartSource.getTemplate(),
			annotations.get("cx.liferay.com/chart-template"));
		Assert.assertEquals(
			"1", annotations.get(HelmChartSource.ANNOTATION_VALUES_CONTRACT));

		File file = temporaryFolder.newFile("able-1.0.0.tgz");

		Files.write(file.toPath(), helmChartArchive.getData());

		HelmChartSource archiveHelmChartSource =
			HelmChartSource.resolveReference(file.getAbsolutePath(), rootDir);

		Map<String, byte[]> chartFiles = archiveHelmChartSource.getChartFiles();

		Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));

		Map<String, Object> archiveValues = yaml.load(
			new ByteArrayInputStream(chartFiles.get("values.yaml")));

		Map<String, Object> service = (Map<String, Object>)archiveValues.get(
			"service");

		Assert.assertEquals(80, service.get("port"));

		Map<String, Object> workload = (Map<String, Object>)archiveValues.get(
			"workload");

		Assert.assertEquals("CronJob", workload.get("kind"));
		Assert.assertEquals("* * * * *", workload.get("schedule"));
	}

	@Test
	public void testOverlayYAML() {
		Map<String, Object> workload = new LinkedHashMap<>();

		workload.put("kind", "Job");
		workload.put("schedule", "* * * * *");

		String yaml = HelmChartArchive.overlayYAML(
			true, _DOCUMENT, Map.of("workload", workload));

		for (String expected :
				new String[] {
					"# The workload.", "# Immutable.", "kind: Job",
					"replicas: 1", "schedule: '* * * * *'"
				}) {

			Assert.assertTrue(yaml, yaml.contains(expected));
		}
	}

	@Test
	public void testOverlayYAMLWhenKeyIsMissing() {
		try {
			HelmChartArchive.overlayYAML(
				false, _DOCUMENT,
				Map.of("workload", Map.of("schedule", "* * * * *")));

			Assert.fail();
		}
		catch (GradleException gradleException) {
			String message = gradleException.getMessage();

			Assert.assertTrue(
				message, message.contains("has no \"workload.schedule\""));
		}
	}

	@Rule
	public final TemporaryFolder temporaryFolder = new TemporaryFolder();

	private static final String _DOCUMENT =
		"# The workload.\nworkload:\n    # Immutable.\n    kind: " +
			"Deployment\n    replicas: 1\n";

}