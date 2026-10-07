/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.environment.activation.poc.internal.osgi.commands;

import com.liferay.osgi.util.osgi.commands.OSGiCommands;
import com.liferay.portal.environment.activation.poc.internal.activator.LiferayEnvironmentActivator;
import com.liferay.portal.kernel.json.JSONFactory;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Gregory Amerson
 */
@Component(
	property = {
		"osgi.command.function=activate", "osgi.command.function=heartbeat",
		"osgi.command.function=status", "osgi.command.scope=liferayEnvironment"
	},
	service = OSGiCommands.class
)
public class LiferayEnvironmentOSGiCommands implements OSGiCommands {

	public String activate(String activationCode) throws Exception {
		return _liferayEnvironmentActivator.activate(activationCode);
	}

	public String heartbeat() throws Exception {
		return _liferayEnvironmentActivator.heartbeat();
	}

	public String status() throws Exception {
		return _liferayEnvironmentActivator.status();
	}

	@Activate
	protected void activate() {
		_liferayEnvironmentActivator = new LiferayEnvironmentActivator(
			_jsonFactory);
	}

	@Reference
	private JSONFactory _jsonFactory;

	private LiferayEnvironmentActivator _liferayEnvironmentActivator;

}