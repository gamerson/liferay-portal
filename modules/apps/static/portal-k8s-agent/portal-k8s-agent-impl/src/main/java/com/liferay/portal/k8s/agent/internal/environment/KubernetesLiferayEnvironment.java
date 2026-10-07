/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.k8s.agent.internal.environment;

import com.liferay.petra.string.StringBundler;
import com.liferay.portal.configuration.metatype.bnd.util.ConfigurableUtil;
import com.liferay.portal.environment.Entitlements;
import com.liferay.portal.environment.LicenseState;
import com.liferay.portal.environment.LiferayEnvironment;
import com.liferay.portal.environment.LiferayEnvironmentStatus;
import com.liferay.portal.environment.LiferayEnvironmentType;
import com.liferay.portal.environment.util.KeyPairUtil;
import com.liferay.portal.k8s.agent.internal.configuration.LiferayEnvironmentConfiguration;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.kernel.util.Validator;

import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.base.ResourceDefinitionContext;
import io.fabric8.kubernetes.client.http.HttpClient;
import io.fabric8.kubernetes.client.http.HttpRequest;
import io.fabric8.kubernetes.client.http.HttpResponse;
import io.fabric8.kubernetes.client.utils.Serialization;

import java.io.InputStream;

import java.net.HttpURLConnection;
import java.net.URL;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import java.security.KeyPair;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;

/**
 * Keeps the environment's state where the DXP operator reads it: the identity
 * and entitlements in the secrets named after the LiferayEnvironment resource,
 * and the status in that resource's status, which the operator turns into the
 * workload's replica ceiling. The status is written with server side apply
 * under its own field manager, so the fields this class reports and the
 * fields the operator reports never overwrite each other.
 *
 * @author Gregory Amerson
 */
@Component(
	configurationPid = "com.liferay.portal.k8s.agent.internal.configuration.LiferayEnvironmentConfiguration",
	configurationPolicy = ConfigurationPolicy.REQUIRE,
	service = LiferayEnvironment.class
)
public class KubernetesLiferayEnvironment implements LiferayEnvironment {

	@Override
	public Entitlements getEntitlements() {
		Secret secret = _getSecret(_SECRET_SUFFIX_ENTITLEMENTS);

		if (secret == null) {
			return null;
		}

		Map<String, String> data = secret.getData();

		return new Entitlements(
			_decode(data.get("add-ons.json")),
			_decodeBytes(data.get("license.xml")),
			GetterUtil.getInteger(_decode(data.get("maxClusterNodes"))));
	}

	@Override
	public String getEnvironmentId() {
		String environmentId = _environmentId;

		if (environmentId != null) {
			return environmentId;
		}

		// The API server assigns a namespace's UID, rejects one a client
		// supplies, and never reuses it, so it identifies the environment for
		// the life of the namespace and is safe to keep once read

		Namespace namespace = _defaultKubernetesClient.namespaces(
		).withName(
			_namespace
		).get();

		if (namespace == null) {
			throw new IllegalStateException(
				"Unable to read namespace \"" + _namespace + "\"");
		}

		ObjectMeta objectMeta = namespace.getMetadata();

		_environmentId = objectMeta.getUid();

		return _environmentId;
	}

	@Override
	public String getEnvironmentName() {
		Map<String, Object> spec = _getMap(
			_getLiferayEnvironmentResource(), "spec");

		String environmentName = GetterUtil.getString(
			spec.get("environmentName"));

		if (Validator.isNotNull(environmentName)) {
			return environmentName;
		}

		return _namespace;
	}

