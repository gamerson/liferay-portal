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
import static com.liferay.apio.architect.internal.annotation.representor.processor.TypeProcessorTestUtil.testBidirectionalData;
import static com.liferay.apio.architect.internal.annotation.representor.processor.TypeProcessorTestUtil.testFieldData;
import static com.liferay.apio.architect.internal.annotation.representor.processor.TypeProcessorTestUtil.testLinkToFieldData;
import static com.liferay.apio.architect.internal.annotation.representor.processor.TypeProcessorTestUtil.testListFieldData;
import static com.liferay.apio.architect.internal.annotation.representor.processor.TypeProcessorTestUtil.testRelativeURLData;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

import static org.junit.Assert.assertThat;

import com.liferay.apio.architect.annotation.Vocabulary;
import com.liferay.apio.architect.annotation.Vocabulary.BidirectionalModel;
import com.liferay.apio.architect.annotation.Vocabulary.LinkTo;
import com.liferay.apio.architect.annotation.Vocabulary.RelativeURL;
import com.liferay.apio.architect.internal.annotation.representor.types.Dummy;
import com.liferay.apio.architect.internal.annotation.representor.types.Dummy.DummyImpl;
import com.liferay.apio.architect.internal.annotation.representor.types.Dummy.IntegerIdentifier;
import com.liferay.apio.architect.internal.annotation.representor.types.Dummy.StringIdentifier;

import java.lang.reflect.Method;

import java.util.Date;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Víctor Galán
 */
public class TypeProcessorTest {

	@BeforeClass
	public static void setUpClass() {
		_parsedType = TypeProcessor.processType(Dummy.class);
	}

	@Test
	public void testBasicFields() {
		List<FieldData<Class<?>>> fieldMetadata = getOrderedList(
			_parsedType::getFieldDataList);

		testFieldData(fieldMetadata.get(0), "booleanField1", Boolean.class);
		testFieldData(fieldMetadata.get(1), "booleanField2", Boolean.class);
		testFieldData(fieldMetadata.get(2), "dateField1", Date.class);
		testFieldData(fieldMetadata.get(3), "dateField2", Date.class);
		testFieldData(fieldMetadata.get(4), "numberField1", Integer.class);
		testFieldData(fieldMetadata.get(5), "numberField2", Long.class);
	}

	@Test
	public void testBidirectionalModels() {
		List<FieldData<BidirectionalModel>> bidirectionalFieldData =
			getOrderedList(_parsedType::getBidirectionalFieldDataList);

		testBidirectionalData(
			bidirectionalFieldData.get(0), "bidirectional1", "linked1",
			IntegerIdentifier.class);

		testBidirectionalData(
			bidirectionalFieldData.get(1), "bidirectional2", "linked2",
			StringIdentifier.class);
	}

	@Test
	public void testId() throws Exception {
		Method idMethod = _parsedType.getIdMethod();

		Long id = (Long)idMethod.invoke(new DummyImpl());

		assertThat(id, is(1L));
	}

	@Test
	public void testListFields() {
		List<FieldData<Class<?>>> listFieldData = getOrderedList(
			_parsedType::getListFieldDataList);

		testListFieldData(
			listFieldData.get(0), "booleanListField1", Boolean.class);
		testListFieldData(
			listFieldData.get(1), "booleanListField2", Boolean.class);
		testListFieldData(listFieldData.get(2), "numberListField1", Long.class);
		testListFieldData(listFieldData.get(3), "numberListField2", Long.class);
		testListFieldData(
			listFieldData.get(4), "stringListField1", String.class);
		testListFieldData(
			listFieldData.get(5), "stringListField2", String.class);
	}

	@Test
	public void testRelatedCollections() {
		List<FieldData<LinkTo>> linkToFieldDataList = getOrderedList(
			_parsedType::getLinkToFieldDataList);

		testLinkToFieldData(
			linkToFieldDataList.get(0), CHILD_COLLECTION,
			"linkToChildCollection1", IntegerIdentifier.class);

		testLinkToFieldData(
			linkToFieldDataList.get(1), CHILD_COLLECTION,
			"linkToChildCollection2", StringIdentifier.class);

		FieldData<LinkTo> linkToFieldData = linkToFieldDataList.get(2);

		Vocabulary.Field field = linkToFieldData.getField();
		LinkTo data = linkToFieldData.getData();
		Method method = linkToFieldData.getMethod();

		assertThat(field.value(), is("linkToChildCollectionList"));
		assertThat(data.resource(), equalTo(IntegerIdentifier.class));
		assertThat(data.resourceType(), is(CHILD_COLLECTION));
		assertThat(method.getReturnType(), equalTo(List.class));

		testLinkToFieldData(
			linkToFieldDataList.get(3), SINGLE, "linkToSingle1",
			IntegerIdentifier.class);

		testLinkToFieldData(
			linkToFieldDataList.get(4), SINGLE, "linkToSingle2",
			StringIdentifier.class);
	}

	@Test
	public void testRelativeUrls() {
		List<FieldData<RelativeURL>> relativeURLFieldData = getOrderedList(
			_parsedType::getRelativeURLFieldDataList);

		testRelativeURLData(
			relativeURLFieldData.get(0), "applicationRelativeUrl", true);

		testRelativeURLData(relativeURLFieldData.get(1), "relativeUrl1", false);
		testRelativeURLData(relativeURLFieldData.get(2), "relativeUrl2", false);
	}

	@Test
	public void testType() {
		Vocabulary.Type type = _parsedType.getType();

		assertThat(type.value(), is("Dummy"));
	}

	private static ParsedType _parsedType;

}