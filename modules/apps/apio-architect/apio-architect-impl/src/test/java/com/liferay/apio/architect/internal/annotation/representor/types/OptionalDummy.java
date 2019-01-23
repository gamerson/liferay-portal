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

import static java.util.Collections.singletonList;
import static java.util.Optional.of;

import com.liferay.apio.architect.annotation.Id;
import com.liferay.apio.architect.annotation.Vocabulary.BidirectionalModel;
import com.liferay.apio.architect.annotation.Vocabulary.Field;
import com.liferay.apio.architect.annotation.Vocabulary.LinkTo;
import com.liferay.apio.architect.annotation.Vocabulary.RelativeURL;
import com.liferay.apio.architect.annotation.Vocabulary.Type;
import com.liferay.apio.architect.file.BinaryFile;
import com.liferay.apio.architect.identifier.Identifier;

import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * @author Víctor Galán
 */
@Type("OptionalDummy")
public interface OptionalDummy extends Identifier<Long> {

	@BidirectionalModel(
		field = @Field("bidirectional"),
		modelClass = OptionalDummyIdentifier.class
	)
	@Field("bidirectional")
	public default Optional<Long> getBidirectionalOptional() {
		return of(1L);
	}

	@Field("binaryFile")
	public default Optional<BinaryFile> getBinaryFileOptional() {
		return of(new BinaryFile(null, 1L, ""));
	}

	@Field("booleanList")
	public default Optional<List<Boolean>> getBooleanListOptional() {
		return of(singletonList(true));
	}

	@Field("boolean")
	public default Optional<Boolean> getBooleanOptional() {
		return of(true);
	}

	@Field("date")
	public default Optional<Date> getDateOptional() {
		return of(new Date(1L));
	}

	@Field("linkTo")
	@LinkTo(resource = OptionalDummyIdentifier.class)
	public default Optional<Long> getLinkToOptional() {
		return of(2L);
	}

	@Field("longList")
	public default Optional<List<Long>> getLongListOptional() {
		return of(singletonList(1L));
	}

	@Field("long")
	public default Optional<Long> getLongOptional() {
		return of(3L);
	}

	@Field("optionalDummyList")
	public default Optional<List<NestedOptionalDummy>>
		getNestedOptionalDummyListOptional() {

		return of(singletonList(new NestedOptionalDummyImpl()));
	}

	@Field("optionalDummy")
	public default Optional<NestedOptionalDummy>
		getNestedOptionalDummyOptional() {

		return of(new NestedOptionalDummyImpl());
	}

	@Field("relativeURL")
	@RelativeURL
	public default Optional<String> getRelativeURLOptional() {
		return of("/relativeURL");
	}

	@Field("stringList")
	public default Optional<List<String>> getStringListOptional() {
		return of(singletonList("element"));
	}

	@Field("string")
	public default Optional<String> getStringOptional() {
		return of("value");
	}

	@Type("nestedOptionalDummy")
	public interface NestedOptionalDummy extends Identifier<Long> {

		@Id
		public default Long getId() {
			return 1L;
		}

		@Field("string")
		public default Optional<String> getStringField() {
			return of("nestedValue");
		}

	}

	public class NestedOptionalDummyImpl implements NestedOptionalDummy {
	}

	public interface OptionalDummyIdentifier extends Identifier<Long> {
	}

	public class OptionalDummyImpl implements OptionalDummy {
	}

}