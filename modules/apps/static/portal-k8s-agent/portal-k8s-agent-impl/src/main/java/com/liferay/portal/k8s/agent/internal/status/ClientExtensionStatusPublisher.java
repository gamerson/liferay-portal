/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.k8s.agent.internal.status;

import com.liferay.portal.k8s.agent.status.ClientExtensionConfigurationError;
import com.liferay.portal.k8s.agent.status.ClientExtensionStatusCondition;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.Validator;

import io.fabric8.kubernetes.client.KubernetesClient;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;

/**
 * Reports whether this virtual instance applied a client extension's
 * configuration payload, by writing the ClientExtension resource's status
 * subresource.
 *
 * The portal is the only component that knows this. The operator delivers the
 * payload and owns the Delivered, Provisioned and Ready conditions; the portal
 * owns ConfigurationAccepted and the errors behind it. Conditions are keyed by
 * type precisely so that more than one controller can contribute, and every
 * other field of the status is preserved on write.
 *
 * @author Gregory Amerson
 */
public class ClientExtensionStatusPublisher {

	public ClientExtensionStatusPublisher(
		ClientExtensionConfigurationErrorRegistry
			clientExtensionConfigurationErrorRegistry,
		KubernetesClient kubernetesClient) {

		_clientExtensionConfigurationErrorRegistry =
			clientExtensionConfigurationErrorRegistry;
		_kubernetesClient = kubernetesClient;
	}

	/**
	 * Writes the outcome recorded for a client extension onto its resource. A
	 * client extension with no resource, which is every deployment that does
	 * not go through the operator, is silently skipped.
	 */
	public void publish(String serviceId, String virtualInstanceId) {
		if (Validator.isNull(serviceId) ||
			Validator.isNull(virtualInstanceId)) {

			return;
		}

		try {
			Map<String, Object> clientExtension = _find(
				serviceId, virtualInstanceId);

			if (clientExtension == null) {
				if (_log.isDebugEnabled()) {
					_log.debug("No client extension resource for " + serviceId);
				}

				return;
			}

			Map<String, Object> metadata = _getMap(clientExtension, "metadata");

			_kubernetesClient.customResource(
				ClientExtensionResource.CONTEXT
			).updateStatus(
				GetterUtil.getString(metadata.get("namespace")),
				GetterUtil.getString(metadata.get("name")),
				_withStatus(clientExtension, serviceId, virtualInstanceId)
			);

			if (_log.isInfoEnabled()) {
				_log.info("Reported configuration status for " + serviceId);
			}
		}
		catch (Exception exception) {
			if (_log.isWarnEnabled()) {
				_log.warn(
					"Unable to report configuration status for " + serviceId,
					exception);
			}
		}
	}

	private Map<String, Object> _condition(
		String message, String reason, String status) {

		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
			"yyyy-MM-dd'T'HH:mm:ss'Z'");

		simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

		Map<String, Object> condition = new HashMap<>();

		condition.put(
			"lastTransitionTime", simpleDateFormat.format(new Date()));
		condition.put("message", message);
		condition.put("reason", reason);
		condition.put("status", status);
		condition.put(
			"type", ClientExtensionStatusCondition.CONFIGURATION_ACCEPTED);

		return condition;
	}

	private Map<String, Object> _find(
		String serviceId, String virtualInstanceId) {

		Map<String, Object> result = _kubernetesClient.customResource(
			ClientExtensionResource.CONTEXT
		).list();

		Object items = result.get("items");

		if (!(items instanceof List)) {
			return null;
		}

		for (Object item : (List<Object>)items) {
			if (!(item instanceof Map)) {
				continue;
			}

			Map<String, Object> clientExtension = (Map<String, Object>)item;

			Map<String, Object> spec = _getMap(clientExtension, "spec");

			if (Objects.equals(spec.get("serviceId"), serviceId) &&
				Objects.equals(
					spec.get("virtualInstanceId"), virtualInstanceId)) {

				return clientExtension;
			}
		}

		return null;
	}

	private Map<String, Object> _getMap(Map<String, Object> map, String key) {
		Object value = map.get(key);

		if (value instanceof Map) {
			return (Map<String, Object>)value;
		}

		return new HashMap<>();
	}

	/**
	 * Replaces only the ConfigurationAccepted condition, leaving every
	 * condition the operator owns in place.
	 */
	private List<Object> _mergeConditions(
		Map<String, Object> status, Map<String, Object> condition) {

		List<Object> merged = new ArrayList<>();

		Object conditions = status.get("conditions");

		if (conditions instanceof List) {
			for (Object object : (List<Object>)conditions) {
				if (!(object instanceof Map)) {
					continue;
				}

				Map<String, Object> existing = (Map<String, Object>)object;

				if (Objects.equals(
						existing.get("type"),
						ClientExtensionStatusCondition.
							CONFIGURATION_ACCEPTED)) {

					// Keep the original transition time when nothing changed,
					// which is what a condition's timestamp is for.

					if (Objects.equals(
							existing.get("status"), condition.get("status")) &&
						Objects.equals(
							existing.get("reason"), condition.get("reason"))) {

						condition.put(
							"lastTransitionTime",
							existing.get("lastTransitionTime"));
					}

					continue;
				}

				merged.add(existing);
			}
		}

		merged.add(condition);

		return merged;
	}

	private Map<String, Object> _withStatus(
		Map<String, Object> clientExtension, String serviceId,
		String virtualInstanceId) {

		List<ClientExtensionConfigurationError> configurationErrors =
			_clientExtensionConfigurationErrorRegistry.getConfigurationErrors(
				serviceId, virtualInstanceId);
		List<String> configurationPids =
			_clientExtensionConfigurationErrorRegistry.getConfigurationPids(
				serviceId, virtualInstanceId);

		Map<String, Object> status = _getMap(clientExtension, "status");

		List<Object> errors = new ArrayList<>();

		for (ClientExtensionConfigurationError configurationError :
				configurationErrors) {

			Map<String, Object> error = new HashMap<>();

			error.put(
				"configMapName",
				GetterUtil.getString(configurationError.getConfigMapName()));
			error.put(
				"message",
				GetterUtil.getString(configurationError.getMessage()));
			error.put(
				"phase", GetterUtil.getString(configurationError.getPhase()));
			error.put("pid", GetterUtil.getString(configurationError.getPid()));

			errors.add(error);
		}

		Map<String, Object> condition = null;

		if (configurationErrors.isEmpty()) {
			condition = _condition(
				"The virtual instance applied " + configurationPids.size() +
					" configuration entries.",
				"Applied", ClientExtensionStatusCondition.STATUS_TRUE);
		}
		else {
			ClientExtensionConfigurationError first = configurationErrors.get(
				0);

			condition = _condition(
				String.format(
					"The virtual instance refused %d configuration entries. " +
						"First failure during %s of \"%s\": %s",
					configurationErrors.size(), first.getPhase(),
					GetterUtil.getString(first.getPid()), first.getMessage()),
				"ConfigurationRejected",
				ClientExtensionStatusCondition.STATUS_FALSE);
		}

		status.put("appliedConfigurationPids", configurationPids);
		status.put("conditions", _mergeConditions(status, condition));
		status.put("configurationErrors", errors);

		clientExtension.put("status", status);

		return clientExtension;
	}

	private static final Log _log = LogFactoryUtil.getLog(
		ClientExtensionStatusPublisher.class);

	private final ClientExtensionConfigurationErrorRegistry
		_clientExtensionConfigurationErrorRegistry;
	private final KubernetesClient _kubernetesClient;

}