	@Override
	public KeyPair getKeyPair() throws Exception {
		Secret secret = _getSecret(_SECRET_SUFFIX_IDENTITY);

		if (secret != null) {
			return _toKeyPair(secret);
		}

		KeyPair keyPair = KeyPairUtil.generateKeyPair();

		try {
			_createSecret(
				HashMapBuilder.put(
					"private.pem",
					KeyPairUtil.getPrivateKeyPEM(keyPair.getPrivate())
				).put(
					"public.pem",
					KeyPairUtil.getPublicKeyBase64(keyPair.getPublic())
				).build(),
				_SECRET_SUFFIX_IDENTITY);

			return keyPair;
		}
		catch (KubernetesClientException kubernetesClientException) {
			if (kubernetesClientException.getCode() !=
					HttpURLConnection.HTTP_CONFLICT) {

				throw kubernetesClientException;
			}

			// Another node created the identity first, and every node must
			// sign with the same key, so the stored one wins

			return _toKeyPair(_getSecret(_SECRET_SUFFIX_IDENTITY));
		}
	}

	@Override
	public LiferayEnvironmentStatus getStatus() {
		LiferayEnvironmentStatus liferayEnvironmentStatus =
			new LiferayEnvironmentStatus();

		Map<String, Object> status = _getMap(
			_getLiferayEnvironmentResource(), "status");

		liferayEnvironmentStatus.setActivatedDate(
			_toDate(status.get("activatedAt")));

		Map<String, Object> license = _getMap(status, "license");

		liferayEnvironmentStatus.setLastVerifiedDate(
			_toDate(license.get("lastVerified")));

		Number maxClusterNodes = (Number)license.get("maxClusterNodes");

		if (maxClusterNodes != null) {
			liferayEnvironmentStatus.setMaxClusterNodes(
				maxClusterNodes.intValue());
		}

		liferayEnvironmentStatus.setValidUntilDate(
			_toDate(license.get("validUntil")));

		for (Map<String, Object> condition : _getConditions(status)) {
			String reason = (String)condition.get("reason");
			String type = (String)condition.get("type");

			boolean conditionStatus = Objects.equals(
				condition.get("status"), "True");

			if (!conditionStatus && _types.contains(type)) {
				liferayEnvironmentStatus.setMessage(
					(String)condition.get("message"));
			}

			if (Objects.equals(type, _TYPE_ACTIVATED) && !conditionStatus &&
				Objects.equals(reason, _REASON_ACTIVATION_REJECTED)) {

				liferayEnvironmentStatus.setLicenseState(
					LicenseState.ACTIVATION_REJECTED);
			}
			else if (Objects.equals(type, _TYPE_LICENSE_VALID)) {
				liferayEnvironmentStatus.setLicenseState(
					_toLicenseState(conditionStatus, reason));
			}
			else if (Objects.equals(type, _TYPE_PROVISIONING_REACHABLE)) {
				liferayEnvironmentStatus.setProvisioningReachable(
					conditionStatus);
			}
		}

		return liferayEnvironmentStatus;
	}

	@Override
	public LiferayEnvironmentType getType() {
		return LiferayEnvironmentType.KUBERNETES;
	}

	@Override
	public void storeAddOn(String fileName, InputStream inputStream)
		throws Exception {

		if (_addOnsPath == null) {
			throw new IllegalStateException(
				"No add-ons directory is configured for this environment");
		}

		Files.createDirectories(_addOnsPath);

		Path fileNamePath = Paths.get(fileName);

		Path temporaryPath = Files.createTempFile(_addOnsPath, ".", ".tmp");

		Files.copy(
			inputStream, temporaryPath, StandardCopyOption.REPLACE_EXISTING);

		Files.move(
			temporaryPath, _addOnsPath.resolve(fileNamePath.getFileName()),
			StandardCopyOption.ATOMIC_MOVE,
			StandardCopyOption.REPLACE_EXISTING);
	}

