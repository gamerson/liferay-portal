/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.environment.activation.poc.internal.activator;

import com.liferay.petra.string.StringBundler;
import com.liferay.portal.environment.Entitlements;
import com.liferay.portal.environment.LicenseState;
import com.liferay.portal.environment.LiferayEnvironment;
import com.liferay.portal.environment.LiferayEnvironmentStatus;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactory;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.module.service.Snapshot;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.ReleaseInfo;
import com.liferay.portal.kernel.util.Validator;

import java.io.InputStream;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.nio.charset.StandardCharsets;

import java.security.KeyPair;
import java.security.Signature;

import java.text.SimpleDateFormat;

import java.time.Duration;

import java.util.Base64;
import java.util.Date;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Stands in for the license activation module that another team builds, only
 * to exercise {@link LiferayEnvironment} against a provisioning service. It
 * speaks the same protocol as the DXP operator's provisioning client and
 * applies no license policy of its own. Do not ship it.
 *
 * @author Gregory Amerson
 */
public class LiferayEnvironmentActivator {

	public LiferayEnvironmentActivator(JSONFactory jsonFactory) {
		_jsonFactory = jsonFactory;
	}

	public String activate(String activationCode) throws Exception {
		LiferayEnvironment liferayEnvironment = _getLiferayEnvironment();

		KeyPair keyPair = liferayEnvironment.getKeyPair();

		HttpResponse<String> httpResponse = _post(
			JSONUtil.put(
				"activationCode", activationCode
			).put(
				"environmentID", liferayEnvironment.getEnvironmentId()
			).put(
				"environmentName", liferayEnvironment.getEnvironmentName()
			).put(
				"publicKey",
				Base64.getEncoder(
				).encodeToString(
					keyPair.getPublic(
					).getEncoded()
				)
			),
			liferayEnvironment, "activation");

		LiferayEnvironmentStatus liferayEnvironmentStatus =
			liferayEnvironment.getStatus();

		if (httpResponse.statusCode() == 404) {
			liferayEnvironmentStatus.setLicenseState(
				LicenseState.ACTIVATION_REJECTED);
			liferayEnvironmentStatus.setMessage(
				"The provisioning service rejected the activation code.");

			liferayEnvironment.updateStatus(liferayEnvironmentStatus);

			return "The activation code was rejected.";
		}

		if (httpResponse.statusCode() != 200) {
			return StringBundler.concat(
				"Unable to activate: HTTP ", httpResponse.statusCode(), " ",
				httpResponse.body());
		}

		liferayEnvironmentStatus.setActivatedDate(new Date());
		liferayEnvironmentStatus.setLicenseState(null);
		liferayEnvironmentStatus.setMessage(null);

		liferayEnvironment.updateStatus(liferayEnvironmentStatus);

		return StringBundler.concat(
			"Activated ", liferayEnvironment.getEnvironmentId(), "\n",
			heartbeat());
	}

	public String heartbeat() throws Exception {
		LiferayEnvironment liferayEnvironment = _getLiferayEnvironment();

		LiferayEnvironmentStatus liferayEnvironmentStatus =
			liferayEnvironment.getStatus();

		try {
			HttpResponse<String> httpResponse = _post(
				JSONUtil.put(
					"dxpVersion", ReleaseInfo.getVersionDisplayName()
				).put(
					"environmentID", liferayEnvironment.getEnvironmentId()
				),
				liferayEnvironment, "manifest");

			if (httpResponse.statusCode() != 200) {
				throw new Exception(
					StringBundler.concat(
						"HTTP ", httpResponse.statusCode(), " ",
						httpResponse.body()));
			}

			JSONObject manifestJSONObject = _jsonFactory.createJSONObject(
				httpResponse.body());

			JSONArray addOnsJSONArray = manifestJSONObject.getJSONArray(
				"add-ons");

			if (addOnsJSONArray == null) {
				addOnsJSONArray = _jsonFactory.createJSONArray();
			}

			byte[] licenseXML = Base64.getDecoder(
			).decode(
				manifestJSONObject.getString("licenseXML")
			);

			int maxClusterNodes = manifestJSONObject.getInt("maxClusterNodes");

			liferayEnvironment.storeEntitlements(
				new Entitlements(
					addOnsJSONArray.toString(), licenseXML, maxClusterNodes));

			_storeAddOns(addOnsJSONArray, liferayEnvironment);

			Date validUntilDate = _getExpirationDate(licenseXML);

			if (validUntilDate == null) {
				liferayEnvironmentStatus.setLicenseState(LicenseState.INVALID);
			}
			else if (validUntilDate.before(new Date())) {
				liferayEnvironmentStatus.setLicenseState(LicenseState.EXPIRED);
			}
			else {
				liferayEnvironmentStatus.setLicenseState(LicenseState.VALID);
			}

			liferayEnvironmentStatus.setLastVerifiedDate(new Date());
			liferayEnvironmentStatus.setMaxClusterNodes(maxClusterNodes);
			liferayEnvironmentStatus.setMessage(null);
			liferayEnvironmentStatus.setProvisioningReachable(true);
			liferayEnvironmentStatus.setValidUntilDate(validUntilDate);
		}
		catch (Exception exception) {
			if (_log.isDebugEnabled()) {
				_log.debug(exception);
			}

			liferayEnvironmentStatus.setMessage(
				"Unable to reach the provisioning service: " +
					exception.getMessage());
			liferayEnvironmentStatus.setProvisioningReachable(false);
		}

		liferayEnvironment.updateStatus(liferayEnvironmentStatus);

		return status();
	}

