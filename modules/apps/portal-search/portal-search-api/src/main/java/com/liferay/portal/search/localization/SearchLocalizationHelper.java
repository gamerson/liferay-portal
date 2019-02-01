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

package com.liferay.portal.search.localization;

import com.liferay.portal.kernel.search.Document;
import com.liferay.portal.kernel.search.SearchContext;

import java.util.Locale;
import java.util.Map;

/**
 * @author Adam Brandizzi
 */
public interface SearchLocalizationHelper {

	public void addLocalizedField(
		Document document, String field, Locale defaultLocale,
		Map<Locale, String> map);

	public Locale[] getLocales(SearchContext searchContext);

	public String[] getLocalizedFieldNames(
		String[] prefixes, SearchContext searchContext);

}