	@Override
	public void storeEntitlements(Entitlements entitlements) {
		Map<String, String> data = HashMapBuilder.put(
			"add-ons.json", entitlements.getAddOnsJSON()
		).put(
			"license.xml",
			new String(entitlements.getLicenseXML(), StandardCharsets.UTF_8)
		).put(
			"maxClusterNodes", String.valueOf(entitlements.getMaxClusterNodes())
		).build();

		Secret secret = _getSecret(_SECRET_SUFFIX_ENTITLEMENTS);

		if (secret == null) {
			_createSecret(data, _SECRET_SUFFIX_ENTITLEMENTS);

			return;
		}

		secret.setData(null);
		secret.setStringData(data);

		_defaultKubernetesClient.secrets(
		).inNamespace(
			_namespace
		).withName(
			_getSecretName(_SECRET_SUFFIX_ENTITLEMENTS)
		).replace(
			secret
		);
	}

	@Override
	public void updateStatus(LiferayEnvironmentStatus liferayEnvironmentStatus)
		throws Exception {

		List<Map<String, Object>> conditions = new ArrayList<>();

		List<Map<String, Object>> currentConditions = _getConditions(
			_getMap(_getLiferayEnvironmentResource(), "status"));
		LicenseState licenseState = liferayEnvironmentStatus.getLicenseState();
		String message = GetterUtil.getString(
			liferayEnvironmentStatus.getMessage());

		if (liferayEnvironmentStatus.getActivatedDate() != null) {
			conditions.add(
				_toCondition(
					currentConditions, "", "Activated", true, _TYPE_ACTIVATED));
		}
		else if (licenseState == LicenseState.ACTIVATION_REJECTED) {
			conditions.add(
				_toCondition(
					currentConditions, message, _REASON_ACTIVATION_REJECTED,
					false, _TYPE_ACTIVATED));
		}

		if (licenseState == LicenseState.EXPIRED) {
			conditions.add(
				_toCondition(
					currentConditions, message, "Expired", false,
					_TYPE_LICENSE_VALID));
		}
		else if (licenseState == LicenseState.INVALID) {
			conditions.add(
				_toCondition(
					currentConditions, message, "Invalid", false,
					_TYPE_LICENSE_VALID));
		}
		else if (licenseState == LicenseState.VALID) {
			conditions.add(
				_toCondition(
					currentConditions, "", "Valid", true, _TYPE_LICENSE_VALID));
		}

		Boolean provisioningReachable =
			liferayEnvironmentStatus.getProvisioningReachable();

		if (provisioningReachable != null) {
			if (provisioningReachable) {
				conditions.add(
					_toCondition(
						currentConditions, "", "Reachable", true,
						_TYPE_PROVISIONING_REACHABLE));
			}
			else {
				conditions.add(
					_toCondition(
						currentConditions, message, "Unreachable", false,
						_TYPE_PROVISIONING_REACHABLE));
			}
		}

		Map<String, Object> license = new TreeMap<>();

		_putDate(
			license, "lastVerified",
			liferayEnvironmentStatus.getLastVerifiedDate());

		if (liferayEnvironmentStatus.getMaxClusterNodes() != null) {
			license.put(
				"maxClusterNodes",
				liferayEnvironmentStatus.getMaxClusterNodes());
		}

		_putDate(
			license, "validUntil",
			liferayEnvironmentStatus.getValidUntilDate());

		Map<String, Object> status = new TreeMap<>();

		_putDate(
			status, "activatedAt", liferayEnvironmentStatus.getActivatedDate());

		status.put("conditions", conditions);

		if (!license.isEmpty()) {
			status.put("license", license);
		}

		_applyStatus(
			Serialization.asJson(
				HashMapBuilder.<String, Object>put(
					"apiVersion", _API_VERSION
				).put(
					"kind", _KIND
				).put(
					"metadata",
					HashMapBuilder.put(
						"name", _liferayEnvironmentName
					).put(
						"namespace", _namespace
					).build()
				).put(
					"status", status
				).build()));
	}

