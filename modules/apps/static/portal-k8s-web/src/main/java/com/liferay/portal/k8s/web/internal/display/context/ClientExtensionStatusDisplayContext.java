/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.k8s.web.internal.display.context;

import com.liferay.petra.string.StringPool;
import com.liferay.portal.k8s.agent.status.ClientExtensionStatus;
import com.liferay.portal.k8s.agent.status.ClientExtensionStatusCondition;
import com.liferay.portal.k8s.agent.status.ClientExtensionStatusStore;
import com.liferay.portal.kernel.dao.search.SearchContainer;
import com.liferay.portal.kernel.portlet.LiferayPortletRequest;
import com.liferay.portal.kernel.portlet.LiferayPortletResponse;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Backs the client extension status view. The list is assembled in memory
 * because it is small by nature: one entry per client extension attached to
 * this portal.
 *
 * @author Gregory Amerson
 */
public class ClientExtensionStatusDisplayContext {

	public static final String ORDER_BY_COL_PHASE = "phase";

	public static final String ORDER_BY_COL_SERVICE_ID = "service-id";

	public static final String ORDER_BY_COL_VIRTUAL_INSTANCE_ID =
		"virtual-instance-id";

	public ClientExtensionStatusDisplayContext(
		ClientExtensionStatusStore clientExtensionStatusStore,
		LiferayPortletRequest liferayPortletRequest,
		LiferayPortletResponse liferayPortletResponse) {

		_clientExtensionStatusStore = clientExtensionStatusStore;
		_liferayPortletRequest = liferayPortletRequest;
		_liferayPortletResponse = liferayPortletResponse;
	}

	public ClientExtensionStatus getClientExtensionStatus(
		String serviceId, String virtualInstanceId) {

		for (ClientExtensionStatus clientExtensionStatus :
				_getClientExtensionStatuses()) {

			if (Objects.equals(
					clientExtensionStatus.getServiceId(), serviceId) &&
				Objects.equals(
					clientExtensionStatus.getVirtualInstanceId(),
					virtualInstanceId)) {

				return clientExtensionStatus;
			}
		}

		return null;
	}

	/**
	 * Returns the CSS label style for a condition, so a failed stage is
	 * recognizable without reading it.
	 */
	public String getConditionStyle(ClientExtensionStatusCondition condition) {
		if (condition == null) {
			return "secondary";
		}

		if (condition.isTrue()) {
			return "success";
		}

		if (condition.isFalse()) {
			return "danger";
		}

		return "warning";
	}

	public int getFailedCount() {
		int failedCount = 0;

		for (ClientExtensionStatus clientExtensionStatus :
				_getClientExtensionStatuses()) {

			if (clientExtensionStatus.isFailed()) {
				failedCount++;
			}
		}

		return failedCount;
	}

	public String getKeywords() {
		if (_keywords == null) {
			_keywords = ParamUtil.getString(_liferayPortletRequest, "keywords");
		}

		return _keywords;
	}

	public String getOrderByCol() {
		if (_orderByCol == null) {
			_orderByCol = ParamUtil.getString(
				_liferayPortletRequest, "orderByCol", ORDER_BY_COL_SERVICE_ID);
		}

		return _orderByCol;
	}

	public String getOrderByType() {
		if (_orderByType == null) {
			_orderByType = ParamUtil.getString(
				_liferayPortletRequest, "orderByType", "asc");
		}

		return _orderByType;
	}

	public String getPhaseStyle(String phase) {
		if (Objects.equals(phase, ClientExtensionStatus.PHASE_READY)) {
			return "success";
		}

		if (Objects.equals(phase, ClientExtensionStatus.PHASE_DEGRADED)) {
			return "danger";
		}

		if (Objects.equals(phase, ClientExtensionStatus.PHASE_PENDING)) {
			return "warning";
		}

		return "secondary";
	}

