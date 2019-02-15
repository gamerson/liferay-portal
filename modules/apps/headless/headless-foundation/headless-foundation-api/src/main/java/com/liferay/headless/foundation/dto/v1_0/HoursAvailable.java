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
@XmlRootElement(name = "HoursAvailable")
public class HoursAvailable {

	public String getCloses() {
		return _closes;
	}

	public String getDayOfWeek() {
		return _dayOfWeek;
	}

	public Long getId() {
		return _id;
	}

	public String getOpens() {
		return _opens;
	}

	public void setCloses(String closes) {
		_closes = closes;
	}

	public void setCloses(
		UnsafeSupplier<String, Throwable> closesUnsafeSupplier) {

		try {
			_closes = closesUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setDayOfWeek(String dayOfWeek) {
		_dayOfWeek = dayOfWeek;
	}

	public void setDayOfWeek(
		UnsafeSupplier<String, Throwable> dayOfWeekUnsafeSupplier) {

		try {
			_dayOfWeek = dayOfWeekUnsafeSupplier.get();
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

	public void setOpens(String opens) {
		_opens = opens;
	}

	public void setOpens(
		UnsafeSupplier<String, Throwable> opensUnsafeSupplier) {

		try {
			_opens = opensUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	private String _closes;
	private String _dayOfWeek;
	private Long _id;
	private String _opens;

}