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

package com.liferay.headless.collaboration.dto.v1_0;

import com.liferay.petra.function.UnsafeSupplier;

import java.util.Date;

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

	public Long[] getCategory() {
		return _category;
	}

	public Comment[] getComment() {
		return _comment;
	}

	public Long getContentSpace() {
		return _contentSpace;
	}

	public Creator getCreator() {
		return _creator;
	}

	public Date getDateCreated() {
		return _dateCreated;
	}

	public Date getDateModified() {
		return _dateModified;
	}

	public Date getDatePublished() {
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

	public Long getId() {
		return _id;
	}

	public ImageObject getImage() {
		return _image;
	}

	public Long getImageId() {
		return _imageId;
	}

	public String[] getKeywords() {
		return _keywords;
	}

	public ImageObjectRepository getRepository() {
		return _repository;
	}

	public Long getRepositoryId() {
		return _repositoryId;
	}

	public void setAggregateRating(AggregateRating aggregateRating) {
		_aggregateRating = aggregateRating;
	}

	public void setAggregateRating(
		UnsafeSupplier<AggregateRating, Throwable>
			aggregateRatingUnsafeSupplier) {

		try {
			_aggregateRating = aggregateRatingUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setAlternativeHeadline(String alternativeHeadline) {
		_alternativeHeadline = alternativeHeadline;
	}

	public void setAlternativeHeadline(
		UnsafeSupplier<String, Throwable> alternativeHeadlineUnsafeSupplier) {

		try {
			_alternativeHeadline = alternativeHeadlineUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setArticleBody(String articleBody) {
		_articleBody = articleBody;
	}

	public void setArticleBody(
		UnsafeSupplier<String, Throwable> articleBodyUnsafeSupplier) {

		try {
			_articleBody = articleBodyUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setCaption(String caption) {
		_caption = caption;
	}

	public void setCaption(
		UnsafeSupplier<String, Throwable> captionUnsafeSupplier) {

		try {
			_caption = captionUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setCategory(Long[] category) {
		_category = category;
	}

	public void setCategory(
		UnsafeSupplier<Long[], Throwable> categoryUnsafeSupplier) {

		try {
			_category = categoryUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setComment(Comment[] comment) {
		_comment = comment;
	}

	public void setComment(
		UnsafeSupplier<Comment[], Throwable> commentUnsafeSupplier) {

		try {
			_comment = commentUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setContentSpace(Long contentSpace) {
		_contentSpace = contentSpace;
	}

	public void setContentSpace(
		UnsafeSupplier<Long, Throwable> contentSpaceUnsafeSupplier) {

		try {
			_contentSpace = contentSpaceUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setCreator(Creator creator) {
		_creator = creator;
	}

	public void setCreator(
		UnsafeSupplier<Creator, Throwable> creatorUnsafeSupplier) {

		try {
			_creator = creatorUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setDateCreated(Date dateCreated) {
		_dateCreated = dateCreated;
	}

	public void setDateCreated(
		UnsafeSupplier<Date, Throwable> dateCreatedUnsafeSupplier) {

		try {
			_dateCreated = dateCreatedUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setDateModified(Date dateModified) {
		_dateModified = dateModified;
	}

	public void setDateModified(
		UnsafeSupplier<Date, Throwable> dateModifiedUnsafeSupplier) {

		try {
			_dateModified = dateModifiedUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setDatePublished(Date datePublished) {
		_datePublished = datePublished;
	}

	public void setDatePublished(
		UnsafeSupplier<Date, Throwable> datePublishedUnsafeSupplier) {

		try {
			_datePublished = datePublishedUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setDescription(String description) {
		_description = description;
	}

	public void setDescription(
		UnsafeSupplier<String, Throwable> descriptionUnsafeSupplier) {

		try {
			_description = descriptionUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setEncodingFormat(String encodingFormat) {
		_encodingFormat = encodingFormat;
	}

	public void setEncodingFormat(
		UnsafeSupplier<String, Throwable> encodingFormatUnsafeSupplier) {

		try {
			_encodingFormat = encodingFormatUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setFriendlyUrlPath(String friendlyUrlPath) {
		_friendlyUrlPath = friendlyUrlPath;
	}

	public void setFriendlyUrlPath(
		UnsafeSupplier<String, Throwable> friendlyUrlPathUnsafeSupplier) {

		try {
			_friendlyUrlPath = friendlyUrlPathUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setHeadline(String headline) {
		_headline = headline;
	}

	public void setHeadline(
		UnsafeSupplier<String, Throwable> headlineUnsafeSupplier) {

		try {
			_headline = headlineUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setId(Long id) {
		_id = id;
	}

	public void setId(UnsafeSupplier<Long, Throwable> idUnsafeSupplier) {
		try {
			_id = idUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setImage(ImageObject image) {
		_image = image;
	}

	public void setImage(
		UnsafeSupplier<ImageObject, Throwable> imageUnsafeSupplier) {

		try {
			_image = imageUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setImageId(Long imageId) {
		_imageId = imageId;
	}

	public void setImageId(
		UnsafeSupplier<Long, Throwable> imageIdUnsafeSupplier) {

		try {
			_imageId = imageIdUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setKeywords(String[] keywords) {
		_keywords = keywords;
	}

	public void setKeywords(
		UnsafeSupplier<String[], Throwable> keywordsUnsafeSupplier) {

		try {
			_keywords = keywordsUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setRepository(ImageObjectRepository repository) {
		_repository = repository;
	}

	public void setRepository(
		UnsafeSupplier<ImageObjectRepository, Throwable>
			repositoryUnsafeSupplier) {

		try {
			_repository = repositoryUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setRepositoryId(Long repositoryId) {
		_repositoryId = repositoryId;
	}

	public void setRepositoryId(
		UnsafeSupplier<Long, Throwable> repositoryIdUnsafeSupplier) {

		try {
			_repositoryId = repositoryIdUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	private AggregateRating _aggregateRating;
	private String _alternativeHeadline;
	private String _articleBody;
	private String _caption;
	private Long[] _category;
	private Comment[] _comment;
	private Long _contentSpace;
	private Creator _creator;
	private Date _dateCreated;
	private Date _dateModified;
	private Date _datePublished;
	private String _description;
	private String _encodingFormat;
	private String _friendlyUrlPath;
	private String _headline;
	private Long _id;
	private ImageObject _image;
	private Long _imageId;
	private String[] _keywords;
	private ImageObjectRepository _repository;
	private Long _repositoryId;

}