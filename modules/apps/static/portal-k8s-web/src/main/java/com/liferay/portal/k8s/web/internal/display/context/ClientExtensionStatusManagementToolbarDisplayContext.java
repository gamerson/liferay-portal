/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.k8s.web.internal.display.context;

import com.liferay.frontend.taglib.clay.servlet.taglib.display.context.SearchContainerManagementToolbarDisplayContext;
import com.liferay.frontend.taglib.clay.servlet.taglib.util.DropdownItem;
import com.liferay.frontend.taglib.clay.servlet.taglib.util.DropdownItemListBuilder;
import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.portlet.LiferayPortletRequest;
import com.liferay.portal.kernel.portlet.LiferayPortletResponse;

import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Objects;

/**
 * Supplies the search box and the sort controls over the client extension
 * list, so an administrator can narrow to one service or one virtual instance.
 *
 * @author Gregory Amerson
 */
public class ClientExtensionStatusManagementToolbarDisplayContext
	extends SearchContainerManagementToolbarDisplayContext {

	public ClientExtensionStatusManagementToolbarDisplayContext(
		ClientExtensionStatusDisplayContext clientExtensionStatusDisplayContext,
		HttpServletRequest httpServletRequest,
		LiferayPortletRequest liferayPortletRequest,
		LiferayPortletResponse liferayPortletResponse) {

		super(
			httpServletRequest, liferayPortletRequest, liferayPortletResponse,
			clientExtensionStatusDisplayContext.getSearchContainer());
	}

	@Override
	public List<DropdownItem> getOrderByDropdownItems() {
		return DropdownItemListBuilder.add(
			dropdownItem -> {
				dropdownItem.setActive(
					Objects.equals(
						getOrderByCol(),
						ClientExtensionStatusDisplayContext.
							ORDER_BY_COL_SERVICE_ID));
				dropdownItem.setHref(
					getPortletURL(), "orderByCol",
					ClientExtensionStatusDisplayContext.
						ORDER_BY_COL_SERVICE_ID);
				dropdownItem.setLabel(
					LanguageUtil.get(httpServletRequest, "service-id"));
			}
		).add(
			dropdownItem -> {
				dropdownItem.setActive(
					Objects.equals(
						getOrderByCol(),
						ClientExtensionStatusDisplayContext.
							ORDER_BY_COL_VIRTUAL_INSTANCE_ID));
				dropdownItem.setHref(
					getPortletURL(), "orderByCol",
					ClientExtensionStatusDisplayContext.
						ORDER_BY_COL_VIRTUAL_INSTANCE_ID);
				dropdownItem.setLabel(
					LanguageUtil.get(httpServletRequest, "virtual-instance"));
			}
		).add(
			dropdownItem -> {
				dropdownItem.setActive(
					Objects.equals(
						getOrderByCol(),
						ClientExtensionStatusDisplayContext.
							ORDER_BY_COL_PHASE));
				dropdownItem.setHref(
					getPortletURL(), "orderByCol",
					ClientExtensionStatusDisplayContext.ORDER_BY_COL_PHASE);
				dropdownItem.setLabel(
					LanguageUtil.get(httpServletRequest, "status"));
			}
		).build();
	}

	@Override
	public String getSearchActionURL() {
		return String.valueOf(getPortletURL());
	}

	@Override
	public Boolean isShowSearch() {
		return true;
	}

	@Override
	protected String[] getOrderByKeys() {
		return new String[] {
			ClientExtensionStatusDisplayContext.ORDER_BY_COL_PHASE,
			ClientExtensionStatusDisplayContext.ORDER_BY_COL_SERVICE_ID,
			ClientExtensionStatusDisplayContext.
				ORDER_BY_COL_VIRTUAL_INSTANCE_ID
		};
	}

}
