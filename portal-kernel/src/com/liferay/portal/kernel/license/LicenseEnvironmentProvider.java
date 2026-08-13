/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.kernel.license;

/**
 * Provides the identifier that a managed environment's control plane assigned
 * to the deployment this node belongs to. Licenses issued for an environment
 * rather than for a host may be validated against this value, in place of the
 * host name, IP address, and MAC address fingerprints that are unstable in an
 * orchestrated environment.
 *
 * <p>
 * Implementations are registered by whichever agent integrates with the
 * environment's control plane. The absence of an implementation means the node
 * does not run in a managed environment.
 * </p>
 *
 * @author Gregory Amerson
 */
@FunctionalInterface
public interface LicenseEnvironmentProvider {

	/**
	 * Returns the identifier the control plane assigned to this deployment's
	 * environment, or <code>null</code> when the environment cannot be
	 * identified. The value is assigned by the control plane, never by the
	 * deployment, and is stable for the life of the environment.
	 *
	 * <p>
	 * A <code>null</code> value means the environment is unknown right now, not
	 * that the environment is unlicensed. Callers validating an
	 * environment-bound license must treat it as a condition to retry rather
	 * than as a failed validation, because an environment may not be resolvable
	 * until the agent that integrates with the control plane is configured.
	 * </p>
	 */
	public String getEnvironmentId();

}
