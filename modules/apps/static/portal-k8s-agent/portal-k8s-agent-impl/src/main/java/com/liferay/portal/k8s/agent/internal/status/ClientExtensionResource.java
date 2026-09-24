/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.k8s.agent.internal.status;

import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;

/**
 * Identifies the ClientExtension resource the operator reconciles.
 *
 * @author Gregory Amerson
 */
public class ClientExtensionResource {

	public static final CustomResourceDefinitionContext CONTEXT =
		new CustomResourceDefinitionContext.Builder(
		).withGroup(
			"cx.liferay.com"
		).withKind(
			"ClientExtension"
		).withName(
			"clientextensions.cx.liferay.com"
		).withPlural(
			"clientextensions"
		).withScope(
			"Namespaced"
		).withVersion(
			"v1alpha1"
		).build();

}