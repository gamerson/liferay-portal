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

package com.liferay.headless.workflow.dto.v1_0;

import com.liferay.petra.function.UnsafeSupplier;

import javax.annotation.Generated;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Javier Gamarra
 * @generated
 */
@Generated("")
@XmlRootElement(name = "ObjectReviewed")
public class ObjectReviewed {

	public Long getId() {
		return _id;
	}

	public String getIdentifier() {
		return _identifier;
	}

	public String getResourceType() {
		return _resourceType;
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

	public void setIdentifier(String identifier) {
		_identifier = identifier;
	}

	public void setIdentifier(
		UnsafeSupplier<String, Throwable> identifierUnsafeSupplier) {

		try {
			_identifier = identifierUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setResourceType(String resourceType) {
		_resourceType = resourceType;
	}

	public void setResourceType(
		UnsafeSupplier<String, Throwable> resourceTypeUnsafeSupplier) {

		try {
			_resourceType = resourceTypeUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	private Long _id;
	private String _identifier;
	private String _resourceType;

}