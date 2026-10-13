/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.gradle.plugins.workspace.internal.client.extension.oci;

import java.nio.charset.StandardCharsets;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.gradle.api.GradleException;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Gregory Amerson
 */
public class ChartValuesTest {

	@Test
	public void testBake() {

		// A batch client extension names its OAuth2 application

		Map<String, Object> workload = _getWorkload(
			"registry/cx/able@sha256:1",
			Map.of(
				"com.liferay.oauth2.provider.configuration." +
					"OAuth2ProviderApplicationHeadlessServerConfiguration~" +
						"able-batch/liferay.com",
				Map.of()),
			"FROM liferay/batch\nCOPY batch /opt/liferay/batch",
			Map.of("kind", "Job"),
			List.of(new ArtifactMount("/opt/liferay/batch", "batch")));

		Assert.assertTrue((Boolean)workload.get("enabled"));
		Assert.assertEquals(
			Map.of("LIFERAY_BATCH_OAUTH_APP_ERC", "able-batch"),
			workload.get("env"));

		// A configuration only client extension runs nothing and mounts no
		// artifact

		Map<String, Object> values = ChartValues.bake(
			"registry/cx/able@sha256:1", Map.of(),
			ClientExtensionDockerfile.parse("FROM liferay/noop"),
			Map.of("kind", "Job"), List.of(), "able");

		Map<String, Object> artifact = (Map<String, Object>)values.get(
			"artifact");

		Assert.assertEquals("", artifact.get("reference"));

		workload = (Map<String, Object>)values.get("workload");

		Assert.assertFalse((Boolean)workload.get("enabled"));

		// LCP.json env wins over the Dockerfile and a probe yields a startup
		// probe

		Map<String, Object> lcp = new LinkedHashMap<>();

		Map<String, Object> lcpEnv = new LinkedHashMap<>();

		lcpEnv.put("A", "lcp");
		lcpEnv.put("PORT", 58081);

		lcp.put("env", lcpEnv);

		Map<String, Object> readinessProbe = new LinkedHashMap<>();

		readinessProbe.put("httpGet", Map.of("path", "/ready"));
		readinessProbe.put("timeoutSeconds", 5);

		lcp.put("readinessProbe", readinessProbe);

		workload = _getWorkload(
			"registry/cx/able@sha256:1", Map.of(),
			"FROM liferay/jar-runner\nENV A=dockerfile B=dockerfile\n" +
				"COPY *.jar /opt/liferay/jar-runner.jar",
			lcp, List.of(new ArtifactMount("/opt/liferay", ".cx-mounts/0")));

		Map<String, Object> expectedEnv = new TreeMap<>();

		expectedEnv.put("A", "lcp");
		expectedEnv.put("B", "dockerfile");
		expectedEnv.put("PORT", "58081");

		Assert.assertEquals(expectedEnv, workload.get("env"));

		Map<String, Object> startupProbe = (Map<String, Object>)workload.get(
			"startupProbe");

		Assert.assertEquals(
			Map.of("path", "/ready"), startupProbe.get("httpGet"));
		Assert.assertNull(startupProbe.get("timeoutSeconds"));
	}

	@Test
	public void testMergeConfigs() {
		Map<String, ArtifactFile> artifactFiles = new HashMap<>();

		for (String name :
				new String[] {
					"a.client-extension-config.json",
					"b.client-extension-config.json"
				}) {

			artifactFiles.put(
				name,
				ArtifactFile.newFile(
					"{\"key~a\": {}}".getBytes(StandardCharsets.UTF_8), 0644,
					name));
		}

		try {
			ChartValues.mergeConfigs(artifactFiles);

			Assert.fail();
		}
		catch (GradleException gradleException) {
			String message = gradleException.getMessage();

			Assert.assertTrue(
				message, message.contains("both declare \"key~a\""));
		}
	}

	private Map<String, Object> _getWorkload(
		String artifactReference, Map<String, Object> configs,
		String dockerfile, Map<String, Object> lcp,
		List<ArtifactMount> mounts) {

		Map<String, Object> values = ChartValues.bake(
			artifactReference, configs,
			ClientExtensionDockerfile.parse(dockerfile), lcp, mounts, "able");

		return (Map<String, Object>)values.get("workload");
	}

}