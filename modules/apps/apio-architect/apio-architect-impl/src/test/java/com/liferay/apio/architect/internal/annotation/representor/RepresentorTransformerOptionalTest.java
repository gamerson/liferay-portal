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

import static java.util.Collections.singletonList;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.notNullValue;

import static org.junit.Assert.assertThat;

import com.liferay.apio.architect.alias.representor.FieldFunction;
import com.liferay.apio.architect.alias.representor.NestedFieldFunction;
import com.liferay.apio.architect.alias.representor.NestedListFieldFunction;
import com.liferay.apio.architect.file.BinaryFile;
import com.liferay.apio.architect.internal.annotation.representor.processor.ParsedType;
import com.liferay.apio.architect.internal.annotation.representor.processor.TypeProcessor;
import com.liferay.apio.architect.internal.annotation.representor.types.OptionalDummy;
import com.liferay.apio.architect.internal.annotation.representor.types.OptionalDummy.NestedOptionalDummy;
import com.liferay.apio.architect.internal.annotation.representor.types.OptionalDummy.NestedOptionalDummyImpl;
import com.liferay.apio.architect.internal.annotation.representor.types.OptionalDummy.OptionalDummyImpl;
import com.liferay.apio.architect.representor.NestedRepresentor;
import com.liferay.apio.architect.representor.Representor;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

/**
 * @author Víctor Galán
 */
public class RepresentorTransformerOptionalTest {

	@Before
	public void setUp() {
		ParsedType parsedType = TypeProcessor.processType(OptionalDummy.class);

		_representor = RepresentorTransformer.toRepresentor(
			parsedType, null, new HashMap<>());
	}

	@Test
	public void testBinaryFile() {
		List<FieldFunction<OptionalDummy, BinaryFile>> binaryFieldFunctions =
			_representor.getBinaryFunctions();

		FieldFunction<OptionalDummy, BinaryFile> binaryFieldFunction =
			binaryFieldFunctions.get(0);

		assertThat(binaryFieldFunction.getKey(), is("binaryFile"));
		assertThat(binaryFieldFunction.apply(_optionalDummy), notNullValue());
	}

	@Test
	public void testBoolean() {
		List<FieldFunction<OptionalDummy, Boolean>> booleanFieldFunctions =
			_representor.getBooleanFunctions();

		FieldFunction<OptionalDummy, Boolean> booleanFieldFunction =
			booleanFieldFunctions.get(0);

		assertThat(booleanFieldFunction.getKey(), is("boolean"));
		assertThat(booleanFieldFunction.apply(_optionalDummy), is(true));
	}

	@Test
	public void testBooleanList() {
		List<FieldFunction<OptionalDummy, List<Boolean>>>
			booleanListFieldFunctions = _representor.getBooleanListFunctions();

		FieldFunction<OptionalDummy, List<Boolean>> booleanListFieldFunction =
			booleanListFieldFunctions.get(0);

		assertThat(booleanListFieldFunction.getKey(), is("booleanList"));
		assertThat(
			booleanListFieldFunction.apply(_optionalDummy),
			is(singletonList(true)));
	}

	@Test
	public void testLong() {
		List<FieldFunction<OptionalDummy, Number>> numberFieldFunctions =
			_representor.getNumberFunctions();

		FieldFunction<OptionalDummy, Number> numberFieldFunction =
			numberFieldFunctions.get(0);

		assertThat(numberFieldFunction.getKey(), is("long"));
		assertThat(numberFieldFunction.apply(_optionalDummy), is(3L));
	}

	@Test
	public void testLongList() {
		List<FieldFunction<OptionalDummy, List<Number>>>
			numberListFieldFunctions = _representor.getNumberListFunctions();

		FieldFunction<OptionalDummy, List<Number>> numberListFieldFunction =
			numberListFieldFunctions.get(0);

		assertThat(numberListFieldFunction.getKey(), is("longList"));
		assertThat(
			numberListFieldFunction.apply(_optionalDummy),
			is(singletonList(1L)));
	}

