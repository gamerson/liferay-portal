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

package com.liferay.design.pack.factory;

import com.liferay.design.pack.factory.DesignPackFactory;
import com.liferay.design.pack.factory.configuration.v1.DesignPackConfiguration;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringUtil;
import com.liferay.portal.configuration.metatype.bnd.util.ConfigurableUtil;
import com.liferay.portal.kernel.servlet.taglib.DynamicInclude;
import com.liferay.portal.kernel.util.Portal;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Raymond Augé
 */
@Component(
	configurationPid = "com.liferay.design.pack.factory.configuration.v1.DesignPackConfiguration",
	configurationPolicy = ConfigurationPolicy.REQUIRE,
	immediate = true,
	service = DynamicInclude.class
)
public class DesignPackFactory implements DynamicInclude {

	@Activate
	public DesignPackFactory(
		@Reference Portal portal, Map<String, Object> properties) {

		_portal = portal;

		_designPackConfiguration =
			ConfigurableUtil.createConfigurable(
				DesignPackConfiguration.class, properties);

		_lastModified = String.valueOf(Instant.now().toEpochMilli());

		if (_designPackConfiguration.clayCss().contains(_TOKEN) ||
				_designPackConfiguration.mainCss().contains(_TOKEN)) {

			_replaceTokens = true;
		}
		else {
			_replaceTokens = false;
		}
	}

	@Override
	public void include(
			HttpServletRequest httpServletRequest,
			HttpServletResponse httpServletResponse, String key)
		throws IOException {

		PrintWriter printWriter = httpServletResponse.getWriter();

		String portalURL = _portal.getPortalURL(httpServletRequest);

		printWriter.println(
			StringBundler.concat(
				"<script charset=\"", _designPackConfiguration.charset(),
				"\" data-senna-track=\"temporary\" type=\"text/javascript\">",
				"  document.querySelector('#liferayAUICSS').href = '",
				_replaceTokens(_designPackConfiguration.clayCss(), portalURL),
				"?t=", _lastModified, "';",
				"  document.querySelector('#liferayThemeCSS').href = '",
				_replaceTokens(_designPackConfiguration.mainCss(), portalURL),
				"?t=", _lastModified, "';", "</script>"
			));

		printWriter.flush();
	}

	@Override
	public void register(DynamicIncludeRegistry dynamicIncludeRegistry) {
		dynamicIncludeRegistry.register("/html/common/themes/top_head.jsp#post");
	}

	private String _replaceTokens(String content, String portalURL) {
		if (!_replaceTokens) {
			return content;
		}

		return StringUtil.replace(content, _TOKEN, portalURL);
	}

	private static final String _TOKEN = "${portalURL}";

	private final DesignPackConfiguration _designPackConfiguration;
	private final String _lastModified;
	private final Portal _portal;
	private final boolean _replaceTokens;

}