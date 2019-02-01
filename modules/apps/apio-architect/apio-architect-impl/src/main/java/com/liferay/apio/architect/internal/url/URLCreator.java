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

package com.liferay.apio.architect.internal.url;

import static java.lang.String.join;

import static java.util.Arrays.asList;

import com.liferay.apio.architect.internal.pagination.PageType;
import com.liferay.apio.architect.pagination.Page;
import com.liferay.apio.architect.resource.Resource;
import com.liferay.apio.architect.resource.Resource.GenericParent;
import com.liferay.apio.architect.resource.Resource.Id;
import com.liferay.apio.architect.resource.Resource.Item;
import com.liferay.apio.architect.resource.Resource.Nested;
import com.liferay.apio.architect.resource.Resource.Paged;
import com.liferay.apio.architect.uri.Path;

import java.net.URI;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.ws.rs.core.UriBuilder;

/**
 * Manages the creation of URLs and has all their necessary information.
 *
 * <p>
 * This class shouldn't be instantiated.
 * </p>
 *
 * @author Alejandro Hernández
 */
public final class URLCreator {

	/**
	 * Returns the absolute version of an application's relative URL.
	 *
	 * @param  applicationURL the application URL
	 * @param  relativeURL the relative URL
	 * @return the absolute URL
	 */
	public static String createAbsoluteURL(
		ApplicationURL applicationURL, String relativeURL) {

		return _buildURL(applicationURL.get(), relativeURL);
	}

	/**
	 * Returns the absolute version of a relative URL.
	 *
	 * @param  serverURL the server URL
	 * @param  relativeURL the relative URL
	 * @return the absolute URL
	 */
	public static String createAbsoluteURL(
		ServerURL serverURL, String relativeURL) {

		return _buildURL(serverURL.get(), relativeURL);
	}

	/**
	 * Returns the URL for a resource's action.
	 *
	 * @param  applicationURL the application URL
	 * @param  resource the action's resource
	 * @param  actionName the action's name
	 * @return the URL for the action
	 * @review
	 */
	public static Optional<String> createActionURL(
		ApplicationURL applicationURL, Resource resource, String actionName) {

		Optional<String> optional = createResourceURL(applicationURL, resource);

		return optional.map(
			url -> {
				if (_resourceActions.contains(actionName)) {
					return url;
				}

				return url + "/" + actionName;
			});
	}

	/**
	 * Returns the URL for a binary resource.
	 *
	 * @param  applicationURL the application URL
	 * @param  binaryId the binary resource's ID
	 * @param  path the resource's {@code com.liferay.apio.architect.uri.Path}
	 * @return the URL for a binary resource
	 */
	public static String createBinaryURL(
		ApplicationURL applicationURL, String binaryId, Path path) {

		URI uri = UriBuilder.fromPath(
			"{name}"
		).path(
			"{id}"
		).path(
			"{binaryId}"
		).build(
			path.getName(), path.getId(), binaryId
		);

		return createAbsoluteURL(applicationURL, uri.toString());
	}

	/**
	 * Returns the URL for a collection page.
	 *
	 * @param  collectionURL the collection URL
	 * @param  page the page
	 * @return the collection page URL
	 */
	public static String createCollectionPageURL(
		String collectionURL, Page page, PageType pageType) {

		return UriBuilder.fromUri(
			collectionURL
		).queryParam(
			"page", pageType.getPageNumber(page)
		).queryParam(
			"per_page", page.getItemsPerPage()
		).build(
		).toString();
	}

	/**
	 * Returns the URL for a generic parent resource, if an ID for the generic
	 * parent is present. Returns {@code Optional#empty()} otherwise.
	 *
	 * @param  applicationURL the application URL
	 * @param  genericParent the resource
	 * @return the resource's URL, if an ID for the generic parent is present;
	 *         {@code Optional#empty()} otherwise
	 * @review
	 */
	public static Optional<String> createGenericParentResourceURL(
		ApplicationURL applicationURL, GenericParent genericParent) {

		Optional<Id> optional = genericParent.getParentIdOptional();

		return optional.map(
			id -> UriBuilder.fromPath(
				"{name}"
			).path(
				"{parentName}"
			).path(
				"{parentId}"
			).build(
				genericParent.getName(), genericParent.getParentName(),
				id.asString()
			)
		).map(
			uri -> createAbsoluteURL(applicationURL, uri.toString())
		);
	}

