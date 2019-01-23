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

package com.liferay.apio.architect.internal.annotation.representor.types;

import static com.liferay.apio.architect.annotation.FieldMode.READ_ONLY;
import static com.liferay.apio.architect.annotation.Vocabulary.LinkTo.ResourceType.CHILD_COLLECTION;

import static java.util.Arrays.asList;

import com.liferay.apio.architect.annotation.Id;
import com.liferay.apio.architect.annotation.Vocabulary.BidirectionalModel;
import com.liferay.apio.architect.annotation.Vocabulary.Field;
import com.liferay.apio.architect.annotation.Vocabulary.LinkTo;
import com.liferay.apio.architect.annotation.Vocabulary.RelativeURL;
import com.liferay.apio.architect.annotation.Vocabulary.Type;
import com.liferay.apio.architect.identifier.Identifier;

import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * @author Víctor Galán
 */
@Type("Dummy")
public interface Dummy extends Identifier<Long> {

	@Field("applicationRelativeUrl")
	@RelativeURL(fromApplication = true)
	public default String getApplicationRelativeUrl() {
		return "/application";
	}

	@BidirectionalModel(
		field = @Field("linked1"), modelClass = IntegerIdentifier.class
	)
	@Field("bidirectional1")
	public default Integer getBidirectional1() {
		return 3;
	}

	@BidirectionalModel(
		field = @Field("linked2"), modelClass = StringIdentifier.class
	)
	@Field("bidirectional2")
	public default String getBidirectional2() {
		return "2d1d";
	}

	@Field("booleanField1")
	public default Boolean getBooleanField1() {
		return true;
	}

	@Field("booleanField2")
	public default Boolean getBooleanField2() {
		return false;
	}

	@Field("booleanListField1")
	public default List<Boolean> getBooleanListField1() {
		return asList(true, true, false);
	}

	@Field("booleanListField2")
	public default List<Boolean> getBooleanListField2() {
		return asList(false, true, true);
	}

	@Field("dateField1")
	public default Date getDateField1() {
		return new Date(1L);
	}

	@Field("dateField2")
	public default Date getDateField2() {
		return new Date(200000L);
	}

	@Id
	public default Long getId() {
		return 1L;
	}

	@Field("linkToChildCollection1")
	@LinkTo(resource = IntegerIdentifier.class, resourceType = CHILD_COLLECTION)
	public default Integer getLinkToChildCollection1() {
		return 1;
	}

	@Field("linkToChildCollection2")
	@LinkTo(resource = StringIdentifier.class, resourceType = CHILD_COLLECTION)
	public default String getLinkToChildCollection2() {
		return "2d1d";
	}

	@Field(mode = READ_ONLY, value = "linkToChildCollectionList")
	@LinkTo(resource = IntegerIdentifier.class, resourceType = CHILD_COLLECTION)
	public default List<Integer> getLinkToChildCollectionList() {
		return asList(1, 2, 3);
	}

	@Field("linkToSingle1")
	@LinkTo(resource = IntegerIdentifier.class)
	public default Integer getLinkToSingle1() {
		return 1;
	}

	@Field("linkToSingle2")
	@LinkTo(resource = StringIdentifier.class)
	public default String getLinkToSingle2() {
		return "2d1d";
	}

	@Field("numberField1")
	public default Integer getNumberField1() {
		return 10;
	}

	@Field("numberField2")
	public default Long getNumberField2() {
		return 20L;
	}

	@Field("numberListField1")
	public default List<Long> getNumberListField1() {
		return asList(1L, 2L, 3L);
	}

	@Field("numberListField2")
	public default List<Long> getNumberListField2() {
		return asList(4L, 5L, 6L);
	}

	@Field(mode = READ_ONLY, value = "relativeUrl1")
	@RelativeURL
	public default String getRelativeUrl1() {
		return "/first";
	}

	@Field("relativeUrl2")
	@RelativeURL
	public default String getRelativeUrl2() {
		return "/second";
	}

	@Field("stringField1")
	public default String getStringField1() {
		return "string1";
	}

	@Field("stringFieldOptional")
	public default Optional<String> getStringFieldOptional() {
		return Optional.of("string2");
	}

	@Field("stringListField1")
	public default List<String> getStringListField1() {
		return asList("one", "two", "three");
	}

	@Field("stringListField2")
	public default List<String> getStringListField2() {
		return asList("four", "five", "six");
	}

	public class DummyImpl implements Dummy {
	}

	public interface IntegerIdentifier extends Identifier<Integer> {
	}

	public interface StringIdentifier extends Identifier<String> {
	}

}