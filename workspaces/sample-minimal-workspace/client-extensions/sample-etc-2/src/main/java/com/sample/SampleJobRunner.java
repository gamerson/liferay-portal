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

package com.sample;

import com.liferay.headless.admin.user.client.dto.v1_0.Site;
import com.liferay.headless.admin.user.client.resource.v1_0.SiteResource;
import com.liferay.headless.delivery.client.dto.v1_0.MessageBoardThread;
import com.liferay.headless.delivery.client.pagination.Page;
import com.liferay.headless.delivery.client.pagination.Pagination;
import com.liferay.headless.delivery.client.resource.v1_0.MessageBoardThreadResource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * @author Gregory Amerson
 */
@Component
public class SampleJobRunner implements CommandLineRunner {

	public void run(String... args) throws Exception {
		System.out.println("_webClient" + _webClient);

		OAuth2AuthorizeRequest oAuth2AuthorizeRequest =
			OAuth2AuthorizeRequest.withClientRegistrationId(
				"dxp"
			).principal(
				"Sample"
			).build();

		OAuth2AuthorizedClient oAuth2AuthorizedClient =
			_authorizedClientServiceOAuth2AuthorizedClientManager.authorize(
				oAuth2AuthorizeRequest);

		// Get the token from the authorized client object

		OAuth2AccessToken oAuth2AccessToken =
			oAuth2AuthorizedClient.getAccessToken();

		System.out.println(
			"Issued: " + oAuth2AccessToken.getIssuedAt() + ", Expires:" +
				oAuth2AccessToken.getExpiresAt());
		System.out.println("Scopes: " + oAuth2AccessToken.getScopes());
		System.out.println("Token: " + oAuth2AccessToken.getTokenValue());

		SiteResource.Builder siteResourceBuilder = SiteResource.builder();

		SiteResource siteResource = siteResourceBuilder.header(
			"Authorization", "Bearer " + oAuth2AccessToken.getTokenValue()
		).endpoint(
			_mainDomain, 443, "https"
		).build();

		Site site = siteResource.getSiteByFriendlyUrlPath("guest");

		Long siteId = site.getId();

		MessageBoardThreadResource.Builder builder =
			MessageBoardThreadResource.builder();

		MessageBoardThreadResource messageBoardThreadResource = builder.header(
			"Authorization", "Bearer " + oAuth2AccessToken.getTokenValue()
		).endpoint(
			_mainDomain, 443, "https"
		).build();

		Page<MessageBoardThread> page =
			messageBoardThreadResource.getSiteMessageBoardThreadsPage(
				siteId, null, null, null, null, Pagination.of(1, 2), null);

		System.out.println("page: " + page);
	}

	@Autowired
	private AuthorizedClientServiceOAuth2AuthorizedClientManager
		_authorizedClientServiceOAuth2AuthorizedClientManager;

	@Value(
		"${sample-oauth-application-headless-server.oauth2.headless.server.client.id}"
	)
	private String _clientId;

	@Value(
		"${sample-oauth-application-headless-server.oauth2.headless.server.client.secret}"
	)
	private String _clientSecret;

	@Value("${com.liferay.lxc.dxp.mainDomain}")
	private String _mainDomain;

	@Value(
		"${sample-oauth-application-headless-server.oauth2.headless.server.scopes}"
	)
	private String _serverScope;

	@Value("${sample-oauth-application-headless-server.oauth2.token.uri}")
	private String _tokenUri;

	@Autowired
	private WebClient _webClient;

}