	@Activate
	protected void activate(Map<String, Object> properties) {
		LiferayEnvironmentConfiguration liferayEnvironmentConfiguration =
			ConfigurableUtil.createConfigurable(
				LiferayEnvironmentConfiguration.class, properties);

		String addOnsDir = liferayEnvironmentConfiguration.addOnsDir();

		if (Validator.isNotNull(addOnsDir)) {
			_addOnsPath = Paths.get(addOnsDir);
		}

		_defaultKubernetesClient = new DefaultKubernetesClient(
			Config.autoConfigure(null));
		_liferayEnvironmentName =
			liferayEnvironmentConfiguration.liferayEnvironmentName();

		_namespace = _defaultKubernetesClient.getNamespace();
	}

	@Deactivate
	protected void deactivate() {
		_defaultKubernetesClient.close();
	}

	private void _applyStatus(String json) throws Exception {
		HttpClient httpClient = _defaultKubernetesClient.getHttpClient();

		HttpRequest.Builder httpRequestBuilder =
			httpClient.newHttpRequestBuilder();

		HttpResponse<String> httpResponse = httpClient.send(
			httpRequestBuilder.url(
				new URL(
					_defaultKubernetesClient.getMasterUrl(),
					StringBundler.concat(
						"apis/", _API_VERSION, "/namespaces/", _namespace,
						"/liferayenvironments/", _liferayEnvironmentName,
						"/status?fieldManager=", _FIELD_MANAGER, "&force=true"))
			).method(
				"PATCH", "application/apply-patch+yaml", json
			).build(),
			String.class);

		if (!httpResponse.isSuccessful()) {
			throw new KubernetesClientException(
				StringBundler.concat(
					"Unable to apply the status of LiferayEnvironment \"",
					_liferayEnvironmentName, "\": ", httpResponse.body()),
				httpResponse.code(), null);
		}
	}

	private void _createSecret(Map<String, String> data, String suffix) {
		GenericKubernetesResource genericKubernetesResource =
			_getLiferayEnvironmentResource();

		ObjectMeta objectMeta = genericKubernetesResource.getMetadata();

		_defaultKubernetesClient.secrets(
		).inNamespace(
			_namespace
		).create(
			new SecretBuilder(
			).withNewMetadata(
			).addToLabels(
				"controller-watched", "yes"
			).withName(
				_getSecretName(suffix)
			).withNamespace(
				_namespace
			).addNewOwnerReference(
			).withApiVersion(
				_API_VERSION
			).withKind(
				_KIND
			).withName(
				_liferayEnvironmentName
			).withUid(
				objectMeta.getUid()
			).endOwnerReference(
			).endMetadata(
			).withStringData(
				data
			).build()
		);
	}

	private String _decode(String value) {
		return new String(_decodeBytes(value), StandardCharsets.UTF_8);
	}

	private byte[] _decodeBytes(String value) {
		if (value == null) {
			return new byte[0];
		}

		Base64.Decoder decoder = Base64.getDecoder();

		return decoder.decode(value);
	}

	private List<Map<String, Object>> _getConditions(
		Map<String, Object> status) {

		List<Map<String, Object>> conditions =
			(List<Map<String, Object>>)status.get("conditions");

		if (conditions == null) {
			return Collections.emptyList();
		}

		return conditions;
	}

	private GenericKubernetesResource _getLiferayEnvironmentResource() {
		GenericKubernetesResource genericKubernetesResource =
			_defaultKubernetesClient.genericKubernetesResources(
				_RESOURCE_DEFINITION_CONTEXT
			).inNamespace(
				_namespace
			).withName(
				_liferayEnvironmentName
			).get();

		if (genericKubernetesResource == null) {
			throw new IllegalStateException(
				StringBundler.concat(
					"LiferayEnvironment \"", _liferayEnvironmentName,
					"\" does not exist in namespace \"", _namespace, "\""));
		}

		return genericKubernetesResource;
	}

	private Map<String, Object> _getMap(
		GenericKubernetesResource genericKubernetesResource, String key) {

		return _getMap(
			genericKubernetesResource.getAdditionalProperties(), key);
	}

