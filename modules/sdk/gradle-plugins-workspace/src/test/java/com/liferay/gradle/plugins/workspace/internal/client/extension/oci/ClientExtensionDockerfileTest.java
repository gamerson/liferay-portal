/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.gradle.plugins.workspace.internal.client.extension.oci;

import java.io.InputStream;

import java.nio.charset.StandardCharsets;

import java.util.List;
import java.util.Map;

import org.gradle.api.GradleException;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Gregory Amerson
 */
public class ClientExtensionDockerfileTest {

	@Test
	public void testGetInstructions() {
		Assert.assertEquals(
			List.of("FROM liferay/caddy:latest", "COPY static/ /public_html/"),
			ClientExtensionDockerfile.getInstructions(
				"# comment\n\nFROM   liferay/caddy:latest\n  COPY " +
					"static/\t/public_html/  \n"));
	}

	@Test
	public void testJarTemplateCopiesOneFileToJarRunner() throws Exception {
		ClientExtensionDockerfile clientExtensionDockerfile =
			ClientExtensionDockerfile.parse(_readTemplate("microservice-java"));

		List<ClientExtensionDockerfile.Copy> copies =
			clientExtensionDockerfile.getCopies();

		ClientExtensionDockerfile.Copy copy = copies.get(0);

		Assert.assertEquals(
			"/opt/liferay/jar-runner.jar", copy.getDestination());
		Assert.assertEquals(List.of("*.jar"), copy.getSources());
	}

	@Test
	public void testNodeTemplateNpmInstallIsReplayable() throws Exception {
		ClientExtensionDockerfile clientExtensionDockerfile =
			ClientExtensionDockerfile.parse(_readTemplate("microservice-node"));

		Assert.assertEquals(
			List.of("npm install"), clientExtensionDockerfile.getRuns());
		Assert.assertTrue(clientExtensionDockerfile.isReplayable());
	}

	@Test
	public void testParseDefaultTemplates() throws Exception {
		Map<String, String> images = Map.of(
			"batch", "liferay/batch:latest", "configuration",
			"liferay/noop:latest", "frontend", "liferay/caddy:latest",
			"microservice-java", "liferay/jar-runner:latest",
			"microservice-node", "liferay/node-runner:latest");

		for (Map.Entry<String, String> entry : images.entrySet()) {
			ClientExtensionDockerfile clientExtensionDockerfile =
				ClientExtensionDockerfile.parse(_readTemplate(entry.getKey()));

			Assert.assertEquals(
				entry.getKey(), entry.getValue(),
				clientExtensionDockerfile.getImage());
		}
	}

	@Test
	public void testUnsupportedImagesAndInstructionsFail() {
		for (String text :
				List.of(
					"COPY . /srv", "FROM eclipse-temurin:21",
					"FROM liferay/caddy\nENV A=1",
					"FROM liferay/caddy\nFROM liferay/batch",
					"FROM liferay/jar-runner\nRUN apt-get install curl")) {

			try {
				ClientExtensionDockerfile.parse(text);

				Assert.fail(text);
			}
			catch (GradleException gradleException) {
			}
		}
	}

	private String _readTemplate(String grouping) throws Exception {
		try (InputStream inputStream =
				ClientExtensionDockerfile.class.getResourceAsStream(
					"/com/liferay/gradle/plugins/workspace/task/dependencies" +
						"/templates/" + grouping + "/Dockerfile.tpl")) {

			Assert.assertNotNull(grouping, inputStream);

			String template = new String(
				inputStream.readAllBytes(), StandardCharsets.UTF_8);

			return template.replace("__BATCH_TYPE__", "batch");
		}
	}

}