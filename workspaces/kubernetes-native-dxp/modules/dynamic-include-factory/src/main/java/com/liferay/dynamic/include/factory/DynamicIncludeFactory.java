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

package com.liferay.dynamic.include.factory;

import com.liferay.dynamic.include.factory.configuration.v1.DynamicIncludeConfiguration;
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
	configurationPid = "com.liferay.dynamic.include.factory.configuration.v1.DynamicIncludeConfiguration",
	configurationPolicy = ConfigurationPolicy.REQUIRE, immediate = true,
	service = DynamicInclude.class
)
public class DynamicIncludeFactory implements DynamicInclude {

	@Activate
	public DynamicIncludeFactory(
		@Reference Portal portal, Map<String, Object> properties) {

		_portal = portal;

		_dynamicIncludeConfiguration = ConfigurableUtil.createConfigurable(
			DynamicIncludeConfiguration.class, properties);

		Instant now = Instant.now();

		_lastModified = String.valueOf(now.toEpochMilli());

		boolean replaceTokens = false;

		for (String url : _dynamicIncludeConfiguration.urls()) {
			if (url.contains(_TOKEN)) {
				replaceTokens = true;
			}
		}

		_replaceTokens = replaceTokens;
	}

	@Override
	public void include(
			HttpServletRequest httpServletRequest,
			HttpServletResponse httpServletResponse, String key)
		throws IOException {

		PrintWriter printWriter = httpServletResponse.getWriter();

		String portalURL = _portal.getPortalURL(httpServletRequest);

		for (String url : _dynamicIncludeConfiguration.urls()) {
			if (url.indexOf(".js") > -1) {
				printWriter.println(
					StringBundler.concat(
						"<script charset=\"",
						_dynamicIncludeConfiguration.charset(),
						"\" data-senna-track=\"temporary\" src=\"",
						_replaceTokens(url, portalURL), "?t=", _lastModified,
						"\" type=\"text/javascript\"></script>"));
			}
			else if (url.indexOf(".css") > -1) {
				printWriter.println(
					StringBundler.concat(
						"<link charset=\"",
						_dynamicIncludeConfiguration.charset(),
						" data-senna-track=\"temporary\" href=\"",
						_replaceTokens(url, portalURL), "?t=", _lastModified,
						"\" rel=\"stylesheet\" type=\"text/css\"/>"));
			}
		}

		printWriter.flush();
	}

	@Override
	public void register(DynamicIncludeRegistry dynamicIncludeRegistry) {
		dynamicIncludeRegistry.register(_dynamicIncludeConfiguration.key());
	}

	private String _replaceTokens(String content, String portalURL) {
		if (!_replaceTokens) {
			return content;
		}

		return StringUtil.replace(content, _TOKEN, portalURL);
	}

	private static final String _TOKEN = "${portalURL}";

	private final DynamicIncludeConfiguration _dynamicIncludeConfiguration;
	private final String _lastModified;
	private final Portal _portal;
	private final boolean _replaceTokens;

}