	private Map<String, Object> _getMap(Map<String, Object> map, String key) {
		Map<String, Object> value = (Map<String, Object>)map.get(key);

		if (value == null) {
			return Collections.emptyMap();
		}

		return value;
	}

	private Secret _getSecret(String suffix) {
		return _defaultKubernetesClient.secrets(
		).inNamespace(
			_namespace
		).withName(
			_getSecretName(suffix)
		).get();
	}

	private String _getSecretName(String suffix) {
		return _liferayEnvironmentName + suffix;
	}

	private void _putDate(Map<String, Object> map, String key, Date date) {
		if (date != null) {
			map.put(key, _toTimestamp(date.toInstant()));
		}
	}

	private Map<String, Object> _toCondition(
		List<Map<String, Object>> currentConditions, String message,
		String reason, boolean status, String type) {

		String conditionStatus = status ? "True" : "False";

		// A condition keeps the time it last changed status, so a heartbeat
		// that reports the same status does not look like a transition

		String lastTransitionTime = _toTimestamp(Instant.now());

		for (Map<String, Object> currentCondition : currentConditions) {
			if (Objects.equals(
					currentCondition.get("status"), conditionStatus) &&
				Objects.equals(currentCondition.get("type"), type)) {

				lastTransitionTime = (String)currentCondition.get(
					"lastTransitionTime");
			}
		}

		return HashMapBuilder.<String, Object>put(
			"lastTransitionTime", lastTransitionTime
		).put(
			"message", message
		).put(
			"reason", reason
		).put(
			"status", conditionStatus
		).put(
			"type", type
		).build();
	}

	private Date _toDate(Object value) {
		if (value == null) {
			return null;
		}

		return Date.from(Instant.parse((String)value));
	}

	private KeyPair _toKeyPair(Secret secret) throws Exception {
		Map<String, String> data = secret.getData();

		return KeyPairUtil.toKeyPair(_decode(data.get("private.pem")));
	}

	private LicenseState _toLicenseState(boolean status, String reason) {
		if (status) {
			return LicenseState.VALID;
		}

		if (Objects.equals(reason, "Expired")) {
			return LicenseState.EXPIRED;
		}

		return LicenseState.INVALID;
	}

	private String _toTimestamp(Instant instant) {
		Instant truncatedInstant = instant.truncatedTo(ChronoUnit.SECONDS);

		return truncatedInstant.toString();
	}

	private static final String _API_VERSION = "licensing.liferay.com/v1alpha1";

	private static final String _FIELD_MANAGER = "liferay-dxp";

	private static final String _KIND = "LiferayEnvironment";

	private static final String _REASON_ACTIVATION_REJECTED =
		"ActivationRejected";

	private static final ResourceDefinitionContext
		_RESOURCE_DEFINITION_CONTEXT = new ResourceDefinitionContext.Builder(
		).withGroup(
			"licensing.liferay.com"
		).withKind(
			_KIND
		).withNamespaced(
			true
		).withPlural(
			"liferayenvironments"
		).withVersion(
			"v1alpha1"
		).build();

	private static final String _SECRET_SUFFIX_ENTITLEMENTS = "-entitlements";

	private static final String _SECRET_SUFFIX_IDENTITY = "-identity";

	private static final String _TYPE_ACTIVATED = "Activated";

	private static final String _TYPE_LICENSE_VALID = "LicenseValid";

	private static final String _TYPE_PROVISIONING_REACHABLE =
		"ProvisioningReachable";

	private static final List<String> _types = List.of(
		_TYPE_ACTIVATED, _TYPE_LICENSE_VALID, _TYPE_PROVISIONING_REACHABLE);

	private Path _addOnsPath;
	private DefaultKubernetesClient _defaultKubernetesClient;
	private volatile String _environmentId;
	private String _liferayEnvironmentName;
	private String _namespace;

}