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

package com.liferay.apio.architect.internal.annotation.representor.processor;

import com.liferay.apio.architect.annotation.Vocabulary.Field;

import java.lang.reflect.Method;

import javax.validation.constraints.NotNull;

/**
 * Holds information about a field annotated with {@link Field}
 *
 * @author Víctor Galán
 * @review
 */
public class FieldData<T> {

	public FieldData(Method method, T data) {
		_field = method.getAnnotation(Field.class);
		_method = method;
		_required = method.isAnnotationPresent(NotNull.class);
		_data = data;
	}

	public T getData() {
		return _data;
	}

	/**
	 * Returns the field annotation extracted
	 *
	 * @return the field annotation
	 * @review
	 */
	public Field getField() {
		return _field;
	}

	/**
	 * Returns the field name
	 *
	 * @return the field name
	 * @review
	 */
	public String getFieldName() {
		return _field.value();
	}

	/**
	 * Returns the method in which the annotation was placed
	 *
	 * @return the method
	 * @review
	 */
	public Method getMethod() {
		return _method;
	}

	/**
	 * Returns the method name in which the annotation was placed
	 *
	 * @return the method name
	 * @review
	 */
	public String getMethodName() {
		return _method.getName();
	}

	public boolean isRequired() {
		return _required;
	}

	private final T _data;
	private final Field _field;
	private final Method _method;
	private final boolean _required;

}