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
@CETType(name="customElement", description="This is some description")
@ProviderType
public interface CETCustomElement {

	@CETProperty(name="cssURLs", type="list", defaultValue="custom-element.css")
	public String getCSSURLs();

	@CETProperty(defaultValue="friendly", description="This is some description")
	public String getFriendlyURLMapping();

	@CETProperty(name="htmlElementName", defaultValue="custom-element", description="This is some description of custom-element property")
	public String getHTMLElementName();

	@CETProperty(defaultValue="category.remote-apps", description="This is some description of custom-element property")
	public String getPortletCategoryName();

	@CETProperty(type="list", defaultValue="index.js", description="This is some description of custom-element property")
	public String getURLs();

	@CETProperty(name="instanceable", description="Set true if extension is instanceable")
	public boolean isInstanceable();

	@CETProperty(name="useESM", description="Set true is using ES modules")
	public boolean isUseESM();

}