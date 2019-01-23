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

package com.liferay.headless.blog.dto;

import com.liferay.apio.architect.annotation.Id;
import com.liferay.apio.architect.annotation.Vocabulary.Field;
import com.liferay.apio.architect.annotation.Vocabulary.Type;
import com.liferay.apio.architect.identifier.Identifier;

import java.util.Date;
import java.util.List;

/**
 * @author Alejandro Hernández
 * @author Víctor Galán
 * @generated
 */
@Type("BlogPosting")
public interface BlogPosting extends Identifier<Long> {

	@Field("alternativeHeadline")
	public String getAlternativeHeadline();

	@Field("articleBody")
	public String getArticleBody();

	@Field("caption")
	public String getCaption();

	@Field("contentSpace")
	public Long getContentSpaceId();

	@Field("dateCreated")
	public Date getCreatedDate();

	@Field("description")
	public String getDescription();

	@Field("encodingFormat")
	public String getEncodingFormat();

	@Field("friendlyUrlPath")
	public String getFriendlyURLPath();

	@Field("headline")
	public String getHeadline();

	@Id
	public Long getId();

	@Field("keywords")
	public List<String> getKeywords();

	@Field("dateModified")
	public Date getModifiedDate();

	@Field("datePublished")
	public Date getPublishedDate();

}