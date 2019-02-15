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

package com.liferay.headless.document.library.dto.v1_0;

import com.liferay.petra.function.UnsafeSupplier;

import java.util.Date;

import javax.annotation.Generated;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Javier Gamarra
 * @generated
 */
@Generated("")
@XmlRootElement(name = "Document")
public class Document {

	public AdaptedMedia[] getAdaptedMedia() {
		return _adaptedMedia;
	}

	public Long[] getCategory() {
		return _category;
	}

	public String getContentUrl() {
		return _contentUrl;
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

	public String getDescription() {
		return _description;
	}

	public String getEncodingFormat() {
		return _encodingFormat;
	}

	public String getFileExtension() {
		return _fileExtension;
	}

	public Long getFolderId() {
		return _folderId;
	}

	public Long getId() {
		return _id;
	}

	public String[] getKeywords() {
		return _keywords;
	}

	public Number getSizeInBytes() {
		return _sizeInBytes;
	}

	public String getTitle() {
		return _title;
	}

	public void setAdaptedMedia(AdaptedMedia[] adaptedMedia) {
		_adaptedMedia = adaptedMedia;
	}

	public void setAdaptedMedia(
		UnsafeSupplier<AdaptedMedia[], Throwable> adaptedMediaUnsafeSupplier) {

		try {
			_adaptedMedia = adaptedMediaUnsafeSupplier.get();
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

	public void setContentUrl(String contentUrl) {
		_contentUrl = contentUrl;
	}

	public void setContentUrl(
		UnsafeSupplier<String, Throwable> contentUrlUnsafeSupplier) {

		try {
			_contentUrl = contentUrlUnsafeSupplier.get();
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

	public void setFileExtension(String fileExtension) {
		_fileExtension = fileExtension;
	}

	public void setFileExtension(
		UnsafeSupplier<String, Throwable> fileExtensionUnsafeSupplier) {

		try {
			_fileExtension = fileExtensionUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setFolderId(Long folderId) {
		_folderId = folderId;
	}

	public void setFolderId(
		UnsafeSupplier<Long, Throwable> folderIdUnsafeSupplier) {

		try {
			_folderId = folderIdUnsafeSupplier.get();
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

	public void setSizeInBytes(Number sizeInBytes) {
		_sizeInBytes = sizeInBytes;
	}

	public void setSizeInBytes(
		UnsafeSupplier<Number, Throwable> sizeInBytesUnsafeSupplier) {

		try {
			_sizeInBytes = sizeInBytesUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setTitle(String title) {
		_title = title;
	}

	public void setTitle(
		UnsafeSupplier<String, Throwable> titleUnsafeSupplier) {

		try {
			_title = titleUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	private AdaptedMedia[] _adaptedMedia;
	private Long[] _category;
	private String _contentUrl;
	private Creator _creator;
	private Date _dateCreated;
	private Date _dateModified;
	private String _description;
	private String _encodingFormat;
	private String _fileExtension;
	private Long _folderId;
	private Long _id;
	private String[] _keywords;
	private Number _sizeInBytes;
	private String _title;

}