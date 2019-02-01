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

import javax.ws.rs.BadRequestException;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Víctor Galán
 */
public class FormTransformerTest {

	@BeforeClass
	public static void setUpClass() {
		ParsedType parsedType = TypeProcessor.processType(Dummy.class);

		_form = FormTransformer.toForm(
			parsedType, path -> Integer.valueOf(path.getId()),
			__ -> Optional.of("something"));
	}

	@Test
	public void testAllFields() {
		Body body = _getBodyOmittingKey("");

		Dummy dummy = _form.get(body);

		assertThat(
			dummy.getApplicationRelativeUrl(),
			is("applicationRelativeUrlValue"));

		assertThat(dummy.getBooleanField1(), is(true));
		assertThat(dummy.getBooleanField2(), is(false));
		assertThat(dummy.getBooleanListField1(), is(asList(true, false)));
		assertThat(dummy.getBooleanListField2(), is(asList(false, true)));
		assertThat(dummy.getDateField1(), equalTo(new Date(1465981200000L)));
		assertThat(dummy.getDateField2(), is(new Date(1491244560000L)));
		assertThat(dummy.getLinkToChildCollectionList(), is(asList(3, 2, 1)));
		assertThat(dummy.getNumberField2(), is(20L));
		assertThat(dummy.getNumberListField1(), is(asList(1L, 2L)));
		assertThat(dummy.getNumberListField2(), is(asList(3L, 4L)));
		assertThat(dummy.getRelativeUrl1(), is("/first"));
		assertThat(dummy.getRelativeUrl2(), is("/second"));
		assertThat(dummy.getStringField1(), is("string1"));
		assertThat(dummy.getStringListField1(), is(asList("one", "two")));
		assertThat(dummy.getStringListField2(), is(asList("three", "four")));
		assertThat(
			dummy.getStringFieldOptional(), is(Optional.of("stringOptional")));
	}

	@Test(expected = BadRequestException.class)
	public void testApplicationRelativeUrlRequired() {
		Body body = _getBodyOmittingKey("applicationRelativeUrl");

		_form.get(body);
	}

	@Test(expected = BadRequestException.class)
	public void testBooleanFieldsRequired() {
		Body body = _getBodyOmittingKey("booleanField1", "booleanField2");

		_form.get(body);
	}

	@Test(expected = BadRequestException.class)
	public void testBooleanListFieldsRequired() {
		Body body = _getBodyOmittingKey(
			"booleanListField1", "booleanListField2");

		_form.get(body);
	}

	@Test(expected = BadRequestException.class)
	public void testDateFieldsRequired() {
		Body body = _getBodyOmittingKey("dateField1", "dateField2");

		_form.get(body);
	}

	@Test(expected = BadRequestException.class)
	public void testLinkToCollectionListFieldsRequired() {
		Body body = _getBodyOmittingKey("linkToChildCollectionList");

		_form.get(body);
	}

	@Test(expected = BadRequestException.class)
	public void testLinkToSingleFieldsRequired() {
		Body body = _getBodyOmittingKey("linkToSingle1", "linkToSingle2");

		_form.get(body);
	}

	@Test(expected = BadRequestException.class)
	public void testNumberFieldsRequired() {
		Body body = _getBodyOmittingKey("numberField1", "numberField2");

		_form.get(body);
	}

	@Test(expected = BadRequestException.class)
	public void testNumberListFieldsRequired() {
		Body body = _getBodyOmittingKey("numberListField1", "numberListField2");

		_form.get(body);
	}

	@Test(expected = BadRequestException.class)
	public void testStringFieldsRequired() {
		Body body = _getBodyOmittingKey(
			"relativeUrl1", "relativeUrl2", "stringField1", "stringField2");

		_form.get(body);
	}

	@Test(expected = BadRequestException.class)
	public void testStringListFieldsRequired() {
		Body body = _getBodyOmittingKey("stringListField1", "stringListField2");

		_form.get(body);
	}

	private Body _getBodyOmittingKey(
		String keyToRemove, String... keysToRemove) {

		HashMap<String, String> bodyStringMap = new HashMap<String, String>() {
			{
				put("applicationRelativeUrl", "applicationRelativeUrlValue");
				put("booleanField1", "true");
				put("booleanField2", "false");
				put("dateField1", "2016-06-15T09:00Z");
				put("dateField2", "2017-04-03T18:36Z");
				put("linkToSingle1", "something/2");
				put("linkToSingle2", "something/3");
				put("numberField1", "10");
				put("numberField2", "20");
				put("relativeUrl1", "/first");
				put("relativeUrl2", "/second");
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

		bodyStringMap.remove(keyToRemove);
		bodyStringListMap.remove(keyToRemove);

		for (String key : keysToRemove) {
			bodyStringMap.remove(key);
			bodyStringListMap.remove(key);
		}

		return Body.create(
			key -> Optional.ofNullable(bodyStringMap.get(key)),
			key -> Optional.ofNullable(bodyStringListMap.get(key)));
	}

	private static Form<Dummy> _form;

}