/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.gradle.plugins.workspace.internal.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;

import com.liferay.gradle.plugins.workspace.internal.client.extension.ClientExtension;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Gregory Amerson
 */
public class ClientExtensionTest {

	@Test
	public void testClientExtensionHeadlessServer() throws Exception {
		Assert.assertEquals(
			"-tt-tes-ttt", StringUtil.getDockerSafeName("-TTTesTTT"));
	}

	@Test
	public void testClientExtensionOauthHeadlessServerToJSON()
		throws Exception {

		ClientExtension clientExtension = new ClientExtension();

		clientExtension.description = "description-test";
		clientExtension.id = "id-test";
		clientExtension.name = "name-test";
		clientExtension.projectName = "project-test";
		clientExtension.sourceCodeURL = "https://liferay.com";
		clientExtension.type = "oauthApplicationHeadlessServer";

		clientExtension.ignored(
			"userAccountEmailAddress",
			"test@$[conf:dxp.lxc.liferay.com.virtualInstanceId]");
		clientExtension.ignored(
			"scopes", Arrays.asList("C_Coupon.read", "C_Coupon.write"));

		Assert.assertEquals(
			StringUtil.read(
				ClientExtensionTest.class.getResourceAsStream(
					"project-test.client-extension-config.json")),
			_toJSON(clientExtension));
	}

	@Test
	public void testClientExtensionOauthUserAgentToJSON() throws Exception {
		ClientExtension clientExtension = new ClientExtension();

		clientExtension.description = "description-test";
		clientExtension.id = "id-test";
		clientExtension.name = "name-test";
		clientExtension.projectName = "project-test";
		clientExtension.sourceCodeURL = "https://liferay.com";
		clientExtension.type = "oauthApplicationUserAgent";

		clientExtension.ignored(
			"scopes", Arrays.asList("C_Coupon.read", "C_Coupon.write"));

		Assert.assertEquals(
			StringUtil.read(
				ClientExtensionTest.class.getResourceAsStream(
					"oauthApplicationUserAgent.client-extension-config.json")),
			_toJSON(clientExtension));
	}

	@Test
	public void testClientExtensionObjectActionToJSON() throws Exception {
		ClientExtension clientExtension = new ClientExtension();

		clientExtension.description = "description-test";
		clientExtension.id = "id-test";
		clientExtension.name = "name-test";
		clientExtension.projectName = "project-test";
		clientExtension.sourceCodeURL = "https://liferay.com";
		clientExtension.type = "objectAction";

		clientExtension.ignored("resourcePath", "/coupons/issued");
		clientExtension.ignored("oauthApplication", "coupon-action-user-agent");

		Assert.assertEquals(
			StringUtil.read(
				ClientExtensionTest.class.getResourceAsStream(
					"objectAction.client-extension-config.json")),
			_toJSON(clientExtension));
	}

	private String _toJSON(ClientExtension clientExtension)
		throws Exception, JsonProcessingException {

		ObjectMapper objectMapper = new ObjectMapper();

		objectMapper.configure(
			SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

		ObjectWriter objectWriter =
			objectMapper.writerWithDefaultPrettyPrinter();

		return objectWriter.writeValueAsString(clientExtension.toJSONMap());
	}

}