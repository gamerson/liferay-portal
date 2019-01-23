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

package com.liferay.apio.architect.internal.annotation.representor;

import static com.liferay.apio.architect.annotation.FieldMode.READ_WRITE;
import static com.liferay.apio.architect.annotation.FieldMode.WRITE_ONLY;
import static com.liferay.apio.architect.annotation.Vocabulary.LinkTo.ResourceType.SINGLE;
import static com.liferay.apio.architect.internal.unsafe.Unsafe.unsafeCast;

import com.liferay.apio.architect.alias.BinaryFunction;
import com.liferay.apio.architect.annotation.FieldMode;
import com.liferay.apio.architect.annotation.Vocabulary.Field;
import com.liferay.apio.architect.annotation.Vocabulary.LinkTo;
import com.liferay.apio.architect.annotation.Vocabulary.RelativeURL;
import com.liferay.apio.architect.file.BinaryFile;
import com.liferay.apio.architect.functional.Try;
import com.liferay.apio.architect.internal.annotation.representor.processor.FieldData;
import com.liferay.apio.architect.internal.annotation.representor.processor.ParsedType;
import com.liferay.apio.architect.language.AcceptLanguage;
import com.liferay.apio.architect.representor.BaseRepresentor;

import java.lang.reflect.Method;

import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Provides utility functions to fill a representor builder by using an instance
 * of an annotated method.
 *
 * @author Víctor Galán
 */
public class RepresentorTransformerUtil {

	/**
	 * Fills the builder using the parsed type provided.
	 *
	 * @param  firstStep the builder's first step
	 * @param  parsedType the parsed type
	 * @review
	 */
	public static void addCommonFields(
		BaseRepresentor.BaseFirstStep<?, ?, ?> firstStep,
		ParsedType parsedType) {

		List<FieldData<LinkTo>> linkToFieldDataList = filterWritableFields(
			parsedType::getLinkToFieldDataList);

		for (FieldData<LinkTo> fieldData : linkToFieldDataList) {
			LinkTo linkTo = fieldData.getData();

			if (!SINGLE.equals(linkTo.resourceType())) {
				continue;
			}

			firstStep.addLinkedModel(
				fieldData.getFieldName(), unsafeCast(linkTo.resource()),
				getMethodFunction(fieldData.getMethod()));
		}

		List<FieldData<Class<?>>> fieldDataList = filterWritableFields(
			parsedType::getFieldDataList);

		fieldDataList.forEach(
			fieldData -> _addBasicFields(firstStep, fieldData));

		List<FieldData<Class<?>>> listFieldData = filterWritableFields(
			parsedType::getListFieldDataList);

		listFieldData.forEach(
			listField -> _addListFields(firstStep, listField));

		List<FieldData<RelativeURL>> relativeURLFieldDataList =
			filterWritableFields(parsedType::getRelativeURLFieldDataList);

		relativeURLFieldDataList.forEach(
			relativeURLFieldData -> {
				RelativeURL relativeURL = relativeURLFieldData.getData();
				String key = relativeURLFieldData.getFieldName();
				Method method = relativeURLFieldData.getMethod();

				if (relativeURL.fromApplication()) {
					firstStep.addApplicationRelativeURL(
						key, getMethodFunction(method));
				}
				else {
					firstStep.addRelativeURL(key, getMethodFunction(method));
				}
			});

		List<FieldData<ParsedType>> nestedParsedTypes = filterWritableFields(
			parsedType::getParsedTypes);

		nestedParsedTypes.forEach(
			nestedParsedType -> {
				ParsedType nested = nestedParsedType.getData();

				firstStep.addNested(
					nestedParsedType.getFieldName(),
					getMethodFunction(nestedParsedType.getMethod()),
					builder -> unsafeCast(
						NestedRepresentorTransformer.toRepresentor(
							nested, builder)));
			});

		List<FieldData<ParsedType>> nestedListParsedTypes =
			filterWritableFields(parsedType::getListParsedTypes);

		nestedListParsedTypes.forEach(
			nestedParsedType -> {
				ParsedType nested = nestedParsedType.getData();

				firstStep.addNestedList(
					nestedParsedType.getFieldName(),
					getMethodFunction(nestedParsedType.getMethod()),
					builder -> unsafeCast(
						NestedRepresentorTransformer.toRepresentor(
							nested, builder)));
			});
	}

