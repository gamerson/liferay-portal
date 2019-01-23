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

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

import static org.junit.Assert.assertThat;

import com.liferay.apio.architect.annotation.Vocabulary.BidirectionalModel;
import com.liferay.apio.architect.annotation.Vocabulary.Field;
import com.liferay.apio.architect.annotation.Vocabulary.LinkTo;
import com.liferay.apio.architect.annotation.Vocabulary.LinkTo.ResourceType;
import com.liferay.apio.architect.annotation.Vocabulary.RelativeURL;
import com.liferay.apio.architect.identifier.Identifier;
import com.liferay.apio.architect.internal.wiring.osgi.util.GenericUtil;

import java.lang.reflect.Method;

import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

/**
 * @author Víctor Galán
 */
public class TypeProcessorTestUtil {

	public static <T> T getIdentifierType(Class<?> identifierClass) {
		return (T)GenericUtil.getGenericTypeArgumentTry(
			identifierClass, Identifier.class, 0
		).orElse(
			null
		);
	}

	public static <T extends FieldData> List<T> getOrderedList(
		Supplier<List<T>> supplier) {

		List<T> list = supplier.get();

		list.sort(Comparator.comparing(FieldData::getFieldName));

		return list;
	}

	public static void testBidirectionalData(
		FieldData<BidirectionalModel> bidirectionalFieldData, String fieldName,
		String bidirectionalName, Class<?> identifierClass) {

		BidirectionalModel bidirectionalModel =
			bidirectionalFieldData.getData();

		Class<?> identifierType = getIdentifierType(identifierClass);

		assertThat(bidirectionalModel.modelClass(), equalTo(identifierClass));

		assertThat(bidirectionalModel.field().value(), is(bidirectionalName));

		testFieldData(bidirectionalFieldData, fieldName, identifierType);
	}

	public static void testFieldData(
		FieldData fieldData, String fieldName, Class<?> returnTypeClass) {

		Field field = fieldData.getField();
		Method method = fieldData.getMethod();

		assertThat(field.value(), is(fieldName));
		assertThat(method.getReturnType(), equalTo(returnTypeClass));
	}

	public static void testLinkToFieldData(
		FieldData<LinkTo> fieldData, ResourceType resourceType,
		String fieldName, Class<?> identifierClass) {

		LinkTo linkTo = fieldData.getData();

		Class<?> identifierType = getIdentifierType(identifierClass);

		assertThat(linkTo.resource(), equalTo(identifierClass));
		assertThat(linkTo.resourceType(), is(resourceType));

		testFieldData(fieldData, fieldName, identifierType);
	}

	public static void testListFieldData(
		FieldData<Class<?>> listFieldData, String fieldName,
		Class<?> listType) {

		assertThat(listFieldData.getData(), equalTo(listType));

		testFieldData(listFieldData, fieldName, List.class);
	}

	public static void testRelativeURLData(
		FieldData<RelativeURL> relativeURLFieldData, String fieldName,
		boolean fromApplication) {

		RelativeURL relativeURL = relativeURLFieldData.getData();

		assertThat(relativeURL.fromApplication(), is(fromApplication));

		testFieldData(relativeURLFieldData, fieldName, String.class);
	}

}