	@Test
	public void testNested() {
		List<NestedFieldFunction<OptionalDummy, ?>> nestedFieldFunctions =
			_representor.getNestedFieldFunctions();

		NestedFieldFunction<OptionalDummy, ?> nestedFieldFunction =
			nestedFieldFunctions.get(0);

		Object object = nestedFieldFunction.apply(_optionalDummy);

		assertThat(nestedFieldFunction.getKey(), is("optionalDummy"));

		assertThat(object.getClass(), equalTo(NestedOptionalDummyImpl.class));
	}

	@Test
	public void testNestedFields() {
		List<NestedListFieldFunction<OptionalDummy, ?>>
			nestedListFieldFunctions =
				_representor.getNestedListFieldFunctions();

		NestedListFieldFunction<OptionalDummy, ?> nestedListFieldFunction =
			nestedListFieldFunctions.get(0);

		NestedRepresentor<NestedOptionalDummy> nestedRepresentor =
			(NestedRepresentor<NestedOptionalDummy>)
				nestedListFieldFunction.getNestedRepresentor();

		List<FieldFunction<NestedOptionalDummy, String>> stringFunctions =
			nestedRepresentor.getStringFunctions();

		FieldFunction<NestedOptionalDummy, String> stringFunction =
			stringFunctions.get(0);

		assertThat(stringFunction.getKey(), is("string"));
		assertThat(
			stringFunction.apply(_nestedOptionalDummy), is("nestedValue"));
	}

	@Test
	public void testNestedList() {
		List<NestedListFieldFunction<OptionalDummy, ?>>
			nestedListFieldFunctions =
				_representor.getNestedListFieldFunctions();

		NestedListFieldFunction<OptionalDummy, ?> nestedListFieldFunction =
			nestedListFieldFunctions.get(0);

		List<?> nestedList = nestedListFieldFunction.apply(_optionalDummy);

		Object object = nestedList.get(0);

		assertThat(nestedListFieldFunction.getKey(), is("optionalDummyList"));
		assertThat(object.getClass(), equalTo(NestedOptionalDummyImpl.class));
	}

	@Test
	public void testRelativeURLFunction() {
		List<FieldFunction<OptionalDummy, String>> relativeURLFieldFunctions =
			_representor.getRelativeURLFunctions();

		FieldFunction<OptionalDummy, String> relativeURLFieldFunction =
			relativeURLFieldFunctions.get(0);

		assertThat(relativeURLFieldFunction.getKey(), is("relativeURL"));
		assertThat(
			relativeURLFieldFunction.apply(_optionalDummy), is("/relativeURL"));
	}

	@Test
	public void testStringFunction() {
		List<FieldFunction<OptionalDummy, String>> stringFieldFunctions =
			_representor.getStringFunctions();

		stringFieldFunctions.sort(Comparator.comparing(FieldFunction::getKey));

		FieldFunction<OptionalDummy, String> dateFieldFunction =
			stringFieldFunctions.get(0);

		assertThat(dateFieldFunction.getKey(), is("date"));
		assertThat(
			dateFieldFunction.apply(_optionalDummy), is("1970-01-01T00:00Z"));

		FieldFunction<OptionalDummy, String> stringFieldFunction =
			stringFieldFunctions.get(1);

		assertThat(stringFieldFunction.getKey(), is("string"));
		assertThat(stringFieldFunction.apply(_optionalDummy), is("value"));
	}

	@Test
	public void testStringList() {
		List<FieldFunction<OptionalDummy, List<String>>>
			stringListFieldFunctions = _representor.getStringListFunctions();

		FieldFunction<OptionalDummy, List<String>> stringListFieldFunction =
			stringListFieldFunctions.get(0);

		assertThat(stringListFieldFunction.getKey(), is("stringList"));
		assertThat(
			stringListFieldFunction.apply(_optionalDummy),
			is(singletonList("element")));
	}

	private final NestedOptionalDummy _nestedOptionalDummy =
		new NestedOptionalDummyImpl();
	private final OptionalDummy _optionalDummy = new OptionalDummyImpl();
	private Representor<OptionalDummy> _representor;

}