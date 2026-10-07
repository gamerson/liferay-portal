/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.k8s.agent.internal.environment;

import com.liferay.portal.environment.Entitlements;
import com.liferay.portal.environment.LicenseState;
import com.liferay.portal.environment.LiferayEnvironmentStatus;
import com.liferay.portal.environment.util.KeyPairUtil;
import com.liferay.portal.kernel.test.ReflectionTestUtil;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.test.rule.LiferayUnitTestRule;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.utils.Serialization;

import java.io.ByteArrayInputStream;
import java.io.File;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import java.security.KeyPair;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author Gregory Amerson
 */
public class KubernetesLiferayEnvironmentTest {

	@ClassRule
	@Rule
	public static final LiferayUnitTestRule liferayUnitTestRule =
		LiferayUnitTestRule.INSTANCE;

	@Before
	public void setUp() throws Exception {
		_mockWebServer.setDispatcher(new APIServerDispatcher());

		_mockWebServer.start();

		_addOnsFolder = temporaryFolder.newFolder("marketplace");

		_kubernetesLiferayEnvironment = _createKubernetesLiferayEnvironment();
	}

	@After
	public void tearDown() throws Exception {
		_mockWebServer.shutdown();
	}

	@Test
	public void testGetEnvironmentId() {
		Assert.assertEquals(
			_NAMESPACE_UID, _kubernetesLiferayEnvironment.getEnvironmentId());
	}

	@Test
	public void testGetEnvironmentNameWhenSpecEnvironmentNameIsUnset() {
		Assert.assertEquals(
			_NAMESPACE, _kubernetesLiferayEnvironment.getEnvironmentName());
	}

	@Test
	public void testGetKeyPairCreatesTheIdentitySecretOnce() throws Exception {
		KeyPair keyPair = _kubernetesLiferayEnvironment.getKeyPair();

		KubernetesLiferayEnvironment kubernetesLiferayEnvironment =
			_createKubernetesLiferayEnvironment();

		_assertEquals(keyPair, kubernetesLiferayEnvironment.getKeyPair());

		Assert.assertEquals(1, _secretCreateCount);

		Map<String, Object> metadata = (Map<String, Object>)_secrets.get(
			"dev-identity"
		).get(
			"metadata"
		);

		List<Map<String, Object>> ownerReferences =
			(List<Map<String, Object>>)metadata.get("ownerReferences");

		Assert.assertEquals(
			_LIFERAY_ENVIRONMENT_UID,
			ownerReferences.get(
				0
			).get(
				"uid"
			));
	}

	@Test
	public void testGetKeyPairWhenAnotherNodeCreatesTheIdentitySecretFirst()
		throws Exception {

		_racingKeyPair = KeyPairUtil.generateKeyPair();

		_assertEquals(
			_racingKeyPair, _kubernetesLiferayEnvironment.getKeyPair());
	}

	@Test
	public void testStoreAddOn() throws Exception {
		_kubernetesLiferayEnvironment.storeAddOn(
			"sample.lpkg",
			new ByteArrayInputStream(
				"sample".getBytes(StandardCharsets.UTF_8)));

		Assert.assertEquals(
			"sample",
			Files.readString(
				_addOnsFolder.toPath(
				).resolve(
					"sample.lpkg"
				)));
	}

	@Test
	public void testStoreEntitlementsReplacesTheEntitlementsSecret()
		throws Exception {

		_kubernetesLiferayEnvironment.storeEntitlements(
			new Entitlements(
				"[]", "<licenses/>".getBytes(StandardCharsets.UTF_8), 3));
		_kubernetesLiferayEnvironment.storeEntitlements(
			new Entitlements(
				"[{}]",
				"<licenses></licenses>".getBytes(StandardCharsets.UTF_8), 1));

		Entitlements entitlements =
			_kubernetesLiferayEnvironment.getEntitlements();

		Assert.assertEquals("[{}]", entitlements.getAddOnsJSON());
		Assert.assertEquals(
			"<licenses></licenses>",
			new String(entitlements.getLicenseXML(), StandardCharsets.UTF_8));
		Assert.assertEquals(1, entitlements.getMaxClusterNodes());

		Assert.assertEquals(1, _secretCreateCount);
	}

