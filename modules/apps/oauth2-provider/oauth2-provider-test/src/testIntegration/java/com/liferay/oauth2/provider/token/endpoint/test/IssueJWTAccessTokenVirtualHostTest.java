/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.oauth2.provider.token.endpoint.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.oauth2.provider.internal.test.JWTAssertionAuthorizationGrant;
import com.liferay.oauth2.provider.internal.test.JWTAssertionClientAuthentication;
import com.liferay.oauth2.provider.internal.test.PasswordAuthorizationGrant;
import com.liferay.oauth2.provider.internal.test.util.JWTAssertionUtil;
import com.liferay.portal.configuration.test.util.ConfigurationTemporarySwapper;
import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.service.CompanyLocalServiceUtil;
import com.liferay.portal.kernel.test.util.UserTestUtil;
import com.liferay.portal.kernel.util.HashMapDictionaryBuilder;
import com.liferay.portal.kernel.util.PortalUtil;
import org.apache.cxf.rs.security.jose.jwk.JwkUtils;
import org.apache.cxf.rs.security.jose.jws.JwsHeaders;
import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactConsumer;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureVerifier;
import org.apache.cxf.rs.security.jose.jws.JwsUtils;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.apache.cxf.rs.security.jose.jwt.JwtToken;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.BundleActivator;

import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import java.net.URI;
import java.util.function.Function;

/**
 * @author Arthur Chan
 */
@RunWith(Arquillian.class)
public class IssueJWTAccessTokenVirtualHostTest extends BaseTokenEndpointTestCase {

	public IssueJWTAccessTokenVirtualHostTest() {
		super(_getInvocationBuilder());
	}

	private static Invocation.Builder _getInvocationBuilder() {
		return getInvocationBuilder(
			null, _getTokenWebTarget(), Function.identity());
	}

	protected static WebTarget _getTokenWebTarget() {
		WebTarget webTarget = _getOAuth2WebTarget();

		return webTarget.path("token");
	}

	private static final URI _baseURI = URI.create("http://test.localtest.me:8080");

	protected static WebTarget _getOAuth2WebTarget() {
		WebTarget webTarget = getWebTarget(_baseURI);

		webTarget = webTarget.path("o");
		webTarget = webTarget.path("oauth2");

		return webTarget;
	}

	@Test
	public void testJWTClaimsVirtualHostIssuer() throws Exception {
		Company company =
			CompanyLocalServiceUtil.fetchCompanyByVirtualHost(
				"test.localtest.me");

		User user = UserTestUtil.getAdminUser(company.getCompanyId());

		JWTAssertionAuthorizationGrant jwtAssertionAuthorizationGrant =
			new JWTAssertionAuthorizationGrant(
				"test.localtest.me", null, user.getUuid(), _getTokenWebTarget());

			String accessToken = getAccessToken(
				jwtAssertionAuthorizationGrant,
				clientAuthentications.get("test.localtest.me"));

			JwsJwtCompactConsumer jwsJwtCompactConsumer =
				new JwsJwtCompactConsumer(
					accessToken);

			JwtToken jwtToken = jwsJwtCompactConsumer.getJwtToken();

			JwtClaims jwtClaims =
				jwtToken.getClaims();

			Assert.assertEquals("test.localtest.me", jwtClaims.getIssuer());
	}


		@Override
	protected BundleActivator getBundleActivator() {
		return new IssueJWTAccessTokenVirtualHostTestPreparatorBundleActivator();
	}

	private class IssueJWTAccessTokenVirtualHostTestPreparatorBundleActivator
		extends TestPreparatorBundleActivator {

		@Override
		protected void prepareTest() throws Exception {
			autoCloseables.add(
				new ConfigurationTemporarySwapper(
					"com.liferay.oauth2.provider.rest.internal.configuration." +
						"OAuth2AuthorizationServerConfiguration",
					HashMapDictionaryBuilder.<String, Object>put(
						"oauth2.authorization.server.issue.jwt.access.token",
						true
					).put(
						"oauth2.authorization.server.jwt.access.token." +
							"signing.json.web.key",
						JWTAssertionUtil.JWK
					).build()));


			createCompany("test.localtest.me", "test.localtest.me");

			clientAuthentications.put(
				"test.localtest.me",
				new JWTAssertionClientAuthentication(
					_getTokenWebTarget(), "test.localtest.me", false,
					"test.localtest.me", JWTAssertionUtil.JWKS, false));

			super.prepareTest();
		}

	}

}