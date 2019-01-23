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

package com.liferay.apio.architect.internal.annotation.form;

import static com.liferay.apio.architect.annotation.FieldMode.READ_ONLY;
import static com.liferay.apio.architect.annotation.FieldMode.READ_WRITE;
import static com.liferay.apio.architect.annotation.Vocabulary.LinkTo.ResourceType.SINGLE;
import static com.liferay.apio.architect.internal.unsafe.Unsafe.unsafeCast;

import com.liferay.apio.architect.alias.IdentifierFunction;
import com.liferay.apio.architect.annotation.FieldMode;
import com.liferay.apio.architect.annotation.Vocabulary.Field;
import com.liferay.apio.architect.annotation.Vocabulary.LinkTo;
import com.liferay.apio.architect.annotation.Vocabulary.RelativeURL;
import com.liferay.apio.architect.file.BinaryFile;
import com.liferay.apio.architect.form.Form;
import com.liferay.apio.architect.form.Form.Builder;
import com.liferay.apio.architect.internal.annotation.representor.processor.FieldData;
import com.liferay.apio.architect.internal.annotation.representor.processor.ParsedType;
import com.liferay.apio.architect.internal.form.FormImpl;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Responsible of building a {@link Form} using a parsed type. This class will
 * create a Proxy of the type and build a form around it
 *
 * @author Víctor Galán
 * @review
 */
public class FormTransformer {

	/**
	 * Builds a form using a parsed type.
	 *
	 * @param  parsedType the parsed type
	 * @param  pathToIdentifierFunction function that extract the identifier
	 *         from a path given
	 * @param  nameFunction function that returns the name of the resource given
	 *         the className of the identifier
	 * @return the resulting form
	 * @review
	 */
	public static <T> Form<T> toForm(
		ParsedType parsedType, IdentifierFunction<?> pathToIdentifierFunction,
		Function<String, Optional<String>> nameFunction) {

		FormImpl.BuilderImpl<T> formBuilder = new FormImpl.BuilderImpl<>(
			pathToIdentifierFunction, nameFunction);

		return _fillForm(parsedType, formBuilder);
	}

