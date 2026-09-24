/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.k8s.agent.status;

import java.io.Serializable;
import java.util.Date;

/**
 * One condition reported on a client extension, mirroring the shape Kubernetes
 * uses so that conditions published by the operator and conditions observed
 * inside the portal can be displayed side by side.
 *
 * @author Gregory Amerson
 */
public class ClientExtensionStatusCondition implements Serializable {

	public static final String CONFIGURATION_ACCEPTED = "ConfigurationAccepted";

	public static final String DELIVERED = "Delivered";

	public static final String PROVISIONED = "Provisioned";

	public static final String READY = "Ready";

	public static final String STATUS_FALSE = "False";

	public static final String STATUS_TRUE = "True";

	public static final String STATUS_UNKNOWN = "Unknown";

	public ClientExtensionStatusCondition(
		Date lastTransitionDate, String message, String reason, String status,
		String type) {

		_lastTransitionDate = lastTransitionDate;
		_message = message;
		_reason = reason;
		_status = status;
		_type = type;
	}

	public Date getLastTransitionDate() {
		if (_lastTransitionDate == null) {
			return null;
		}

		return new Date(_lastTransitionDate.getTime());
	}

	public String getMessage() {
		return _message;
	}

	public String getReason() {
		return _reason;
	}

	public String getStatus() {
		return _status;
	}

	public String getType() {
		return _type;
	}

	public boolean isFalse() {
		return STATUS_FALSE.equals(_status);
	}

	public boolean isTrue() {
		return STATUS_TRUE.equals(_status);
	}

	private static final long serialVersionUID = 1L;

	private final Date _lastTransitionDate;
	private final String _message;
	private final String _reason;
	private final String _status;
	private final String _type;

}
