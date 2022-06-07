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
 * @author Iván Zaera Avellón
 */
@CETType(name="themeJS", description="This is some description of themeJS type")
@ProviderType
public interface CETThemeJS {

	@CETProperty(type="list", defaultValue="theme.js", description="List of JS urls to load in theme")
	public String getURLs();

}