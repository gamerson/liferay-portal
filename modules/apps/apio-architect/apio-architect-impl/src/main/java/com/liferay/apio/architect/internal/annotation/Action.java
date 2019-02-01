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

package com.liferay.apio.architect.internal.annotation;

import java.util.Set;

import javax.servlet.http.HttpServletRequest;

/**
 * Instances of this interface represent an API action. An action is just an
 * alias for a function that receives the HTTP request and returns an object.
 *
 * <p>Using this action class will have the same effects as using {@link Ok}.
 *
 * @author Alejandro Hernández
 * @review
 */
@FunctionalInterface
public interface Action {

	/**
	 * Executes the action and returns its output, using the current HTTP
	 * request.
	 *
	 * @review
	 */
	public Object execute(HttpServletRequest request);

	/**
	 * Types implementing this interface represent errors relative to an action.
	 *
	 * @review
	 */
	public interface Error {

		/**
		 * This error would be returned when an action exist for the provided
		 * params, but the HTTP method is not allowed.
		 *
		 * <p>
		 * The error contains the list of allowed HTTP methods, so they can be
		 * returned using the {@code Allow} header.
		 * </p>
		 *
		 * @review
		 */
		@FunctionalInterface
		public interface NotAllowed extends Error {

			/**
			 * The list of allowed HTTP methods.
			 *
			 * @review
			 */
			public Set<String> getAllowedMethods();

		}

		/**
		 * This error would be returned when no action is found for the provided
		 * params.
		 *
		 * @review
		 */
		public interface NotFound extends Error {
		}

	}

	/**
	 * Instances of this interface represent a no-content {@link Action}, a
	 * success action that does not return anything.
	 *
	 * @review
	 */
	@FunctionalInterface
	public interface NoContent extends Action {
	}

	/**
	 * Instances of this interface represent a common successful {@link Action},
	 * an action that return something.
	 *
	 * @review
	 */
	@FunctionalInterface
	public interface Ok extends Action {
	}

}