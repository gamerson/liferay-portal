/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.k8s.agent.configuration.v1;

import aQute.bnd.annotation.metatype.Meta;

import org.osgi.service.component.annotations.ComponentPropertyType;

/**
 * @author Raymond Augé
 */
@ComponentPropertyType
@Meta.OCD(
	factory = true,
	id = "com.liferay.k8s.agent.configuration.v1.K8sAgentConfiguration",
	localization = "content/Language", name = "k8s-agent-configuration-name"
)
public @interface K8sAgentConfiguration {

	@Meta.AD(
		deflt = "dxp.liferay.com/configs=true",
		description = "label-selector-description", name = "label-selector",
		required = false, type = Meta.Type.String
	)
	public String labelSelector();

	@Meta.AD(
		description = "namespace-description", name = "namespace",
		type = Meta.Type.String
	)
	public String namespace();

}