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

package com.liferay.portal.vulcan.util;

import com.liferay.portal.kernel.search.BooleanClauseOccur;
import com.liferay.portal.kernel.search.BooleanQuery;
import com.liferay.portal.kernel.search.Hits;
import com.liferay.portal.kernel.search.IndexSearcherHelperUtil;
import com.liferay.portal.kernel.search.Indexer;
import com.liferay.portal.kernel.search.Query;
import com.liferay.portal.kernel.search.QueryConfig;
import com.liferay.portal.kernel.search.SearchContext;
import com.liferay.portal.kernel.search.SearchException;
import com.liferay.portal.kernel.search.SearchResultPermissionFilter;
import com.liferay.portal.kernel.search.SearchResultPermissionFilterFactory;
import com.liferay.portal.kernel.search.SearchResultPermissionFilterSearcher;
import com.liferay.portal.kernel.search.Sort;
import com.liferay.portal.kernel.search.filter.BooleanFilter;
import com.liferay.portal.kernel.search.filter.Filter;
import com.liferay.portal.kernel.security.permission.PermissionChecker;
import com.liferay.portal.kernel.security.permission.PermissionThreadLocal;
import com.liferay.portal.vulcan.pagination.Pagination;

import java.util.function.Consumer;

/**
 * @author Brian Wing Shun Chan
 */
public class SearchUtil {

	public static Hits getHits(
			Filter filter, Indexer<?> indexer, Pagination pagination,
			Consumer<BooleanQuery> booleanQueryConsumer,
			Consumer<QueryConfig> queryConfigConsumer,
			Consumer<SearchContext> searchContextConsumer,
			SearchResultPermissionFilterFactory
				searchResultPermissionFilterFactory,
			Sort[] sorts)
		throws SearchException {

		PermissionChecker permissionChecker =
			PermissionThreadLocal.getPermissionChecker();

		SearchContext searchContext = _createSearchContext(
			pagination, permissionChecker, queryConfigConsumer,
			searchContextConsumer, sorts);

		Query query = _getQuery(
			filter, indexer, booleanQueryConsumer, searchContext);

		SearchResultPermissionFilter searchResultPermissionFilter =
			searchResultPermissionFilterFactory.create(
				new SearchResultPermissionFilterSearcher() {

					public Hits search(SearchContext searchContext)
						throws SearchException {

						return IndexSearcherHelperUtil.search(
							searchContext, query);
					}

				},
				permissionChecker);

		return searchResultPermissionFilter.search(searchContext);
	}

	private static SearchContext _createSearchContext(
		Pagination pagination, PermissionChecker permissionChecker,
		Consumer<QueryConfig> queryConfigConsumer,
		Consumer<SearchContext> searchContextConsumer, Sort[] sorts) {

		SearchContext searchContext = new SearchContext();

		searchContext.setEnd(pagination.getEndPosition());
		searchContext.setSorts(sorts);
		searchContext.setStart(pagination.getStartPosition());
		searchContext.setUserId(permissionChecker.getUserId());

		searchContextConsumer.accept(searchContext);

		QueryConfig queryConfig = searchContext.getQueryConfig();

		queryConfig.setHighlightEnabled(false);
		queryConfig.setScoreEnabled(false);

		queryConfigConsumer.accept(queryConfig);

		return searchContext;
	}

	private static Query _getQuery(
			Filter filter, Indexer<?> indexer,
			Consumer<BooleanQuery> booleanQueryConsumer,
			SearchContext searchContext)
		throws SearchException {

		BooleanQuery booleanQuery = indexer.getFullQuery(searchContext);

		if (filter != null) {
			BooleanFilter booleanFilter = booleanQuery.getPreBooleanFilter();

			booleanFilter.add(filter, BooleanClauseOccur.MUST);
		}

		booleanQueryConsumer.accept(booleanQuery);

		return booleanQuery;
	}

}