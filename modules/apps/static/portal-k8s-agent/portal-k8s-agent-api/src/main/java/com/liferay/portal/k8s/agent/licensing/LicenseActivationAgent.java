/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.k8s.agent.licensing;

import java.io.InputStream;

/**
 * Reads and drives the <code>LiferayEnvironment</code> the DXP operator
 * manages this instance with. Every method returns <code>null</code> when the
 * portal does not run inside such an environment, so a caller can treat the
 * absence of an environment and the absence of an agent alike.
 *
 * @author Gregory Amerson
 */
public interface LicenseActivationAgent {

	public ActivationWorkflow getActivationWorkflow(String workflowName);

	public LiferayEnvironment getLiferayEnvironment();

	public String getOfflineActivationRequest();

	public ActivationWorkflow startOfflineActivation(String bundleFileName);

	public ActivationWorkflow startOnlineActivation(String activationCode);

	public String storeOfflineActivationBundle(
		String fileName, InputStream inputStream);

	public interface ActivationWorkflow {

		public boolean isFinished();

		public String name();

		public String phase();

		public String url();

	}

	public interface LiferayEnvironment {

		public String activatedAt();

		public String environmentName();

		public boolean isActivated();

		public boolean isOffline();

		public String name();

		public String namespace();

		public String offlineActivationBundle();

		public String phase();

	}

}