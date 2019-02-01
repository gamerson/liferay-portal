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

package com.liferay.apio.architect.annotation;

/**
 * Defines the mode in which a field of a type can be used.
 *
 * @author Víctor Galán
 */
public enum FieldMode {

	/**
	 * Indicates that a field should only be used when instantiating the
	 * interface from the HTTP request body. A field with this mode is ignored
	 * when representing the type in any format.
	 *
	 * <p>
	 * This attribute is the opposite of the write-only attribute.
	 * </p>
	 *
	 * @review
	 */
	READ_ONLY,

	/**
	 * Indicates that a field should be used when instantiating the interface
	 * from the HTTP request body, and when representing the type. Note that
	 * this is effectively a combination of the read-only and write-only
	 * attributes.
	 *
	 * @review
	 */
	READ_WRITE,

	/**
	 * Indicates that a field should only be used when representing the type in
	 * any format. A field with this mode is ignored when instantiating the
	 * interface from the HTTP request body.
	 *
	 * <p>
	 * This attribute is the opposite of the read-only attribute.
	 * </p>
	 *
	 * @review
	 */
	WRITE_ONLY

}