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

package com.liferay.apio.architect.internal.jaxrs.filter;

import java.util.List;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;

import org.osgi.service.component.annotations.Component;

/**
 * This filter ensures that when a request contains a HTTP Accept-Language
 * header with an empty value the header is removed from the request.
 * This prevents a 500 error from happening and results in the default locale
 * being used.
 *
 * @author Víctor Galán
 * @author Rubén Pulido
 * @review
 */
@Component(
	immediate = true,
	property = {
		"osgi.jaxrs.application.select=(liferay.apio.architect.application=true)",
		"osgi.jaxrs.extension=true",
		"osgi.jaxrs.name=Filter.AcceptLanguageEmptyFilter"
	},
	service = ContainerRequestFilter.class
)
@PreMatching
public class AcceptLanguageEmptyFilter implements ContainerRequestFilter {

	@Override
	public void filter(ContainerRequestContext requestContext) {
		MultivaluedMap<String, String> headersMultivaluedMap =
			requestContext.getHeaders();

		List<String> values = headersMultivaluedMap.get(
			HttpHeaders.ACCEPT_LANGUAGE);

		if ((values != null) && values.isEmpty()) {
			headersMultivaluedMap.remove(HttpHeaders.ACCEPT_LANGUAGE);
		}
	}

}