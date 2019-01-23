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

package com.liferay.apio.architect.internal.jaxrs.resource;

import static io.vavr.API.$;

import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;

import static javax.ws.rs.core.Response.Status.METHOD_NOT_ALLOWED;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

import com.liferay.apio.architect.internal.annotation.Action;
import com.liferay.apio.architect.internal.annotation.Action.Error;
import com.liferay.apio.architect.internal.annotation.Action.Error.NotAllowed;
import com.liferay.apio.architect.internal.annotation.Action.NoContent;
import com.liferay.apio.architect.internal.annotation.ActionManager;
import com.liferay.apio.architect.internal.documentation.Documentation;
import com.liferay.apio.architect.internal.entrypoint.EntryPoint;

import io.vavr.control.Either;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Declares the resource from which all of the APIs originate.
 *
 * @author Alejandro Hernández
 * @review
 */
@Component(service = RootResource.class)
public class RootResource {

	/**
	 * Returns the application schema.
	 *
	 * @review
	 */
	@GET
	@Path("/doc")
	public Documentation documentation() {
		return _actionManager.getDocumentation(_request);
	}

	/**
	 * Returns the entry point of the application.
	 *
	 * @review
	 */
	@GET
	@Path("/")
	public EntryPoint home() {
		return _actionManager.getEntryPoint();
	}

	/**
	 * Returns the nested resource that handles the actions provided by the
	 * {@link ActionManager}.
	 *
	 * @review
	 */
	@Path("/{param}")
	public NestedResource nestedResource(@PathParam("param") String param) {
		return NestedResource.Builder.params(
			singletonList(param)
		).responseFunction(
			this::_getResponse
		).allowedMethodsFunction(
			__ -> emptySet()
		).build();
	}

	private Response _getResponse(String method, List<String> params) {
		Either<Error, Action> either = _actionManager.getAction(method, params);

		return either.fold(
			error -> {
				if (error instanceof Action.Error.NotAllowed) {
					return Response.status(
						METHOD_NOT_ALLOWED
					).allow(
						((Action.Error.NotAllowed)error).getAllowedMethods()
					).build();
				}

				return _notFoundResponse;
			},
			action -> {
				Object result = action.execute(_request);

				if (action instanceof Action.NoContent) {
					return _noContentResponse;
				}

				return Response.ok(
					result
				).build();
			});
	}

	private static final Response _noContentResponse = Response.noContent(
	).build();
	private static final Response _notFoundResponse = Response.status(
		NOT_FOUND
	).build();

	@Reference
	private ActionManager _actionManager;

	@Context
	private HttpServletRequest _request;

}