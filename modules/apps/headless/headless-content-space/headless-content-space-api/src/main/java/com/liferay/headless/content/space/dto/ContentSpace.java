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

package com.liferay.headless.content.space.dto;

import com.liferay.apio.architect.annotation.Id;
import com.liferay.apio.architect.annotation.Vocabulary.Field;
import com.liferay.apio.architect.annotation.Vocabulary.LinkTo;
import com.liferay.apio.architect.annotation.Vocabulary.Type;
import com.liferay.apio.architect.identifier.Identifier;
import com.liferay.headless.person.dto.Person;

import java.util.List;
import java.util.Locale;

/**
 * @author Javier Gamarra
 * @author Cristina González
 * @generated
 */
@Type("ContentSpace")
public interface ContentSpace extends Identifier<Long> {

	@Field("availableLanguages")
	public List<String> getAvailableLanguages();

	@Field("creator")
	@LinkTo(resource = Person.class)
	public Long getCreatorId();

	@Field("description")
	public String getDescription();

	@Id
	public long getId();

	@Field("name")
	public String getName(Locale locale);

}