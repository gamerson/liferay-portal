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

package com.liferay.headless.foundation.dto.v1_0;

import com.liferay.petra.function.UnsafeSupplier;

import javax.annotation.Generated;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Javier Gamarra
 * @generated
 */
@Generated("")
@XmlRootElement(name = "Phone")
public class Phone {

	public String getExtension() {
		return _extension;
	}

	public Long getId() {
		return _id;
	}

	public String getPhoneNumber() {
		return _phoneNumber;
	}

	public String getPhoneType() {
		return _phoneType;
	}

	public void setExtension(String extension) {
		_extension = extension;
	}

	public void setExtension(
		UnsafeSupplier<String, Throwable> extensionUnsafeSupplier) {

		try {
			_extension = extensionUnsafeSupplier.get();
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

	public void setPhoneNumber(String phoneNumber) {
		_phoneNumber = phoneNumber;
	}

	public void setPhoneNumber(
		UnsafeSupplier<String, Throwable> phoneNumberUnsafeSupplier) {

		try {
			_phoneNumber = phoneNumberUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setPhoneType(String phoneType) {
		_phoneType = phoneType;
	}

	public void setPhoneType(
		UnsafeSupplier<String, Throwable> phoneTypeUnsafeSupplier) {

		try {
			_phoneType = phoneTypeUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	private String _extension;
	private Long _id;
	private String _phoneNumber;
	private String _phoneType;

}