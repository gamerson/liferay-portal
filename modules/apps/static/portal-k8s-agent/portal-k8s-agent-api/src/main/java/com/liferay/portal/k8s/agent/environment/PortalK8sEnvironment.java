/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.k8s.agent.environment;

/**
 * Identifies the Kubernetes namespace this node runs in.
 *
 * @author Gregory Amerson
 */
public interface PortalK8sEnvironment {

	/**
	 * Returns the UID that the Kubernetes API server assigned to the namespace
	 * this node runs in, or <code>null</code> when the UID cannot be read. The
	 * API server assigns the UID when the namespace is created, rejects a UID
	 * supplied by a client, and never reuses one, which makes it a stable
	 * identifier for the lifetime of the namespace.
	 *
	 * <p>
	 * Reading the UID requires the <code>get</code> verb on the
	 * <code>namespaces</code> resource. A namespaced role is sufficient because
	 * the API server attributes the request to the namespace being read.
	 * </p>
	 */
	public String getEnvironmentId();

	/**
	 * Returns the name of the namespace this node runs in.
	 */
	public String getNamespace();

}