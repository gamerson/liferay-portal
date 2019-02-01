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

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Locale;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.junit.Before;
import org.junit.Test;

/**
 * @author Víctor Galán
 * @author Rubén Pulido
 */
public class AcceptLanguageEmptyFilterTest {

	@Before
	public void setUp() {
		_acceptLanguageEmptyFilter = new AcceptLanguageEmptyFilter();
		_containerRequestContext = mock(ContainerRequestContext.class);
	}

	@Test
	public void testFilterRequestWithEmptyAcceptLanguageHeader() {
		MultivaluedMap<String, String> multivaluedMap =
			new MultivaluedHashMap<>();

		multivaluedMap.put(HttpHeaders.ACCEPT_LANGUAGE, emptyList());

		when(_containerRequestContext.getHeaders()).thenReturn(multivaluedMap);

		_acceptLanguageEmptyFilter.filter(_containerRequestContext);

		assertThat(
			multivaluedMap.get(HttpHeaders.ACCEPT_LANGUAGE), nullValue());
	}

	@Test
	public void testFilterRequestWithNonemptyAcceptLanguageHeader() {
		MultivaluedMap<String, String> multivaluedMap =
			new MultivaluedHashMap<>();

		List<String> locales = singletonList(Locale.US.toLanguageTag());

		multivaluedMap.put(HttpHeaders.ACCEPT_LANGUAGE, locales);

		when(_containerRequestContext.getHeaders()).thenReturn(multivaluedMap);

		_acceptLanguageEmptyFilter.filter(_containerRequestContext);

		assertThat(
			multivaluedMap.get(HttpHeaders.ACCEPT_LANGUAGE), notNullValue());
	}

	private AcceptLanguageEmptyFilter _acceptLanguageEmptyFilter;
	private ContainerRequestContext _containerRequestContext;

}