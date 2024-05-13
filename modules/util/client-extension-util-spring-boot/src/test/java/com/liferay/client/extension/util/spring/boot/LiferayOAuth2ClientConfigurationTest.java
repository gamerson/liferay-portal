/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.client.extension.util.spring.boot;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.mockito.ArgumentMatchers;
import org.mockito.BDDMockito;
import org.mockito.Mockito;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ClientCredentialsOAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2ClientCredentialsGrantRequest;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Gregory Amerson
 */
@ContextConfiguration(
	classes = {
		LiferayOAuth2AccessTokenManager.class,
		LiferayOAuth2ClientConfiguration.class
	}
)
@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource("LiferayOAuth2ClientConfigurationTest.properties")
public class LiferayOAuth2ClientConfigurationTest {

	@Before
	public void setUp() {
		ClientCredentialsOAuth2AuthorizedClientProvider
			clientCredentialsOAuth2AuthorizedClientProvider =
				new ClientCredentialsOAuth2AuthorizedClientProvider();
		_oAuth2AccessTokenResponseClient = Mockito.mock(
			OAuth2AccessTokenResponseClient.class);
		clientCredentialsOAuth2AuthorizedClientProvider.
			setAccessTokenResponseClient(_oAuth2AccessTokenResponseClient);
		_authorizedClientServiceOAuth2AuthorizedClientManager.
			setAuthorizedClientProvider(
				clientCredentialsOAuth2AuthorizedClientProvider);
	}

	@Test
	public void testFirst() {
		OAuth2AccessTokenResponse accessTokenResponse =
			OAuth2AccessTokenResponse.withToken(
				"token"
			).tokenType(
				OAuth2AccessToken.TokenType.BEARER
			).build();

		BDDMockito.given(
			_oAuth2AccessTokenResponseClient.getTokenResponse(
				ArgumentMatchers.any())
		).willReturn(
			accessTokenResponse
		);

		String authorization =
			_liferayOAuth2AccessTokenManager.getAuthorization(
				"foo-bar-headless-server");

		Assert.assertNotNull(authorization);
	}

	@Autowired
	private AuthorizedClientServiceOAuth2AuthorizedClientManager
		_authorizedClientServiceOAuth2AuthorizedClientManager;

	@Autowired
	private Environment _environment;

	@Autowired
	private LiferayOAuth2AccessTokenManager _liferayOAuth2AccessTokenManager;

	private OAuth2AccessTokenResponseClient<OAuth2ClientCredentialsGrantRequest>
		_oAuth2AccessTokenResponseClient;

}