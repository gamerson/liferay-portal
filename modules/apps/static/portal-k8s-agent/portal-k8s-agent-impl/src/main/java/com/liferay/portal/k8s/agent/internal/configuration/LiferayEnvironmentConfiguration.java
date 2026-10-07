/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.k8s.agent.internal.configuration;

import aQute.bnd.annotation.metatype.Meta;

import com.liferay.portal.configuration.metatype.annotations.ExtendedObjectClassDefinition;

/**
 * Identifies the LiferayEnvironment resource that the DXP operator manages for
 * this workload. The workload's chart supplies it, and its presence is what
 * makes the portal treat itself as running in Kubernetes. The connection to
 * the API server comes from the pod's own service account.
 *
 * @author Gregory Amerson
 */
@ExtendedObjectClassDefinition(generateUI = false)
@Meta.OCD(
	id = "com.liferay.portal.k8s.agent.internal.configuration.LiferayEnvironmentConfiguration"
)
public interface LiferayEnvironmentConfiguration {

	@Meta.AD(required = false, type = Meta.Type.String)
	public String addOnsDir();

	@Meta.AD(type = Meta.Type.String)
	public String liferayEnvironmentName();

}