	/**
	 * Returns the URL for an item resource, if the resource ID is present.
	 * Returns {@code Optional#empty()} otherwise.
	 *
	 * @param  applicationURL the application URL
	 * @param  item the resource
	 * @return the resource's URL, if the resource ID is present; {@code
	 *         Optional#empty()} otherwise
	 * @review
	 */
	public static Optional<String> createItemResourceURL(
		ApplicationURL applicationURL, Item item) {

		Optional<Id> optional = item.getIdOptional();

		return optional.map(
			id -> UriBuilder.fromPath(
				"{name}"
			).path(
				"{id}"
			).build(
				item.getName(), id.asString()
			)
		).map(
			uri -> createAbsoluteURL(applicationURL, uri.toString())
		);
	}

	/**
	 * Returns the URL for a nested resource, if an ID for the parent resource
	 * is present. Returns {@code Optional#empty()} otherwise.
	 *
	 * @param  applicationURL the application URL
	 * @param  nested the resource
	 * @return the resource's URL, if an ID for the parent resource is present;
	 *         {@code Optional#empty()} otherwise
	 * @review
	 */
	public static Optional<String> createNestedResourceURL(
		ApplicationURL applicationURL, Nested nested) {

		Item parent = nested.getParentItem();

		Optional<Id> optional = parent.getIdOptional();

		return optional.map(
			id -> UriBuilder.fromPath(
				"{parentName}"
			).path(
				"{id}"
			).path(
				"{name}"
			).build(
				parent.getName(), id.asString(), nested.getName()
			)
		).map(
			uri -> createAbsoluteURL(applicationURL, uri.toString())
		);
	}

	/**
	 * Returns the URL for a paged resource.
	 *
	 * @param  applicationURL the application URL
	 * @param  paged the paged resource
	 * @return the resource's URL
	 * @review
	 */
	public static String createPagedResourceURL(
		ApplicationURL applicationURL, Paged paged) {

		URI uri = UriBuilder.fromPath(
			"{name}"
		).build(
			paged.getName()
		);

		return createAbsoluteURL(applicationURL, uri.toString());
	}

	/**
	 * Returns the URL for a resource.
	 *
	 * @param  applicationURL the application URL
	 * @param  resource the resource
	 * @return the resource's URL
	 * @review
	 */
	public static Optional<String> createResourceURL(
		ApplicationURL applicationURL, Resource resource) {

		if (resource instanceof Paged) {
			Paged paged = (Paged)resource;

			return Optional.of(createPagedResourceURL(applicationURL, paged));
		}

		if (resource instanceof Nested) {
			Nested nested = (Nested)resource;

			return createNestedResourceURL(applicationURL, nested);
		}

		if (resource instanceof GenericParent) {
			GenericParent genericParent = (GenericParent)resource;

			return createGenericParentResourceURL(
				applicationURL, genericParent);
		}

		if (resource instanceof Item) {
			Item item = (Item)resource;

			return createItemResourceURL(applicationURL, item);
		}

		return Optional.empty();
	}

	/**
	 * Returns a {@link Path} from the URL if it's a valid URL for a resource
	 * with the provided name. Returns {@link Optional#empty()} otherwise.
	 *
	 * @param  url the resource URL to parse
	 * @param  name the resource's name
	 * @return a {@link Path} if the URL is valid; {@link Optional#empty()}
	 *         otherwise
	 * @review
	 */
	public static Optional<Path> getPath(String url, String name) {
		return Optional.of(
			url.lastIndexOf(name)
		).filter(
			index -> index != -1
		).map(
			url::substring
		).map(
			uri -> uri.split("/")
		).filter(
			components -> components.length == 2
		).map(
			components -> new Path(components[0], components[1])
		).filter(
			_isNotEmpty(Path::getName)
		).filter(
			_isNotEmpty(Path::getId)
		);
	}

	private static String _buildURL(String baseUrl, String relativeURL) {
		if ((relativeURL == null) || relativeURL.isEmpty()) {
			return null;
		}

		if (baseUrl.endsWith("/")) {
			baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
		}

		if (relativeURL.startsWith("/")) {
			relativeURL = relativeURL.substring(1);
		}

		return join("/", baseUrl, relativeURL);
	}

	private static Predicate<Path> _isNotEmpty(
		Function<Path, String> function) {

		return function.andThen(s -> (s != null) && !s.isEmpty())::apply;
	}

	private URLCreator() {
		throw new UnsupportedOperationException();
	}

	private static final List<String> _resourceActions = asList(
		"create", "remove", "replace", "retrieve");

}