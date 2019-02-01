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

package com.liferay.headless.collaboration.dto;

import javax.annotation.Generated;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Javier Gamarra
 * @generated
 */
@Generated("")
@XmlRootElement(name = "BlogPosting")
public class BlogPosting {

	public AggregateRating getAggregateRating() {
		return _aggregateRating;
	}

	public String getAlternativeHeadline() {
		return _alternativeHeadline;
	}

	public String getArticleBody() {
		return _articleBody;
	}

	public String getCaption() {
		return _caption;
	}

	public Comment getComment() {
		return _comment;
	}

	public String getContentSpace() {
		return _contentSpace;
	}

	public String getCreator() {
		return _creator;
	}

	public String getDateCreated() {
		return _dateCreated;
	}

	public String getDateModified() {
		return _dateModified;
	}

	public String getDatePublished() {
		return _datePublished;
	}

	public String getDescription() {
		return _description;
	}

	public String getEncodingFormat() {
		return _encodingFormat;
	}

	public String getFriendlyUrlPath() {
		return _friendlyUrlPath;
	}

	public String getHeadline() {
		return _headline;
	}

	public Integer getId() {
		return _id;
	}

	public ImageObject getImage() {
		return _image;
	}

	public String[] getKeywords() {
		return _keywords;
	}

	public ImageObjectRepository getRepository() {
		return _repository;
	}

	public String getSelf() {
		return _self;
	}

	public void setAggregateRating(AggregateRating aggregateRating) {
		_aggregateRating = aggregateRating;
	}

	public void setAlternativeHeadline(String alternativeHeadline) {
		_alternativeHeadline = alternativeHeadline;
	}

	public void setArticleBody(String articleBody) {
		_articleBody = articleBody;
	}

	public void setCaption(String caption) {
		_caption = caption;
	}

	public void setComment(Comment comment) {
		_comment = comment;
	}

	public void setContentSpace(String contentSpace) {
		_contentSpace = contentSpace;
	}

	public void setCreator(String creator) {
		_creator = creator;
	}

	public void setDateCreated(String dateCreated) {
		_dateCreated = dateCreated;
	}

	public void setDateModified(String dateModified) {
		_dateModified = dateModified;
	}

	public void setDatePublished(String datePublished) {
		_datePublished = datePublished;
	}

	public void setDescription(String description) {
		_description = description;
	}

	public void setEncodingFormat(String encodingFormat) {
		_encodingFormat = encodingFormat;
	}

	public void setFriendlyUrlPath(String friendlyUrlPath) {
		_friendlyUrlPath = friendlyUrlPath;
	}

	public void setHeadline(String headline) {
		_headline = headline;
	}

	public void setId(Integer id) {
		_id = id;
	}

	public void setImage(ImageObject image) {
		_image = image;
	}

	public void setKeywords(String[] keywords) {
		_keywords = keywords;
	}

	public void setRepository(ImageObjectRepository repository) {
		_repository = repository;
	}

	public void setSelf(String self) {
		_self = self;
	}

	private AggregateRating _aggregateRating;
	private String _alternativeHeadline;
	private String _articleBody;
	private String _caption;
	private Comment _comment;
	private String _contentSpace;
	private String _creator;
	private String _dateCreated;
	private String _dateModified;
	private String _datePublished;
	private String _description;
	private String _encodingFormat;
	private String _friendlyUrlPath;
	private String _headline;
	private Integer _id;
	private ImageObject _image;
	private String[] _keywords;
	private ImageObjectRepository _repository;
	private String _self;

}