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

import static com.liferay.apio.architect.annotation.Vocabulary.LinkTo.ResourceType.CHILD_COLLECTION;
import static com.liferay.apio.architect.annotation.Vocabulary.LinkTo.ResourceType.SINGLE;
import static com.liferay.apio.architect.internal.annotation.representor.processor.TypeProcessorTestUtil.getOrderedList;
import static com.liferay.apio.architect.internal.annotation.representor.processor.TypeProcessorTestUtil.testFieldData;
import static com.liferay.apio.architect.internal.annotation.representor.processor.TypeProcessorTestUtil.testLinkToFieldData;
import static com.liferay.apio.architect.internal.annotation.representor.processor.TypeProcessorTestUtil.testListFieldData;

import static org.hamcrest.core.IsEqual.equalTo;

import static org.junit.Assert.assertThat;

import com.liferay.apio.architect.annotation.Vocabulary.LinkTo;
import com.liferay.apio.architect.internal.annotation.representor.types.Dummy.IntegerIdentifier;
import com.liferay.apio.architect.internal.annotation.representor.types.DummyWithNested;
import com.liferay.apio.architect.internal.annotation.representor.types.DummyWithNested.NestedDummy;

import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Víctor Galán
 */
public class TypeProcessorNestedTest {

	@BeforeClass
	public static void setUpClass() {
		ParsedType parsedType = TypeProcessor.processType(
			DummyWithNested.class);

		List<FieldData<ParsedType>> parsedTypes = parsedType.getParsedTypes();

		_nestedParsedType = parsedTypes.get(0);

		_parsedType = _nestedParsedType.getData();
	}

	@Test
	public void testBasicFields() {
		List<FieldData<Class<?>>> fieldMetadata = getOrderedList(
			_parsedType::getFieldDataList);

		testFieldData(fieldMetadata.get(0), "stringField", String.class);
	}

	@Test
	public void testLinkedModels() {
		List<FieldData<LinkTo>> linkToFieldDataList = getOrderedList(
			_parsedType::getLinkToFieldDataList);

		testLinkToFieldData(
			linkToFieldDataList.get(0), CHILD_COLLECTION,
			"linkToChildCollection", IntegerIdentifier.class);

		testLinkToFieldData(
			linkToFieldDataList.get(1), SINGLE, "linkToSingle",
			IntegerIdentifier.class);
	}

	@Test
	public void testListFields() {
		List<FieldData<Class<?>>> listFieldData = getOrderedList(
			_parsedType::getListFieldDataList);

		testListFieldData(
			listFieldData.get(0), "stringListField", String.class);
	}

	@Test
	public void testNested() {
		ParsedType parsedType = _nestedParsedType.getData();

		assertThat(parsedType.getTypeClass(), equalTo(NestedDummy.class));

		testFieldData(_nestedParsedType, "nestedDummy", NestedDummy.class);
	}

	private static FieldData<ParsedType> _nestedParsedType;
	private static ParsedType _parsedType;

}