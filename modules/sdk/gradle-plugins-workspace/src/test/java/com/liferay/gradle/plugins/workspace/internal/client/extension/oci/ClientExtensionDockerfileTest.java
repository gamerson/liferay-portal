/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.gradle.plugins.workspace.internal.client.extension.oci;

import java.io.InputStream;

import java.nio.charset.StandardCharsets;

import java.util.List;

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
	public void testParseDefaultTemplates() throws Exception {
		for (String grouping :
				List.of(
					"batch", "configuration", "frontend", "microservice-java",
					"microservice-node")) {

			ClientExtensionDockerfile.parse(_readTemplate(grouping));
		}
	}

	@Test
	public void testUnsupportedImagesAndInstructionsFail() {
		for (String text :
				List.of(
					"COPY . /srv", "FROM eclipse-temurin:21",
					"FROM liferay/caddy\nENV A=1",
					"FROM liferay/caddy\nFROM liferay/batch",
					"FROM liferay/jar-runner\nRUN apt-get install curl",
					"FROM liferay/node-runner\nCOPY src /opt/liferay\nRUN " +
						"npm install",
					"FROM liferay/node-runner\nCOPY . /opt/liferay\nCOPY " +
						"app.js /opt/app.js\nRUN npm install")) {

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