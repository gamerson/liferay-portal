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

package com.liferay.apio.architect.sample.internal.router;

import com.liferay.apio.architect.annotation.Actions.Create;
import com.liferay.apio.architect.annotation.Actions.Remove;
import com.liferay.apio.architect.annotation.Actions.Replace;
import com.liferay.apio.architect.annotation.Actions.Retrieve;
import com.liferay.apio.architect.annotation.Body;
import com.liferay.apio.architect.annotation.EntryPoint;
import com.liferay.apio.architect.annotation.Id;
import com.liferay.apio.architect.annotation.Permissions.CanCreate;
import com.liferay.apio.architect.credentials.Credentials;
import com.liferay.apio.architect.pagination.PageItems;
import com.liferay.apio.architect.pagination.Pagination;
import com.liferay.apio.architect.router.ActionRouter;
import com.liferay.apio.architect.sample.internal.action.Subscribe;
import com.liferay.apio.architect.sample.internal.type.BlogPosting;
import com.liferay.apio.architect.sample.internal.type.BlogSubscription;

/**
 * Provides all the information necessary to expose <a
 * href="http://schema.org/BlogPosting">BlogPosting</a> resources through a web
 * API. The resources are mapped from the internal {@link BlogPostingModel}
 * model.
 *
 * @author Alejandro Hernández
 */
public interface BlogPostingActionRouter extends ActionRouter<BlogPosting> {

	@CanCreate
	public boolean canCreate(Credentials credentials);

	@Create
	public BlogPosting create(
		@Body BlogPosting blogPosting, Credentials credentials);

	@Remove
	public void remove(@Id long id, Credentials credentials);

	@Replace
	public BlogPosting replace(
		@Id long id, @Body BlogPosting blogPosting, Credentials credentials);

	@Retrieve
	public BlogPosting retrieve(@Id long id);

	@EntryPoint
	@Retrieve
	public PageItems<BlogPosting> retrievePage(Pagination pagination);

	@Subscribe
	public BlogSubscription subscribe(
		@Id long id, @Body BlogSubscription blogSubscription);

}