	private static <T> Form<T> _fillForm(
		ParsedType parsedType, Form.Builder<T> formBuilder) {

		Class<T> typeClass = unsafeCast(parsedType.getTypeClass());

		Map<String, Object> resultsMap = new HashMap<>();

		InvocationHandler invocationHandler =
			(object, method, args) -> _getReturnValue(resultsMap, method);

		Function<String, BiConsumer<T, ?>> formFunction =
			methodName -> (object, value) -> resultsMap.put(methodName, value);

		Builder.FieldStep<T> fieldStep = formBuilder.title(
			__ -> ""
		).description(
			__ -> ""
		).constructor(
			() -> (T)Proxy.newProxyInstance(
				typeClass.getClassLoader(), new Class<?>[] {typeClass},
				invocationHandler)
		);

		List<FieldData<RelativeURL>> relativeURLFieldDataList =
			_filterReadableFields(parsedType::getRelativeURLFieldDataList);

		relativeURLFieldDataList.forEach(
			relativeURLFieldData -> {
				Method method = relativeURLFieldData.getMethod();

				fieldStep.addOptionalString(
					relativeURLFieldData.getFieldName(),
					unsafeCast(formFunction.apply(method.getName())));
			});

		List<FieldData<Class<?>>> fieldDataList = _filterReadableFields(
			parsedType::getFieldDataList);

		fieldDataList.forEach(
			fieldData -> {
				String key = fieldData.getFieldName();

				Method method = fieldData.getMethod();

				Class<?> returnTypeClass = fieldData.getData();

				if (returnTypeClass == String.class) {
					fieldStep.addOptionalString(
						key, unsafeCast(formFunction.apply(method.getName())));
				}
				else if (returnTypeClass == Date.class) {
					fieldStep.addOptionalDate(
						key, unsafeCast(formFunction.apply(method.getName())));
				}
				else if (returnTypeClass == Boolean.class) {
					fieldStep.addOptionalBoolean(
						key, unsafeCast(formFunction.apply(method.getName())));
				}
				else if (returnTypeClass == BinaryFile.class) {
					fieldStep.addOptionalFile(
						key, unsafeCast(formFunction.apply(method.getName())));
				}
				else if (returnTypeClass == Double.class) {
					fieldStep.addOptionalDouble(
						key, unsafeCast(formFunction.apply(method.getName())));
				}
				else if (Number.class.isAssignableFrom(returnTypeClass)) {
					fieldStep.addOptionalLong(
						key, unsafeCast(formFunction.apply(method.getName())));
				}
			});

		List<FieldData<Class<?>>> listFieldDataList = _filterReadableFields(
			parsedType::getListFieldDataList);

		listFieldDataList.forEach(
			listFieldData -> {
				Class<?> listClass = listFieldData.getData();
				String key = listFieldData.getFieldName();
				String methodName = listFieldData.getMethodName();

				if (listClass == String.class) {
					fieldStep.addOptionalStringList(
						key, unsafeCast(formFunction.apply(methodName)));
				}
				else if (listClass == Date.class) {
					fieldStep.addOptionalDateList(
						key, unsafeCast(formFunction.apply(methodName)));
				}
				else if (listClass == Boolean.class) {
					fieldStep.addOptionalBooleanList(
						key, unsafeCast(formFunction.apply(methodName)));
				}
				else if (listClass == BinaryFile.class) {
					fieldStep.addOptionalFileList(
						key, unsafeCast(formFunction.apply(methodName)));
				}
				else if (listClass == Double.class) {
					fieldStep.addOptionalDoubleList(
						key, unsafeCast(formFunction.apply(methodName)));
				}
				else if (Number.class.isAssignableFrom(listClass)) {
					fieldStep.addOptionalLongList(
						key, unsafeCast(formFunction.apply(methodName)));
				}
			});

		List<FieldData<LinkTo>> linkToFieldDataList = _filterReadableFields(
			parsedType::getLinkToFieldDataList);

		for (FieldData<LinkTo> fieldData : linkToFieldDataList) {
			LinkTo linkTo = fieldData.getData();

			String key = fieldData.getFieldName();
			String methodName = fieldData.getMethodName();
			Method method = fieldData.getMethod();

			if (SINGLE.equals(linkTo.resourceType())) {
				fieldStep.addOptionalLinkedModel(
					key, unsafeCast(linkTo.resource()),
					unsafeCast(formFunction.apply(methodName)));
			}
			else if (method.getReturnType() == List.class) {
				fieldStep.addOptionalLinkedModelList(
					key, unsafeCast(linkTo.resource()),
					unsafeCast(formFunction.apply(methodName)));
			}
		}

		List<FieldData<ParsedType>> nestedParsedTypes = _filterReadableFields(
			parsedType::getParsedTypes);

		nestedParsedTypes.forEach(
			nestedParsedType -> {
				ParsedType parsedTypeNested = nestedParsedType.getData();

				fieldStep.addOptionalNestedModel(
					nestedParsedType.getFieldName(),
					builder -> _fillForm(parsedTypeNested, builder),
					formFunction.apply(nestedParsedType.getMethodName()));
			});

		List<FieldData<ParsedType>> nestedListParsedTypes =
			_filterReadableFields(parsedType::getListParsedTypes);

		nestedListParsedTypes.forEach(
			nestedParsedType -> {
				ParsedType parsedTypeNested = nestedParsedType.getData();

				String methodName = nestedParsedType.getMethodName();

				fieldStep.addOptionalNestedModelList(
					nestedParsedType.getFieldName(),
					builder ->
						_fillForm(parsedTypeNested, builder),
					unsafeCast(formFunction.apply(methodName)));
			});

		return fieldStep.build();
	}

	private static <T extends FieldData> List<T> _filterReadableFields(
		Supplier<List<T>> supplier) {

		List<T> list = supplier.get();

		Stream<T> stream = list.stream();

		return stream.filter(
			_isReadableField
		).collect(
			Collectors.toList()
		);
	}

	private static Object _getReturnValue(
		Map<String, Object> resultsMap, Method method) {

		Object value = resultsMap.get(method.getName());

		if (method.getReturnType() == Optional.class) {
			return Optional.ofNullable(value);
		}

		return value;
	}

	private static final Predicate<FieldData> _isReadableField = fieldData -> {
		Field field = fieldData.getField();

		FieldMode fieldMode = field.mode();

		if (READ_ONLY.equals(fieldMode) || READ_WRITE.equals(fieldMode)) {
			return true;
		}

		return false;
	};

}