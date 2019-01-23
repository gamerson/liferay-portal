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

package com.liferay.headless.content.space.internal.resource;

import com.liferay.apio.architect.pagination.PageItems;
import com.liferay.apio.architect.pagination.Pagination;
import com.liferay.apio.architect.router.ActionRouter;
import com.liferay.headless.content.space.dto.ContentSpace;
import com.liferay.headless.content.space.resource.ContentSpaceResource;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.model.Company;

import java.util.Collections;

import org.osgi.service.component.annotations.Component;

/**
 * @author Javier Gamarra
 * @author Cristina González
 */
@Component(immediate = true, service = ActionRouter.class)
public class ContentSpaceResourceImpl implements ContentSpaceResource {

	@Override
	public ContentSpace getContentSpace(long contentSpaceId)
		throws PortalException {

		return null;
	}

	@Override
	public PageItems<ContentSpace> getPageItems(
		Pagination pagination, Company company) {

		return new PageItems<>(Collections.emptyList(), 0);
	}

}