	public SearchContainer<ClientExtensionStatus> getSearchContainer() {
		if (_searchContainer != null) {
			return _searchContainer;
		}

		SearchContainer<ClientExtensionStatus> searchContainer =
			new SearchContainer<>(
				_liferayPortletRequest,
				_liferayPortletResponse.createRenderURL(), null,
				"no-client-extensions-were-found");

		searchContainer.setId("clientExtensionStatuses");
		searchContainer.setOrderByCol(getOrderByCol());
		searchContainer.setOrderByType(getOrderByType());

		List<ClientExtensionStatus> clientExtensionStatuses = _filter(
			_getClientExtensionStatuses());

		_sort(clientExtensionStatuses);

		searchContainer.setResultsAndTotal(
			() -> clientExtensionStatuses.subList(
				Math.min(searchContainer.getStart(),
					clientExtensionStatuses.size()),
				Math.min(
					searchContainer.getEnd(), clientExtensionStatuses.size())),
			clientExtensionStatuses.size());

		_searchContainer = searchContainer;

		return _searchContainer;
	}

	public int getTotalCount() {
		List<ClientExtensionStatus> clientExtensionStatuses =
			_getClientExtensionStatuses();

		return clientExtensionStatuses.size();
	}

	public boolean isAvailable() {
		if (_clientExtensionStatusStore == null) {
			return false;
		}

		return _clientExtensionStatusStore.isAvailable();
	}

	private List<ClientExtensionStatus> _filter(
		List<ClientExtensionStatus> clientExtensionStatuses) {

		String keywords = getKeywords();

		if (Validator.isNull(keywords)) {
			return new ArrayList<>(clientExtensionStatuses);
		}

		String lowerCaseKeywords = StringUtil.toLowerCase(keywords);

		List<ClientExtensionStatus> filtered = new ArrayList<>();

		for (ClientExtensionStatus clientExtensionStatus :
				clientExtensionStatuses) {

			if (_matches(
					clientExtensionStatus.getServiceId(), lowerCaseKeywords) ||
				_matches(
					clientExtensionStatus.getVirtualInstanceId(),
					lowerCaseKeywords) ||
				_matches(
					clientExtensionStatus.getNamespace(), lowerCaseKeywords) ||
				_matches(
					clientExtensionStatus.getProjectName(),
					lowerCaseKeywords)) {

				filtered.add(clientExtensionStatus);
			}
		}

		return filtered;
	}

	private List<ClientExtensionStatus> _getClientExtensionStatuses() {
		if (_clientExtensionStatuses != null) {
			return _clientExtensionStatuses;
		}

		if (!isAvailable()) {
			_clientExtensionStatuses = Collections.emptyList();

			return _clientExtensionStatuses;
		}

		_clientExtensionStatuses =
			_clientExtensionStatusStore.getClientExtensionStatuses();

		return _clientExtensionStatuses;
	}

	private boolean _matches(String value, String lowerCaseKeywords) {
		if (Validator.isNull(value)) {
			return false;
		}

		String lowerCaseValue = StringUtil.toLowerCase(value);

		return lowerCaseValue.contains(lowerCaseKeywords);
	}

	private void _sort(List<ClientExtensionStatus> clientExtensionStatuses) {
		Comparator<ClientExtensionStatus> comparator = Comparator.comparing(
			ClientExtensionStatus::getServiceId,
			Comparator.nullsFirst(String::compareToIgnoreCase));

		String orderByCol = getOrderByCol();

		if (Objects.equals(orderByCol, ORDER_BY_COL_VIRTUAL_INSTANCE_ID)) {
			comparator = Comparator.comparing(
				ClientExtensionStatus::getVirtualInstanceId,
				Comparator.nullsFirst(String::compareToIgnoreCase)
			).thenComparing(
				ClientExtensionStatus::getServiceId,
				Comparator.nullsFirst(String::compareToIgnoreCase)
			);
		}
		else if (Objects.equals(orderByCol, ORDER_BY_COL_PHASE)) {
			comparator = Comparator.comparing(
				ClientExtensionStatus::getPhase,
				Comparator.nullsFirst(String::compareToIgnoreCase)
			).thenComparing(
				ClientExtensionStatus::getServiceId,
				Comparator.nullsFirst(String::compareToIgnoreCase)
			);
		}

		if (Objects.equals(getOrderByType(), "desc")) {
			comparator = comparator.reversed();
		}

		clientExtensionStatuses.sort(comparator);
	}

	private List<ClientExtensionStatus> _clientExtensionStatuses;
	private final ClientExtensionStatusStore _clientExtensionStatusStore;
	private String _keywords;
	private final LiferayPortletRequest _liferayPortletRequest;
	private final LiferayPortletResponse _liferayPortletResponse;
	private String _orderByCol;
	private String _orderByType;
	private SearchContainer<ClientExtensionStatus> _searchContainer;

}
