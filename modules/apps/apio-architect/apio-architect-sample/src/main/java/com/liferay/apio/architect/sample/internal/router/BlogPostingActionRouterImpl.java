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

import static com.liferay.apio.architect.sample.internal.auth.PermissionChecker.hasPermission;
import static com.liferay.apio.architect.sample.internal.converter.BlogPostingConverter.toBlogPosting;
import static com.liferay.apio.architect.sample.internal.converter.BlogSubscriptionConverter.toBlogSubscription;
import static com.liferay.apio.architect.sample.internal.converter.ReviewConverter.toReviewModels;

import com.liferay.apio.architect.credentials.Credentials;
import com.liferay.apio.architect.pagination.PageItems;
import com.liferay.apio.architect.pagination.Pagination;
import com.liferay.apio.architect.router.ActionRouter;
import com.liferay.apio.architect.sample.internal.converter.BlogPostingConverter;
import com.liferay.apio.architect.sample.internal.dao.BlogPostingModelService;
import com.liferay.apio.architect.sample.internal.dao.BlogSubscriptionModelService;
import com.liferay.apio.architect.sample.internal.dao.PersonModelService;
import com.liferay.apio.architect.sample.internal.dto.BlogPostingModel;
import com.liferay.apio.architect.sample.internal.dto.BlogSubscriptionModel;
import com.liferay.apio.architect.sample.internal.dto.PersonModel;
import com.liferay.apio.architect.sample.internal.type.BlogPosting;
import com.liferay.apio.architect.sample.internal.type.BlogSubscription;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotFoundException;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Alejandro Hernández
 */
@Component(
	immediate = true,
	service = {ActionRouter.class, BlogPostingActionRouterImpl.class}
)
public class BlogPostingActionRouterImpl implements BlogPostingActionRouter {

	@Override
	public boolean canCreate(Credentials credentials) {
		if (hasPermission(credentials)) {
			return true;
		}

		return false;
	}

	@Override
	public BlogPosting create(
		BlogPosting blogPosting, Credentials credentials) {

		if (!hasPermission(credentials)) {
			throw new ForbiddenException();
		}

		BlogPostingModel blogPostingModel = _blogPostingModelService.create(
			blogPosting.getArticleBody(), blogPosting.getCreatorId(),
			blogPosting.getAlternativeHeadline(), blogPosting.getHeadline(),
			toReviewModels(blogPosting.getReviews()));

		return toBlogPosting(blogPostingModel);
	}

	@Override
	public void remove(long id, Credentials credentials) {
		if (!hasPermission(credentials)) {
			throw new ForbiddenException();
		}

		_blogPostingModelService.remove(id);
	}

	@Override
	public BlogPosting replace(
		long id, BlogPosting blogPosting, Credentials credentials) {

		if (!hasPermission(credentials)) {
			throw new ForbiddenException();
		}

		Optional<BlogPostingModel> optional = _blogPostingModelService.update(
			id, blogPosting.getArticleBody(), blogPosting.getCreatorId(),
			blogPosting.getAlternativeHeadline(), blogPosting.getHeadline(),
			toReviewModels(blogPosting.getReviews()));

		return optional.map(
			BlogPostingConverter::toBlogPosting
		).orElseThrow(
			() -> new NotFoundException("Unable to get blog posting " + id)
		);
	}

	@Override
	public BlogPosting retrieve(long id) {
		Optional<BlogPostingModel> optional = _blogPostingModelService.get(id);

		return optional.map(
			BlogPostingConverter::toBlogPosting
		).orElseThrow(
			() -> new NotFoundException("Unable to get blog posting " + id)
		);
	}

	@Override
	public PageItems<BlogPosting> retrievePage(Pagination pagination) {
		List<BlogPostingModel> blogPostingModels =
			_blogPostingModelService.getPage(
				pagination.getStartPosition(), pagination.getEndPosition());
		int count = _blogPostingModelService.getCount();

		Stream<BlogPostingModel> stream = blogPostingModels.stream();

		List<BlogPosting> blogPostings = stream.map(
			BlogPostingConverter::toBlogPosting
		).collect(
			Collectors.toList()
		);

		return new PageItems<>(blogPostings, count);
	}

	@Override
	public BlogSubscription subscribe(
		long id, BlogSubscription blogSubscription) {

		Optional<PersonModel> personModelOptional = _personModelService.get(
			blogSubscription.getPersonId());

		PersonModel personModel = personModelOptional.orElseThrow(
			NotFoundException::new);

		Optional<BlogPostingModel> blogPostingModelOptional =
			_blogPostingModelService.get(id);

		BlogPostingModel blogPostingModel =
			blogPostingModelOptional.orElseThrow(NotFoundException::new);

		BlogSubscriptionModel blogSubscriptionModel =
			_blogSubscriptionModelService.create(blogPostingModel, personModel);

		return toBlogSubscription(blogSubscriptionModel);
	}

	@Reference
	private BlogPostingModelService _blogPostingModelService;

	@Reference
	private BlogSubscriptionModelService _blogSubscriptionModelService;

	@Reference
	private PersonModelService _personModelService;

}