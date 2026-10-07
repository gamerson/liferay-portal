/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.environment;

import java.io.InputStream;

import java.security.KeyPair;

import org.osgi.annotation.versioning.ProviderType;

/**
 * Describes the deployment this node runs in, and stores the state that
 * license activation keeps for it in the way that deployment persists state.
 *
 * <p>
 * Exactly one implementation applies to a node. The self hosted default is
 * always registered at the lowest ranking, and a deployment specific
 * implementation, such as the Kubernetes one, replaces it once that deployment
 * is configured. Callers must look the service up each time they use it,
 * rather than holding on to the instance they first received, so that a call
 * never reaches the default after the deployment specific implementation
 * arrives.
 * </p>
 *
 * <p>
 * Every node of the deployment shares the stored state, so an implementation
 * must tolerate the same call arriving from several nodes at once.
 * </p>
 *
 * @author Gregory Amerson
 */
@ProviderType
public interface LiferayEnvironment {

	/**
	 * Returns the entitlements last stored with {@link
	 * #storeEntitlements(Entitlements)}, or <code>null</code> when none were
	 * stored.
	 */
	public Entitlements getEntitlements() throws Exception;

	/**
	 * Returns the identifier the provisioning service binds an activation to.
	 * The deployment assigns it, never the caller, and it is stable for the
	 * life of the deployment.
	 */
	public String getEnvironmentId() throws Exception;

	/**
	 * Returns the human readable name the provisioning service shows for this
	 * environment.
	 */
	public String getEnvironmentName() throws Exception;

	/**
	 * Returns the key pair that signs requests to the provisioning service,
	 * creating and storing it on first use. Every node of the deployment
	 * receives the same key pair.
	 */
	public KeyPair getKeyPair() throws Exception;

	/**
	 * Returns the status last reported with {@link
	 * #updateStatus(LiferayEnvironmentStatus)}, or an empty status when none
	 * was reported.
	 */
	public LiferayEnvironmentStatus getStatus() throws Exception;

	public LiferayEnvironmentType getType();

	/**
	 * Stores an add-on's package where every node of the deployment installs
	 * it from.
	 */
	public void storeAddOn(String fileName, InputStream inputStream)
		throws Exception;

	public void storeEntitlements(Entitlements entitlements) throws Exception;

	/**
	 * Reports the status to whatever enforces it outside of the portal. The
	 * status replaces the previous one as a whole, so a field left unset is
	 * cleared rather than kept; read the current status with {@link
	 * #getStatus()} and change it, rather than building a partial one.
	 */
	public void updateStatus(LiferayEnvironmentStatus liferayEnvironmentStatus)
		throws Exception;

}