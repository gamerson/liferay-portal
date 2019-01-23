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

import static java.util.Arrays.asList;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.nullValue;

import static org.junit.Assert.assertThat;

import com.liferay.apio.architect.form.Body;
import com.liferay.apio.architect.form.Form;
import com.liferay.apio.architect.internal.annotation.representor.processor.ParsedType;
import com.liferay.apio.architect.internal.annotation.representor.processor.TypeProcessor;
import com.liferay.apio.architect.internal.annotation.representor.types.Dummy;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Víctor Galán
 */
public class FormTransformerTest {

	@BeforeClass
	public static void setUpClass() {
		HashMap<String, String> bodyStringMap = new HashMap<String, String>() {
			{
				put("applicationRelativeUrl", "applicationRelativeUrlValue");
				put("booleanField1", "true");
				put("booleanField2", "false");
				put("dateField1", "2016-06-15T09:00Z");
				put("dateField2", "2017-04-03T18:36Z");
				put("linkedModel1", "something/1");
				put("linkedModel2", "something/2d1d");
				put("numberField1", "10");
				put("numberField2", "20");
				put("relativeUrl1", "/first");
				put("stringField1", "string1");
				put("stringFieldOptional", "stringOptional");
			}
		};

		HashMap<String, List<String>> bodyStringListMap =
			new HashMap<String, List<String>>() {
				{
					put("booleanListField1", asList("true", "false"));
					put("booleanListField2", asList("false", "true"));
					put(
						"linkToChildCollectionList",
						asList("something/3", "something/2", "something/1"));
					put("numberListField1", asList("1", "2"));
					put("numberListField2", asList("3", "4"));
					put("stringListField1", asList("one", "two"));
					put("stringListField2", asList("three", "four"));
				}
			};

		Body body = Body.create(
			key -> Optional.ofNullable(bodyStringMap.get(key)),
			key -> Optional.ofNullable(bodyStringListMap.get(key)));

		ParsedType parsedType = TypeProcessor.processType(Dummy.class);

		Form<Dummy> objectForm = FormTransformer.toForm(
			parsedType, path -> Integer.valueOf(path.getId()),
			__ -> Optional.of("something"));

		_dummy = objectForm.get(body);
	}

	@Test
	public void testFields() {
		assertThat(_dummy.getBooleanField1(), is(true));
		assertThat(_dummy.getBooleanField2(), is(false));
		assertThat(_dummy.getDateField1(), equalTo(new Date(1465981200000L)));
		assertThat(_dummy.getDateField2(), is(new Date(1491244560000L)));
		assertThat(_dummy.getNumberField2(), is(20L));
		assertThat(_dummy.getStringField1(), is("string1"));
	}

	@Test
	public void testListFields() {
		assertThat(_dummy.getBooleanListField1(), is(asList(true, false)));
		assertThat(_dummy.getBooleanListField2(), is(asList(false, true)));
		assertThat(_dummy.getNumberListField1(), is(asList(1L, 2L)));
		assertThat(_dummy.getNumberListField2(), is(asList(3L, 4L)));
		assertThat(_dummy.getStringListField1(), is(asList("one", "two")));
		assertThat(_dummy.getStringListField2(), is(asList("three", "four")));
		assertThat(_dummy.getLinkToChildCollectionList(), is(asList(3, 2, 1)));
	}

	@Test
	public void testNotKeyInBodyShouldReturnNull() {
		assertThat(_dummy.getRelativeUrl2(), is(nullValue()));
	}

	@Test
	public void testOptionalFields() {
		assertThat(
			_dummy.getStringFieldOptional(), is(Optional.of("stringOptional")));
	}

	@Test
	public void testRelativeURL() {
		assertThat(
			_dummy.getApplicationRelativeUrl(),
			is("applicationRelativeUrlValue"));
		assertThat(_dummy.getRelativeUrl1(), is("/first"));
	}

	private static Dummy _dummy;

}