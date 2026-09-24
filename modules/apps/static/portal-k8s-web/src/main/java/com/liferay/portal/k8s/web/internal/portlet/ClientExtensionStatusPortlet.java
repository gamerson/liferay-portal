/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.k8s.web.internal.portlet;

import com.liferay.portal.k8s.agent.status.ClientExtensionStatusStore;
import com.liferay.portal.k8s.web.internal.constants.PortalK8sPortletKeys;
import com.liferay.portal.k8s.web.internal.constants.PortalK8sWebKeys;
import com.liferay.portal.k8s.web.internal.display.context.ClientExtensionStatusDisplayContext;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCPortlet;
import com.liferay.portal.kernel.util.Portal;

import jakarta.portlet.Portlet;
import jakarta.portlet.PortletException;
import jakarta.portlet.RenderRequest;
import jakarta.portlet.RenderResponse;

import java.io.IOException;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

/**
 * Shows every client extension attached to this portal together with both
 * halves of its deployment handshake.
 *
 * @author Gregory Amerson
 */
@Component(
	property = {
		"com.liferay.portlet.css-class-wrapper=portlet-client-extension-status",
		"com.liferay.portlet.display-category=category.hidden",
		"com.liferay.portlet.preferences-owned-by-group=false",
		"com.liferay.portlet.private-request-attributes=false",
		"com.liferay.portlet.private-session-attributes=false",
		"com.liferay.portlet.scopeable=true",
		"com.liferay.portlet.use-default-template=true",
		"jakarta.portlet.display-name=Client Extension Status",
		"jakarta.portlet.expiration-cache=0",
		"jakarta.portlet.init-param.template-path=/META-INF/resources/",
		"jakarta.portlet.init-param.view-template=/view.jsp",
		"jakarta.portlet.name=" + PortalK8sPortletKeys.CLIENT_EXTENSION_STATUS_ADMIN,
		"jakarta.portlet.resource-bundle=content.Language",
		"jakarta.portlet.security-role-ref=administrator",
		"jakarta.portlet.version=4.0"
	},
	service = Portlet.class
)
public class ClientExtensionStatusPortlet extends MVCPortlet {

	@Override
	public void render(
			RenderRequest renderRequest, RenderResponse renderResponse)
		throws IOException, PortletException {

		renderRequest.setAttribute(
			PortalK8sWebKeys.CLIENT_EXTENSION_STATUS_DISPLAY_CONTEXT,
			new ClientExtensionStatusDisplayContext(
				_clientExtensionStatusStore,
				_portal.getLiferayPortletRequest(renderRequest),
				_portal.getLiferayPortletResponse(renderResponse)));

		super.render(renderRequest, renderResponse);
	}

	@Reference(
		cardinality = ReferenceCardinality.OPTIONAL,
		policy = ReferencePolicy.DYNAMIC,
		policyOption = ReferencePolicyOption.GREEDY
	)
	private volatile ClientExtensionStatusStore _clientExtensionStatusStore;

	@Reference
	private Portal _portal;

}
