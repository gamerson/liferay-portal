/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.k8s.agent.status;

import java.io.Serializable;
import java.util.Date;

/**
 * A failure the portal hit while injecting a client extension's configuration
 * payload. Without this, a rejected payload is visible only in the portal log,
 * and the client extension appears to be delivered while nothing was applied.
 *
 * @author Gregory Amerson
 */
public class ClientExtensionConfigurationError implements Serializable {

	/**
	 * The payload could not be parsed into configurations at all.
	 */
	public static final String PHASE_PARSE = "Parse";

	/**
	 * A single configuration in the payload could not be applied.
	 */
	public static final String PHASE_APPLY = "Apply";

	public ClientExtensionConfigurationError(
		String configMapName, Date date, String message, String phase,
		String pid) {

		_configMapName = configMapName;
		_date = date;
		_message = message;
		_phase = phase;
		_pid = pid;
	}

	public String getConfigMapName() {
		return _configMapName;
	}

	public Date getDate() {
		if (_date == null) {
			return null;
		}

		return new Date(_date.getTime());
	}

	public String getMessage() {
		return _message;
	}

	public String getPhase() {
		return _phase;
	}

	public String getPid() {
		return _pid;
	}

	private static final long serialVersionUID = 1L;

	private final String _configMapName;
	private final Date _date;
	private final String _message;
	private final String _phase;
	private final String _pid;

}
