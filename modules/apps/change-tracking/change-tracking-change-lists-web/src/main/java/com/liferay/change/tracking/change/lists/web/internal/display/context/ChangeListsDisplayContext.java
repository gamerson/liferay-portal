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

package com.liferay.change.tracking.change.lists.web.internal.display.context;

import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.template.soy.util.SoyContext;
import com.liferay.portal.template.soy.util.SoyContextFactoryUtil;

import java.util.HashMap;
import java.util.Map;

import javax.portlet.RenderResponse;

import javax.servlet.http.HttpServletRequest;

/**
 * @author Máté Thurzó
 */
public class ChangeListsDisplayContext {

	public ChangeListsDisplayContext(
		HttpServletRequest httpServletRequest, RenderResponse renderResponse) {

		_httpServletRequest = httpServletRequest;
		_renderResponse = renderResponse;

		_themeDisplay = (ThemeDisplay)_httpServletRequest.getAttribute(
			WebKeys.THEME_DISPLAY);
	}

	public SoyContext getChangeListsContext() {
		SoyContext soyContext = SoyContextFactoryUtil.createSoyContext();

		soyContext.put("portletNamespace", _renderResponse.getNamespace());
		soyContext.put(
			"spritemap",
			_themeDisplay.getPathThemeImages() + "/lexicon/icons.svg");

		// Translations

		Map<String, String> translations = new HashMap<>();

		translations.put(
			"description",
			LanguageUtil.get(_httpServletRequest, "description"));
		translations.put(
			"production-view",
			LanguageUtil.get(_httpServletRequest, "production-view"));
		translations.put(
			"published-by",
			LanguageUtil.get(_httpServletRequest, "published-by"));
		translations.put(
			"published-change-list",
			LanguageUtil.get(_httpServletRequest, "published-change-list"));

		soyContext.put("translations", translations);

		// URLs

		soyContext.put(
			"urlProductionCollection",
			_themeDisplay.getPortalURL() +
				"/o/change-tracking/collections/production/" +
					_themeDisplay.getCompanyId());
		soyContext.put("urlProductionView", _themeDisplay.getPortalURL());

		return soyContext;
	}

	private final HttpServletRequest _httpServletRequest;
	private final RenderResponse _renderResponse;
	private final ThemeDisplay _themeDisplay;

}