	@Test
	public void testUpdateStatus() throws Exception {
		_liferayEnvironmentStatus.put(
			"conditions",
			List.of(
				HashMapBuilder.<String, Object>put(
					"lastTransitionTime", "2020-01-01T00:00:00Z"
				).put(
					"message", ""
				).put(
					"reason", "Activated"
				).put(
					"status", "True"
				).put(
					"type", "Activated"
				).build()));

		LiferayEnvironmentStatus liferayEnvironmentStatus =
			new LiferayEnvironmentStatus();

		Date activatedDate = new Date(1700000000000L);

		liferayEnvironmentStatus.setActivatedDate(activatedDate);

		liferayEnvironmentStatus.setLicenseState(LicenseState.VALID);
		liferayEnvironmentStatus.setMaxClusterNodes(3);
		liferayEnvironmentStatus.setMessage("Provisioning is unreachable.");
		liferayEnvironmentStatus.setProvisioningReachable(false);

		_kubernetesLiferayEnvironment.updateStatus(liferayEnvironmentStatus);

		RecordedRequest recordedRequest = _statusRecordedRequests.get(0);

		Assert.assertEquals(
			"Bearer token", recordedRequest.getHeader("Authorization"));
		Assert.assertTrue(
			recordedRequest.getHeader(
				"Content-Type"
			).startsWith(
				"application/apply-patch+yaml"
			));
		Assert.assertEquals(
			"liferay-dxp",
			recordedRequest.getRequestUrl(
			).queryParameter(
				"fieldManager"
			));
		Assert.assertEquals(
			"true",
			recordedRequest.getRequestUrl(
			).queryParameter(
				"force"
			));

		List<Map<String, Object>> conditions =
			(List<Map<String, Object>>)_liferayEnvironmentStatus.get(
				"conditions");

		Assert.assertEquals(
			"2020-01-01T00:00:00Z",
			conditions.get(
				0
			).get(
				"lastTransitionTime"
			));

		liferayEnvironmentStatus = _kubernetesLiferayEnvironment.getStatus();

		Assert.assertEquals(
			activatedDate, liferayEnvironmentStatus.getActivatedDate());
		Assert.assertEquals(
			LicenseState.VALID, liferayEnvironmentStatus.getLicenseState());
		Assert.assertEquals(
			Integer.valueOf(3), liferayEnvironmentStatus.getMaxClusterNodes());
		Assert.assertEquals(
			"Provisioning is unreachable.",
			liferayEnvironmentStatus.getMessage());
		Assert.assertEquals(
			Boolean.FALSE, liferayEnvironmentStatus.getProvisioningReachable());
	}

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	private void _assertEquals(KeyPair expectedKeyPair, KeyPair keyPair) {
		Assert.assertArrayEquals(
			expectedKeyPair.getPrivate(
			).getEncoded(),
			keyPair.getPrivate(
			).getEncoded());
		Assert.assertArrayEquals(
			expectedKeyPair.getPublic(
			).getEncoded(),
			keyPair.getPublic(
			).getEncoded());
	}

	private KubernetesLiferayEnvironment _createKubernetesLiferayEnvironment() {
		KubernetesLiferayEnvironment kubernetesLiferayEnvironment =
			new KubernetesLiferayEnvironment();

		Config config = Config.empty();

		config.setMasterUrl(String.valueOf(_mockWebServer.url(_ROOT_PATH)));
		config.setNamespace(_NAMESPACE);
		config.setOauthToken("token");

		ReflectionTestUtil.setFieldValue(
			kubernetesLiferayEnvironment, "_addOnsPath",
			_addOnsFolder.toPath());
		ReflectionTestUtil.setFieldValue(
			kubernetesLiferayEnvironment, "_defaultKubernetesClient",
			new DefaultKubernetesClient(config));
		ReflectionTestUtil.setFieldValue(
			kubernetesLiferayEnvironment, "_liferayEnvironmentName", "dev");
		ReflectionTestUtil.setFieldValue(
			kubernetesLiferayEnvironment, "_namespace", _NAMESPACE);

		return kubernetesLiferayEnvironment;
	}

	private static final String _LIFERAY_ENVIRONMENT_PATH =
		"/apis/licensing.liferay.com/v1alpha1/namespaces/test" +
			"/liferayenvironments/dev";

	private static final String _LIFERAY_ENVIRONMENT_UID =
		"6f3c0f02-5fd0-4f4e-9a8e-6c6a43b6e2a1";

	private static final String _NAMESPACE = "test";

	private static final String _NAMESPACE_UID =
		"1fa0bf48-3347-488c-99d9-31f60b7f080a";

	private static final String _ROOT_PATH = "/";

	private static final String _SECRETS_PATH =
		"/api/v1/namespaces/test/secrets";

	private File _addOnsFolder;
	private KubernetesLiferayEnvironment _kubernetesLiferayEnvironment;
	private Map<String, Object> _liferayEnvironmentStatus = new HashMap<>();
	private final MockWebServer _mockWebServer = new MockWebServer();
	private KeyPair _racingKeyPair;
	private int _secretCreateCount;
	private final Map<String, Map<String, Object>> _secrets = new HashMap<>();
	private final List<RecordedRequest> _statusRecordedRequests =
		new ArrayList<>();

	private class APIServerDispatcher extends Dispatcher {

		@Override
		public MockResponse dispatch(RecordedRequest recordedRequest) {
			try {
				return _dispatch(recordedRequest);
			}
			catch (Exception exception) {
				return new MockResponse(
				).setBody(
					String.valueOf(exception)
				).setResponseCode(
					500
				);
			}
		}

