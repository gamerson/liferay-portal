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

import com.liferay.apio.architect.annotation.Vocabulary.BidirectionalModel;
import com.liferay.apio.architect.annotation.Vocabulary.LinkTo;
import com.liferay.apio.architect.annotation.Vocabulary.RelativeURL;
import com.liferay.apio.architect.annotation.Vocabulary.Type;

import java.lang.reflect.Method;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds information about a class annotated with {@link Type}.
 *
 * @author Víctor Galán
 * @review
 */
public class ParsedType {

	/**
	 * Returns the list of bidirectionalfield data.
	 *
	 * @return the list of bidirectionalfield data
	 * @review
	 */
	public List<FieldData<BidirectionalModel>> getBidirectionalFieldDataList() {
		return _bidirectionalFieldData;
	}

	/**
	 * Returns the list of field data.
	 *
	 * @return the list of field data
	 * @review
	 */
	public List<FieldData<Class<?>>> getFieldDataList() {
		return _fieldDataList;
	}

	/**
	 * The method used to obtain the ID of the type.
	 *
	 * @review
	 */
	public Method getIdMethod() {
		return _method;
	}

	/**
	 * Returns the list of linkedModelField data.
	 *
	 * @return the list of linkedModelField data
	 * @review
	 */
	public List<FieldData<LinkTo>> getLinkToFieldDataList() {
		return _linkToFieldData;
	}

	/**
	 * Returns the list of listField data.
	 *
	 * @return the list of listField data
	 * @review
	 */
	public List<FieldData<Class<?>>> getListFieldDataList() {
		return _listFieldData;
	}

	/**
	 * Returns the list of listParsedTypes data.
	 *
	 * @return the list of listParsedTypes data
	 * @review
	 */
	public List<FieldData<ParsedType>> getListParsedTypes() {
		return _listParsedTypes;
	}

	/**
	 * Returns the list of nestedList parsed type data.
	 *
	 * @return the list of nestedList parsed type data
	 * @review
	 */
	public List<FieldData<ParsedType>> getParsedTypes() {
		return _parsedTypes;
	}

	/**
	 * Returns the list of relativeURLData data.
	 *
	 * @return the list of relativeURLData data
	 * @review
	 */
	public List<FieldData<RelativeURL>> getRelativeURLFieldDataList() {
		return _relativeURLFieldData;
	}

	/**
	 * Returns the type annotation.
	 *
	 * @return the type annotation
	 * @review
	 */
	public Type getType() {
		return _type;
	}

	/**
	 * Returns the typeClass.
	 *
	 * @return the typeClass
	 * @review
	 */
	public Class<?> getTypeClass() {
		return _typeClass;
	}

	public static class Builder {

		public Builder(Type type, Class<?> typeClass) {
			_parsedType = new ParsedType();

			_parsedType._type = type;
			_parsedType._typeClass = typeClass;
		}

		public void addBidirectionalFieldData(
			FieldData<BidirectionalModel> fieldData) {

			_parsedType._bidirectionalFieldData.add(fieldData);
		}

		public void addFieldData(FieldData fieldData) {
			_parsedType._fieldDataList.add(fieldData);
		}

		public void addLinkToFieldData(FieldData<LinkTo> fieldData) {
			_parsedType._linkToFieldData.add(fieldData);
		}

		public void addListFieldData(FieldData<Class<?>> fieldData) {
			_parsedType._listFieldData.add(fieldData);
		}

		public void addListParsedType(FieldData<ParsedType> fieldData) {
			_parsedType._listParsedTypes.add(fieldData);
		}

		public void addParsedType(FieldData<ParsedType> fieldData) {
			_parsedType._parsedTypes.add(fieldData);
		}

		public void addRelativeURLFieldData(FieldData<RelativeURL> fieldData) {
			_parsedType._relativeURLFieldData.add(fieldData);
		}

		public ParsedType build() {
			return _parsedType;
		}

		public void idMethod(Method method) {
			_parsedType._method = method;
		}

		private final ParsedType _parsedType;

	}

	private ParsedType() {
	}

	private List<FieldData<BidirectionalModel>> _bidirectionalFieldData =
		new ArrayList<>();
	private List<FieldData<Class<?>>> _fieldDataList = new ArrayList<>();
	private List<FieldData<LinkTo>> _linkToFieldData = new ArrayList<>();
	private List<FieldData<Class<?>>> _listFieldData = new ArrayList<>();
	private List<FieldData<ParsedType>> _listParsedTypes = new ArrayList<>();
	private Method _method;
	private List<FieldData<ParsedType>> _parsedTypes = new ArrayList<>();
	private List<FieldData<RelativeURL>> _relativeURLFieldData =
		new ArrayList<>();
	private Type _type;
	private Class<?> _typeClass;

}