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

package com.liferay.client.extension.type;

import org.osgi.annotation.versioning.ProviderType;

import com.liferay.client.extension.type.annotation.CETProperty;
import com.liferay.client.extension.type.annotation.CETType;

/**
 * @author Brian Wing Shun Chan
 */
@CETType(name="iframe", description="This is some description of iframe type")
@ProviderType
public interface CETIFrame {

	@CETProperty(defaultValue="friendlyurl", description="This is some description of friendly url mapping property")
	public String getFriendlyURLMapping();

	@CETProperty(defaultValue="category.remote-apps", description="Portlet category name")
	public String getPortletCategoryName();

	@CETProperty(defaultValue="https://example.com", description="URL to load in iframe")
	public String getURL();

	@CETProperty(name="instanceable", description="Set true if extension is instanceable")
	public boolean isInstanceable();

}