	public void heartbeatIfActivated() throws Exception {
		LiferayEnvironment liferayEnvironment =
			_liferayEnvironmentSnapshot.get();

		if (liferayEnvironment == null) {
			return;
		}

		LiferayEnvironmentStatus liferayEnvironmentStatus =
			liferayEnvironment.getStatus();

		if (liferayEnvironmentStatus.getActivatedDate() != null) {
			heartbeat();
		}
	}

	public String status() throws Exception {
		LiferayEnvironment liferayEnvironment = _getLiferayEnvironment();

		LiferayEnvironmentStatus liferayEnvironmentStatus =
			liferayEnvironment.getStatus();

		Entitlements entitlements = liferayEnvironment.getEntitlements();

		return StringBundler.concat(
			"Type: ", liferayEnvironment.getType(), "\nEnvironment ID: ",
			liferayEnvironment.getEnvironmentId(), "\nEnvironment name: ",
			liferayEnvironment.getEnvironmentName(), "\nActivated: ",
			liferayEnvironmentStatus.getActivatedDate(), "\nLicense state: ",
			liferayEnvironmentStatus.getLicenseState(),
			"\nMaximum cluster nodes: ",
			liferayEnvironmentStatus.getMaxClusterNodes(), "\nValid until: ",
			liferayEnvironmentStatus.getValidUntilDate(),
			"\nProvisioning reachable: ",
			liferayEnvironmentStatus.getProvisioningReachable(), "\nMessage: ",
			liferayEnvironmentStatus.getMessage(), "\nEntitlements stored: ",
			entitlements != null);
	}

	private String _encode(byte[] bytes) {
		Base64.Encoder encoder = Base64.getUrlEncoder();

		return encoder.withoutPadding(
		).encodeToString(
			bytes
		);
	}

