package com.sample;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.InMemoryReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class OAuth2ClientConfiguration {

	@Bean
	public OAuth2AuthorizedClientService auth2AuthorizedClientService(
		ClientRegistrationRepository clientRegistrationRepository) {

		return new InMemoryOAuth2AuthorizedClientService(
			clientRegistrationRepository);
	}

	@Bean
	public AuthorizedClientServiceOAuth2AuthorizedClientManager
		authorizedClientServiceAndManager(
			ClientRegistrationRepository crr,
			OAuth2AuthorizedClientService oacs) {

		OAuth2AuthorizedClientProvider authorizedClientProvider =
			OAuth2AuthorizedClientProviderBuilder.builder(
			).clientCredentials(
			).build();

		AuthorizedClientServiceOAuth2AuthorizedClientManager
			authorizedClientManager =
				new AuthorizedClientServiceOAuth2AuthorizedClientManager(
					crr, oacs);
		authorizedClientManager.setAuthorizedClientProvider(
			authorizedClientProvider);

		return authorizedClientManager;
	}

	@Bean
	public ClientRegistrationRepository clientRegistrationRepository(
		ClientRegistration clientRegistration) {

		return new InMemoryClientRegistrationRepository(clientRegistration);
	}

	@Bean
	public ClientRegistrationRepository clientRegistrationRepository(
		@Value("${sample-oauth-application-headless-server.oauth2.token.uri}")
			String tokenUri,
		@Value(
			"${sample-oauth-application-headless-server.oauth2.headless.server.client.id}"
		)
		String clientId,
		@Value(
			"${sample-oauth-application-headless-server.oauth2.headless.server.client.secret}"
		)
		String clientSecret,
		@Value(
			"${sample-oauth-application-headless-server.oauth2.headless.server.scopes}"
		)
		String scope) {

		ClientRegistration registration = ClientRegistration.withRegistrationId(
			"dxp"
		).tokenUri(
			_protocol + "://" + _mainDomain + tokenUri
		).clientId(
			clientId
		).clientSecret(
			clientSecret
		).scope(
			scope
		).authorizationGrantType(
			AuthorizationGrantType.CLIENT_CREDENTIALS
		).clientAuthenticationMethod(
			ClientAuthenticationMethod.CLIENT_SECRET_POST
		).build();

		return new InMemoryClientRegistrationRepository(registration);
	}

	@Bean
	public ReactiveClientRegistrationRepository clientRegistrations(
		@Value("${sample-oauth-application-headless-server.oauth2.token.uri}")
			String tokenUri,
		@Value(
			"${sample-oauth-application-headless-server.oauth2.headless.server.client.id}"
		)
		String clientId,
		@Value(
			"${sample-oauth-application-headless-server.oauth2.headless.server.client.secret}"
		)
		String clientSecret,
		@Value(
			"${sample-oauth-application-headless-server.oauth2.headless.server.scopes}"
		)
		String scope) {

		ClientRegistration registration = ClientRegistration.withRegistrationId(
			"dxp"
		).tokenUri(
			_protocol + "://" + _mainDomain + tokenUri
		).clientId(
			clientId
		).clientSecret(
			clientSecret
		).scope(
			scope
		).authorizationGrantType(
			AuthorizationGrantType.CLIENT_CREDENTIALS
		).clientAuthenticationMethod(
			ClientAuthenticationMethod.CLIENT_SECRET_POST
		).build();

		return new InMemoryReactiveClientRegistrationRepository(registration);
	}

	@Bean
	public WebClient webClient(
		ReactiveClientRegistrationRepository clientRegistrations) {

		ServerOAuth2AuthorizedClientExchangeFilterFunction oauth =
			new ServerOAuth2AuthorizedClientExchangeFilterFunction(
				new AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(
					clientRegistrations,
					new InMemoryReactiveOAuth2AuthorizedClientService(
						clientRegistrations)));

		oauth.setDefaultClientRegistrationId("dxp");

		return WebClient.builder(
		).filter(
			oauth
		).build();
	}

	@Value("${com.liferay.lxc.dxp.mainDomain}")
	private String _mainDomain;

	@Value("${com.liferay.lxc.dxp.server.protocol}")
	private String _protocol;

}