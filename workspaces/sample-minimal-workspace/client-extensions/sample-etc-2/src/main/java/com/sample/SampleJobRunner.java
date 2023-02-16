package com.sample;

import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.liferay.headless.delivery.client.dto.v1_0.MessageBoardSection;
import com.liferay.headless.delivery.client.pagination.Page;
import com.liferay.headless.delivery.client.pagination.Pagination;
import com.liferay.headless.delivery.client.resource.v1_0.MessageBoardSectionResource;

@Component
public class SampleJobRunner implements CommandLineRunner {

	public void run(String... args) throws Exception {
		System.out.println("_webClient" + _webClient);

		OAuth2AuthorizeRequest authorizeRequest =
			OAuth2AuthorizeRequest.withClientRegistrationId(
				"dxp"
			).principal(
				"Sample"
			).build();

		OAuth2AuthorizedClient authorizedClient =
			this.authorizedClientServiceAndManager.authorize(authorizeRequest);

		// Get the token from the authorized client object

		OAuth2AccessToken accessToken = Objects.requireNonNull(
			authorizedClient
		).getAccessToken();

		System.out.println(
			"Issued: " +
				accessToken.getIssuedAt(
				).toString() + ", Expires:" +
					accessToken.getExpiresAt(
					).toString());
		System.out.println(
			"Scopes: " +
				accessToken.getScopes(
				).toString());
		System.out.println("Token: " + accessToken.getTokenValue());

		 MessageBoardSectionResource.Builder builder =
		 MessageBoardSectionResource.builder();

		 MessageBoardSectionResource messageBoardSectionResource =
		 	builder.header("Authorization", "Bearer " + accessToken.getTokenValue()).build();

		 Page<MessageBoardSection> page =
		 	messageBoardSectionResource.getSiteMessageBoardSectionsPage(
		 		Long.valueOf("20121"), null, null, null,
		 		null, Pagination.of(1, 2), null);

        System.out.println(page.fetchFirstItem().getDescription());
	}

	@Value(
		"${sample-oauth-application-headless-server.oauth2.headless.server.client.id}"
	)
	private String _clientId;

	@Value("${sample-oauth-application-headless-server.oauth2.token.uri}")
	private String _tokenUri;

	@Autowired
	private WebClient _webClient;

	@Value(
		"${sample-oauth-application-headless-server.oauth2.headless.server.scopes}"
	)
	String _scope;

	@Autowired
	private AuthorizedClientServiceOAuth2AuthorizedClientManager
		authorizedClientServiceAndManager;

	@Value(
		"${sample-oauth-application-headless-server.oauth2.headless.server.client.secret}"
	)
	private String clientSecret;

}