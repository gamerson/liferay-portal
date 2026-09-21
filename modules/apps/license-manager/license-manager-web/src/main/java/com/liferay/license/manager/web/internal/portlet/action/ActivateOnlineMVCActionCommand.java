/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.license.manager.web.internal.portlet.action;

import com.liferay.license.manager.web.internal.constants.LicenseManagerPortletKeys;
import com.liferay.portal.k8s.agent.licensing.LicenseActivationAgent;
import com.liferay.portal.kernel.module.service.Snapshot;
import com.liferay.portal.kernel.portlet.bridges.mvc.BaseMVCActionCommand;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCActionCommand;
import com.liferay.portal.kernel.servlet.SessionErrors;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.Validator;

import jakarta.portlet.ActionRequest;
import jakarta.portlet.ActionResponse;

import org.osgi.service.component.annotations.Component;

/**
 * Submits the Argo workflow that writes the activation code onto the secret
 * the <code>LiferayEnvironment</code> points at, then waits for the operator
 * to reach the provisioning server and activate.
 *
 * @author Gregory Amerson
 */
@Component(
	property = {
		"jakarta.portlet.name=" + LicenseManagerPortletKeys.LICENSE_MANAGER,
		"mvc.command.name=/license_manager/activate_online"
	},
	service = MVCActionCommand.class
)
public class ActivateOnlineMVCActionCommand extends BaseMVCActionCommand {

	@Override
	protected void doProcessAction(
			ActionRequest actionRequest, ActionResponse actionResponse)
		throws Exception {

		LicenseActivationAgent licenseActivationAgent =
			_licenseActivationAgentSnapshot.get();

		if (licenseActivationAgent == null) {
			SessionErrors.add(actionRequest, "agentUnavailable");

			return;
		}

		String activationCode = ParamUtil.getString(
			actionRequest, "activationCode");

		if (Validator.isNull(activationCode)) {
			SessionErrors.add(actionRequest, "noActivationCode");

			return;
		}

		LicenseActivationAgent.ActivationWorkflow activationWorkflow =
			licenseActivationAgent.startOnlineActivation(activationCode);

		if (activationWorkflow == null) {
			SessionErrors.add(actionRequest, "workflowNotSubmitted");

			return;
		}

		actionResponse.getRenderParameters(
		).setValue(
			"workflowName", activationWorkflow.name()
		);
	}

	private static final Snapshot<LicenseActivationAgent>
		_licenseActivationAgentSnapshot = new Snapshot<>(
			ActivateOnlineMVCActionCommand.class, LicenseActivationAgent.class);

}