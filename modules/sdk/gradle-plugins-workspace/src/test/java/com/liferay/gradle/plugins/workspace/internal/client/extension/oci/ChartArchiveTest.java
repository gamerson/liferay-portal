/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.gradle.plugins.workspace.internal.client.extension.oci;

import java.io.IOException;

import java.nio.file.Paths;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Gregory Amerson
 */
public class ChartArchiveTest {

	@Test
	public void testCreate() throws IOException {
		Map<String, byte[]> chartFiles = ChartArchive.readChartFiles(
			Paths.get("../../../cloud/helm/client-extension"));

		Map<String, Object> values = Map.of(
			"clientExtension", Map.of("serviceId", "able"));

		ChartArchive chartArchive = ChartArchive.create(
			chartFiles, "able", values, "1.0.0");

		ChartArchive secondChartArchive = ChartArchive.create(
			chartFiles, "able", values, "1.0.0");

		Assert.assertArrayEquals(
			chartArchive.getData(), secondChartArchive.getData());

		Map<String, Object> chartMetadata = chartArchive.getChartMetadata();

		Assert.assertEquals("able", chartMetadata.get("name"));
		Assert.assertEquals("1.0.0", chartMetadata.get("version"));
	}

	@Test
	public void testOverlayYAML() {
		Map<String, Object> workload = new LinkedHashMap<>();

		workload.put("kind", "Job");
		workload.put("schedule", "* * * * *");

		String yaml = ChartArchive.overlayYAML(
			"# The workload.\nworkload:\n    # Immutable.\n    kind: " +
				"Deployment\n    replicas: 1\n",
			Map.of("workload", workload));

		for (String expected :
				new String[] {
					"# The workload.", "# Immutable.", "kind: Job",
					"replicas: 1", "schedule: '* * * * *'"
				}) {

			Assert.assertTrue(yaml, yaml.contains(expected));
		}
	}

}