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
@XmlRootElement(name = "Location")
public class Location {

	public String getAddressCountry() {
		return _addressCountry;
	}

	public String getAddressRegion() {
		return _addressRegion;
	}

	public Long getId() {
		return _id;
	}

	public void setAddressCountry(String addressCountry) {
		_addressCountry = addressCountry;
	}

	public void setAddressCountry(
		UnsafeSupplier<String, Throwable> addressCountryUnsafeSupplier) {

		try {
			_addressCountry = addressCountryUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setAddressRegion(String addressRegion) {
		_addressRegion = addressRegion;
	}

	public void setAddressRegion(
		UnsafeSupplier<String, Throwable> addressRegionUnsafeSupplier) {

		try {
			_addressRegion = addressRegionUnsafeSupplier.get();
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

	private String _addressCountry;
	private String _addressRegion;
	private Long _id;

}