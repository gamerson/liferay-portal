/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.gradle.plugins.workspace.internal.client.extension.oci;

import java.util.HashMap;
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
	public void testParse() {

		// A base image with no tag gets latest, and a COPY to a file keeps
		// its glob

		ClientExtensionDockerfile clientExtensionDockerfile =
			ClientExtensionDockerfile.parse(
				"# comment\nFROM liferay/jar-runner\n" +
					"COPY *.jar /opt/liferay/jar-runner.jar");

		Assert.assertEquals(
			"liferay/jar-runner:latest", clientExtensionDockerfile.getImage());

		List<ClientExtensionDockerfile.Copy> copies =
			clientExtensionDockerfile.getCopies();

		ClientExtensionDockerfile.Copy copy = copies.get(0);

		Assert.assertEquals(
			"/opt/liferay/jar-runner.jar", copy.getDestination());
		Assert.assertEquals(List.of("*.jar"), copy.getSources());

		// A digest and a registry port are not tags

		clientExtensionDockerfile = ClientExtensionDockerfile.parse(
			"FROM liferay/caddy@sha256:abc");

		Assert.assertEquals(
			"liferay/caddy@sha256:abc", clientExtensionDockerfile.getImage());

		clientExtensionDockerfile = ClientExtensionDockerfile.parse(
			"FROM registry.example.com:5000/liferay/caddy");

		Assert.assertEquals(
			"registry.example.com:5000/liferay/caddy:latest",
			clientExtensionDockerfile.getImage());

		// Both ENV forms, the JSON COPY form and a continued RUN

		clientExtensionDockerfile = ClientExtensionDockerfile.parse(
			"FROM liferay/node-runner:1\nENV A=1 B=\"two words\"\n" +
				"ENV C three\nCOPY [\"./\", \"/opt/liferay/\"]\n" +
					"RUN npm install \\\n\t--omit=dev");

		Map<String, String> env = new HashMap<>();

		env.put("A", "1");
		env.put("B", "two words");
		env.put("C", "three");

		Assert.assertEquals(env, clientExtensionDockerfile.getEnv());

		Assert.assertEquals(
			List.of("npm install  --omit=dev"),
			clientExtensionDockerfile.getRuns());
		Assert.assertTrue(clientExtensionDockerfile.isReplayable());

		// An instruction a mount cannot apply is a warning

		clientExtensionDockerfile = ClientExtensionDockerfile.parse(
			"FROM liferay/caddy\nWORKDIR /srv");

		List<String> warnings = clientExtensionDockerfile.getWarnings();

		Assert.assertEquals(warnings.toString(), 1, warnings.size());

		// A second FROM and no FROM are refused

		for (String text :
				List.of(
					"FROM liferay/node-runner AS build\nFROM liferay/caddy",
					"COPY . /srv")) {

			try {
				ClientExtensionDockerfile.parse(text);

				Assert.fail(text);
			}
			catch (GradleException gradleException) {
			}
		}
	}

}