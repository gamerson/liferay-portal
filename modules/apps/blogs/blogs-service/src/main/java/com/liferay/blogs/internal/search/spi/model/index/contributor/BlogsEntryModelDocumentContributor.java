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

package com.liferay.blogs.internal.search.spi.model.index.contributor;

import com.liferay.blogs.model.BlogsEntry;
import com.liferay.portal.kernel.search.Document;
import com.liferay.portal.kernel.search.Field;
import com.liferay.portal.kernel.util.HtmlUtil;
import com.liferay.portal.search.spi.model.index.contributor.ModelDocumentContributor;

import org.osgi.service.component.annotations.Component;

/**
 * @author Luan Maoski
 */
@Component(
	immediate = true,
	property = "indexer.class.name=com.liferay.blogs.model.BlogsEntry",
	service = ModelDocumentContributor.class
)
public class BlogsEntryModelDocumentContributor
	implements ModelDocumentContributor<BlogsEntry> {

	@Override
	public void contribute(Document document, BlogsEntry blogsEntry) {
		document.addText(Field.CAPTION, blogsEntry.getCoverImageCaption());
		document.addText(
			Field.CONTENT, HtmlUtil.extractText(blogsEntry.getContent()));
		document.addText(Field.DESCRIPTION, blogsEntry.getDescription());
		document.addDate(Field.DISPLAY_DATE, blogsEntry.getDisplayDate());
		document.addDate(Field.MODIFIED_DATE, blogsEntry.getModifiedDate());
		document.addText(Field.SUBTITLE, blogsEntry.getSubtitle());
		document.addText(Field.TITLE, blogsEntry.getTitle());
	}

}