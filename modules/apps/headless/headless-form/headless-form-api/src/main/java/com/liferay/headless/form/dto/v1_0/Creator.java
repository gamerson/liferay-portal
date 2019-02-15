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

import javax.annotation.Generated;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Javier Gamarra
 * @generated
 */
@Generated("")
@XmlRootElement(name = "Creator")
public class Creator {

	public String getAdditionalName() {
		return _additionalName;
	}

	public String getAlternateName() {
		return _alternateName;
	}

	public String getEmail() {
		return _email;
	}

	public String getFamilyName() {
		return _familyName;
	}

	public String getGivenName() {
		return _givenName;
	}

	public Long getId() {
		return _id;
	}

	public String getImage() {
		return _image;
	}

	public String getJobTitle() {
		return _jobTitle;
	}

	public String getName() {
		return _name;
	}

	public String getProfileURL() {
		return _profileURL;
	}

	public void setAdditionalName(String additionalName) {
		_additionalName = additionalName;
	}

	public void setAdditionalName(
		UnsafeSupplier<String, Throwable> additionalNameUnsafeSupplier) {

		try {
			_additionalName = additionalNameUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setAlternateName(String alternateName) {
		_alternateName = alternateName;
	}

	public void setAlternateName(
		UnsafeSupplier<String, Throwable> alternateNameUnsafeSupplier) {

		try {
			_alternateName = alternateNameUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setEmail(String email) {
		_email = email;
	}

	public void setEmail(
		UnsafeSupplier<String, Throwable> emailUnsafeSupplier) {

		try {
			_email = emailUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setFamilyName(String familyName) {
		_familyName = familyName;
	}

	public void setFamilyName(
		UnsafeSupplier<String, Throwable> familyNameUnsafeSupplier) {

		try {
			_familyName = familyNameUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setGivenName(String givenName) {
		_givenName = givenName;
	}

	public void setGivenName(
		UnsafeSupplier<String, Throwable> givenNameUnsafeSupplier) {

		try {
			_givenName = givenNameUnsafeSupplier.get();
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

	public void setImage(String image) {
		_image = image;
	}

	public void setImage(
		UnsafeSupplier<String, Throwable> imageUnsafeSupplier) {

		try {
			_image = imageUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setJobTitle(String jobTitle) {
		_jobTitle = jobTitle;
	}

	public void setJobTitle(
		UnsafeSupplier<String, Throwable> jobTitleUnsafeSupplier) {

		try {
			_jobTitle = jobTitleUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setName(String name) {
		_name = name;
	}

	public void setName(UnsafeSupplier<String, Throwable> nameUnsafeSupplier) {
		try {
			_name = nameUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setProfileURL(String profileURL) {
		_profileURL = profileURL;
	}

	public void setProfileURL(
		UnsafeSupplier<String, Throwable> profileURLUnsafeSupplier) {

		try {
			_profileURL = profileURLUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	private String _additionalName;
	private String _alternateName;
	private String _email;
	private String _familyName;
	private String _givenName;
	private Long _id;
	private String _image;
	private String _jobTitle;
	private String _name;
	private String _profileURL;

}