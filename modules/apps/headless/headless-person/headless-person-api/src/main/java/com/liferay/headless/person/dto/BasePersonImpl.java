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

package com.liferay.headless.person.dto;

/**
 * @author Brian Wing Shun Chan
 * @generated
 */
public abstract class BasePersonImpl implements Person {

	@Override
	public String getFamilyName() {
		return _familyName;
	}

	@Override
	public long getId() {
		return _id;
	}

	public void setFamilyName(String familyName) {
		_familyName = familyName;
	}

	public void setId(long id) {
		_id = id;
	}

	private String _familyName;
	private long _id;

}