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

import static org.apache.commons.lang3.reflect.MethodUtils.getMethodsListWithAnnotation;

import com.liferay.apio.architect.annotation.Id;
import com.liferay.apio.architect.annotation.Vocabulary.BidirectionalModel;
import com.liferay.apio.architect.annotation.Vocabulary.Field;
import com.liferay.apio.architect.annotation.Vocabulary.LinkTo;
import com.liferay.apio.architect.annotation.Vocabulary.RelativeURL;
import com.liferay.apio.architect.annotation.Vocabulary.Type;
import com.liferay.apio.architect.functional.Try;
import com.liferay.apio.architect.identifier.Identifier;
import com.liferay.apio.architect.internal.annotation.representor.processor.ParsedType.Builder;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;

import java.util.List;
import java.util.Optional;

/**
 * Class responsible of extracting the data of an annotated type and construct a
 * {@link ParsedType} with that information.
 *
 * @author Víctor Galán
 * @review
 */
public class TypeProcessor {

	/**
	 * Returns a parsed type created from the type provided.
	 *
	 * @param  typeClass the annotated class
	 * @return the parsed type
	 * @review
	 */
	public static ParsedType processType(
		Class<? extends Identifier> typeClass) {

		return _processType(typeClass, false);
	}

	private static void _addFields(
		Builder builder, Method method, Class<?> returnType) {

		if (returnType.isAnnotationPresent(Type.class)) {
			ParsedType parsedType = _processType(returnType, true);

			builder.addParsedType(new FieldData<>(method, parsedType));
		}
		else {
			FieldData fieldData = new FieldData<>(method, returnType);

			builder.addFieldData(fieldData);
		}
	}

	private static void _addListType(
		Builder builder, Method method, Class<?> listClass) {

		if (listClass.isAnnotationPresent(Type.class)) {
			ParsedType parsedType = _processType(listClass, true);

			builder.addListParsedType(new FieldData<>(method, parsedType));
		}
		else {
			FieldData<Class<?>> listFieldData = new FieldData<>(
				method, listClass);

			builder.addListFieldData(listFieldData);
		}
	}

	private static void _addOptionalType(Builder builder, Method method) {
		java.lang.reflect.Type type = _getGenericType(method);

		if (type instanceof ParameterizedType) {
			ParameterizedType innerParametrizedType = (ParameterizedType)type;

			if (innerParametrizedType.getRawType() == List.class) {
				_addListType(
					builder, method,
					(Class<?>)
						innerParametrizedType.getActualTypeArguments()[0]);
			}
		}
		else {
			Class<?> returnTypeClass = (Class<?>)type;

			_addFields(builder, method, returnTypeClass);
		}
	}

	private static java.lang.reflect.Type _getGenericType(Method method) {
		ParameterizedType parameterizedType =
			(ParameterizedType)method.getGenericReturnType();

		return parameterizedType.getActualTypeArguments()[0];
	}

	private static void _processMethod(Builder builder, Method method) {
		LinkTo linkTo = method.getAnnotation(LinkTo.class);

		if (linkTo != null) {
			builder.addLinkToFieldData(new FieldData<>(method, linkTo));

			return;
		}

		RelativeURL relativeURL = method.getAnnotation(RelativeURL.class);

		if (relativeURL != null) {
			FieldData<RelativeURL> relativeURLFieldData = new FieldData<>(
				method, relativeURL);

			builder.addRelativeURLFieldData(relativeURLFieldData);

			return;
		}

		BidirectionalModel bidirectionalModel = method.getAnnotation(
			BidirectionalModel.class);

		if (bidirectionalModel != null) {
			FieldData<BidirectionalModel> bidirectionalFieldData =
				new FieldData<>(method, bidirectionalModel);

			builder.addBidirectionalFieldData(bidirectionalFieldData);

			return;
		}

		Class<?> returnTypeClass = method.getReturnType();

		if (returnTypeClass == Optional.class) {
			_addOptionalType(builder, method);
		}
		else if (returnTypeClass == List.class) {
			_addListType(builder, method, (Class<?>)_getGenericType(method));
		}
		else {
			_addFields(builder, method, returnTypeClass);
		}
	}

	private static ParsedType _processType(Class<?> typeClass, boolean nested) {
		Type type = typeClass.getAnnotation(Type.class);

		Builder builder = new Builder(type, typeClass);

		if (!nested) {
			Method idMethod = Try.fromFallible(
				() -> getMethodsListWithAnnotation(typeClass, Id.class)
			).filter(
				methods -> !methods.isEmpty()
			).map(
				methods -> methods.get(0)
			).orElse(
				null
			);

			builder.idMethod(idMethod);
		}

		List<Method> methods = getMethodsListWithAnnotation(
			typeClass, Field.class);

		methods.forEach(method -> _processMethod(builder, method));

		return builder.build();
	}

}