		private MockResponse _dispatch(RecordedRequest recordedRequest)
			throws Exception {

			String path = recordedRequest.getPath();

			if (path.equals("/api/v1/namespaces/test")) {
				return _toMockResponse(
					200,
					HashMapBuilder.<String, Object>put(
						"apiVersion", "v1"
					).put(
						"kind", "Namespace"
					).put(
						"metadata",
						HashMapBuilder.put(
							"name", _NAMESPACE
						).put(
							"uid", _NAMESPACE_UID
						).build()
					).build());
			}

			String method = recordedRequest.getMethod();

			if (path.startsWith(_LIFERAY_ENVIRONMENT_PATH + "/status") &&
				method.equals("PATCH")) {

				_statusRecordedRequests.add(recordedRequest);

				Map<String, Object> liferayEnvironment = _readBody(
					recordedRequest);

				_liferayEnvironmentStatus =
					(Map<String, Object>)liferayEnvironment.get("status");

				return _toMockResponse(200, _toLiferayEnvironment());
			}

			if (path.equals(_LIFERAY_ENVIRONMENT_PATH)) {
				return _toMockResponse(200, _toLiferayEnvironment());
			}

			if (path.equals(_SECRETS_PATH) && method.equals("POST")) {
				Map<String, Object> secret = _readBody(recordedRequest);

				String name = _getName(secret);

				if (_racingKeyPair != null) {
					_secrets.put(
						name,
						_toSecret(
							HashMapBuilder.put(
								"private.pem",
								KeyPairUtil.getPrivateKeyPEM(
									_racingKeyPair.getPrivate())
							).build(),
							secret));
				}

				if (_secrets.containsKey(name)) {
					return _toStatusMockResponse(409, "AlreadyExists");
				}

				_secretCreateCount++;

				_secrets.put(name, _toSecret(secret));

				return _toMockResponse(201, _secrets.get(name));
			}

			if (path.startsWith(_SECRETS_PATH + "/")) {
				String name = path.substring(_SECRETS_PATH.length() + 1);

				if (method.equals("PUT")) {
					_secrets.put(name, _toSecret(_readBody(recordedRequest)));
				}

				if (!_secrets.containsKey(name)) {
					return _toStatusMockResponse(404, "NotFound");
				}

				return _toMockResponse(200, _secrets.get(name));
			}

			return _toStatusMockResponse(404, "NotFound");
		}

		private String _getName(Map<String, Object> resource) {
			Map<String, Object> metadata = (Map<String, Object>)resource.get(
				"metadata");

			return (String)metadata.get("name");
		}

		private Map<String, Object> _readBody(RecordedRequest recordedRequest)
			throws Exception {

			return Serialization.jsonMapper(
			).readValue(
				recordedRequest.getBody(
				).readUtf8(),
				Map.class
			);
		}

		private Map<String, Object> _toLiferayEnvironment() {
			return HashMapBuilder.<String, Object>put(
				"apiVersion", "licensing.liferay.com/v1alpha1"
			).put(
				"kind", "LiferayEnvironment"
			).put(
				"metadata",
				HashMapBuilder.put(
					"name", "dev"
				).put(
					"namespace", _NAMESPACE
				).put(
					"uid", _LIFERAY_ENVIRONMENT_UID
				).build()
			).put(
				"spec",
				HashMapBuilder.put(
					"activationMode", "DXP"
				).build()
			).put(
				"status", _liferayEnvironmentStatus
			).build();
		}

		private MockResponse _toMockResponse(
				int code, Map<String, Object> resource)
			throws Exception {

			return new MockResponse(
			).setBody(
				Serialization.asJson(resource)
			).setHeader(
				"Content-Type", "application/json"
			).setResponseCode(
				code
			);
		}

		private Map<String, Object> _toSecret(Map<String, Object> secret) {
			return _toSecret(
				(Map<String, String>)secret.get("stringData"), secret);
		}

		private Map<String, Object> _toSecret(
			Map<String, String> stringData, Map<String, Object> secret) {

			Map<String, String> data = new HashMap<>();

			Base64.Encoder encoder = Base64.getEncoder();

			stringData.forEach(
				(key, value) -> data.put(
					key,
					encoder.encodeToString(
						value.getBytes(StandardCharsets.UTF_8))));

			Map<String, Object> storedSecret =
				HashMapBuilder.<String, Object>putAll(
					secret
				).put(
					"data", data
				).build();

			storedSecret.remove("stringData");

			return storedSecret;
		}

		private MockResponse _toStatusMockResponse(int code, String reason)
			throws Exception {

			return _toMockResponse(
				code,
				HashMapBuilder.<String, Object>put(
					"apiVersion", "v1"
				).put(
					"code", code
				).put(
					"kind", "Status"
				).put(
					"reason", reason
				).put(
					"status", "Failure"
				).build());
		}

	}

}