	private Date _getExpirationDate(byte[] licenseXML) {
		Matcher matcher = _expirationDatePattern.matcher(
			new String(licenseXML, StandardCharsets.UTF_8));

		if (!matcher.find()) {
			return null;
		}

		try {
			SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
				"EEEE, MMMM d, yyyy h:mm:ss a z", LocaleUtil.US);

			return simpleDateFormat.parse(matcher.group(1));
		}
		catch (Exception exception) {
			if (_log.isDebugEnabled()) {
				_log.debug(exception);
			}

			return null;
		}
	}

	private LiferayEnvironment _getLiferayEnvironment() {
		LiferayEnvironment liferayEnvironment =
			_liferayEnvironmentSnapshot.get();

		if (liferayEnvironment == null) {
			throw new IllegalStateException(
				"No LiferayEnvironment is available");
		}

		return liferayEnvironment;
	}

	private String _getProvisioningBaseURL() {
		String provisioningBaseURL = System.getenv(
			"LIFERAY_PROVISIONING_BASE_URL");

		if (Validator.isNull(provisioningBaseURL)) {
			return "https://api.one.liferay.com";
		}

		return provisioningBaseURL;
	}

	private HttpResponse<String> _post(
			JSONObject claimsJSONObject, LiferayEnvironment liferayEnvironment,
			String operation)
		throws Exception {

		String environmentId = liferayEnvironment.getEnvironmentId();

		return _httpClient.send(
			HttpRequest.newBuilder(
				URI.create(
					StringBundler.concat(
						_getProvisioningBaseURL(), "/cloud/v1/environments/",
						environmentId, "/", operation))
			).header(
				"Content-Type", "text/plain"
			).POST(
				HttpRequest.BodyPublishers.ofString(
					_signJWT(
						claimsJSONObject, environmentId,
						liferayEnvironment.getKeyPair()))
			).timeout(
				Duration.ofSeconds(30)
			).build(),
			HttpResponse.BodyHandlers.ofString());
	}

	private String _signJWT(
			JSONObject claimsJSONObject, String environmentId, KeyPair keyPair)
		throws Exception {

		long now = System.currentTimeMillis() / 1000;

		// JSONObject writes a long as a string, and a JWT NumericDate must be a
		// number, so the claims pass boxed values that keep their JSON type

		claimsJSONObject.put(
			"exp", Long.valueOf(now + 60)
		).put(
			"iat", Long.valueOf(now)
		).put(
			"iss", environmentId
		).put(
			"jti", String.valueOf(UUID.randomUUID())
		);

		String signingInput = StringBundler.concat(
			_encode(
				JSONUtil.put(
					"alg", "RS256"
				).put(
					"typ", "JWT"
				).toString(
				).getBytes(
					StandardCharsets.UTF_8
				)),
			".",
			_encode(
				claimsJSONObject.toString(
				).getBytes(
					StandardCharsets.UTF_8
				)));

		Signature signature = Signature.getInstance("SHA256withRSA");

		signature.initSign(keyPair.getPrivate());
		signature.update(signingInput.getBytes(StandardCharsets.UTF_8));

		return signingInput + "." + _encode(signature.sign());
	}

	private void _storeAddOns(
			JSONArray addOnsJSONArray, LiferayEnvironment liferayEnvironment)
		throws Exception {

		for (int i = 0; i < addOnsJSONArray.length(); i++) {
			JSONObject addOnJSONObject = addOnsJSONArray.getJSONObject(i);

			String sha256Checksum = addOnJSONObject.getString("sha256Checksum");

			if (_storedAddOnChecksums.contains(sha256Checksum)) {
				continue;
			}

			long virtualEntryId = addOnJSONObject.getLong("virtualEntryId");

			HttpResponse<InputStream> httpResponse = _httpClient.send(
				HttpRequest.newBuilder(
					URI.create(addOnJSONObject.getString("downloadURL"))
				).header(
					"Content-Type", "text/plain"
				).POST(
					HttpRequest.BodyPublishers.ofString(
						_signJWT(
							JSONUtil.put(
								"environmentID",
								liferayEnvironment.getEnvironmentId()
							).put(
								"virtualEntryId", Long.valueOf(virtualEntryId)
							),
							liferayEnvironment.getEnvironmentId(),
							liferayEnvironment.getKeyPair()))
				).build(),
				HttpResponse.BodyHandlers.ofInputStream());

			try (InputStream inputStream = httpResponse.body()) {
				if (httpResponse.statusCode() != 200) {
					_log.error(
						StringBundler.concat(
							"Unable to download add-on ", virtualEntryId,
							": HTTP ", httpResponse.statusCode()));

					continue;
				}

				liferayEnvironment.storeAddOn(
					virtualEntryId + ".lpkg", inputStream);

				_storedAddOnChecksums.add(sha256Checksum);
			}
			catch (Exception exception) {
				_log.error(
					"Unable to store add-on " + virtualEntryId, exception);
			}
		}
	}

	private static final Log _log = LogFactoryUtil.getLog(
		LiferayEnvironmentActivator.class);

	private static final Pattern _expirationDatePattern = Pattern.compile(
		"<expiration-date>([^<]+)</expiration-date>");
	private static final Snapshot<LiferayEnvironment>
		_liferayEnvironmentSnapshot = new Snapshot<>(
			LiferayEnvironmentActivator.class, LiferayEnvironment.class, null,
			true);

	private final HttpClient _httpClient = HttpClient.newHttpClient();
	private final JSONFactory _jsonFactory;
	private final Set<String> _storedAddOnChecksums =
		ConcurrentHashMap.newKeySet();

}