/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.k8s.agent.internal.status;

import com.liferay.petra.string.StringPool;
import com.liferay.portal.k8s.agent.status.ClientExtensionConfigurationError;
import com.liferay.portal.kernel.util.Validator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.component.annotations.Component;

/**
 * Remembers why a client extension's configuration payload was rejected.
 *
 * The portal otherwise reports injection failures only to the log, so a client
 * extension whose payload was refused looks delivered from outside while
 * nothing was applied. Entries are keyed by service and virtual instance and
 * are replaced wholesale on every apply, so a payload that starts working
 * clears its own errors.
 *
 * @author Gregory Amerson
 */
@Component(service = ClientExtensionConfigurationErrorRegistry.class)
public class ClientExtensionConfigurationErrorRegistry {

	/**
	 * Discards everything recorded for a client extension, called when its
	 * configuration is withdrawn.
	 */
	public void clear(String serviceId, String virtualInstanceId) {
		_errors.remove(_key(serviceId, virtualInstanceId));
		_pids.remove(_key(serviceId, virtualInstanceId));
	}

	public List<ClientExtensionConfigurationError> getConfigurationErrors(
		String serviceId, String virtualInstanceId) {

		return _copy(_errors.get(_key(serviceId, virtualInstanceId)));
	}

	public List<String> getConfigurationPids(
		String serviceId, String virtualInstanceId) {

		return _copy(_pids.get(_key(serviceId, virtualInstanceId)));
	}

	/**
	 * Returns every recorded error as a flat map of service key to errors, for
	 * publishing back to Kubernetes.
	 */
	public Map<String, List<ClientExtensionConfigurationError>> getErrors() {
		return Collections.unmodifiableMap(_errors);
	}

	public void recordApplyError(
		String configMapName, String pid, String serviceId,
		String virtualInstanceId, Throwable throwable) {

		_record(
			new ClientExtensionConfigurationError(
				configMapName, new Date(), _message(throwable),
				ClientExtensionConfigurationError.PHASE_APPLY, pid),
			serviceId, virtualInstanceId);
	}

	public void recordParseError(
		String configMapName, String message, String serviceId,
		String virtualInstanceId) {

		_record(
			new ClientExtensionConfigurationError(
				configMapName, new Date(), message,
				ClientExtensionConfigurationError.PHASE_PARSE, null),
			serviceId, virtualInstanceId);
	}

	public void recordSuccess(
		String pid, String serviceId, String virtualInstanceId) {

		List<String> pids = _pids.computeIfAbsent(
			_key(serviceId, virtualInstanceId),
			key -> Collections.synchronizedList(new ArrayList<>()));

		if (!pids.contains(pid)) {
			pids.add(pid);
		}
	}

	/**
	 * Drops the previous result for a client extension so that one pass over
	 * its payload produces one complete picture rather than an accumulation.
	 */
	public void startApply(String serviceId, String virtualInstanceId) {
		clear(serviceId, virtualInstanceId);
	}

	private <T> List<T> _copy(List<T> list) {
		if (list == null) {
			return Collections.emptyList();
		}

		synchronized (list) {
			return new ArrayList<>(list);
		}
	}

	private String _key(String serviceId, String virtualInstanceId) {
		return serviceId + StringPool.SLASH + virtualInstanceId;
	}

	private String _message(Throwable throwable) {
		if (throwable == null) {
			return StringPool.BLANK;
		}

		String message = throwable.getMessage();

		if (Validator.isNull(message)) {
			Class<?> clazz = throwable.getClass();

			message = clazz.getName();
		}

		Throwable cause = throwable.getCause();

		if ((cause != null) && (cause != throwable)) {
			return message + ": " + _message(cause);
		}

		return message;
	}

	private void _record(
		ClientExtensionConfigurationError clientExtensionConfigurationError,
		String serviceId, String virtualInstanceId) {

		if (Validator.isNull(serviceId) || Validator.isNull(virtualInstanceId)) {
			return;
		}

		List<ClientExtensionConfigurationError> errors =
			_errors.computeIfAbsent(
				_key(serviceId, virtualInstanceId),
				key -> Collections.synchronizedList(new ArrayList<>()));

		synchronized (errors) {
			if (errors.size() < _MAX_ERRORS) {
				errors.add(clientExtensionConfigurationError);
			}
		}
	}

	private static final int _MAX_ERRORS = 25;

	private final Map<String, List<ClientExtensionConfigurationError>> _errors =
		new ConcurrentHashMap<>();
	private final Map<String, List<String>> _pids = new ConcurrentHashMap<>();

}