	public static <T extends FieldData> List<T> filterWritableFields(
		Supplier<List<T>> supplier) {

		List<T> list = supplier.get();

		Stream<T> stream = list.stream();

		return stream.filter(
			_isWritableField
		).collect(
			Collectors.toList()
		);
	}

	public static <T> BinaryFunction<T> getBinaryFunction(Method method) {
		return t -> Try.fromFallible(
			() -> (BinaryFile)_unwrapOptionalIfNeeded(method.invoke(t))
		).orElse(
			null
		);
	}

	public static <A, T, S> BiFunction<T, A, S> getMethodBiFunction(
		Method method) {

		return (t, a) -> Try.fromFallible(
			() -> (S)_unwrapOptionalIfNeeded(method.invoke(t, a))
		).orElse(
			null
		);
	}

	public static <T, S> Function<T, S> getMethodFunction(Method method) {
		return t -> Try.fromFallible(
			() -> (S)_unwrapOptionalIfNeeded(method.invoke(t))
		).orElse(
			null
		);
	}

	private static void _addBasicFields(
		BaseRepresentor.BaseFirstStep<?, ?, ?> firstStep,
		FieldData<Class<?>> fieldData) {

		Method method = fieldData.getMethod();
		Field field = fieldData.getField();
		Class<?> returnTypeClass = fieldData.getData();

		String key = field.value();

		if (returnTypeClass == String.class) {
			_addStringFields(firstStep, method, key);
		}
		else if (returnTypeClass == Date.class) {
			firstStep.addDate(key, getMethodFunction(method));
		}
		else if (returnTypeClass == Boolean.class) {
			firstStep.addBoolean(key, getMethodFunction(method));
		}
		else if (returnTypeClass == BinaryFile.class) {
			firstStep.addBinary(key, getBinaryFunction(method));
		}
		else if (Number.class.isAssignableFrom(returnTypeClass)) {
			firstStep.addNumber(key, getMethodFunction(method));
		}
	}

	private static void _addListFields(
		BaseRepresentor.BaseFirstStep<?, ?, ?> firstStep,
		FieldData<Class<?>> listFieldData) {

		Class<?> listClass = listFieldData.getData();
		String key = listFieldData.getFieldName();
		Method method = listFieldData.getMethod();

		if (listClass == String.class) {
			firstStep.addStringList(key, getMethodFunction(method));
		}
		else if (listClass == Boolean.class) {
			firstStep.addBooleanList(key, getMethodFunction(method));
		}
		else if (Number.class.isAssignableFrom(listClass)) {
			firstStep.addNumberList(key, getMethodFunction(method));
		}
	}

	private static void _addStringFields(
		BaseRepresentor.BaseFirstStep<?, ?, ?> firstStep, Method method,
		String key) {

		Class<?>[] parameters = method.getParameterTypes();

		if (parameters.length > 0) {
			Class<?> firstParameter = parameters[0];

			if (firstParameter == Locale.class) {
				firstStep.addLocalizedStringByLocale(
					key, getMethodBiFunction(method));
			}
			else if (firstParameter == AcceptLanguage.class) {
				firstStep.addLocalizedStringByLanguage(
					key, getMethodBiFunction(method));
			}
		}
		else {
			firstStep.addString(key, getMethodFunction(method));
		}
	}

	private static <T> T _unwrapOptionalIfNeeded(Object object) {
		if (object.getClass() == Optional.class) {
			Optional<T> optional = (Optional<T>)object;

			return optional.orElse(null);
		}

		return (T)object;
	}

	private static final Predicate<FieldData> _isWritableField = fieldData -> {
		Field field = fieldData.getField();

		FieldMode fieldMode = field.mode();

		if (WRITE_ONLY.equals(fieldMode) || READ_WRITE.equals(fieldMode)) {
			return true;
		}

		return false;
	};

}