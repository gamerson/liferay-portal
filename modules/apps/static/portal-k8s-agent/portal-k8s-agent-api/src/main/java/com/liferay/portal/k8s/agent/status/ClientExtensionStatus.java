/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.k8s.agent.status;

import java.io.Serializable;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * The whole state of one client extension, gathered from both halves of the
 * deployment handshake: what the operator reports about delivering the
 * configuration and running the workload, and what the portal observed while
 * injecting that configuration.
 *
 * @author Gregory Amerson
 */
public class ClientExtensionStatus implements Serializable {

	public static final String PHASE_DEGRADED = "Degraded";

	public static final String PHASE_PENDING = "Pending";

	public static final String PHASE_READY = "Ready";

	public static final String PHASE_UNKNOWN = "Unknown";

	public ClientExtensionStatus(
		List<ClientExtensionStatusCondition> conditions,
		List<ClientExtensionConfigurationError> configurationErrors,
		List<String> configurationPids, String namespace, String phase,
		String projectName, String serviceId, String virtualInstanceId,
		String workloadKind, String workloadName) {

		_conditions = _emptyIfNull(conditions);
		_configurationErrors = _emptyIfNull(configurationErrors);
		_configurationPids = _emptyIfNull(configurationPids);
		_namespace = namespace;
		_phase = phase;
		_projectName = projectName;
		_serviceId = serviceId;
		_virtualInstanceId = virtualInstanceId;
		_workloadKind = workloadKind;
		_workloadName = workloadName;
	}

	public ClientExtensionStatusCondition getCondition(String type) {
		for (ClientExtensionStatusCondition condition : _conditions) {
			if (Objects.equals(condition.getType(), type)) {
				return condition;
			}
		}

		return null;
	}

	public List<ClientExtensionStatusCondition> getConditions() {
		return _conditions;
	}

	public List<ClientExtensionConfigurationError> getConfigurationErrors() {
		return _configurationErrors;
	}

	public List<String> getConfigurationPids() {
		return _configurationPids;
	}

	public String getNamespace() {
		return _namespace;
	}

	/**
	 * The point in the handshake that failed, or an empty string when nothing
	 * has failed. This is what a devops administrator scans for.
	 */
	public String getFailedStage() {
		if (!_configurationErrors.isEmpty()) {
			return ClientExtensionStatusCondition.CONFIGURATION_ACCEPTED;
		}

		for (String type :
				new String[] {
					ClientExtensionStatusCondition.DELIVERED,
					ClientExtensionStatusCondition.PROVISIONED,
					ClientExtensionStatusCondition.READY
				}) {

			ClientExtensionStatusCondition condition = getCondition(type);

			if ((condition != null) && condition.isFalse()) {
				return type;
			}
		}

		return "";
	}

	public String getPhase() {
		if ((_phase == null) || _phase.isEmpty()) {
			return PHASE_UNKNOWN;
		}

		return _phase;
	}

	public String getProjectName() {
		return _projectName;
	}

	public String getServiceId() {
		return _serviceId;
	}

	public String getVirtualInstanceId() {
		return _virtualInstanceId;
	}

	public String getWorkloadKind() {
		return _workloadKind;
	}

	public String getWorkloadName() {
		return _workloadName;
	}

	public boolean isFailed() {
		String failedStage = getFailedStage();

		return !failedStage.isEmpty();
	}

	private <T> List<T> _emptyIfNull(List<T> list) {
		if (list == null) {
			return Collections.emptyList();
		}

		return Collections.unmodifiableList(list);
	}

	private static final long serialVersionUID = 1L;

	private final List<ClientExtensionStatusCondition> _conditions;
	private final List<ClientExtensionConfigurationError> _configurationErrors;
	private final List<String> _configurationPids;
	private final String _namespace;
	private final String _phase;
	private final String _projectName;
	private final String _serviceId;
	private final String _virtualInstanceId;
	private final String _workloadKind;
	private final String _workloadName;

}
