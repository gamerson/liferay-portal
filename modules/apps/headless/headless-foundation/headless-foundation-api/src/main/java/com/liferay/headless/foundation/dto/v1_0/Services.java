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
@XmlRootElement(name = "Services")
public class Services {

	public HoursAvailable[] getHoursAvailable() {
		return _hoursAvailable;
	}

	public Long getId() {
		return _id;
	}

	public String getServiceType() {
		return _serviceType;
	}

	public void setHoursAvailable(HoursAvailable[] hoursAvailable) {
		_hoursAvailable = hoursAvailable;
	}

	public void setHoursAvailable(
		UnsafeSupplier<HoursAvailable[], Throwable>
			hoursAvailableUnsafeSupplier) {

		try {
			_hoursAvailable = hoursAvailableUnsafeSupplier.get();
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

	public void setServiceType(String serviceType) {
		_serviceType = serviceType;
	}

	public void setServiceType(
		UnsafeSupplier<String, Throwable> serviceTypeUnsafeSupplier) {

		try {
			_serviceType = serviceTypeUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	private HoursAvailable[] _hoursAvailable;
	private Long _id;
	private String _serviceType;

}