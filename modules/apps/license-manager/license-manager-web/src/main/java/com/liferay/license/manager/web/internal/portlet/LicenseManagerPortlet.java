/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.license.manager.web.internal.portlet;

import com.liferay.license.manager.web.internal.constants.LicenseManagerPortletKeys;
import com.liferay.portal.k8s.agent.licensing.LicenseActivationAgent;
import com.liferay.portal.kernel.model.Release;
import com.liferay.portal.kernel.module.service.Snapshot;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCPortlet;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.Validator;

import jakarta.portlet.Portlet;
import jakarta.portlet.PortletException;
import jakarta.portlet.RenderRequest;
import jakarta.portlet.RenderResponse;

import java.io.IOException;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Peter Fellwock
 */
@Component(
	property = {
		"com.liferay.portlet.css-class-wrapper=license-manager",
		"com.liferay.portlet.display-category=category.hidden",
		"com.liferay.portlet.preferences-owned-by-group=true",
		"com.liferay.portlet.private-request-attributes=false",
		"com.liferay.portlet.private-session-attributes=false",
		"com.liferay.portlet.render-weight=50",
		"com.liferay.portlet.use-default-template=true",
		"jakarta.portlet.display-name=License Manager",
		"jakarta.portlet.expiration-cache=0",
		"jakarta.portlet.init-param.template-path=/META-INF/resources/",
		"jakarta.portlet.init-param.view-template=/view.jsp",
		"jakarta.portlet.name=" + LicenseManagerPortletKeys.LICENSE_MANAGER,
		"jakarta.portlet.resource-bundle=content.Language",
		"jakarta.portlet.security-role-ref=administrator",
		"jakarta.portlet.version=4.0"
	},
	service = Portlet.class
)
public class LicenseManagerPortlet extends MVCPortlet {

	/**
	 * Supplies the activation tab with what it renders. The agent is absent
	 * outside a cluster the DXP operator manages, and so is the tab, which
	 * leaves this portlet behaving exactly as it did before.
	 */
	@Override
	public void render(
			RenderRequest renderRequest, RenderResponse renderResponse)
		throws IOException, PortletException {

		LicenseActivationAgent licenseActivationAgent =
			_licenseActivationAgentSnapshot.get();

		if (licenseActivationAgent != null) {
			renderRequest.setAttribute(
				"liferayEnvironment",
				licenseActivationAgent.getLiferayEnvironment());

			if (ParamUtil.getBoolean(renderRequest, "revealRequest")) {
				renderRequest.setAttribute(
					"offlineActivationRequest",
					licenseActivationAgent.getOfflineActivationRequest());
			}

			String workflowName = ParamUtil.getString(
				renderRequest, "workflowName");

			if (Validator.isNotNull(workflowName)) {
				renderRequest.setAttribute(
					"activationWorkflow",
					licenseActivationAgent.getActivationWorkflow(workflowName));
			}
		}

		super.render(renderRequest, renderResponse);
	}

	private static final Snapshot<LicenseActivationAgent>
		_licenseActivationAgentSnapshot = new Snapshot<>(
			LicenseManagerPortlet.class, LicenseActivationAgent.class);

	@Reference(
		target = "(&(release.bundle.symbolic.name=com.liferay.license.manager.web)(&(release.schema.version>=1.0.0)(!(release.schema.version>=2.0.0))))"
	)
	private Release _release;

}