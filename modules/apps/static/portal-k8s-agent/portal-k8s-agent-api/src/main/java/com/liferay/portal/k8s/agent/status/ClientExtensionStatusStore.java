/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.k8s.agent.status;

import java.util.List;

/**
 * Reads the state of every client extension known to this portal, merging what
 * the operator publishes on the ClientExtension resource with what the portal
 * itself observed while injecting the configuration payload.
 *
 * @author Gregory Amerson
 */
public interface ClientExtensionStatusStore {

	/**
	 * Returns every client extension, or an empty list when the portal is not
	 * running under the Kubernetes agent.
	 */
	public List<ClientExtensionStatus> getClientExtensionStatuses();

	/**
	 * Returns whether this portal is running under the Kubernetes agent, so
	 * the user interface can explain an empty list.
	 */
	public boolean isAvailable();

}
