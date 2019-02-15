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

package com.liferay.headless.form.dto.v1_0;

import com.liferay.petra.function.UnsafeSupplier;

import java.util.Date;

import javax.annotation.Generated;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Javier Gamarra
 * @generated
 */
@Generated("")
@XmlRootElement(name = "FormRecord")
public class FormRecord {

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

	public Boolean getDraft() {
		return _draft;
	}

	public FieldValues[] getFieldValues() {
		return _fieldValues;
	}

	public Form getForm() {
		return _form;
	}

	public Long getFormId() {
		return _formId;
	}

	public Long getId() {
		return _id;
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

	public void setDraft(Boolean draft) {
		_draft = draft;
	}

	public void setDraft(
		UnsafeSupplier<Boolean, Throwable> draftUnsafeSupplier) {

		try {
			_draft = draftUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setFieldValues(FieldValues[] fieldValues) {
		_fieldValues = fieldValues;
	}

	public void setFieldValues(
		UnsafeSupplier<FieldValues[], Throwable> fieldValuesUnsafeSupplier) {

		try {
			_fieldValues = fieldValuesUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setForm(Form form) {
		_form = form;
	}

	public void setForm(UnsafeSupplier<Form, Throwable> formUnsafeSupplier) {
		try {
			_form = formUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setFormId(Long formId) {
		_formId = formId;
	}

	public void setFormId(
		UnsafeSupplier<Long, Throwable> formIdUnsafeSupplier) {

		try {
			_formId = formIdUnsafeSupplier.get();
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

	private Creator _creator;
	private Date _dateCreated;
	private Date _dateModified;
	private Date _datePublished;
	private Boolean _draft;
	private FieldValues[] _fieldValues;
	private Form _form;
	private Long _formId;
	private Long _id;

}