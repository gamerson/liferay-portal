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

import org.gradle.api.GradleException;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Gregory Amerson
 */
public class HelmChartValuesTest {

	@Test
	public void testDerive() {
		Map<String, Object> lcp = new LinkedHashMap<>();

		lcp.put("env", Map.of("A", "lcp", "PORT", 58081));
		lcp.put("loadBalancer", Map.of("targetPort", 58081));
		lcp.put("scale", 2);

		Map<String, Object> values = _derive(
			Map.of(),
			"FROM liferay/jar-runner\nCOPY *.jar /opt/liferay/jar-runner.jar",
			lcp, List.of(new ArtifactMount("/opt/liferay", ".mounts/0")));

		Assert.assertEquals("able", values.get("fullnameOverride"));
		Assert.assertEquals(
			Map.of("repository", "liferay/jar-runner", "tag", "latest"),
			values.get("image"));
		Assert.assertEquals(Map.of("enabled", true), values.get("service"));

		Map<String, Object> workload = (Map<String, Object>)values.get(
			"workload");

		Assert.assertEquals(58081, workload.get("containerPort"));
		Assert.assertEquals(
			Map.of("A", "lcp", "PORT", "58081"), workload.get("env"));
		Assert.assertEquals("Deployment", workload.get("kind"));
		Assert.assertEquals(2, workload.get("replicas"));
	}

	@Test
	public void testDeriveWhenClientExtensionIsBatch() {
		Map<String, Object> values = _derive(
			Map.of(
				"com.liferay.oauth2.provider.configuration." +
					"OAuth2ProviderApplicationHeadlessServerConfiguration~" +
						"able-batch/liferay.com",
				Map.of()),
			"FROM liferay/batch\nCOPY batch /opt/liferay/batch",
			Map.of("kind", "Job"),
			List.of(new ArtifactMount("/opt/liferay/batch", "batch")));

		Map<String, Object> workload = (Map<String, Object>)values.get(
			"workload");

		Assert.assertTrue((Boolean)workload.get("enabled"));
		Assert.assertEquals(
			Map.of("LIFERAY_BATCH_OAUTH_APP_ERC", "able-batch"),
			workload.get("env"));
	}

	@Test
	public void testDeriveWhenClientExtensionIsConfigurationOnly() {
		Map<String, Object> values = _derive(
			Map.of(), "FROM liferay/noop", Map.of("kind", "Job"), List.of());

		Map<String, Object> artifact = (Map<String, Object>)values.get(
			"artifact");

		Assert.assertEquals("", artifact.get("reference"));

		Assert.assertEquals(Map.of("enabled", false), values.get("service"));

		Map<String, Object> workload = (Map<String, Object>)values.get(
			"workload");

		Assert.assertFalse((Boolean)workload.get("enabled"));
	}

	@Test
	public void testDeriveWhenLCPJsonHasReadinessProbe() {
		Map<String, Object> readinessProbe = new LinkedHashMap<>();

		readinessProbe.put("httpGet", Map.of("path", "/ready"));
		readinessProbe.put("timeoutSeconds", 5);

		Map<String, Object> values = _derive(
			Map.of(),
			"FROM liferay/jar-runner\nCOPY *.jar /opt/liferay/jar-runner.jar",
			Map.of("readinessProbe", readinessProbe),
			List.of(new ArtifactMount("/opt/liferay", ".mounts/0")));

		Map<String, Object> workload = (Map<String, Object>)values.get(
			"workload");

		Assert.assertEquals(readinessProbe, workload.get("readinessProbe"));
		Assert.assertEquals(
			Map.of(
				"failureThreshold", 60, "httpGet", Map.of("path", "/ready"),
				"periodSeconds", 5),
			workload.get("startupProbe"));
	}

	@Test
	public void testMergeConfigs() {
		Map<String, Object> configs = HelmChartValues.mergeConfigs(
			_getArtifactFiles(
				"a.client-extension-config.json", "{\"key~a\": {}}",
				"b.client-extension-config.json", "{\"key~b\": {}}"));

		Assert.assertEquals(
			Map.of("key~a", Map.of(), "key~b", Map.of()), configs);
	}

	@Test
	public void testMergeConfigsWhenKeyIsDeclaredTwice() {
		try {
			HelmChartValues.mergeConfigs(
				_getArtifactFiles(
					"a.client-extension-config.json", "{\"key~a\": {}}",
					"b.client-extension-config.json", "{\"key~a\": {}}"));

			Assert.fail();
		}
		catch (GradleException gradleException) {
			String message = gradleException.getMessage();

			Assert.assertTrue(
				message, message.contains("both declare \"key~a\""));
		}
	}

	private Map<String, Object> _derive(
		Map<String, Object> configs, String dockerfile, Map<String, Object> lcp,
		List<ArtifactMount> mounts) {

		return HelmChartValues.derive(
			"registry/cx/able@sha256:1", configs,
			ClientExtensionDockerfile.parse(dockerfile), lcp, mounts, "able");
	}

	private Map<String, ArtifactFile> _getArtifactFiles(String... strings) {
		Map<String, ArtifactFile> artifactFiles = new HashMap<>();

		for (int i = 0; i < strings.length; i += 2) {
			artifactFiles.put(
				strings[i],
				ArtifactFile.newFile(
					strings[i + 1].getBytes(StandardCharsets.UTF_8), 0644,
					strings[i]));
		}

		return artifactFiles;
	}

}