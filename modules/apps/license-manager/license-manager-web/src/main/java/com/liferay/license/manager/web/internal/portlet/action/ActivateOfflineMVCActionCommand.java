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
import com.liferay.portal.kernel.upload.UploadPortletRequest;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.portal.kernel.util.Validator;

import jakarta.portlet.ActionRequest;
import jakarta.portlet.ActionResponse;

import java.io.InputStream;

import org.osgi.service.component.annotations.Component;

/**
 * Writes the uploaded bundle onto the marketplace volume the operator reads
 * from, then submits the Argo workflow that names it on the
 * <code>LiferayEnvironment</code> and waits for the operator to activate.
 *
 * @author Gregory Amerson
 */
@Component(
	property = {
		"jakarta.portlet.name=" + LicenseManagerPortletKeys.LICENSE_MANAGER,
		"mvc.command.name=/license_manager/activate_offline"
	},
	service = MVCActionCommand.class
)
public class ActivateOfflineMVCActionCommand extends BaseMVCActionCommand {

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

		UploadPortletRequest uploadPortletRequest =
			PortalUtil.getUploadPortletRequest(actionRequest);

		String fileName = uploadPortletRequest.getFileName("bundle");

		if (Validator.isNull(fileName)) {
			SessionErrors.add(actionRequest, "noBundleSelected");

			return;
		}

		try (InputStream inputStream = uploadPortletRequest.getFileAsStream(
				"bundle")) {

			if (inputStream == null) {
				SessionErrors.add(actionRequest, "noBundleSelected");

				return;
			}

			String bundleFileName =
				licenseActivationAgent.storeOfflineActivationBundle(
					fileName, inputStream);

			if (bundleFileName == null) {
				SessionErrors.add(actionRequest, "bundleNotStored");

				return;
			}

			LicenseActivationAgent.ActivationWorkflow activationWorkflow =
				licenseActivationAgent.startOfflineActivation(bundleFileName);

			if (activationWorkflow == null) {
				SessionErrors.add(actionRequest, "workflowNotSubmitted");

				return;
			}

			actionResponse.getRenderParameters(
			).setValue(
				"workflowName", activationWorkflow.name()
			);
		}
	}

	private static final Snapshot<LicenseActivationAgent>
		_licenseActivationAgentSnapshot = new Snapshot<>(
			ActivateOfflineMVCActionCommand.class,
			LicenseActivationAgent.class);

}