/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.k8s.agent.internal.status;

import com.liferay.petra.string.StringPool;
import com.liferay.portal.k8s.agent.status.ClientExtensionConfigurationError;
import com.liferay.portal.k8s.agent.status.ClientExtensionStatus;
import com.liferay.portal.k8s.agent.status.ClientExtensionStatusCondition;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.Validator;

import io.fabric8.kubernetes.client.KubernetesClient;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

/**
 * Reads ClientExtension resources and folds in what this portal observed while
 * injecting their configuration, so both halves of the handshake can be shown
 * in one place.
 *
 * @author Gregory Amerson
 */
public class ClientExtensionStatusReader {

	public ClientExtensionStatusReader(
		ClientExtensionConfigurationErrorRegistry
			clientExtensionConfigurationErrorRegistry,
		KubernetesClient kubernetesClient, String namespace) {

		_clientExtensionConfigurationErrorRegistry =
			clientExtensionConfigurationErrorRegistry;
		_kubernetesClient = kubernetesClient;
		_namespace = namespace;
	}

	public List<ClientExtensionStatus> getClientExtensionStatuses() {
		List<Map<String, Object>> items = _listItems();

		List<ClientExtensionStatus> clientExtensionStatuses = new ArrayList<>(
			items.size());

		for (Map<String, Object> item : items) {
			ClientExtensionStatus clientExtensionStatus = _toStatus(item);

			if (clientExtensionStatus != null) {
				clientExtensionStatuses.add(clientExtensionStatus);
			}
		}

		return clientExtensionStatuses;
	}

	private List<ClientExtensionStatusCondition> _getConditions(
		Map<String, Object> status) {

		List<ClientExtensionStatusCondition> conditions = new ArrayList<>();

		for (Object object : _getList(status, "conditions")) {
			Map<String, Object> condition = _toMap(object);

			if (condition == null) {
				continue;
			}

			conditions.add(
				new ClientExtensionStatusCondition(
					_toDate(_getString(condition, "lastTransitionTime")),
					_getString(condition, "message"),
					_getString(condition, "reason"),
					_getString(condition, "status"),
					_getString(condition, "type")));
		}

		return conditions;
	}

	private List<Object> _getList(Map<String, Object> map, String key) {
		if (map == null) {
			return Collections.emptyList();
		}

		Object value = map.get(key);

		if (value instanceof List) {
			return (List<Object>)value;
		}

		return Collections.emptyList();
	}

	private Map<String, Object> _getMap(Map<String, Object> map, String key) {
		if (map == null) {
			return Collections.emptyMap();
		}

		Object value = map.get(key);

		if (value instanceof Map) {
			return (Map<String, Object>)value;
		}

		return Collections.emptyMap();
	}

	private String _getString(Map<String, Object> map, String key) {
		if (map == null) {
			return StringPool.BLANK;
		}

		return GetterUtil.getString(map.get(key));
	}

	private List<Map<String, Object>> _listItems() {
		Map<String, Object> result = null;

		try {
			result = _kubernetesClient.customResource(
				ClientExtensionResource.CONTEXT
			).list();
		}
		catch (Exception exception) {
			if (_log.isDebugEnabled()) {
				_log.debug(
					"Unable to list client extensions across namespaces",
					exception);
			}

			try {
				result = _kubernetesClient.customResource(
					ClientExtensionResource.CONTEXT
				).list(
					_namespace
				);
			}
			catch (Exception namespaceException) {
				if (_log.isWarnEnabled()) {
					_log.warn(
						"Unable to list client extensions in namespace " +
							_namespace,
						namespaceException);
				}

				return Collections.emptyList();
			}
		}

		List<Map<String, Object>> items = new ArrayList<>();

		for (Object object : _getList(result, "items")) {
			Map<String, Object> item = _toMap(object);

			if (item != null) {
				items.add(item);
			}
		}

		return items;
	}

	private Date _toDate(String value) {
		if (Validator.isNull(value)) {
			return null;
		}

		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
			"yyyy-MM-dd'T'HH:mm:ss'Z'");

		simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

		try {
			return simpleDateFormat.parse(value);
		}
		catch (ParseException parseException) {
			if (_log.isDebugEnabled()) {
				_log.debug("Unable to parse date " + value, parseException);
			}

			return null;
		}
	}

	private Map<String, Object> _toMap(Object object) {
		if (object instanceof Map) {
			return (Map<String, Object>)object;
		}

		return null;
	}

	private ClientExtensionStatus _toStatus(Map<String, Object> item) {
		Map<String, Object> metadata = _getMap(item, "metadata");
		Map<String, Object> spec = _getMap(item, "spec");
		Map<String, Object> status = _getMap(item, "status");

		String serviceId = _getString(spec, "serviceId");

		if (Validator.isNull(serviceId)) {
			serviceId = _getString(metadata, "name");
		}

		String virtualInstanceId = _getString(spec, "virtualInstanceId");

		Map<String, Object> workload = _getMap(spec, "workload");

		List<ClientExtensionConfigurationError> configurationErrors =
			_clientExtensionConfigurationErrorRegistry.getConfigurationErrors(
				serviceId, virtualInstanceId);

		List<ClientExtensionStatusCondition> conditions = _getConditions(
			status);

		return new ClientExtensionStatus(
			conditions, configurationErrors,
			_clientExtensionConfigurationErrorRegistry.getConfigurationPids(
				serviceId, virtualInstanceId),
			_getString(metadata, "namespace"), _getString(status, "phase"),
			_getString(spec, "projectName"), serviceId, virtualInstanceId,
			_getString(workload, "kind"), _getString(status, "workloadName"));
	}

	private static final Log _log = LogFactoryUtil.getLog(
		ClientExtensionStatusReader.class);

	private final ClientExtensionConfigurationErrorRegistry
		_clientExtensionConfigurationErrorRegistry;
	private final KubernetesClient _kubernetesClient;
	private final String _namespace;

}